package com.wawa.admin.web.ext

import com.mongodb.BasicDBList
import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.wawa.base.anno.Rest
import com.wawa.common.util.MsgDigestUtil
import com.wawa.base.Crud
import com.wawa.base.BaseController
import com.wawa.admin.web.QdController
import com.wawa.api.Web
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import java.text.SimpleDateFormat

import static com.wawa.common.doc.MongoKey.*
import static com.wawa.common.util.WebUtils.$$

/**
 * date: 13-4-22 10:40
 * @author: yangyang.cong@ttpod.com
 */
@Rest
class UnionController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(UnionController.class)
    @Resource
    QdController qdController

    DBCollection table() { adminMongo.getCollection('stat_channels') }
    private static final SimpleDateFormat sdf = new SimpleDateFormat('yyyyMMdd')
    private static final Date date = sdf.parse('20150403')

    private static final Map<String, String> CPA_PERM_API = ['/union/login_rate':'1']
    private static final Map<String, String> CPS_PERM_API = ['/union/login_rate':'1','/union/reg_list':'1','/union/pay_list':'1','/union/reg_pay_list':'1' , '/union/login_total':'1' ]
    private static final Map<String, Map<String, String>> MENUS_LIST = ['1' : CPA_PERM_API, '2' : CPS_PERM_API]

    def login(HttpServletRequest request) {
        String input = request[auth_code]
        if (codeVerifError(request, input)) {
            return [code: 30419, msg: '验证码错误']
        }

        String name = request["name"]
        String password = MsgDigestUtil.SHA.digest2HEX(request["password"].toString())
        logger.info("password: {}", password)
        def user = adminMongo.getCollection("channel_users").findOne(
                new BasicDBObject(_id: name, password: password), new BasicDBObject('password', 0)) as Map
        if (null == user) {
            return [code: 0, msg: '密码错误']
        }

        if (!user['qd']) {
            return [code: 0, msg: '权限不足']
        }

        def qd = adminMongo.getCollection("channels").findOne(user['qd'], new BasicDBObject('comment', 0))
        if (!qd) {
            return [code: 0, msg: '权限不足']
        }
        //是否顶级渠道用户
        user.put('parent', (qd['parent_qd'] == null))
        def qds = [user['qd'] as String] as List
        //添加子渠道信息
        def query = new BasicDBObject("parent_qd": user['qd'])
        adminMongo.getCollection("channels").find(query, new BasicDBObject('comment', 0))
                .toArray().each { DBObject obj ->
            qds.add(obj.get('_id') as String)
        }
        user.put("qdinfo", qd)
        user.put("qdall", qds)
        user.put("permission", user['permission'] ?: false)
        user.put("timestamp", System.currentTimeMillis())//加时间戳
        request.getSession().setAttribute("union_user", user)
        //赋予CPA CPS不同菜单访问权限  1:CPA 2:CPS
        String type = qd?.get('type')
        request.getSession().setAttribute("allow_menus",MENUS_LIST[type])
        return [code: 1, data: user]
    }

    def modif_pwd(HttpServletRequest req) {
        Map user = req.getSession().getAttribute("union_user") as Map
        if (null == user) {
            return [code: 0]
        }
        String pwd = MsgDigestUtil.SHA.digest2HEX(req['password'].toString())
        adminMongo.getCollection('channel_users')
                .update(new BasicDBObject(_id, user.get(_id)), new BasicDBObject('$set', [password: pwd]))
        [code: 1]
    }

    def show(HttpServletRequest req) {
        def user = req.getSession().getAttribute("union_user")
        [code: user ? 1 : 0, data: user]
    }


    def doInQd(HttpServletRequest req, Closure closure) {
        def user = req.getSession().getAttribute('union_user')
        if (null == user) {
            return [code: 0, msg: "请登录"]
        }

        String uri = req.getRequestURI();
        String menu = uri.substring(0,uri.indexOf('.'))
        def allow_menus = req.getSession().getAttribute("allow_menus") as Map
        logger.debug("uri : {} menu:{}, allow_menus:{}", req.getRequestURI(),menu,allow_menus)
        if(!allow_menus.containsKey(menu)){
            return Web.notAllowed();
        }
        def timestamp = user['timestamp'] as Long
        //若session中的信息保存超过一天,强制更新session
        def channel = null
        if (timestamp == null || (System.currentTimeMillis() - timestamp > DAY_MILLON)) {
            user = adminMongo.getCollection("channel_users").findOne(
                    new BasicDBObject(_id: user['name']), new BasicDBObject('password', 0))
            logger.debug("doInQd user : {}", user)
            if (null == user) {
                return [code: 0, msg: '用户失效，请退出后重新登录']
            }
            if (!user['qd']) {
                return [code: 0, msg: '权限不足']
            }
            channel = adminMongo.getCollection("channels").findOne(user['qd'], new BasicDBObject('comment', 0))
            if (!channel) {
                logger.debug("channel: {}", channel)
                return [code: 0, msg: '权限不足']
            }
            def qds = [user['qd']] as List
            //添加子渠道信息
            def query = new BasicDBObject("parent_qd": user['qd'])
            adminMongo.getCollection("channels").find(query, new BasicDBObject('comment', 0))
                    .toArray().each { DBObject obj ->
                qds.add(obj['_id'])
            }
            user.put("qdinfo", channel)
            user.put("qdall", qds)
            user.put("timestamp", System.currentTimeMillis())//加时间戳
            req.getSession().setAttribute("union_user", user)
            //赋予CPA CPS不同菜单访问权限  1:CPA 2:CPS
            String type = channel?.get('type')
            req.getSession().setAttribute("allow_menus",MENUS_LIST[type])
        }
        def childQd = user['qdall'] as List
        def qdId = user['qd']
        closure.call(qdId, childQd, user)
    }

    //隐藏收益渠道ID 列表
    static final List<String> HideCnyQdList = ['nduo', 'tuli','renzhong','zhaocai_h5']
    /**
     * 注册收益表
     */
    def reg_pay_list(HttpServletRequest req) {
        doInQd(req) {String qid, List<String> ids, Map user ->
            def _id = req.getParameter('_id') as String
            def query = Web.fillTimeBetween(req)
            def desc = $$(timestamp, -1)
            if (StringUtils.isBlank(_id)) {
                //默认的查询方式，父渠道子渠道同时查询
                query.and('qd').in(ids)
                desc.append('reg', -1)
            } else {
                if (ids.contains(_id)) {
                    query.and('qd').is(_id)
                } else {
                    return [code: 1, data: [], count: 0, all_page: 0]
                }
            }
            //查询特殊处理(mengjing，mengjing_mp)
//            if ('mengjing'.equals(qid) || 'mengjing_mp'.equals(qid)) {
//                query.and("timestamp").lessThan(date.clearTime().getTime());
//            }
            def map = qdController.reg_pay_list_service(query.get(), desc, req) as Map
            //需要特殊处理的渠道(nduo, tuli)
            if(HideCnyQdList.contains(qid)){
            //if ('nduo'.equals(qid)) {
                map.get("data")?.each { BasicDBObject obj ->
                    def cny = obj.get('cny') as Double
                    if (cny >= 1000) {
                        obj.put('cny', 0)
                        obj.put('pay', 0)
                    }
                }
            }
            return map
        }
    }

    /**
     * 注册详情
     * @param req
     * @return
     */
    def reg_list(HttpServletRequest req) {
        doInQd(req) { String qid, List<String> ids, Map user ->
            def query = Web.fillTimeBetween(req)
            //默认查询其下所有子渠道的注册情况
            def _id = req.getParameter('_id') as String
            if (StringUtils.isNotBlank(_id)) {
                if (!ids.contains(_id)) {
                    return [code: 0]
                }
                query.and('qd').is(_id as String).get()
            } else {
                query.and('qd').in(ids)
            }
            //查询特殊处理(mengjing，mengjing_mp)
//            if ('mengjing'.equals(qid) || 'mengjing_mp'.equals(qid)) {
//                query.and("timestamp").lessThan(date.clearTime().getTime());
//            }
            qdController.reg_list_service(query.get(), req)
        }
    }

    /**
     * 充值详情
     * @param req
     * @return
     */
    def pay_list(HttpServletRequest req) {
        doInQd(req) { String qid, List<String> ids, Map user ->
            if ("nduo".equals(qid)) {
                return pay_list_service(req)
            }
            //默认查询其下所有子渠道的充值情况
            def query = Web.fillTimeBetween(req)
            def _id = req.getParameter('_id') as String
            if (StringUtils.isNotBlank(_id)) {
                if (!ids.contains(_id)) {
                    return [code: 0]
                }
                query.and('qd').is(_id as String).get()
            } else {
                query.and('qd').in(ids)
            }
            //查询特殊处理(mengjing，mengjing_mp)
//            if ('mengjing'.equals(qid) || 'mengjing_mp'.equals(qid)) {
//                query.and("timestamp").lessThan(date.clearTime().getTime());
//            }
            return qdController.pay_list_service(query.get(), req)
        }
    }

    /**
     * 付费转化（只支持查询某一渠道）
     * @param req
     * @return
     */
    def pay_rate(HttpServletRequest req) {
        doInQd(req) { String qid, List<String> cids, Map user ->
            def _id = req.getParameter('_id') as String
            if (cids == null) cids = [] as List
            cids.add(qid)
            if (StringUtils.isNotBlank(_id) && cids.contains(_id)) {
                qid = _id
            }
            qdController.pay_rate_service(Web.fillTimeBetween(req).and('qd').is(qid).get(), new BasicDBObject('timestamp', -1), req)
        }
    }

    /**
     * 留存数据(reg_pay_list中已有相应统计数据)
     * @param req
     * @return
     */
    @Deprecated
    def login_rate(HttpServletRequest req) {
        doInQd(req) { String qid, List<String> cids, Map user ->
            def _id = req.getParameter('_id') as String
            if (cids == null) cids = [] as List
            cids.add(qid)
            if (StringUtils.isNotBlank(_id) && cids.contains(_id)) {
                qid = _id
            }
            def query = Web.fillTimeBetween(req)
            //王奋凯渠道特殊处理(子渠道只能查询当前时间前三天的数据)
            if ("wfk".equals((user.get('qdinfo') as Map)?.get('parent_qd')) && !user.get('parent')) {
                def start = new Date().clearTime().getTime() - 15 * DAY_MILLON
                query.and("timestamp").greaterThanEquals(start)
            }
            //查询特殊处理(mengjing，mengjing_mp)
//            if ('mengjing'.equals(qid) || 'mengjing_mp'.equals(qid)) {
//                query.and("timestamp").lessThan(date.clearTime().getTime());
//            }
            qdController.login_rate_service(query.and('qd').is(qid).get(), new BasicDBObject('timestamp', -1), req)
        }
    }

    /**
     * 活跃统计（只支持查询某一渠道）
     * @param req
     * @return
     */
    def login_total(HttpServletRequest req) {
        doInQd(req) { String qid, List<String> cids, Map user ->
            def _id = req.getParameter('_id') as String
            if (cids == null) cids = [] as List
            cids.add(qid)
            if (StringUtils.isNotBlank(_id) && cids.contains(_id)) {
                qid = _id
            }
            qdController.login_total_service(qid, req)
        }
    }

    final Long DAY_MILLON = 24 * 3600 * 1000L

    //nduo专用接口
    private pay_list_service(HttpServletRequest req) {
        Date sDate = Web.getStime(req)
        Date eDate = Web.getEtime(req)
        if (sDate == null || eDate == null) {
            return [code: 0, msg: "请输入查询时间"]
        }
        QueryBuilder query = Web.fillTimeBetween(sDate, eDate)
        BasicDBList times = new BasicDBList()
        Long prevEnd = null
        query = query.and('via').notEquals('Admin').and('qd').is('nduo').and('cny').greaterThan(1000)
        adminMongo.getCollection('stat_channels').find(query.get(),
                $$(timestamp: 1)).sort($$(timestamp: 1)).toArray().each { DBObject obj ->
            def timestamp = obj.get('timestamp') as Long
            if (timestamp != null) {
                if (prevEnd == null) {
                    def condition = QueryBuilder.start()
                    if (timestamp > sDate.getTime()) {
                        condition.and('timestamp').greaterThanEquals(sDate.getTime()).lessThan(timestamp)
                        times.add(condition.get())
                    }
                } else {
                    if (prevEnd < timestamp) {
                        def condition = QueryBuilder.start()
                        condition.and('timestamp').greaterThanEquals(prevEnd).lessThan(timestamp)
                        times.add(condition.get())
                    }
                }
                prevEnd = timestamp + DAY_MILLON
            }
        }
        if (prevEnd == null) {
            prevEnd == sDate.getTime()
        }
        def condition = QueryBuilder.start()
        if (sDate.getTime() == new Date().clearTime().getTime()) {
            condition.and('timestamp').greaterThanEquals(prevEnd).lessThan(sDate.getTime())
        } else {
            condition.and('timestamp').greaterThanEquals(prevEnd).lessThan(eDate.getTime())
        }
        times.add(condition.get())
        def q = QueryBuilder.start().and('via').notEquals('Admin').and('qd').is('nduo')
        def con = q.get()
        con.put($or, times)
        Crud.list(req, adminMongo.getCollection('finance_log'), con, ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            def users = users()
            for (BasicDBObject obj : list) {
                obj.put('nick_name', users.findOne(obj['user_id'], $$('nick_name', 1))?.get('nick_name'))
            }
        }
    }

}
