package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.admin.Web
import com.ttpod.star.model.RedPacketAcquireType
import com.ttpod.star.model.RedPacketCostType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.groovy.CrudClosures.*

/**
 * 2016/10/12
 * 用户特权列表
 */
@Rest
class RedPacketController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(RedPacketController.class)


    @Delegate
    Crud crud = new Crud(gameLogMongo.getCollection('red_packet_conditions'), Boolean.TRUE,
            [_id                  : Int, desc: Str, cool_down: Int, order: Int, type: Int,
             award_limit          : Int, status: Bool, timestamp: Timestamp, cost_coin_condition: Int, check_in_condition: Int,
             game_number_condition: Int, send_gift_number_condition: Int, reward_coin_min: Int, reward_coin_max: Int,
             reward_cash_amount   : Str, reward_cash_ratio: Str, lock: Bool],
            new Crud.QueryCondition() {
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject('order', -1);
                }
            }
    )

    /**
     * 红包领取日志
     * @param req
     */
    def acquire_logs(HttpServletRequest req) {
        logger.debug('Received acquire_logs params is {}', req.getParameterMap())
        def red_packet_logs = gameLogMongo.getCollection('red_packet_logs')

        def userId = ServletRequestUtils.getIntParameter(req, '_id', 0)
        def query = Web.fillTimeBetween(req)
        if (userId != 0) {
            query.and('user_id').is(userId)
        }

        query.and('type').notEquals(RedPacketAcquireType.提现拒绝.actionName)
        RedPacketAcquireType[] redPacketAcquireTypes = RedPacketAcquireType.values()
        Map<String, String> map = new HashMap<String, String>()
        for (RedPacketAcquireType tmp : redPacketAcquireTypes) {
            map.put(tmp.actionName, tmp.name())
        }

        Crud.list(req, red_packet_logs, query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            for (BasicDBObject obj : list) {
                def type = obj['type'] as String
                obj.put('type', map[type].toString())
            }
        }
    }

    /**
     * 红包解锁日志
     * @param req
     */
    def unlock_logs(HttpServletRequest req) {
        logger.debug('Received unlock_logs params is {}', req.getParameterMap())
        def red_packet_unlock_logs = gameLogMongo.getCollection('red_packet_unlock_logs')
        def userId = ServletRequestUtils.getIntParameter(req, '_id', 0)
        def query = Web.fillTimeBetween(req)
        if (userId != 0) {
            query.and('user_id').is(userId)
        }

        Crud.list(req, red_packet_unlock_logs, query.get(), ALL_FIELD, SJ_DESC)
    }

    /**
     * 好友邀请
     * @param req
     */
    def friend_invite(HttpServletRequest req) {
        logger.debug('Received friend_invite params is {}', req.getParameterMap())
        def red_packet_friend_logs = gameLogMongo.getCollection('red_packet_friend_logs')
        def userId = ServletRequestUtils.getIntParameter(req, '_id', 0)
        def query = Web.fillTimeBetween(req)
        if (userId != 0) {
            query.and('user_id').is(userId)
        }
        Crud.list(req, red_packet_friend_logs, query.get(), ALL_FIELD, SJ_DESC)
    }

    /**
     * 红包现金流出
     * @param req
     * @return
     */
    def cash_cost_logs(HttpServletRequest req) {
        logger.debug('Received cash_cost_logs params is {}', req.getParameterMap())
        def red_packet_cost_logs = gameLogMongo.getCollection('red_packet_cost_logs')
        def userId = ServletRequestUtils.getIntParameter(req, '_id', 0)
        def query = Web.fillTimeBetween(req)
        if (userId != 0) {
            query.and('user_id').is(userId)
        }

        RedPacketCostType[] redPacketCostTypes = RedPacketCostType.values()
        Map<String, String> map = new HashMap<String, String>()
        for (RedPacketCostType redPacketCostType : redPacketCostTypes) {
            map.put(redPacketCostType.actionName, redPacketCostType.name())
        }

        Crud.list(req, red_packet_cost_logs, query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            for (BasicDBObject obj : list) {
                def type = obj['type'] as String
                obj.put('type', map[type].toString())
            }
        }

    }
}
