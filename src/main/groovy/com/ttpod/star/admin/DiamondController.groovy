package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.model.DiamondActionType
import com.ttpod.star.model.OpType
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.servlet.http.HttpServletRequest
import static com.ttpod.rest.common.util.WebUtils.$$

import static com.ttpod.rest.common.doc.MongoKey.$inc
import static com.ttpod.rest.common.doc.MongoKey.$ne
import static com.ttpod.rest.common.doc.MongoKey.$pull
import static com.ttpod.rest.common.doc.MongoKey.$push
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC

/**
 * 钻石的统计
 */

@Rest
class DiamondController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(DiamondController.class)

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
        def data = Crud.list(req, diamond_reports, query.get(), null, SJ_DESC)
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
        def type = num >0 ? DiamondActionType.后台加钻.name() : DiamondActionType.后台减钻.name()
        def diamondId = userId + '_' + type + '_' + timestamp
        def logWithId = $$(_id: diamondId, user_id: userId, cost: num, diamond_count: num,via: 'Admin', timestamp: timestamp,type:type)
        Boolean flag = num > 0 ? addCoin(userId, num, logWithId, obj) : minusCoin(userId, num, logWithId, obj)
        if(flag){
            Crud.opLog(OpType.diamond_add, [user_id: userId, order_id: diamondId, coin: num, remark: remark])
        }

        [code: 1]
    }


    private boolean addCoin(Integer userId, Long coin, BasicDBObject logWithId, BasicDBObject obj) {
        String log_id = (String) logWithId.get("_id");
        if (coin < 0 || log_id == null) {
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
        if (coin < 0 || log_id == null) {
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
