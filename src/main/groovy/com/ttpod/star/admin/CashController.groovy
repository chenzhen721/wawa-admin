package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.TwoTableCommit
import com.ttpod.rest.web.Crud
import com.ttpod.star.common.util.AddressUtils
import com.ttpod.star.common.util.HttpsClientUtils
import com.ttpod.star.common.util.WeixinUtils
import com.ttpod.star.model.CashApplyType
import com.ttpod.star.model.RedPacketAcquireType
import com.ttpod.star.model.RedPacketCostType
import groovy.xml.MarkupBuilder
import groovy.xml.MarkupBuilderHelper
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * 现金相关接口
 */
@RestWithSession
class CashController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(CashController.class)

    DBCollection cash_daily_report() { adminMongo.getCollection('cash_dailyReport_stat') }
    DBCollection cash_apply_logs() { adminMongo.getCollection('cash_apply_logs') }
    DBCollection cash_logs() { logMongo.getCollection('cash_logs') }

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

        def query = $$('_id': ['$in': arr], 'status': CashApplyType.未处理.ordinal())
        def applyList = cash_apply_logs().find(query).toArray()
        if (applyList.size() != arr.length) {
            return Web.notAllowed()
        }
        //
        applyList.each {BasicDBObject obj ->
            Map xml = buildSendPack(obj.toMap())
            def update = $$('$set': $$('status': CashApplyType.通过.ordinal(), 'last_modify': new Date().getTime(), 'weixin_params': $$(xml)))
            //默认发送成功
            if (cash_apply_logs().update($$(_id: obj.get("_id")), update, false, true, writeConcern).getN() == 1) {
                String rtnXml = HttpsClientUtils.execute(WeixinUtils.SEND_PACK_URL, WeixinUtils.mapToXml(xml), WeixinUtils.CERT_PATH, WeixinUtils.MCH_ID)
                Map result = xmlToMap(rtnXml)
                //TODO 判断是否发送成功，若失败更新当前状态，记录失败原因

            }
        }

        return [code: 1]
    }

    private Map buildSendPack(Map map) {
        Map<String, Object> result = new TreeMap<String, Object>();
        result.put("nonce_str", WeixinUtils.createNoncestr(32))
        // 商户订单号（每个订单号必须唯一）。组成：mch_id+yyyymmdd+10位一天内不能重复的数字。接口根据商户订单号支持重入，如出现超时可再调用。
        result.put("mch_billno", WeixinUtils.getMchBillNo())
        result.put("mch_id", WeixinUtils.MCH_ID) // 微信支付分配的商户号
        result.put("wxappid", WeixinUtils.APP_ID) // 微信分配的公众账号ID（企业号corpid即为此appId）
        result.put("send_name", "来吼官方") //TODO 商户名称, 红包发送者名称
        result.put("re_openid", map.get("account")) // "oy-yfuAMQ4tynMP98bOdMmuS4Bk4"; // 接受红包的用户.用户在wxappid下的openid
        result.put("total_amount", map.get("amount")) // 付款金额，单位分
        result.put("total_num", "1") // 红包发放总人数
        result.put("wishing", "大吉大利，今晚吃鸡") //TODO 红包祝福语
        result.put("client_ip", AddressUtils.getLocalHost()) // 调用接口的机器Ip地址
        result.put("act_name", "提现") //TODO 活动名称
        result.put("remark", "ThankYou2") //TODO 备注信息
        String sign = WeixinUtils.createSign("UTF-8", result) // 签名
        result.put("sign", sign)
        result
//        String xmlParam = mapToXml(map)
//        logger.info("sendRedPack, 入参xmlParam=" + xmlParam)
//        xmlParam
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

    //发红包接口测试，不行回退
    def testRedPack(HttpServletRequest req) {
        def map = [amount: 100, account: "opUFIwXkPyH9kULk1xrDllhO1PsQ"]
        String result = HttpsClientUtils.execute(WeixinUtils.SEND_PACK_URL, WeixinUtils.mapToXml(buildSendPack(map)), WeixinUtils.CERT_PATH, WeixinUtils.MCH_ID)
        logger.info(result)
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

    private static Map xmlToMap(String xml) {
        xml
    }

}
