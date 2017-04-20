package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.TwoTableCommit
import com.ttpod.rest.persistent.KGS
import com.ttpod.rest.web.Crud
import com.ttpod.rest.web.StaticSpring
import com.ttpod.star.admin.BaseController
import com.ttpod.star.admin.Web
import com.ttpod.star.model.ApplyType
import com.ttpod.star.model.RedPacketAcquireType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.web.bind.ServletRequestUtils

import static com.ttpod.rest.common.doc.MongoKey.$inc
import static com.ttpod.rest.common.util.WebUtils.$$

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.groovy.CrudClosures.*


/**
 * 2016/10/12
 * 用户特权列表
 */
@RestWithSession
class RedPacketApplyController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(RedPacketApplyController.class)

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
    Crud crud = new Crud(gameLogMongo.getCollection('red_packet_withdraw_conditions'), Boolean.TRUE,
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
     * 提现日志
     * @param req
     * @return
     */
    def apply_logs(HttpServletRequest req) {
        def log = gameLogMongo.getCollection('red_packet_apply_logs')
        def userId = ServletRequestUtils.getIntParameter(req, 'user_id', 0)
        def query = Web.fillTimeBetween(req)
        if (userId != 0) {
            query.and('user_id').is(userId)
        }
        def field = $$('match_condition': 0)
        Crud.list(req, log, query.get(), field, SJ_DESC)
    }

    /**
     * 批量通过
     * @param req
     * @return
     */
    def batch_pass(HttpServletRequest req) {
        logger.debug('Received batch pass params is {}',req.getParameterMap())

        def ids = req.getParameterValues('_ids')
        if (ids.length == 0) {
            return Web.missParam()
        }

        def apply_logs = gameLogMongo.getCollection('red_packet_apply_logs')
        def query = $$('_id': ['$in': ids], 'status': ApplyType.未处理.ordinal())
        def applyList = apply_logs.find(query).toArray()
        if (applyList.size() != ids.length) {
            return Web.notAllowed()
        }
        def update = $$('$set': $$('status': ApplyType.通过.ordinal(), 'last_modify': new Date().getTime()))
        apply_logs.update(query, update, false, true, writeConcern)
        return [code: 1]
    }

    /**
     * 批量拒绝
     * 拒绝要退钱
     * 今日之
     * @param req
     */
    def batch_refuse(HttpServletRequest req) {
        logger.debug('Received batch refuse params is {}',req.getParameterMap())
        def ids = req.getParameterValues('_ids')
        if (ids.length == 0) {
            return Web.missParam()
        }

        def apply_logs = gameLogMongo.getCollection('red_packet_apply_logs')
        def query = $$('_id': ['$in': ids], 'status': ApplyType.未处理.ordinal())
        def applyList = apply_logs.find(query).toArray()
        if (applyList.size() != ids.length) {
            return Web.notAllowed()
        }
        def red_packet_logs = gameLogMongo.getCollection('red_packet_logs')
        def apply_update = $$('$set': $$('status': ApplyType.未通过.ordinal(), 'last_modify': new Date().getTime()))
        for (DBObject apply : applyList) {
            def userId = apply['user_id'] as Integer
            def income = apply['income'] as Long
            def amount = apply['amount'] as Long
            def user_query = $$('_id': userId, 'status': Boolean.TRUE)
            def user_update = $$('$inc': ['finance.cash_count': income])
            if (users().update(user_query, user_update).getN() == 1) {
                logger.debug('update users ok')
                def applyId = apply['_id'].toString()
                def apply_query = $$('_id': applyId, 'status': ApplyType.未处理.ordinal())
                def red_packet_log = $$('_id': logId, 'user_id': userId, 'coin_count': 0, 'cash_count': amount, 'type': RedPacketAcquireType.提现拒绝.actionName, date: new Date().format('yyyyMMdd'), refuse_id: applyId)

                // 先审批更新，不成功则跳过
                if(apply_logs.update(apply_query,apply_update).getN() == 0){
                    continue
                }

                Crud.doTwoTableCommit(red_packet_log, [
                        main           : { users() },
                        logColl        : { red_packet_logs },
                        queryWithId    : { $$('_id': userId) },
                        update         : {
                            $$($inc, ['finance.cash_count': amount])
                        },
                        successCallBack: { true },
                        rollBack       : {}
                ] as TwoTableCommit)
            }
        }
        return [code: 1]
    }


}
