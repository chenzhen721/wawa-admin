package com.ttpod.star.admin

import com.mongodb.*
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.MongoKey
import com.ttpod.rest.web.Crud
import com.ttpod.star.common.util.ExportUtils
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

    DBCollection stat_lives() { adminMongo.getCollection('stat_lives') }

    DBCollection statChannel() { adminMongo.getCollection('stat_channels') }

    DBCollection channel() { adminMongo.getCollection('channels') }

    DBCollection finance_monthReport() { adminMongo.getCollection('finance_monthReport') }

    DBCollection finance_daily_log() { adminMongo.getCollection('finance_daily_log') }

    def pool_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('luck').get())
    }

    def login_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('login').get())
    }

    def login_month_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).and('type').is('login').get()
        Crud.list(req, adminMongo.getCollection('stat_month'), query, ALL_FIELD, MongoKey.SJ_DESC) { List<BasicDBObject> data ->
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

//    private static final Map COST_TYPES = [
//            key : ['send_gift'],
//            name: ['礼物']
//    ]

    def cost_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).and('type').is('allcost')
        def gameList = adminMongo.getCollection('games').find().toArray()
        List<String> keys = ['send_gift']
        List<String> names = ['礼物']
        gameList.each {
            BasicDBObject obj ->
                String id = obj['_id']
                String name = obj['name']
                keys.add(id)
                names.add(name)
        }
        def cost_type = [
                key : keys,
                name: names
        ]
        def alltitle = new HashMap(cost_type)
        def map = Crud.list(req, table(), query.get(), ALL_FIELD, SJ_DESC, cost_log_closure(alltitle)) as Map
        map.put('title', alltitle)
        return map
    }

    /**
     * 阳光的入账统计
     * 任务 + 签到 + 游戏下注 + 手工加币
     * @param req
     * @return
     */
    def coin_income(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        def daily_report = adminMongo.getCollection('finance_dailyReport').find(query.get()).toArray()
        def result = new ArrayList()
        def title = ['时间', '任务', '签到', '游戏', '后台加币', '合计']
        def column = ['timestamp', 'mission_coin', 'login_coin', 'game_coin', 'hand_coin', 'total']
        for (DBObject obj : daily_report) {
            def tmp = new HashMap()
            def mission_coin = 0L
            def game_coin = 0L
            def login_coin = 0L
            def hand_coin = 0L
            if (obj.containsField('mission_coin')) {
                mission_coin = obj['mission_coin'] as Long
            }

            if (obj.containsField('game_coin')) {
                game_coin = obj['game_coin'] as Long
            }

            if (obj.containsField('hand_coin')) {
                hand_coin = obj['hand_coin'] as Long
            }

            if (obj.containsField('login_coin')) {
                login_coin = obj['login_coin'] as Long
            }

            def timestamp = obj['timestamp'] as Long
            tmp.put('timestamp', timestamp)
            tmp.put('mission_coin', mission_coin)
            tmp.put('login_coin', login_coin)
            tmp.put('game_coin', game_coin)
            tmp.put('hand_coin', hand_coin)
            def total = mission_coin + login_coin + game_coin + hand_coin
            tmp.put('total', total)
            result.add(tmp)
        }

        def map = new HashMap(
                keys: title,
                props: column
        )

        return ['title': map, 'data': result]
    }

    def gift_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).and('type').is('gift_detail')
        def id = req[_id]
        if (id) {
            query.and('gift_id').is(id as Integer)
        }
        String name = req['name']
        if (name) {
            query.and('name').regex(Pattern.compile(name))
        }
        Crud.list(req, table(), query.get(), ALL_FIELD, SJ_DESC, gift_log_closure())
    }

    def car_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).and('type').is('buy_car')
        def id = req[_id]
        if (id) {
            query.and('car_id').is(id as Integer)
        }
        super.list(req, query.get())
    }

    def vip_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('buy_vip').get())
    }

    def sofa_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('grab_sofa').get())
    }

    def egg_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('open_egg').get())
    }

    def bingo_egg_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('open_bingo_egg').get())
    }

    def label_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('grab_label').get())
    }

    def guard_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('buy_guard').get())
    }

    def open_card_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('open_card').get())
    }

    def football_shoot_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('football_shoot').get())
    }

    def bell_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('send_bell').get())
    }

    def daily_log(HttpServletRequest req) {
        def type = req['type']
        if (StringUtils.isBlank(type)) return [code: 0, msg: 'type为空']
        super.list(req, Web.fillTimeBetween(req).and('type').is(type).get())
    }

    def car_race_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('car_race').get())
    }

    def funding_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        def id = req[_id] as String
        if (StringUtils.isNotBlank(id)) {
            query.put('gid').is(id as Integer)
        }
        def name = req['name'] as String
        if (StringUtils.isNotBlank(name)) {
            query.put('title').is(name)
        }
        query.put('type').is('fundings_logs')
        super.list(req, query.get())
    }

    /**
     * 财神
     */
    def fortune_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('send_fortune').get())
    }

    def treasure_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('send_treasure').get())
    }

    /**
     * 家族
     */
    def family_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('apply_family').get())
    }

    def broadcast_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('broadcast').get())
    }

    def levelup_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('level_up').get())
    }

    def song_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('song').get())
    }

    def nest_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('nest_send_gift').get())
    }

    /**
     * 周星日报
     */
    def weekstar_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('week_stars').get())
    }

    def prettynum_log(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('buy_prettynum').get())
    }

    def finance_log(HttpServletRequest req) {
        QueryBuilder queryBuilder = Web.fillTimeBetween(req).and('type').is('finance')
        super.list(req, queryBuilder.get())
    }

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
            [k: "game_coin", v: '游戏赢币'],
            [k: "total", v: '增加阳光总数']
    ]

    private static final def DEC_HEADS = [
            [k: "send_gift", v: '送礼'],
            [k: "game_spend_coin", v: '游戏输币'],
            [k: "total", v: '消费阳光总计']
    ]

    def finance_log_month(HttpServletRequest req) {
        QueryBuilder queryBuilder = Web.fillTimeBetween(req)
        Map result = Crud.list(req, finance_monthReport(), queryBuilder.get(), ALL_FIELD, SJ_DESC);
        def list = result.get('data')
        Map data = new HashMap();
        data.put('list', list)
        data.put('heads', [inc: INC_HEADS, dec: DEC_HEADS])
        result.put('data', data)
        // 游戏列表
//        def map = new HashMap()
//        adminMongo.getCollection('games').find().each {
//            BasicDBObject obj ->
//                map.put(obj['_id'].toString(), obj['name'].toString())
//        }
//        result.put('gameList',map)
        return result;
    }

    //财务充值统计
    def finance_charge_log(HttpServletRequest req) {
        QueryBuilder queryBuilder = Web.fillTimeBetween(req).and('type').is('finance')
        //super.list(req, queryBuilder.get())
        [code: 1, data: table().find(queryBuilder.get(), ALL_FIELD).sort(SJ_DESC).limit(800).toArray()]
    }

    //主播月vc统计
    def finance_log_month_star_vc(HttpServletRequest req) {
        QueryBuilder queryBuilder = Web.fillTimeBetween(req)
        def field = $$(star: 1, date: 1, timestamp: 1)
        Crud.list(req, finance_monthReport(), queryBuilder.get(), field, SJ_DESC);
    }

    //礼物消费比例
    def finance_log_month_gift(HttpServletRequest req) {
        QueryBuilder queryBuilder = Web.fillTimeBetween(req)
        def field = $$(gifts: 1, date: 1, timestamp: 1)
        Crud.list(req, finance_monthReport(), queryBuilder.get(), field, SJ_DESC);
    }

    //充值柠檬币比例表
    def charge_coin_log(HttpServletRequest req) {
        QueryBuilder queryBuilder = Web.fillTimeBetween(req)
        def field = $$(charge_cny: 1, cut_charge_cny: 1, begin_surplus: 1, charge_coin: 1, inc_coin: 1, inc_total: 1, dec_total: 1, end_surplus: 1, date: 1, hand_cut_coin: 1)
        //Crud.list(req, finance_daily_log(), queryBuilder.get(), field, SJ_DESC);
        [code: 1, data: finance_daily_log().find(queryBuilder.get(), field).sort(SJ_DESC).limit(800).toArray()]
    }

    def buy_car_records(HttpServletRequest req) {
        Crud.list(req, logMongo.getCollection('room_cost'),
                Web.fillTimeBetween(req).and('type').is('buy_car').get(), ALL_FIELD, SJ_DESC)
    }

    def sign_static(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        def qd = req.getParameter('qd') as String
        if (StringUtils.isNotBlank(qd)) {
            query.and('qd').is(qd)
        }
        def version = req.getParameter('version') as String
        if (StringUtils.isNotBlank(version)) {
            query.and('version').is(version)
        }
        if (StringUtils.isBlank(qd) && StringUtils.isBlank(version) ||
                (StringUtils.isNotBlank(qd) && StringUtils.isBlank(version))) {
            return Crud.list(req, adminMongo.getCollection('stat_sign'), query.get(), ALL_FIELD, $$(timestamp: -1, user_count: -1)) { List<BasicDBObject> list ->
                for (BasicDBObject obj : list) {
                    def qid = obj.get('qd') as String
                    def name = channel().findOne(new BasicDBObject(_id: qid))?.get('name')
                    obj.put('qname', name ?: '')
                }
            }
        }
        def list = adminMongo.getCollection('stat_sign').find(query.get(), ALL_FIELD).sort($$(timestamp: -1, user_count: -1)).toArray()
        for (DBObject obj : list) {
            def qid = obj.get('qd') as String
            def name = channel().findOne(new BasicDBObject(_id: qid))?.get('name')
            obj.put('qname', name ?: '')
        }
        [code: 1, data: list]
    }

    def sign_total_static(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        def pc = channel().find($$(client: '1'), $$(_id: 1)).collect { it[_id] }
        def result = []
        def map = new TreeMap(), award_map = new HashMap()
        adminMongo.getCollection('stat_sign').find(query.get(), $$(version: 0)).sort($$(timestamp: -1, user_count: -1)).toArray().each { BasicDBObject obj ->
            def timestamp = obj.get('timestamp') as Long
            def count = obj.get('user_count') as Integer
            def awards = obj.get('award') as Map
            def data = map.get(timestamp) as Integer[]
            def pc_award = award_map.get(timestamp + "_pc") as Map
            def mobile_award = award_map.get(timestamp + "_mob") as Map
            if (data == null) {
                data = [0, 0] as Integer[]
                map.put(timestamp, data)
            }
            if (pc_award == null) {
                pc_award = new HashMap()
                award_map.put(timestamp + "_pc", pc_award)
            }
            if (mobile_award == null) {
                mobile_award = new HashMap()
                award_map.put(timestamp + "_mob", mobile_award)
            }
            def qid = (obj.get('qd') as String) ?: 'MM'
            if (!pc.contains(qid)) {
                data[1] = data[1] + count
                awards?.each { String k, Integer v ->
                    def val = (mobile_award.get(k) ?: 0) as Integer
                    mobile_award.put(k, v + val)
                }
            } else {
                data[0] = data[0] + count
                awards?.each { String k, Integer v ->
                    def val = (pc_award.get(k) ?: 0) as Integer
                    pc_award.put(k, v + val)
                }
            }
        }
        for (Map.Entry entry : map.entrySet()) {
            def key = entry.getKey() as Long
            def value = entry.getValue() as Integer[]
            result.add(0, [timestamp   : key,
                           pc          : value[0],
                           pc_award    : award_map.get(key + "_pc"),
                           mobile      : value[1],
                           mobile_award: award_map.get(key + "_mob")])
        }
        [code: 1, data: result]
    }

    /**
     * 运营数据总表
     * @param req
     */
    def total_report(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        query.put('type').is('allreport')
        Crud.list(req, adminMongo.getCollection('stat_report'), query.get(), ALL_FIELD, SJ_DESC)
    }

    /**
     * PC运营数据
     * @param req
     */
    def total_pc_report(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        query.put('type').is('pcreport')
        Crud.list(req, adminMongo.getCollection('stat_report'), query.get(), ALL_FIELD, SJ_DESC)
    }

    /**
     * 手机运营数据
     * @param req
     */
    def total_mobile_report(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        query.put('type').is('mobilereport')
        Crud.list(req, adminMongo.getCollection('stat_report'), query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            for (BasicDBObject obj : list) {
                //获取30日内累计新增激活总数
                def timestamp = obj.get('timestamp') as Long
                def start = timestamp - 30 * 24 * 60 * 60 * 1000L
                adminMongo.getCollection('stat_report').aggregate(
                        $$('$match', [type: 'mobilereport', timestamp: [$gt: start, $lte: timestamp]]),
                        $$('$project', [_id: '$type', new_active: '$new_active', active: '$active']),
                        $$('$group', [_id: '$_id', total_new_active: [$sum: '$new_active'], total_active: [$sum: '$active']])
                ).results().each { BasicDBObject item ->
                    def new_active30 = item.get('total_new_active') ?: 0
                    def active30 = item.get('total_active') ?: 0
                    obj.put('new_active30', new_active30)
                    obj.put('active30', active30)
                }
            }
        }
    }

    def coin_bean_static(HttpServletRequest req) {
        def query = new QueryBuilder()
        def stime = getStime(req)
        if (stime != null) {
            query.and('_id').greaterThanEquals('finance_' + stime.format('yyyyMMdd'))
        }
        def etime = getEtime(req)
        if (etime != null) {
            query.and('_id').lessThan('finance_' + etime.format('yyyyMMdd'))
        }
        Crud.list(req, adminMongo.getCollection('finance_dailyReport'), query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> data ->
            for (BasicDBObject obj : data) {
                def id = obj.remove('_id') as String
                obj.append('_id', id?.replace('finance_', ''))
            }
        }
    }

    /**
     * 主播直播统计
     * @param req
     * @return
     */
    def star_live_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        Integer _id = req['_id'] as Integer
        if (_id != null) {
            query.and('user_id').is(_id)
        }
        Crud.list(req, stat_lives(), query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> data ->
            for (BasicDBObject obj : data) {
                def user = users().findOne($$('_id', obj['user_id'] as Integer), $$(nick_name: 1, 'finance.bean_count_total': 1))
                if (user != null)
                    obj.putAll(user)
            }
        }
    }

    def cost_log_export(HttpServletRequest req, HttpServletResponse res) {
        Date[] dates = ExportUtils.checkDate(Web.getStime(req), Web.getEtime(req))
        def query = Web.fillTimeBetween(dates[0], dates[1]).and('type').is('allcost')
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
        def bodyBuf = new StringBuffer()
        def map = new HashMap()
        def result = ExportUtils.list(req, table(), query.get(), ALL_FIELD, SJ_DESC,) { List<BasicDBObject> list ->
            cost_log_closure(map).call(list)
            for (BasicDBObject obj : list) {
                def timestamp = obj[timestamp] as String
                if (StringUtils.isNotBlank(timestamp)) {
                    bodyBuf.append(sdf.format(new Date(Long.parseLong(timestamp))))
                }
                bodyBuf.append(",")
                ["send_gift", "song", "broadcast", "open_egg", "grab_sofa", "user_cost"].each {
                    def key = it as String
                    def cost = '0', user = '0', avg = 0, costKey = "cost", userKey = "user"
                    if (key.equals("open_egg")) {
                        costKey = "total_price"
                        userKey = "users"
                    }
                    if (key.equals("grab_sofa")) {
                        userKey = "users"
                    }
                    cost = obj[key]?.getAt(costKey) as String
                    user = obj[key]?.getAt(userKey) as String
                    cost = (cost != null) ? cost : "0"
                    user = (user != null) ? user : "0"
                    if (StringUtils.isNotBlank(cost) && StringUtils.isNotBlank(user) && Integer.parseInt(user) > 0) {
                        avg = Integer.parseInt(cost) / Integer.parseInt(user)
                    }
                    bodyBuf.append(cost).append(",").append(user).append(",").append(avg).append(",")
                }
                def bean = 0, coin = 0
                if (obj["user_remain"] instanceof BasicDBObject) {
                    bean = obj["user_remain"]?.getAt("bean")
                    coin = obj["user_remain"]?.getAt("coin")
                    bean = (bean == null) ? "0" : bean
                    coin = (coin == null) ? "0" : coin
                }
                bodyBuf.append(bean).append(",").append(coin)
                bodyBuf.append(ExportUtils.ls)
            }
        } as Map
        def count = result['count'] as String
        def page = result['all_page'] as String
        StringBuffer buf = ExportUtils.generateTitle(dates, count, page)
        String filename = ExportUtils.generateFilename(dates, "消费统计");
        String[] titles = ["日期", "礼物总额", "礼物人数", "礼物人均", "点歌总额", "点歌人数", "点歌人均",
                           "广播总额", "广播人数", "广播人均", "砸蛋总额", "砸蛋人数", "砸蛋人均", "抢座总额", "抢座人数", "抢座人均",
                           "累计总额", "累计人数", "累计人均", "剩余维C", "剩余金额"]
        buf.append(titles.join(",")).append(ExportUtils.ls)
        ExportUtils.response(res, filename, buf.append(bodyBuf).toString())
    }

    def finance_log_export(HttpServletRequest req, HttpServletResponse res) {
        Date[] dates = ExportUtils.checkDate(Web.getStime(req), Web.getEtime(req))
        def query = Web.fillTimeBetween(dates[0], dates[1]).and('type').is('finance')
        def bodyBuf = new StringBuffer()
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
        def result = ExportUtils.list(req, table(), query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            for (BasicDBObject obj : list) {
                def timestamp = obj[timestamp] as String//充值日期
                def total = obj['total']//总计（元）
                def total_coin = obj['total_coin']//总维C
                def all_person = 0 as Integer
                if (StringUtils.isNotBlank(timestamp)) {
                    bodyBuf.append(sdf.format(new Date(Long.parseLong(timestamp))))
                }
                bodyBuf.append(',').append(total_coin != null ? total_coin : 0).append(',')
                        .append(total != null ? total : 0).append(',')
                StringBuffer buffer = new StringBuffer()
                [PayType.PC_LIST*.id, PayType.MOBILE_LIST*.id, PayType.QD_LIST*.id].each { List<String> ids ->
                    buffer.append(',').append('mark')
                    ids.each { String id ->
                        buffer.append(',')
                        def item = obj[id.toLowerCase()]
                        def cny = '0', user = '0', avg = 0
                        cny = item?.getAt('cny') as String
                        user = item?.getAt('user') as String
                        cny = (cny != null) ? cny : '0'
                        user = (user != null) ? user : '0'
                        if (Integer.parseInt(user) > 0) {
                            avg = Double.parseDouble(cny) / Integer.parseInt(user)
                        }
                        all_person += Integer.parseInt(user)
                        buffer.append(cny).append(',').append(user).append(',').append(avg)
                    }
                }
                bodyBuf.append(all_person).append(buffer).append(ExportUtils.ls)
            }
        } as Map
        def count = result['count'] as String
        def page = result['all_page'] as String
        StringBuffer titleBuf = ExportUtils.generateTitle(dates, count, page)
        String filename = ExportUtils.generateFilename(dates, "充值统计");
        def titles = ["充值日期", "总维C", "总计（元）", "付费人数（人）"] as List
        [PayType.PC_LIST*.desc, PayType.MOBILE_LIST*.desc, PayType.QD_LIST*.desc].each { List descs ->
            titles.add('mark')//三个类别的分割
            descs.each { String desc -> ['金额', '人数', '人均'].each { titles.add(desc + it) } }
        }
        titleBuf.append(titles.join(",")).append(ExportUtils.ls)
        ExportUtils.response(res, filename, titleBuf.append(bodyBuf).toString())
    }

    def gift_log_export(HttpServletRequest req, HttpServletResponse res) {
        Date[] dates = ExportUtils.checkDate(Web.getStime(req), Web.getEtime(req))
        def query = Web.fillTimeBetween(dates[0], dates[1]).and('type').is('gift_detail')
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
        def bodyBuf = new StringBuffer()
        def result = ExportUtils.list(req, table(), query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            gift_log_closure().call(list)
            for (BasicDBObject obj : list) {
                [timestamp, 'gift_id', 'name', 'category_name', 'count', 'cost'].each { String key ->
                    def value = obj[key] as String
                    if (StringUtils.isBlank(value)) {
                        value = ''
                    }
                    if (timestamp.equals(key)) {
                        value = sdf.format(new Date(Long.parseLong(value)))
                    }
                    bodyBuf.append(value).append(',')
                }
                bodyBuf.append(ExportUtils.ls)
            }
        } as Map
        def count = result['count'] as String
        def page = result['all_page'] as String
        StringBuffer buf = ExportUtils.generateTitle(dates, count, page)
        String filename = ExportUtils.generateFilename(dates, "礼物统计");
        def titles = ["消费日期", "礼物ID", "礼物名称", "分类名称", "消费数量", "消费金额（元）"]
        buf.append(titles.join(",")).append(ExportUtils.ls)
        ExportUtils.response(res, filename, buf.append(bodyBuf).toString())
    }

    def login_log_export(HttpServletRequest req, HttpServletResponse res) {
        Date[] dates = ExportUtils.checkDate(Web.getStime(req), Web.getEtime(req))
        def query = Web.fillTimeBetween(dates[0], dates[1]).and('type').is('login')
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
        def bodyBuf = new StringBuffer()
        def result = ExportUtils.list(req, table(), query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            for (BasicDBObject obj : list) {
                def timestamp = obj[timestamp] as String
                if (StringUtils.isNotBlank(timestamp)) {
                    bodyBuf.append(sdf.format(new Date(Long.parseLong(timestamp))))
                }
                bodyBuf.append(",")
                def total = obj['total'] as Number
                def mobile = obj['mobile'] as Number
                def pc = 0
                total = (total == null) ? 0 : total
                mobile = (mobile == null) ? 0 : mobile
                pc = total - mobile
                pc = (pc >= 0) ? pc : 0
                bodyBuf.append(pc).append(',').append(mobile).append(',').append(total).append(ExportUtils.ls)
            }
        } as Map
        def count = result['count'] as String
        def page = result['all_page'] as String
        StringBuffer buf = ExportUtils.generateTitle(dates, count, page)
        String filename = ExportUtils.generateFilename(dates, "登录日统计");
        String[] titles = ["日期", "PC人数", "手机人数", "总计"]
        buf.append(titles.join(",")).append(ExportUtils.ls)
        ExportUtils.response(res, filename, buf.append(bodyBuf).toString())
    }

    def login_month_log_export(HttpServletRequest req, HttpServletResponse res) {
        Date[] dates = ExportUtils.checkDate(Web.getStime(req), Web.getEtime(req))
        def query = Web.fillTimeBetween(dates[0], dates[1]).and('type').is('login')
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM")
        def bodyBuf = new StringBuffer()
        def result = ExportUtils.list(req, adminMongo.getCollection('stat_month'), query.get(),
                ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            for (BasicDBObject obj : list) {
                def timestamp = obj[timestamp] as String
                if (StringUtils.isNotBlank(timestamp)) {
                    bodyBuf.append(sdf.format(new Date(Long.parseLong(timestamp))))
                }
                bodyBuf.append(",")
                def total = obj['total'] as Number
                def mobile = obj['mobile'] as Number
                def pc = 0
                total = (total == null) ? 0 : total
                mobile = (mobile == null) ? 0 : mobile
                pc = total - mobile
                pc = (pc >= 0) ? pc : 0
                bodyBuf.append(pc).append(',').append(mobile).append(',').append(total).append(ExportUtils.ls)
            }
        } as Map
        def count = result['count'] as String
        def page = result['all_page'] as String
        StringBuffer buf = ExportUtils.generateTitle(dates, count, page)
        String filename = ExportUtils.generateFilename(dates, "登录月统计");
        String[] titles = ["日期", "PC人数", "手机人数", "总计"]
        buf.append(titles.join(",")).append(ExportUtils.ls)
        ExportUtils.response(res, filename, buf.append(bodyBuf).toString())
    }

    def pool_log_export(HttpServletRequest req, HttpServletResponse res) {
        Date[] dates = ExportUtils.checkDate(Web.getStime(req), Web.getEtime(req))
        def query = Web.fillTimeBetween(dates[0], dates[1]).and('type').is('luck')
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
        def bodyBuf = new StringBuffer()
        def result = ExportUtils.list(req, table(), query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            for (BasicDBObject obj : list) {
                def timestamp = obj[timestamp] as String
                if (StringUtils.isNotBlank(timestamp)) {
                    bodyBuf.append(sdf.format(new Date(Long.parseLong(timestamp))))
                }
                bodyBuf.append(",")
                def total = obj['in'] as Number
                def mobile = obj['out'] as Number
                def pc = 0
                total = (total == null) ? 0 : total
                mobile = (mobile == null) ? 0 : mobile
                pc = total - mobile
                bodyBuf.append(pc).append(',').append(mobile).append(',').append(total).append(ExportUtils.ls)
            }
        } as Map
        def count = result['count'] as String
        def page = result['all_page'] as String
        StringBuffer buf = ExportUtils.generateTitle(dates, count, page)
        String filename = ExportUtils.generateFilename(dates, "奖池信息");
        String[] titles = ["日期", "进入金额（元）", "出奖金额（元）", "净流入（元）"]
        buf.append(titles.join(",")).append(ExportUtils.ls)
        ExportUtils.response(res, filename, buf.append(bodyBuf).toString())
    }

    def pk_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        Crud.list(req, table(), query.and('type').is('allpk').get(), ALL_FIELD, SJ_DESC)
    }

    /**
     * 游戏兑换每日统计
     * @param req
     * @return
     */
    def game_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        Crud.list(req, adminMongo.getCollection('finance_dailyReport'), query.get(), $$('kunbo_game_coin': 1, texasholdem_game_coin: 1, fishing_game_coin: 1, niuniu_game_coin: 1,
                kunbo_subtract_coin: 1, texasholdem_subtract_coin: 1, fishing_subtract_coin: 1, niuniu_subtract_coin: 1, timestamp: 1), SJ_DESC)
    }

    //更改统计内容
    private cost_log_closure(Map map) {
        { List<BasicDBObject> list ->
            if (list != null && list.size() > 0) {
                def props = (map.get('key') ?: new ArrayList<String>()) as List;
                def names = (map.get('name') ?: new ArrayList<String>()) as List;
                list.each { BasicDBObject obj ->
                    def keys = obj.keySet() as Set
                    for (String key : keys) {
                        if (!['_id', 'user_cost', 'type', 'user_remain', 'timestamp'].contains(key) && !props.contains(key)) {
                            props.add(key)
                            names.add(key)
                        }
                    }
                }
            }
        }
    }

    private gift_log_closure() {
        { List<BasicDBObject> list ->
            //查礼物类型
            Map<Integer, String> categoryMap = new HashMap<Integer, String>();
            DBCursor cursor = adminMongo.getCollection('gift_categories').find(null, new BasicDBObject(['_id': 1, 'name': 1]));
            while (cursor.hasNext()) {
                def cateObj = cursor.next() as DBObject;
                def cateId = cateObj.get('_id') as Integer
                def cateName = cateObj.get('name') as String
                categoryMap.put(cateId, cateName)
            }
            Map<Integer, String> giftMap = new HashMap<Integer, String>()
            for (BasicDBObject obj : list) {
                if (!obj.get('category_name')) {
                    def giftId = obj.get('gift_id') as Integer
                    def cateName
                    if (!giftMap.containsKey(giftId)) {
                        def gift = adminMongo.getCollection('gifts').findOne(new BasicDBObject('_id', giftId))
                        def cateId = gift?.get('category_id') as Integer
                        cateName = categoryMap.get(cateId)
                        giftMap.put(giftId, cateName)
                    } else {
                        cateName = giftMap.get(giftId)
                    }
                    obj.put('category_name', cateName)
                }
            }
        }
    }

    /**
     * 游戏统计
     * * @param req
     * @return
     */
    def game_stat(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('game').get())
    }

    /**
     * 签到记录
     * @param req
     */
    def check_in_logs(HttpServletRequest req) {
        super.list(req, Web.fillTimeBetween(req).and('type').is('check_in').get())
    }

    /**
     * 任务统计
     * @param req
     */
    // 任务名
    private final static String MISSION_ID = 'six_free_sun'

    def mission_logs(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).and('type').is('mission').get()
        def map = new HashMap()
        adminMongo.getCollection('missions').find().each {
            BasicDBObject obj ->
                map.put(obj['_id'].toString(), obj['title'].toString())
        }
        // 每日6次免费阳光领取单独加
        map.put(MISSION_ID,'免费6次阳光')
        def list = table().find(query).sort($$('timestamp':-1)).toArray()
        return [keys: map, data: list]
    }

    /**
     * 任务明细
     * @param req
     * @return
     */
    def mission_logs_detail(HttpServletRequest req){
        logger.debug('Received mission_logs_detail params is {}',req.getParameterMap())
        def query = Web.fillTimeBetween(req)
        def userId = ServletRequestUtils.getIntParameter(req,'_id',0)
        if(userId > 0){
            query.and('user_id').is(userId)
        }
         Crud.list(req, logMongo.getCollection('mission_logs'), query.get(), ALL_FIELD, MongoKey.SJ_DESC);
    }

    /**
     * 签到统计
     * @param req
     */
    def check_in_logs_detail(HttpServletRequest req){
        logger.debug('Received check_in_logs_detail params is {}',req.getParameterMap())
        def query = Web.fillTimeBetween(req)
        def userId = ServletRequestUtils.getIntParameter(req,'_id',0)
        if(userId > 0){
            query.and('user_id').is(userId)
        }
        Crud.list(req, logMongo.getCollection('sign_logs'), query.get(), ALL_FIELD, MongoKey.SJ_DESC);
    }

}
