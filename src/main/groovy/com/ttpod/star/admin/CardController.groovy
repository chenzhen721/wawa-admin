package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.IMessageCode
import com.ttpod.rest.persistent.KGS
import com.ttpod.rest.web.Crud
import com.ttpod.star.model.DiamondActionType
import com.ttpod.star.model.OpType
import org.apache.commons.lang.StringUtils
import org.springframework.web.bind.ServletRequestUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.$inc
import static com.ttpod.rest.common.doc.MongoKey.$ne
import static com.ttpod.rest.common.doc.MongoKey.$pull
import static com.ttpod.rest.common.doc.MongoKey.$push
import static com.ttpod.rest.common.doc.MongoKey.$set
import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.groovy.CrudClosures.Int
import static com.ttpod.rest.groovy.CrudClosures.Str
import static com.ttpod.rest.groovy.CrudClosures.Timestamp

/**
 * 卡牌维护,后期考虑批量导入
 */
@RestWithSession
class CardController extends BaseController{

    /**
     * 卡牌分类图
     * 攻 a:"https://aiimg.sumeme.com/58/2/1496816695418.png"
     * 防 b:"https://aiimg.sumeme.com/8/0/1496816754312.png"
     * 金 c:"https://aiimg.sumeme.com/18/2/1496816785298.png"
     * 盗 d:https://aiimg.sumeme.com/11/3/1496816812299.png
     */
    private static final String ack_pic = "https://aiimg.sumeme.com/58/2/1496816695418.png"
    private static final String def_pic = "https://aiimg.sumeme.com/8/0/1496816754312.png"
    private static final String coin_pic = "https://aiimg.sumeme.com/18/2/1496816785298.png"
    private static final String steal_pic = "https://aiimg.sumeme.com/11/3/1496816812299.png"
    private static final String[] cates = ["", ack_pic, def_pic, coin_pic, steal_pic]

    /**
     * 卡牌等级图
     * [1-10]  https://aiimg.sumeme.com/63/7/1496816851967.png
     * [11-20] https://aiimg.sumeme.com/30/6/1496816883486.png
     * [21-30] https://aiimg.sumeme.com/28/4/1496816902812.png
     * [31-40] https://aiimg.sumeme.com/1/1/1496816918785.png
     * [41-50] https://aiimg.sumeme.com/23/7/1496816935127.png
     */
    private static final String level_1_10 = "https://aiimg.sumeme.com/63/7/1496816851967.png"
    private static final String level_11_20 = "https://aiimg.sumeme.com/30/6/1496816883486.png"
    private static final String level_21_30 = "https://aiimg.sumeme.com/28/4/1496816902812.png"
    private static final String level_31_40 = "https://aiimg.sumeme.com/1/1/1496816918785.png"
    private static final String level_41_50 = "https://aiimg.sumeme.com/23/7/1496816935127.png"
    private static final List<String> level_pics = new ArrayList<>(50)

    //冷却时间
    private static final Integer[] cds =  [60,66,72,79,86,94,103,113,124,136,149,163,179,196,215,236,259,284,312,343,377,414,455,500,550,605,665,731,804,884,972,1069,1175,1292,1421,1563,1719,1890,2079,2286,2514,2765,3041,3345,3679,4046,4450,4895,5384,5922,6514,7165,7881,8669,9535,10488,11536,12689,13957,15352,16887,18575,20432,22475,24722,27194,29913,32904,36194,39813,43794,48173,52990,58289,64117,70528,77580,85338,93871,103258,113583,124941,137435,151178,166295,182924,201216,221337,243470,267817,294598,324057,356462,392108,431318,474449,521893,574082,631490];

    static {
        //卡牌等级背景图
        for (int i = 0; i <= 50; i++) {
            if(i <= 10){
                level_pics.add(level_1_10)
            }else if(i <= 20){
                level_pics.add(level_11_20)
            }else if(i <= 30){
                level_pics.add(level_21_30)
            }else if(i <= 40){
                level_pics.add(level_31_40)
            }else if(i <= 50){
                level_pics.add(level_41_50)
            }
        }
    }

    //默认翻牌cd，如果不填使用默认cd
    Map<String, Closure> props = [_id:{it != null ?: seqKGS.nextId()}, status: Int, type: Int, category: Int, level: Int,
                 next_level_id: Str, pic: Str, cate_pic: Str, levelup: Int, timestamp:Timestamp,
                 cds: {String cd -> StringUtils.isNotBlank(cd) ?: StringUtils.join(cds, ",")},
                 coin_rate: { StringUtils.isBlank(it as String) ? 0: it as Double}, coin_min: Int, coin_max: Int,
                 cash_rate: { StringUtils.isBlank(it as String) ? 0: it as Double}, cash_min: Int, cash_max: Int,
                 exp_rate: { StringUtils.isBlank(it as String) ? 0: it as Double}, exp_min: Int, exp_max: Int,
                 diamond_rate: { StringUtils.isBlank(it as String) ? 0: it as Double}, diamond_min: Int, diamond_max: Int,
                 ack_rate: { StringUtils.isBlank(it as String) ? 0: it as Double}, ack_min: Int, ack_max: Int,
                 def_rate: { StringUtils.isBlank(it as String) ? 0: it as Double}, def_min: Int, def_max: Int,
                 steal_rate: { StringUtils.isBlank(it as String) ? 0: it as Double}, steal_min: Int, steal_max: Int
    ]

    DBCollection table() {adminMongo.getCollection('cards')}

    @Resource
    KGS    seqKGS
    @Delegate Crud crud = new Crud(table(), props,
             new Crud.QueryCondition(){
                public DBObject query(HttpServletRequest req) {
                    def qb = QueryBuilder.start()
                    if (req['status']){
                        qb.and('status').is(req['status'] as Boolean)
                    }
                    if (req['category']) {
                        qb.and('category').is(req['category'] as Integer)
                    }
                    if (req['type']) {
                        qb.and('type').is(req['type'] as Integer)
                    }
                    return qb.get()
                }
                public DBObject sortby(HttpServletRequest req) {
                 return new BasicDBObject(["status": -1, level: 1, timestamp: -1])
                }
            }
    )

    def add_card(HttpServletRequest req) {
        //TODO 必填校验

        Map map = new HashMap()
        props.each {String key, Closure value ->
            Object val = value.call(req.getParameter(key))
            if (val != null) {
                map.put(key, val)
            }
        }
        def category = ServletRequestUtils.getIntParameter(req, 'category')
        def level = ServletRequestUtils.getIntParameter(req, 'level')

        map.put('cate_pic', cates[category])
        map.put('pic', level_pics.get(level))

        if(table().save(new BasicDBObject(map)).getN() == 1){
            Crud.opLog(table().getName() + "_add", map)
        }

        return IMessageCode.OK
    }

    def edit_card(HttpServletRequest req) {
        Object id = parseId(req)
        if(null == id){
            return IMessageCode.CODE0
        }

        Map map = new HashMap()
        props.each {String key, Closure value ->
            if(!key.equals(_id)){
                String strValue = req.getParameter(key)
                if(StringUtils.isNotEmpty(strValue)){
                    Object val = value.call(strValue)
                    if (val != null) {
                        map.put(key, val)
                    }
                }
            }
        }

        if(map.size() > 0 && table().update(new BasicDBObject(_id,id),new BasicDBObject($set,map)).getN() == 1){
            map.put(_id,id)
            Crud.opLog(table().getName() + "_edit", map)
        }
        return IMessageCode.OK
    }

    /**
     * 钻石加币
     * @param req
     */
    def add_logs(HttpServletRequest req) {
        def userId = ServletRequestUtils.getIntParameter(req, '_id', 0)
        def query = Web.fillTimeBetween(req)
        if (userId != 0) {
            query.and('user_id').is(userId)
        }
        def diamond_logs = adminMongo.getCollection("diamond_logs")
        def map = Crud.list(req, diamond_logs, query.get(), null, SJ_DESC)
        DiamondActionType[] das = DiamondActionType.values()
        def types = [:]
        das.each {
            DiamondActionType d ->
                types.put(d.actionName, d.name())
        }
        def diamondList = map['data'] as List<DBObject>
        diamondList.each {
            DBObject obj ->
                def type = obj.containsField('type') ? types[obj['type']] : ''
                obj.put('type', type)
        }
        return map
    }

    /**
     * 钻石消费
     * @param req
     */
    def cost_logs(HttpServletRequest req) {
        def userId = ServletRequestUtils.getIntParameter(req, '_id', 0)
        def query = Web.fillTimeBetween(req)
        if (userId != 0) {
            query.and('user_id').is(userId)
        }
        def diamond_cost_logs = adminMongo.getCollection("diamond_cost_logs")
        def map = Crud.list(req, diamond_cost_logs, query.get(), null, SJ_DESC)
        DiamondActionType[] das = DiamondActionType.values()
        def types = [:]
        das.each {
            DiamondActionType d ->
                types.put(d.actionName, d.name())
        }
        def diamondList = map['data'] as List<DBObject>
        diamondList.each {
            DBObject obj ->
                def type = obj.containsField('type') ? types[obj['type']] : ''
                obj.put('type', type)
        }
        return map
    }

    /**
     * 钻石聚合
     * @param req
     */
    def daily_stat(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        def diamond_reports = adminMongo.getCollection('diamond_dailyReport_stat')
        def data = Crud.list(req, diamond_reports, query.get(), ALL_FIELD, SJ_DESC)
        DiamondActionType[] das = DiamondActionType.values()
        def inc = [:]
        def desc = [:]
        das.each {
            DiamondActionType diamondActionType ->
                if (diamondActionType.getIsIncAction() == 0) {
                    inc.put(diamondActionType.actionName, diamondActionType.name())
                } else {
                    desc.put(diamondActionType.actionName, diamondActionType.name())
                }
        }

        def map = new HashMap(
                inc: inc,
                desc: desc
        )

        return ['title': map, 'data': data['data']]
    }

    /**
     * 钻石加币
     * @param req
     * @return
     */
    def add(HttpServletRequest req) {
        Integer userId = ServletRequestUtils.getIntParameter(req, '_id', 0)
        Long num = ServletRequestUtils.getLongParameter(req, 'num', 0L)
        if (userId == 0 || num == 0L) {
            return Web.notAllowed()
        }
        Long timestamp = new Date().getTime()
        String remark = req['remark'] as String
        def obj = $$('finance.diamond_count', num);
        def type = num > 0 ? DiamondActionType.后台加钻.actionName : DiamondActionType.后台减钻.actionName
        def diamondId = userId + '_' + type + '_' + timestamp
        def diamond_count = Math.abs(num)
        def logWithId = $$(_id: diamondId, user_id: userId, cost: num, diamond_count: diamond_count, via: 'Admin', timestamp: timestamp, type: type, remark: remark)
        Boolean flag = num > 0 ? addCoin(userId, num, logWithId, obj) : minusCoin(userId, num, logWithId, obj)
        if (flag) {
            Crud.opLog(OpType.diamond_add, [user_id: userId, order_id: diamondId, coin: num, remark: remark])
        }

        [code: 1]
    }


    private boolean addCoin(Integer userId, Long coin, BasicDBObject logWithId, BasicDBObject obj) {
        String log_id = (String) logWithId.get("_id");
        if (coin <= 0 || log_id == null) {
            return false;
        }
        if (logWithId.get("to_id") == null) {
            logWithId.put("to_id", userId);
        }
        if (logWithId.get(timestamp) == null) {
            logWithId.put(timestamp, System.currentTimeMillis());
        }
        DBCollection users = users();
        DBObject my_user = users.findOne(new BasicDBObject(_id, userId));
        if (my_user != null) {
            if (null != my_user.get("qd")) {
                String qd = my_user.get("qd").toString();
                logWithId.append("qd", qd);
            }
        }
        DBCollection logColl = adminMongo.getCollection('diamond_logs');
        if (logColl.count(new BasicDBObject(_id, log_id)) == 0 &&
                users.update(new BasicDBObject('_id', userId).append('diamond_logs._id', new BasicDBObject($ne, log_id)),
                        new BasicDBObject($inc, obj)
                                .append($push, new BasicDBObject('diamond_logs', logWithId)),
                        false, false, writeConcern
                ).getN() == 1) {

            logColl.save(logWithId, writeConcern);
            users.update(new BasicDBObject(_id, userId),
                    new BasicDBObject($pull, new BasicDBObject('diamond_logs', new BasicDBObject(_id, log_id))),
                    false, false, writeConcern);

            return true;
        }
        return false;
    }

    private boolean minusCoin(Integer userId, Long coin, BasicDBObject logWithId, BasicDBObject obj) {
        String log_id = (String) logWithId.get("_id");
        if (coin >= 0 || log_id == null) {
            return false;
        }
        if (logWithId.get("to_id") == null) {
            logWithId.put("to_id", userId);
        }
        if (logWithId.get(timestamp) == null) {
            logWithId.put(timestamp, System.currentTimeMillis());
        }
        DBCollection users = users();
        DBObject my_user = users.findOne(new BasicDBObject(_id, userId));
        if (my_user != null) {
            if (null != my_user.get("qd")) {
                String qd = my_user.get("qd").toString();
                logWithId.append("qd", qd);
            }
        }
        DBCollection logColl = adminMongo.getCollection('diamond_cost_logs');
        if (logColl.count(new BasicDBObject(_id, log_id)) == 0 &&
                users.update(new BasicDBObject('_id', userId).append('diamond_cost_logs._id', new BasicDBObject($ne, log_id)),
                        new BasicDBObject($inc, obj)
                                .append($push, new BasicDBObject('diamond_cost_logs', logWithId)),
                        false, false, writeConcern
                ).getN() == 1) {

            logColl.save(logWithId, writeConcern);
            users.update(new BasicDBObject(_id, userId),
                    new BasicDBObject($pull, new BasicDBObject('diamond_cost_logs', new BasicDBObject(_id, log_id))),
                    false, false, writeConcern);

            return true;
        }
        return false;
    }

}
