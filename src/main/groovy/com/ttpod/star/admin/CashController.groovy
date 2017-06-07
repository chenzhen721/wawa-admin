package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.TwoTableCommit
import com.ttpod.rest.persistent.KGS
import com.ttpod.rest.web.Crud
import com.ttpod.star.model.ApplyType
import com.ttpod.star.model.RedPacketAcquireType
import com.ttpod.star.model.RedPacketCostType
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.$inc
import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.groovy.CrudClosures.Bool
import static com.ttpod.rest.groovy.CrudClosures.Int
import static com.ttpod.rest.groovy.CrudClosures.Str
import static com.ttpod.rest.groovy.CrudClosures.Timestamp

/**
 * date: 2017-04-25
 * @author: monkey
 */
@RestWithSession
class CashController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(CashController.class)

    DBCollection cash_daily_report() { adminMongo.getCollection('cash_dailyReport_stat') }
    DBCollection cash_apply_logs() { gameLogMongo.getCollection('cash_apply_logs') }
    DBCollection withdraw_rules() { gameLogMongo.getCollection('withdraw_rules') }
    DBCollection cash_logs() { gameLogMongo.getCollection('cash_logs') }

    @Resource
    KGS productKGS
    /**
     * day_apply_limit 每日限制申请提现次数
     * withdraw_min_cash 最小提现金额 单位 分
     * level_condition 提现等级限制
     * count_condition 提现次数
     * _id 随便用个id计数器
     * tariff 税率填写0.2 则需要 扣除20%的提现金额，填写0 则不扣税
     */
    @Delegate
    Crud crud = new Crud(withdraw_rules(), Boolean.TRUE,
            [_id            : {
                productKGS.nextId()
            }, desc         : Str, day_apply_limit: Int, order: Int, withdraw_min_cash: Int, tariff: Str,
             level_condition: Int, status: Bool, timestamp: Timestamp, count_condition: Int],
            new Crud.QueryCondition() {
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject('order', 1);
                }
            }
    )

    /**
     * 提现申请
     * @param req
     * @return
     */
    def apply_logs(HttpServletRequest req) {
        def userId = ServletRequestUtils.getIntParameter(req, 'user_id', 0)
        def query = Web.fillTimeBetween(req)
        if (userId != 0) {
            query.and('user_id').is(userId)
        }
        def field = $$('match_condition': 0)
        Crud.list(req, cash_apply_logs(), query.get(), field, SJ_DESC)
    }

    /**
     * 批量通过
     * @param req
     * @return
     */
    def batch_pass(HttpServletRequest req) {
        def ids = ServletRequestUtils.getStringParameter(req,'_ids','')
        if (StringUtils.isBlank(ids)) {
            return Web.missParam()
        }
        String [] arr = ids.split(',')

        def query = $$('_id': ['$in': arr], 'status': ApplyType.未处理.ordinal())
        def applyList = cash_apply_logs().find(query).toArray()
        if (applyList.size() != arr.length) {
            return Web.notAllowed()
        }
        def update = $$('$set': $$('status': ApplyType.通过.ordinal(), 'last_modify': new Date().getTime()))
        cash_apply_logs().update(query, update, false, true, writeConcern)
        return [code: 1]
    }

    /**
     * 批量拒绝
     * 拒绝要退钱
     * 今日之
     * @param req
     */
    def batch_refuse(HttpServletRequest req) {
        def ids = ServletRequestUtils.getStringParameter(req,'_ids','')
        if (StringUtils.isBlank(ids)) {
            return Web.missParam()
        }
        String [] arr = ids.split(',')

        def query = $$('_id': ['$in': arr], 'status': ApplyType.未处理.ordinal())
        def applyList = cash_apply_logs().find(query).toArray()
        if (applyList.size() != arr.length) {
            return Web.notAllowed()
        }
        def apply_update = $$('$set': $$('status': ApplyType.未通过.ordinal(), 'last_modify': new Date().getTime()))
        for (DBObject apply : applyList) {
            def userId = apply['user_id'] as Integer
            def amount = apply['amount'] as Long
            logger.debug('update users ok')
            def applyId = apply['_id'].toString()
            def apply_query = $$('_id': applyId, 'status': ApplyType.未处理.ordinal())

            // 先审批更新，不成功则跳过
            if(cash_apply_logs().update(apply_query,apply_update).getN() == 0){
                continue
            }
            def logId = userId + '_' + new Date().getTime()
            def red_packet_log = $$('_id': logId, 'user_id': userId, 'coin_count': 0, 'cash_count': amount,
                            'type': RedPacketAcquireType.提现拒绝.actionName, date: new Date().format('yyyyMMdd'), refuse_id: applyId)

            def transaction = Crud.doTwoTableCommit(red_packet_log, [
                    main           : { users() },
                    logColl        : { cash_logs() },
                    queryWithId    : { $$('_id': userId, 'status': Boolean.TRUE) },
                    update         : {
                        $$($inc, ['finance.cash_count': amount])
                    },
                    successCallBack: { true },
                    rollBack       : {}
            ] as TwoTableCommit)

            if(!transaction){
                logger.error('mongodb update error')
            }
        }
        return [code: 1]
    }

    /**
     * 现金日报表
     * @param req
     * @return
     */
    def daily_report(HttpServletRequest req){
        logger.debug('Received daily_report params is {}',req.getParameterMap())
        def query = Web.fillTimeBetween(req)
        def data = Crud.list(req, cash_daily_report(), query.get(), ALL_FIELD, SJ_DESC)
        def inc = [:]
        def desc = ['apply_pass_amount':'税前提现','apply_pass_income':'个人所得','apply_refuse':'提现拒绝']

        RedPacketAcquireType[] redPacketAcquireTypes = RedPacketAcquireType.values()
        for(RedPacketAcquireType redPacketAcquireType : redPacketAcquireTypes){
            inc.put(redPacketAcquireType.actionName,redPacketAcquireType.name())
        }

        RedPacketCostType[] redPacketCostTypes = RedPacketCostType.values()
        for(RedPacketCostType redPacketCostType : redPacketCostTypes){
            desc.put(redPacketCostType.actionName,redPacketCostType.name())
        }


        def map = new HashMap(
                inc: inc,
                desc: desc
        )

        return [code: 1,'title': map, 'data': data['data']]
    }
}
