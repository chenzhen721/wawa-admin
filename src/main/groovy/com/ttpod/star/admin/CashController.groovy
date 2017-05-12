package com.ttpod.star.admin

import com.mongodb.DBCollection
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.model.RedPacketAcquireType
import com.ttpod.star.model.RedPacketCostType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC

/**
 * date: 2017-04-25
 * @author: monkey
 */
@RestWithSession
class CashController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(CashController.class)

    DBCollection cash_daily_report() { adminMongo.getCollection('cash_dailyReport_stat') }


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
