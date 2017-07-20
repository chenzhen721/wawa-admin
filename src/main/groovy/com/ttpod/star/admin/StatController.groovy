package com.ttpod.star.admin

import com.mongodb.*
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.MongoKey
import com.ttpod.rest.web.Crud
import com.ttpod.star.common.util.ExportUtils
import com.ttpod.star.model.DiamondActionType
import com.ttpod.star.model.PayType
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat
import java.util.regex.Pattern

import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.common.util.WebUtils.*

/**
 * date: 13-3-28 下午2:31
 * @author: yangyang.cong@ttpod.com
 */
@Rest
class StatController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(StatController.class)

    DBCollection table() { adminMongo.getCollection('stat_daily') }

    DBCollection finance_monthReport() { adminMongo.getCollection('finance_monthReport') }

    DBCollection finance_daily_log() { adminMongo.getCollection('finance_daily_log') }

    /**
     * 翻卡
     * @param req
     * @return
     */
    def open_card_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('open_card').get())
    }

    /**
     * 寻宝
     * @param req
     * @return
     */
    def family_event_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('family_event').get())
    }

    /**
     * 道具使用统计
     * @param req
     * @return
     */
    def use_item_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('use_item').get())
    }

    /**
     * 上麦统计
     * @param req
     * @return
     */
    def mic_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).and('type').is('on_mic').get()
        Crud.list(req, adminMongo.getCollection('stat_mic'), query, ALL_FIELD, SJ_DESC)
    }

    def diamond_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        def diamond_reports = adminMongo.getCollection('finance_daily_log')
        def data = Crud.list(req, diamond_reports, query.get(), $$(begin_surplus: 1, end_surplus: 1, inc: 1, dec: 1, timestamp: 1, today_balance: 1), SJ_DESC)
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
        data.putAll([title: [inc: inc, desc: desc]])
        data
    }


    //保留
    def login_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('login').get())
    }

    //保留
    def login_month_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).and('type').is('login').get()
        Crud.list(req, adminMongo.getCollection('stat_month'), query, ALL_FIELD, SJ_DESC) { List<BasicDBObject> data ->
            for (BasicDBObject obj : data) {
                obj.remove("type")
                def timestamp = obj.get("timestamp") as Long
                if (timestamp != null) {
                    def pay = adminMongo.getCollection('stat_month').findOne($$([timestamp: timestamp, type: 'allpay']))
                    obj.put("pay_total", pay?.get("total"))
                    obj.put("pay_pc", pay?.get("pc"))
                    obj.put("pay_mobile", pay?.get("moblie"))
                }
            }
        }
    }

    /*def daily_log(HttpServletRequest req) {
        def type = req['type']
        if (StringUtils.isBlank(type)) return [code: 0, msg: 'type为空']
        super.list(req, Web.fillTimeBetween(req).and('type').is(type).get())
    }*/

    def finance_log(HttpServletRequest req) {
        QueryBuilder queryBuilder = Web.fillTimeBetween(req).and('type').is('finance')
        super.list(req, queryBuilder.get())
    }

    /**
     * 充值渠道 注册统计
     * @param req
     * @return
     */
    def finance_log_user(HttpServletRequest req) {
        QueryBuilder queryBuilder = Web.fillTimeBetween(req).and('type').is('allpay')
        Crud.list(req, table(), queryBuilder.get(), $$(user_pc: 1, user_mobile: 1, user_ios: 1, user_h5: 1, user_ria: 1, timestamp: 1), SJ_DESC)
    }

    //财务月报
    private static final def INC_HEADS = [
            [k: "charge_cny", v: '充值金额'],
            [k: "charge_coin", v: '充值阳光'],
            [k: "direct_total_cny", v: '直充金额'],
            [k: "direct_total_coin", v: '直充阳光'],
            [k: "proxy_total_cny", v: '代充金额'],
            [k: "proxy_total_coin", v: '代充阳光'],
            [k: "hand_coin", v: '运营手动加币'],
            [k: "hand_cut_coin", v: '运营手动减币'],
            [k: "mission_coin", v: '任务奖励'],
            [k: "login_coin", v: '签到奖励'],
            [k: "game_coin", v: '游戏奖励'],
            [k: "red_packet_coin", v: '红包奖励'],
            [k: "total", v: '增加阳光总数']
    ]

    private static final def DEC_HEADS = [
            [k: "send_gift", v: '送礼'],
            [k: "game_spend_coin", v: '游戏消费'],
            [k: "unlock_spend_coin", v: '红包解锁'],
            [k: "total", v: '消费阳光总计']
    ]

    /**
     * 保留
     * @param req
     * @return
     */
    def finance_log_month(HttpServletRequest req) {
        QueryBuilder queryBuilder = Web.fillTimeBetween(req)
        Map result = Crud.list(req, finance_monthReport(), queryBuilder.get(), ALL_FIELD, SJ_DESC);
        def list = result.get('data')
        Map data = new HashMap();
        data.put('list', list)
        data.put('heads', [inc: INC_HEADS, dec: DEC_HEADS])
        result.put('data', data)
        return result;
    }

    //财务充值统计
    def finance_charge_log(HttpServletRequest req) {
        QueryBuilder queryBuilder = Web.fillTimeBetween(req).and('type').is('finance')
        //super.list(req, queryBuilder.get())
        [code: 1, data: table().find(queryBuilder.get(), ALL_FIELD).sort(SJ_DESC).limit(800).toArray()]
    }

    //保留 充值柠檬币比例表
    def charge_coin_log(HttpServletRequest req) {
        QueryBuilder queryBuilder = Web.fillTimeBetween(req)
        def field = $$(charge_cny: 1, cut_charge_cny: 1, begin_surplus: 1, charge_coin: 1, inc_coin: 1, inc_total: 1, dec_total: 1, end_surplus: 1, date: 1, hand_cut_coin: 1)
        //Crud.list(req, finance_daily_log(), queryBuilder.get(), field, SJ_DESC);
        [code: 1, data: finance_daily_log().find(queryBuilder.get(), field).sort(SJ_DESC).limit(800).toArray()]
    }

    /**
     * 保留
     * 运营数据总表
     * @param req
     */
    def total_report(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        query.put('type').is('allreport')
        Crud.list(req, adminMongo.getCollection('stat_report'), query.get(), ALL_FIELD, SJ_DESC)
    }

}
