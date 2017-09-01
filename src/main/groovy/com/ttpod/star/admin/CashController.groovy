package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.TwoTableCommit
import com.ttpod.rest.web.Crud
import com.ttpod.star.common.util.AddressUtils
import com.ttpod.star.common.util.ExportUtils
import com.ttpod.star.common.util.HttpsClientUtils
import com.ttpod.star.common.util.WeixinUtils
import com.ttpod.star.model.CashApplyType
import com.ttpod.star.model.RedPacketAcquireType
import com.ttpod.star.model.RedPacketCostType
import org.apache.commons.lang.StringUtils
import org.bson.types.ObjectId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * 现金相关接口
 */
@RestWithSession
class CashController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(CashController.class)

    public static final String INVITOR_HERO_CARD = "hinvitor0001"

    DBCollection cash_daily_report() { adminMongo.getCollection('cash_dailyReport_stat') }
    DBCollection cash_apply_logs() { adminMongo.getCollection('cash_apply_logs') } //提现申请日志
    DBCollection cash_logs() { logMongo.getCollection('cash_logs') } //提现申请操作日志
    DBCollection invitor_logs() { adminMongo.getCollection('invitor_logs')}
    DBCollection user_cards() { gameLogMongo.getCollection('user_cards')}

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
        def field = $$('match_condition': 0, 'tongdun.device_info': 0)
        def result = Crud.list(req, cash_apply_logs(), query.get(), field, SJ_DESC) {List<BasicDBObject> list->
            for(BasicDBObject obj : list) {
                obj.put('level', users().findOne($$(_id: obj.get('user_id') as Integer), $$(level: 1))?.get('level'))
            }
        }
        //查询聚合信息，提现总人次、总人数、总金额、已发放金额、到账金额、已退回
        def total = [users: 0, total: 0, amount: 0, income: 0, fallback: 0] as Map
        def users = [] as Set
        cash_apply_logs().aggregate(
                $$($match: query.get()),
                $$($project: [status: '$status', user_id: '$user_id', amount: '$amount', income: '$income']),
                $$($group: [_id: '$status', count: [$sum: 1], user_id: [$addToSet: '$user_id'], amount: [$sum: '$amount'], income: [$sum: '$income']])
        ).results().each {BasicDBObject row ->
            users.addAll(row['user_id'] as Set)
            total.put('total', (total['total'] as Integer) + (row['amount'] as Integer))
            if ((row[_id] as Integer) == 2) {
                total.put('amount', row['amount'])
                total.put('income', row['income'])
            }
            if ((row[_id] as Integer) == 3) {
                total.put('fallback', row['amount'])
            }
        }
        total.put('users', users.size())
        result.putAll(total)
        result
    }

    /**
     * 批量通过
     * @param req
     * @return
     */
    def batch_pass(HttpServletRequest req, HttpServletResponse res) {
        def ids = ServletRequestUtils.getStringParameter(req,'_ids','')
        if (StringUtils.isBlank(ids)) {
            return Web.missParam()
        }
        String [] arr = ids.split(',')

        def query = $$('_id': ['$in': arr], 'status': CashApplyType.未处理.ordinal())
        def lastModify = new Date().getTime()
        def applyList = cash_apply_logs().find(query).toArray()
        if (applyList.size() != arr.length) {
            return Web.notAllowed()
        }
        def sb = new StringBuilder(WeixinUtils.LAIHOU_APP_ID).append(ExportUtils.ls)
        def update = $$('$set': $$('status': CashApplyType.通过.ordinal(), 'last_modify': lastModify, 'batch_id': lastModify))
        if (cash_apply_logs().update(query, update, false, true, writeConcern).getN() > 0) {
            logger.info("batch_pass cash apply success, ids:" + arr)
            def logs = invitor_logs().find($$(status: 1, user_id: [$in: applyList*.user_id as Set]))?.toArray()
            if (logs != null) {
                logs.each {BasicDBObject obj ->
                    //do two table commit
                    def _id = obj['_id'] as ObjectId
                    def invitor = obj['invitor'] as Integer
                    if (!setHeroCard(invitor, INVITOR_HERO_CARD, 0, 1, _id)) {
                        logger.error("card send error, invitor_logs._id: ${_id}, invitor: ${invitor}, batch_id: ${lastModify}")
                    }
                }
            }
            applyList.each { BasicDBObject obj ->
                if (StringUtils.isNotBlank(obj.get('account') as String)) {
                    sb.append(obj.get('account')).append("  ").append(obj.get('income')).append(ExportUtils.ls)
                }
            }
            return [code: 1, data: sb.toString()]
        }
        return [code: 0]
    }

    Boolean setHeroCard(Integer userId, String cardId, Integer count, Integer available_count, Object logId) {
        String entryKey = "cards.${cardId}.count"
        String availableKey = "cards.${cardId}.available_count"

        Long cd = 0
        if (available_count > 0) {
            cd = System.currentTimeMillis() + 30 * 1000
        }
        def cards = new HashMap()
        cards.put("cards.${cardId}".toString(), $$(cd:cd, count: count, available_count: available_count))

        if(user_cards().update($$(_id : userId, 'invitor_logs._id': [$ne: logId]).append(entryKey,[$not: [$gte:1]]),
                $$($set: cards, $push: [invitor_logs: [_id:logId, timestamp: System.currentTimeMillis()]])).getN() == 1
                || user_cards().update($$(_id : userId, 'invitor_logs._id': [$ne: logId]).append(entryKey,[$gte:1]),
                $$($inc: $$(entryKey, count).append(availableKey, available_count),
                        $push: [invitor_logs: [_id:logId, timestamp: System.currentTimeMillis()]])).getN() == 1){
            //发牌成功，更新日志
            invitor_logs().update($$(_id: logId), $$($set: [status: 2, last_modify: System.currentTimeMillis()]))
            user_cards().update($$(_id: userId), $$($pull: [invitor_logs: [_id: logId]]))
            return true
        }
        return false
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

        def query = $$('_id': ['$in': arr], 'status': CashApplyType.未处理.ordinal())
        def applyList = cash_apply_logs().find(query).toArray()
        if (applyList.size() != arr.length) {
            return Web.notAllowed()
        }
        def apply_update = $$('$set': $$('status': CashApplyType.拒绝.ordinal(), 'last_modify': new Date().getTime()))
        for (DBObject apply : applyList) {
            def userId = apply['user_id'] as Integer
            def amount = apply['amount'] as Long
            logger.debug('update users ok')
            def applyId = apply['_id'].toString()
            def apply_query = $$('_id': applyId, 'status': CashApplyType.未处理.ordinal())

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

    @Deprecated
    private Map buildSendPack(Map map) {
        Map<String, Object> result = new TreeMap<String, Object>();
        result.put("nonce_str", WeixinUtils.createNoncestr(32))
        // 商户订单号（每个订单号必须唯一）。组成：mch_id+yyyymmdd+10位一天内不能重复的数字。接口根据商户订单号支持重入，如出现超时可再调用。
        result.put("mch_billno", WeixinUtils.getMchBillNo())
        result.put("mch_id", WeixinUtils.MCH_ID) // 微信支付分配的商户号
        result.put("wxappid", WeixinUtils.APP_ID) // 微信分配的公众账号ID（企业号corpid即为此appId）
        result.put("send_name", "来吼官方") // 商户名称, 红包发送者名称
        result.put("re_openid", map.get("account")) // "oy-yfuAMQ4tynMP98bOdMmuS4Bk4"; // 接受红包的用户.用户在wxappid下的openid
        result.put("total_amount", map.get("amount")) // 付款金额，单位分
        result.put("total_num", "1") // 红包发放总人数
        result.put("wishing", "大吉大利，今晚吃鸡") // 红包祝福语
        result.put("client_ip", AddressUtils.getLocalHost()) // 调用接口的机器Ip地址
        result.put("act_name", "提现") // 活动名称
        result.put("remark", "ThankYou2") // 备注信息
        String sign = WeixinUtils.createSign("UTF-8", result) // 签名
        result.put("sign", sign)
        result
    }

    //发红包接口测试，不行回退
    @Deprecated
    def testRedPack(HttpServletRequest req) {
        //微信公众号对应的openId，例如：ok2Ip1kwAkESfDpIsP-EXli4ikXY
        Map xml = buildSendPack([amount: 100, account: "ok2Ip1kwAkESfDpIsP-EXli4ikXY"])
        String rtnXml = HttpsClientUtils.execute(WeixinUtils.SEND_PACK_URL, WeixinUtils.mapToXml(xml), WeixinUtils.CERT_PATH, WeixinUtils.MCH_ID)
    }

}
