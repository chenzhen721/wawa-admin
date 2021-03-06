package com.wawa.admin.web

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.wawa.base.BaseController
import com.wawa.base.anno.RestWithSession
import com.wawa.common.doc.IMessageCode
import com.wawa.base.Crud
import com.wawa.admin.web.crud.MessageController
import com.wawa.api.Web
import com.wawa.common.util.ExportUtils
import com.wawa.common.util.KeyUtils
import com.wawa.model.ExportType
import com.wawa.model.OpType
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.math.NumberUtils
import org.springframework.web.bind.ServletRequestUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

import static com.wawa.common.doc.MongoKey.*
import static com.wawa.common.util.WebUtils.$$

/**
 * date: 13-3-28 下午2:31
 * @author: yangyang.cong@ttpod.com
 */
//@Rest
@RestWithSession
class FinanceController extends BaseController {
    DBCollection table() { adminMongo.getCollection('finance_log') }

    DBCollection basicSalary() { adminMongo.getCollection('basic_salary') }

    def list(HttpServletRequest req) {

        def q = Web.fillTimeBetween(req)
        intQuery(q, req, "user_id")
        stringQuery(q, req, _id)
        stringQuery(q, req, 'via')
        intQuery(q, req, 'to_id')
        stringQuery(q, req, 'rid')
        stringQuery(q, req, '_id')
        String gte = req.getParameter('coingte')
        if (StringUtils.isNotBlank(gte)) {
            q.and('diamond').greaterThanEquals(Integer.valueOf(gte))
        }
        Crud.list(req, table(), q.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> data ->
            def users = users()
            for (BasicDBObject obj : data) { // 更新昵称 http://192.168.1.181/redmine/issues/4086
                def user = users.findOne(obj['user_id'], new BasicDBObject(nick_name: 1, finance: 1))
                if (user) {
                    user.removeField(_id)
                    obj.putAll(user)
                }
            }
        }
    }

    def add(HttpServletRequest req) {
        String input = req.getParameter(auth_code)
        /*if (codeVerifError(req, input)) {
            return [code: 30419, msg: '验证码错误']

        }*/
        String idStr = req.getParameter(_id)
        if (StringUtils.isBlank(idStr)) {
            return [code: 0]
        }
        def ids = idStr.split(',')
        ids.each {String str ->
            if (StringUtils.isNotBlank(str)) {
                Integer id = Integer.valueOf(str.trim())
                Long num = req.getParameter('num') as Long
                String remark = req.getParameter('remark') as String
                def orderId = "${id}_${num}_Admin_${System.currentTimeMillis()}".toString()
                def logWithId = new BasicDBObject(
                        _id: orderId,//订单号
                        user_id: id,
                        diamond: num,
                        via: 'Admin',
                        session: Web.getSession(),
                        remark: remark
                )
                if (addDiamond(id, num, logWithId)) {
                    Crud.opLog(OpType.finance_add, [user_id: id, order_id: orderId, coin: num, remark: remark])
                }
            }
        }
        [code: 1]
    }

    /**
     * 支付补单
     */
    def repair_order(HttpServletRequest req) {
        def order_id = req.getParameter('order_id')
        def userId = req.getParameter('user_id') as Integer
        def target = req.getParameter('target') as Integer
        def cny = req.getParameter('cny') as Integer
        def diamond = req.getParameter('coin') as Long
        def via = req.getParameter('via')
        def shop = req.getParameter('shop')
        def broker = req.getParameter('broker')
        def transaction_id = req.getParameter('transaction_id')

        if (adminMongo.getCollection('finance_log').count(new BasicDBObject("_id", order_id)) == 1) {
            return [code: 30414]
        }
        def logWithId = new BasicDBObject(
                _id: order_id,
                user_id: userId,
                cny: cny,
                diamond: diamond,
                via: via,
                shop: shop,//商品类型
                ext: broker, // 如果经纪人id 填写在这里
                transactionId: transaction_id,
                c: 10000,// 充值成功 消耗时间
                repair_time: System.currentTimeMillis()
        )
        if (Web.getTime(req, 'date')) {
            logWithId.put(timestamp, Web.getTime(req, 'date').getTime())
        }
        def to_id = userId
        if (target != null && target != userId) {
            logWithId.put('to_id', target)
            to_id = target
        }
        if (addDiamond(to_id, diamond, logWithId)) {
            Crud.opLog(OpType.repair_order, logWithId)
            return [code: 1]
        }
        return [code: 30414]
    }

    /**
     * 查询延迟订单
     * @param req
     * @return
     */
    def query_delay_order(HttpServletRequest req) {
        def order_id = req.getParameter('order_id') as String
        if (StringUtils.isEmpty(order_id)) {
            return Web.missParam();
        }
        Map order = Web.api("pay/find_delay_order?order_id=${order_id}") as Map
        if (order == null) {
            return [code: 1, data: null]
        }
        Map result = new HashMap();
        if (order_id.equals(order['orderId'] as String)) {
            String[] arr = order_id.split('_')
            Integer userId = arr[0] as Integer
            Long date = Long.parseLong(arr[1])
            Integer target = getTarget(arr) ?: userId
            String broker = getBroker(arr)
            result.put('order_id', order['orderId'])
            result.put('user_id', userId)
            result.put('target', target)
            result.put('broker', broker)
            Double cny = Double.valueOf(order['cny'] as String)
            Integer rate = Integer.valueOf(order['rate'] as String)
            Long coin = cny * rate as Long
            result.put('cny', cny)
            result.put('coin', coin)
            result.put('via', order['via'])
            result.put('transaction_id', order['tradeNo'])
            result.put('date', date)
        }

        return [code: 1, data: result]
    }

    /**
     * 自动批量补单
     * @param req
     * @return
     */
    def auto_repair_order(HttpServletRequest req) {
        Map result = Web.api("pay/delay_order_fix") as Map
        Crud.opLog(OpType.auto_batch_repair_order, result)
        return [code: 1, data: result]
    }

    public static Integer getTarget(String[] arr) {
        String target = (arr.length == 3) ? arr[2] : null;
        if (NumberUtils.isNumber(target)) {
            return Integer.valueOf(target);
        }
        return null;
    }

    public static String getBroker(String[] arr) {
        return (arr.length == 4) ? arr[3] : null;
    }

    def cut_coin(HttpServletRequest req) {
        String input = req.getParameter(auth_code)
        /*if (codeVerifError(req, input)) {
            return [code: 30419, msg: '验证码错误']
        }*/
        Integer id = req.getParameter(_id) as Integer
        Long num = req.getParameter('num') as Long
        if (num >= 0) {
            return [code: 0, msg: 'num must < 0']
        }
        def remark = req.getParameter('remark') as String
        if (users().update(new BasicDBObject(_id, id).append('finance.diamond_count', [$gte: 0 - num])
                , new BasicDBObject('$inc', ['finance.diamond_count': num]), false, false, writeConcern).getN() == 1) {
            Crud.opLog(OpType.finance_cut_coin, [user_id: id, coin: num, remark: remark])
            return [code: 1]
        }
        [code: 0, msg: '再扣就负了']
    }

    // 减币记录
    def cut_list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        query.put('type').is(OpType.finance_cut_coin.name())
        def oid = req.getParameter('oid') as String
        if (StringUtils.isNotBlank(oid)) query.put('session._id').is(oid)
        def uid = req.getParameter('uid') as Integer
        if (uid != null) query.put('data.user_id').is(uid)
        def map = Crud.opLoglist(req, query.get()) as Map
        def list = map.get('data') as List<BasicDBObject>
        list.each { BasicDBObject obj ->
            def data = obj.get('data') as Map
            if (data != null) {
                def user_id = data.get('user_id') as Integer
                def nick_name = users().findOne($$(_id: user_id), $$(nick_name: 1))?.get('nick_name')
                data.put('nick_name', nick_name)
            }
        }
        return map
    }



    /**
     * 充值消费比例
     * @param req
     */
    def pay_cost_rate(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).put("type").is("alllogin")
        def stat_daily = adminMongo.getCollection("stat_daily")
        Crud.list(req, stat_daily, query.get(),
                $$([login_total: 1, timestamp: 1]), SJ_DESC) { List<BasicDBObject> list ->
            for (BasicDBObject obj : list) {
                def login_total = obj.remove("login_total")
                def login = login_total?.getAt("daylogin") as Integer
                def timestamp = obj.get("timestamp") as Long
                if (login == null) login = 0
                if (login != 0) {
                    def prefix = new Date(timestamp).format("yyyyMMdd_") as String
                    def payObj = stat_daily.findOne($$(_id, prefix + "allpay"),
                            $$([user_pay: 1, user_pay_pc: 1, user_pay_mobile: 1]))
                    def all_pay = (payObj?.get("user_pay"))?.getAt("user") as Integer
                    def pc_pay = (payObj?.get("user_pay_pc"))?.getAt("user") as Integer
                    def mobile_pay = (payObj?.get("user_pay_mobile"))?.getAt("user") as Integer
                    def costObj = stat_daily.findOne($$(_id, prefix + "allcost"), $$([user_cost: 1]))
                    def all_cost = (costObj?.get("user_cost"))?.getAt("user") as Integer
                    obj.put("all_pay_rate", (all_pay != null ? all_pay : 0) / login)
                    obj.put("cost_rate", (all_cost != null ? all_cost : 0) / login)
                    obj.put("pc_pay_rate", (pc_pay != null ? pc_pay : 0) / login)
                    obj.put("mobile_pay_rate", (mobile_pay != null ? mobile_pay : 0) / login)
                }
            }
        }
    }



    static class PayStat {
        final Set user = new HashSet(2000)
        final AtomicInteger count = new AtomicInteger()
        final AtomicLong coin = new AtomicLong()
        def BigDecimal cny = new BigDecimal(0)

        def toMap() { [user: user.size(), count: count.get(), coin: coin.get(), cny: cny.doubleValue()] }

        def add(def user_id, BigDecimal deltaCny, Long deltaCoin) {
            count.incrementAndGet()
            user.add(user_id)
            cny = cny.add(deltaCny)
            coin.addAndGet(deltaCoin)
        }
    }

    def pay_all_delta(HttpServletRequest req) {
        if (StringUtils.isBlank((String) req.getParameter('stime'))) {
            return [code: 0, msg: 'stime is must.']
        }
        def timeQuery = Web.fillTimeBetween(req)
        pay_all_delta_service(timeQuery)
    }

    def pay_types() {
        def map = new HashMap()
        ["pc": "1", "mobile": "2"].each { String k, String client ->
            def typeQuery = new BasicDBObject([client: client, _id: [$ne: 'Admin']])
            def v = [] as List
            adminMongo.getCollection("channel_pay").find(typeQuery)
                    .toArray().collect { DBObject obj ->
                def id = obj.get('_id') as String
                def desc = obj.get('name') as String
                v.add([id: id, desc: desc])
            }
            map.put(k, v)
        }

        def chargeTypeMap = new HashMap()
        ["direct": "1", "proxy": "2"].each { String k, String charge_type ->
            def typeQuery = new BasicDBObject([charge_type: charge_type, _id: [$ne: 'Admin']])
            def v = [] as List
            adminMongo.getCollection("channel_pay").find(typeQuery)
                    .toArray().collect { DBObject obj ->
                def id = obj.get('_id') as String
                def desc = obj.get('name') as String
                v.add([id: id, desc: desc])
            }
            chargeTypeMap.put(k, v)
        }
        [code: 1, data: [pc: map.get('pc'), moblie: map.get('mobile'), direct: chargeTypeMap.get('direct'), proxy: chargeTypeMap.get('proxy')]]
    }

    static final Long DAY_MILL = 24 * 3600 * 1000L

    def change_vip(HttpServletRequest req) {
        def day = req.getParameter('day') as Integer
        def userId = req.getParameter(_id) as Integer
        Long delta_mills = day * DAY_MILL

        Integer vip_type = "2".equals(req.getParameter('type')) ? 2 : 1

        def users = users()
        def query = new BasicDBObject(_id, userId)
        def user = users.findOne(query, $$("vip_expires", 1).append("vip", 1))
        if (null == user) {
            return [code: 0, msg: "user with id ${userId} not found."]
        } else {
            Integer oldVip = (Integer) user.get("vip")
            if (null != oldVip && oldVip != vip_type) {
                users.update(query, $$($unset, (Map) [vip: 1, vip_expires: 1, vip_hiding: 1]))
                user.removeField("vip")
                user.removeField("vip_expires")
            }
        }
        def vip_expires = user.get("vip_expires") as Long
        Long now = System.currentTimeMillis()
        if (vip_expires == null) {
            if (delta_mills <= 0) { //nothing to do.
                return IMessageCode.OK
            }
            vip_expires = now
        }
        def total = vip_expires + delta_mills

        def key = KeyUtils.USER.vip(userId)
        def vipLimitKey = KeyUtils.USER.vip_limit(userId)
        if (total > now) {
            users.update(query, new BasicDBObject($set, (Map) [vip: vip_type, vip_expires: total]))
            // vip .toString
            def second = (total - now).intdiv(1000).longValue()
            def valOp = userRedis.opsForValue()
            valOp.set(key, vip_type.toString(), second, TimeUnit.SECONDS)
            if (vip_type == 2 && !userRedis.hasKey(vipLimitKey)) { // 10 times  to shutup or forbid erverday.
                valOp.set(vipLimitKey, "10", second, TimeUnit.SECONDS)
            }
        } else {
            users.update(query, new BasicDBObject($unset, (Map) [vip: 1, vip_expires: 1, vip_hiding: 1]))
            userRedis.delete([key, vipLimitKey])
        }
        Crud.opLog(OpType.finance_change_vip, [user_id: userId, day: day, vip: vip_type])
        IMessageCode.OK
    }


    def donate_car(HttpServletRequest req) {
        def day = req.getParameter('day') as Integer
        def userId = req.getParameter(_id) as Integer
        def carId = req.getParameter('car_id') as String
        def carIdInt = Integer.valueOf(carId)
        Long delta_mills = day * DAY_MILL

        String entryKey = "car." + carId
        def users = users()
        Long now = System.currentTimeMillis()
        if (users.update($$(_id, userId).append(entryKey, [$not: [$gte: now]]),
                $$($set, $$(entryKey, now + delta_mills))).getN() == 1
                || 1 == users.update($$(_id, userId).append(entryKey, [$gt: now]), $$($inc, $$(entryKey, delta_mills))).getN()) {
            def valOp = userRedis.opsForValue()
            String key = KeyUtils.USER.car(userId)
            String currRedis = valOp.get(key)
            if (StringUtils.isNotBlank(currRedis)) {
                valOp.set(key, carId, delta_mills, TimeUnit.MILLISECONDS)
                users.update($$(_id, userId), $$($set, $$("car.curr", carIdInt)))
            } else if (carId.equals(currRedis)) {
                Long expSeconds = userRedis.getExpire(key) + delta_mills.intdiv(1000)
                userRedis.expire(key, expSeconds, TimeUnit.SECONDS)
            }
        }

        Crud.opLog(OpType.finance_donate_car, [user_id: userId, day: day, car_id: carIdInt])
        IMessageCode.OK
    }

    def donate_medal(HttpServletRequest req) {
        def medal_award_logs = logMongo.getCollection('medal_award_logs')
        def day = req.getParameter('day') as Integer
        def userIds = req.getParameter('ids')
        def medalId = Integer.valueOf(req.getParameter('medal_id') as String)
        Long delta_mills = day * DAY_MILL
        String entryKey = "medals." + medalId
        def users = users()
        Long now = System.currentTimeMillis()
        userIds.split(',').collect {
            def userId = it as Integer
            if (users.update($$(_id, userId).append(entryKey, [$not: [$gte: now]]),
                    $$($set, $$(entryKey, now + delta_mills))).getN() == 1
                    || 1 == users.update($$(_id, userId).append(entryKey, [$gt: now]), $$($inc, $$(entryKey, delta_mills))).getN()) {
                String medalsListkey = KeyUtils.MEDAL.medalsList(userId)
                userRedis.opsForSet().add(medalsListkey, medalId.toString())
                //徽章日志
                medal_award_logs.insert($$([_id      : userId + "_" + now,
                                            mid      : medalId,
                                            uid      : userId,
                                            timestamp: now, via: 'donate']
                ))
            }
        }
        Crud.opLog(OpType.finance_donate_medal, [userIds: userIds, day: day, medal_id: medalId])
        IMessageCode.OK
    }

    def donate_horn(HttpServletRequest req) {
        def num = req.getParameter('num') as Integer
        def userId = req.getParameter(_id) as Integer
        def row = users().update($$(_id, userId), $$($inc, $$('horn', num)), false, false, writeConcern).getN()
        if (1 == row) {
            Crud.opLog(OpType.finance_donate_horn, [user_id: userId, num: num])
        }
        [code: row]
    }

    @Resource
    MessageController messageController

    def donate_exp(HttpServletRequest req) {
        def num = req.getParameter('num') as Integer
        def userId = req.getParameter(_id) as Integer
        def query = $$(_id, userId)
        def update = $$($inc, $$('exp', num))
        def user = users().findAndModify(query, null, null, false, update, true, false)
        if (user != null) {
//            messageController.sendSingleMsg(userId, '经验奖励', "尊敬的么么用户，您好！恭喜你获得了${num}经验奖励！", MsgType.系统消息);
            Crud.opLog(OpType.finance_donate_exp, [user_id: userId, num: num])
        }
        [code: 1]
    }

    def pay_all_delta_export(HttpServletRequest req, HttpServletResponse res) {
        Date[] dates = ExportUtils.checkDate(Web.getStime(req), Web.getEtime(req))
        def query = Web.fillTimeBetween(dates[0], dates[1])
        def result = pay_all_delta_service(query) as Map
        def count = result['count'] as String
        def page = result['all_page'] as String
        StringBuffer buf = ExportUtils.generateTitle(dates, count, page)
        String filename = ExportUtils.generateFilename(dates, "充值概要");
        def titles = ['日期', '总计金额', '总计人数',
                      'PC全部金额', 'PC全部人数', 'PC全部人均', 'PC新增金额', 'PC新增人数', 'PC新增人均',
                      '手机全部金额', '手机全部人数', '手机全部人均', '手机新增金额', '手机新增人数', '手机新增人均',
                      '渠道全部金额', '渠道全部人数', '渠道全部人均', '渠道新增金额', '渠道新增人数', '渠道新增人均']
        buf.append(titles.join(",")).append(ExportUtils.ls)
        //获得值列表
        def timeLine = result['data']?.getAt('timeLine') as Map
        List<String> times = result['data']?.getAt('timeSort') as List
        for (String timeKey : times) {
            def obj = timeLine.get(timeKey) as Map
            def allCny = 0 as Number, allUser = 0 as Number
            StringBuffer itemBuffer = new StringBuffer()
            ['pc', 'mobile', 'qd'].each { String key ->
                ['all', 'delta'].each {
                    def item = (obj?.get(key) as Map)?.get(it) as Map
                    def cny = 0, user = 0, avg = 0
                    cny = item?.get('cny') as Number
                    user = item?.get('user') as Number
                    cny = (cny == null) ? 0 : cny
                    user = (user == null) ? 0 : user
                    if (user > 0) {
                        avg = cny / user
                    }
                    allCny += cny
                    allUser += user
                    itemBuffer.append(',').append(cny).append(',').append(user).append(',').append(avg)
                }
            }
            buf.append(timeKey).append(',').append(allCny).append(',').append(allUser)
                    .append(itemBuffer).append(ExportUtils.ls)
        }
        ExportUtils.response(res, filename, buf.toString())
    }

    /**
     * 今日充值
     * @param req
     * @return
     */
    def today_total(HttpServletRequest req) {
        Long today = new Date().clearTime().getTime();
        def query = $$('via': [$ne: 'Admin'], timestamp: [$gte: today, $lt: System.currentTimeMillis()])
        Long cny = 0;
        Long coin = 0;
        Long count = 0;
        Integer users = 0;
        table().aggregate(
                new BasicDBObject('$match', query),
                new BasicDBObject('$project', [user_id: '$user_id', cny: '$cny', coin: '$coin']),
                new BasicDBObject('$group', [_id: null, coin: [$sum: '$coin'], cny: [$sum: '$cny'], count: [$sum: 1], users: [$addToSet: '$user_id']])
        ).results().each {
            def obj = new BasicDBObject(it as Map)
            cny = obj?.get('cny') as Long;
            coin = obj?.get('coin') as Long;
            count = obj?.get('count') as Long;
            def userSet = new HashSet(obj?.get('users') as Set)
            users = userSet.size();
        }
        [code: 1, data: [cny: cny, coin: coin, count: count, users: users]]
    }

    private pay_all_delta_service(QueryBuilder query) {
        def coll = table()
        Map<String, Number> old_ids = new HashMap<String, Number>()
        coll.aggregate(
                new BasicDBObject('$match', new BasicDBObject('via', [$ne: 'Admin'])),
                new BasicDBObject('$project', [_id: '$user_id', timestamp: '$' + timestamp]),
                new BasicDBObject('$group', [_id: '$_id', timestamp: [$min: '$' + timestamp]])
        ).results().each {
            def obj = new BasicDBObject(it as Map)
            old_ids.put(obj.get('_id') as String, obj.get(timestamp) as Number)
        }

        def data = new HashMap()
        data.put('timeLine', new HashMap())
        data.put('total', new HashMap())
        ["pc": "1", "moblie": "2"].each { String k, String client ->
            def typeQuery = new BasicDBObject([client: client, _id: [$ne: 'Admin']])
            def v = [] as List
            adminMongo.getCollection("channel_pay").find(typeQuery)
                    .toArray().collect { DBObject obj ->
                v.add(obj.get('_id') as String)
            }
//        }
//        [pc    : PayType.PC_LIST*.id.toArray(new String[0]),
//         moblie: PayType.MOBILE_LIST*.id.toArray(new String[0]),
//         qd    : PayType.QD_LIST*.id.toArray(new String[0])
//        ].each { String k, String[] v ->
            def timeList = new HashSet<String>()
            PayStat totalall = new PayStat()
            def typeMap = new HashMap<String, PayStat>()
            def cursor = coll.find(query.put('via').in(v).get(),
                    new BasicDBObject(user_id: 1, cny: 1, coin: 1, timestamp: 1))
            while (cursor.hasNext()) {
                def obj = cursor.next()
                def user_id = obj['user_id'] as String
                def cny = new BigDecimal(((Number) obj.get('cny')).doubleValue())
                def coin = obj.get('coin') as Long
                def timestamp = obj.get(timestamp) as Long
                String thatday = new Date(timestamp).format('yyyyMMdd')
                PayStat all = typeMap.get(thatday + 'all')
                PayStat delta = typeMap.get(thatday + 'delta')
                timeList.add(thatday)
                if (all == null) {
                    all = new PayStat()
                    typeMap.put(thatday + 'all', all)
                }
                if (delta == null) {
                    delta = new PayStat()
                    typeMap.put(thatday + 'delta', delta)
                }
                all.add(user_id, cny, coin)
                totalall.add(user_id, cny, coin)
                //该用户之前无充值记录或首冲记录为当天则算为当天新增用户
                if (old_ids.containsKey(user_id)) {
                    def userTimestamp = old_ids.get(user_id) as Long
                    Long day = new Date(timestamp).clearTime().getTime()
                    Long userday = new Date(userTimestamp).clearTime().getTime()
                    if (day.equals(userday)) {
                        delta.add(user_id, cny, coin)
                    }
                }
            }
            cursor.close()
            //将结果装入map中
            timeList.collect {
                def thatday = it as String
                PayStat all = typeMap.get(thatday + 'all')
                PayStat delta = typeMap.get(thatday + 'delta')
                def timeMap = data.get('timeLine') as Map
                def map = timeMap.get(thatday) as Map
                if (map == null) {
                    map = new HashMap()
                    timeMap.put(thatday, map)
                }
                map.put(k, [all: all?.toMap(), delta: delta?.toMap()])
            }
            def totalMap = data.get('total') as Map
            totalMap.put(k, totalall.toMap())
        }
        //时间排序
        Map<String, Object> timeMap = data.get('timeLine') as Map<String, Object>
        if (!timeMap.isEmpty()) {
            def timeSort = timeMap.keySet().toArray().sort { Object a, Object b ->
                return ((String)b).compareTo((String)a)
            }
            data.put('timeSort', timeSort)
        }
        [code: 1, data: data]
    }

    def finance_list_export(HttpServletRequest req, HttpServletResponse res) {
        Date[] dates = ExportUtils.checkDate(Web.getStime(req), Web.getEtime(req))
        QueryBuilder query = QueryBuilder.start();
        def bodyBuf = new StringBuffer()
        def title = Boolean.TRUE
        def result = ExportUtils.list(req, table(), query.get(), ALL_FIELD, NATURAL_DESC) { List<BasicDBObject> data ->
            ExportUtils.render(data, ExportType.FINANCE_LIST, bodyBuf, title)
            if (Boolean.TRUE.equals(title)) title = Boolean.FALSE
        } as Map
        def count = result['count'] as String
        def page = result['all_page'] as String
        StringBuffer buf = ExportUtils.generateTitle(dates, count, page)
        String filename = ExportUtils.generateFilename(dates, "网站管理-加减柠檬流水")
        //获得值列表
        ExportUtils.response(res, filename, buf.append(bodyBuf).toString())
    }

    /**
     * 每日财务统计
     * @param req
     */
    def daily_report(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        def dailyReport = adminMongo.getCollection("finance_dailyReport")
        return Crud.list(req, dailyReport, query.get(), $$(total_coin: 0), SJ_DESC)
    }

    /**
     * 商品日报表
     * @param req
     */
    def product_stat_report(HttpServletRequest req) {
        def productId = ServletRequestUtils.getIntParameter(req, '_id', 0)

        def product_stat = adminMongo.getCollection('product_stat')
        def query = Web.fillTimeBetween(req)
        if (productId != 0) {
            query.and('product_id').is(productId)
        }
        return Crud.list(req, product_stat, query.get(), null, SJ_DESC)
    }
}
