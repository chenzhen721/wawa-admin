package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.crud.MessageController
import com.ttpod.star.common.util.KeyUtils
import com.ttpod.star.model.OpType
import com.ttpod.star.model.PrettyNumSaleStatus
import com.ttpod.star.model.UserType
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.$set
import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.util.MsgDigestUtil.MD5
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * 靓号销售管理
 */
//@Rest
@RestWithSession
class PrettyNumController extends BaseController {


    def DBCollection prettys(){return mainMongo.getCollection("pretty4sale");}

    static final Logger logger = LoggerFactory.getLogger(PrettyNumController.class)

    @Resource
    MessageController messageController

    def list(HttpServletRequest req) {
        def query = QueryBuilder.start();
        if (req[_id]) {
            query.and("_id").is(req.getInt("_id"))
        }
        intQuery(query, req, "length") //靓号长度
        intQuery(query, req, "price")//价格
        intQuery(query, req, "sale")//销售状态 1:可售, 2:不可售,3:已售
        booleanQuery(query, req, "show")//前台展示

        Crud.list(req, prettys(), query.get(), ALL_FIELD, $$('price' : -1)) { List<BasicDBObject> data ->
            def users = users()
            for (BasicDBObject obj : data) {
                if(obj['user_id'])
                    obj.put("nick_name", users.findOne(obj['user_id'], new BasicDBObject("nick_name", 1))?.get("nick_name"))
            }
        }
    }

    def edit(HttpServletRequest req) {
        def update = new HashMap()
        Integer id = req.getInt(_id)
        //销售状态 1:可售, 2:不可售,3:已售
        String sale = req.getParameter("sale")
        if (StringUtils.isNotBlank(sale)) {
            update.put("sale", sale.toInteger())
        }
        String price = req.getParameter("price")
        if (StringUtils.isNotBlank(price)) {
            update.put("price", price as Integer)
        }
        String show = req.getParameter("show")
        if (StringUtils.isNotBlank(show)) {
            update.put("show", show.equals("1"))
        }
        String show_price = req.getParameter("show_price")
        if (StringUtils.isNotBlank(show)) {
            update.put("show_price", show_price as Integer)
        }
        if (update.size() > 0) {
            if (1 == prettys().update(new BasicDBObject(_id, id), new BasicDBObject($set, update), false, false, writeConcern).getN()){
                Crud.opLog(OpType.prettynum_edit, update)
                return [code: 1]
            }

        }
        return [code: 0]
    }

    private final static String PRIV_KEY = "meme#*&07071zhibo";
    /**
     * 靓号赠送
     * @param req
     */
    def present(HttpServletRequest req){
        Integer number = req.getInt(_id) //靓号
        Integer userId = req.getInt('uid') //用户ID
        def user = users().findOne($$(_id:userId), $$(tuid:1,'mm_no':1,'pretty':1,priv:1))
        if(user == null)
            return [code:0]
        Boolean hasPretty = (user['pretty'] ?: Boolean.FALSE) as Boolean
        /*if(hasPretty)
            return [code:30490] //用户已经拥有靓号*/
        //当前靓号状态是否可售
        def pretty = prettys().findOne($$(_id: number), $$(sale:1,price:1))
        if(pretty == null)
            return Web.missParam()
        def sale = pretty['sale'] as Integer
        if(sale != PrettyNumSaleStatus.可售.ordinal())
            return [code:30490+sale] //30492 不可售 30493 已售出

        if(1 == prettys().update($$(_id:number,sale:PrettyNumSaleStatus.可售.ordinal()),
                $$($set: [sale:PrettyNumSaleStatus.已售.ordinal(), user_id:userId, timestamp:System.currentTimeMillis()])
                , false, false, writeConcern).getN()) {
            //设置靓号
            if(users().update($$(_id:userId),$$($set :['mm_no':number,pretty:Boolean.TRUE]).append($addToSet , $$("mm_nos", number as Integer)),
                    false, false ,writeConcern).getN() == 1){
                //同步到用户库
                String sign = MD5.digest2HEX(PRIV_KEY + user['tuid'] + number);
                Map result = Web.userApi("info/synNo?tuid="+user['tuid']+"&mm_no="+number+"&sign="+sign)
                if (result == null)
                    return [code: 0]
                if (((Number) result.get("code")).intValue() == 1) {
                    //记录拥有靓号用户 检测是否90天没有登录
                    def log_id = "${userId}_${number}".toString()
                    logMongo.getCollection('pretty_users').save($$(_id:log_id,userId:userId, num:number,coin:0,his_sale:sale,timestamp:System.currentTimeMillis()))
                    //主播则更新房间靓号
                    int priv = user['priv'] as Integer
                    if(priv == UserType.主播.ordinal()){
                        rooms().update($$(xy_star_id:userId), $$($set:$$('room_ids':number.toString())))
                    }
                    //更新操作日志
                    Crud.opLog(OpType.prettynum_present, [_id:userId, num:number])
                    //更新用户信息
                    String token = userRedis.opsForValue().get(KeyUtils.USER.token(userId))
                    //userRedis.opsForHash().put(KeyUtils.accessToken(token),"mm_no", number.toString())
                    Web.putUserInfoToSession(KeyUtils.accessToken(token),"mm_no", number.toString())
                    return [code: 1]
                }
            }
        }
        return [code : 0]
    }

    /**
     * 解除靓号绑定
     * @param req
     * @return
     */
    def unbind(HttpServletRequest req){
        Integer number = req.getInt(_id) //靓号
        def pretty_users = logMongo.getCollection('pretty_users')
        def pretty = prettys().findOne($$(_id:number))
        if(pretty == null)
            return [code : 0]
        Integer uid = pretty['user_id'] as Integer

        logger.debug("unbind:{}",uid)
        //删除用户靓号库中靓号
        users().update($$(_id:uid),$$($pull, $$("mm_nos", number)),false,false, writeConcern)

        logger.debug("unbind:number{}",number)
        //如果用户设置默认靓号恢复为ID
        if(users().update($$([_id:uid]),
                $$('$set':$$(mm_no : uid, pretty : Boolean.FALSE)),false,false, writeConcern).getN() == 1){

            //直播间靓号清除
            rooms().update($$(xy_star_id:uid),$$($set:$$('room_ids':uid.toString())))

            //同步到用户库
            def user = users().findOne($$(_id:uid), $$(tuid:1,'mm_no':1,'pretty':1,priv:1))
            def sign = MD5.digest2HEX("${PRIV_KEY}${user['tuid'].toString()}${uid}".toString())
            Map result = Web.userApi("info/synNo?tuid="+user['tuid']+"&mm_no="+uid+"&sign="+sign)

            String id2token = KeyUtils.USER.token(uid)
            //更新redis
            //userRedis.opsForHash().put(KeyUtils.accessToken(userRedis.opsForValue().get(id2token)),"mm_no",uid.toString())
            Web.putUserInfoToSession(KeyUtils.accessToken(userRedis.opsForValue().get(id2token)),"mm_no",uid.toString())

        }

        def his_pretty = pretty_users.findOne($$(num:number))
        def his_sale = PrettyNumSaleStatus.不可售.ordinal()
        if(his_pretty){
            def log_id = his_pretty['_id'] as String
            his_sale = his_pretty['his_sale'] as Integer //历史销售状态
            //删除靓号用户库
            pretty_users.remove($$(_id:log_id))
        }
        //靓号恢复初始化状态
        prettys().update($$(_id:number, user_id:uid),$$($set: [sale:his_sale, user_id:0]), false, false, writeConcern)
        //更新操作日志
        Crud.opLog(OpType.prettynum_unbind, [uid : uid, num: number])
        return [code: 1]
    }
}
