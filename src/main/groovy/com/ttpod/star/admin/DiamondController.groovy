package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.model.DiamondActionType
import com.ttpod.star.model.OrderType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC

/**
 * 钻石的统计
 */
@Rest
//@RestWithSession
class DiamondController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(DiamondController.class)

    /**
     * 钻石加币
     * @param req
     */
    def add_logs(HttpServletRequest req){
        def userId = ServletRequestUtils.getIntParameter(req,'_id',0)
        def query = Web.fillTimeBetween(req)
        if(userId != 0){
            query.and('user_id').is(userId)
        }
        def diamond_logs = adminMongo.getCollection("diamond_logs")
        def map =  Crud.list(req, diamond_logs, query.get(), null, SJ_DESC)
        DiamondActionType[] das = DiamondActionType.values()
        def types = [:]
        das.each {
            DiamondActionType d ->
            types.put(d.actionName,d.name())
        }
        def diamondList = map['data'] as List<DBObject>
        diamondList.each {
            DBObject obj ->
                def type = obj.containsField('type') ? types[obj['type']] : ''
                obj.put('type',type)
        }
        return map
    }

    /**
     * 钻石消费
     * @param req
     */
    def cost_logs(HttpServletRequest req){
        def userId = ServletRequestUtils.getIntParameter(req,'_id',0)
        def query = Web.fillTimeBetween(req)
        if(userId != 0){
            query.and('user_id').is(userId)
        }
        def diamond_cost_logs = adminMongo.getCollection("diamond_cost_logs")
        def map =  Crud.list(req, diamond_cost_logs, query.get(), null, SJ_DESC)
        DiamondActionType[] das = DiamondActionType.values()
        def types = [:]
        das.each {
            DiamondActionType d ->
                types.put(d.actionName,d.name())
        }
        def diamondList = map['data'] as List<DBObject>
        diamondList.each {
            DBObject obj ->
                def type = obj.containsField('type') ? types[obj['type']] : ''
                obj.put('type',type)
        }
        return map
    }

    /**
     * 钻石聚合
     * @param req
     */
    def daily_stat(HttpServletRequest req){
        def query = Web.fillTimeBetween(req)
        def diamond_reports = adminMongo.getCollection('diamond_daily_stat')
        return Crud.list(req, diamond_reports, query.get(), null, SJ_DESC)
    }


}
