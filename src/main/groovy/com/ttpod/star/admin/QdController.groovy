package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.common.util.ExportUtils
import com.ttpod.star.model.ExportType
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * 渠道统计相关
 */
//@Rest
@RestWithSession
class QdController extends BaseController {
    DBCollection table() { adminMongo.getCollection('stat_channels') }

    static final Logger logger = LoggerFactory.getLogger(QdController.class);

    /**
     * cpa1:激活扣量
     * cpa2:注册扣量
     * cpa3:安装扣量
     * @param req
     */
    def set_cpa(HttpServletRequest req) {
        def updateInfo = new BasicDBObject();
        if (req['cpa1']) {
            updateInfo.append('cpa1', new Integer(req['cpa1']))
        }
        if (req['cpa2']) {
            updateInfo.append('cpa2', new Integer(req['cpa2']))
        }
        if (req['cpa3']) {
            updateInfo.append('cpa3', new Integer(req['cpa3']))
        }
        [code: table().update($$(_id, req[_id]), $$($set, updateInfo)).getN()]
    }

    Map reg_pay_list_service(DBObject query, HttpServletRequest req) {
        reg_pay_list_service(query, null, req)
    }

    Map reg_pay_list_service(DBObject query, BasicDBObject desc, HttpServletRequest req) {
        if (desc == null) {
            desc = $$(reg: -1)
        }
        Crud.list(req, table(), query, $$(pays: 0), desc) { List<BasicDBObject> qd_list ->
            fillQdList(qd_list);
        }
    }

    public void fillQdList(List<BasicDBObject> qd_list) {
        def channel = adminMongo.getCollection('channels')
        for (BasicDBObject obj : qd_list) {
            def id = obj['qd']
            obj.put('name', channel.findOne(id, $$('name', 1))?.get('name'))
            def stay = obj.remove("stay") as Map
            def stay1 = stay?.get("1_day") as Long
            def stay3 = stay?.get("3_day") as Long
            def stay7 = stay?.get("7_day") as Long
            def stay30 = stay?.get("30_day") as Long
            obj.put("1_day", (stay1 != null ? stay1 : 0))
            obj.put("3_day", (stay3 != null ? stay3 : 0))
            obj.put("7_day", (stay7 != null ? stay7 : 0))
            obj.put("30_day", (stay30 != null ? stay30 : 0))
            def active = obj.get("active") as Integer
            def reg = obj.get("reg") as Integer

            // 统计发言率 新增发言率，新增消费率

            def speechs = obj.get("speechs")  as Integer
            def first_speechs = obj.get("first_speechs") as Integer
            def first_cost = obj.get('first_cost') as Integer
            def login_count = obj.get("login_count") as Integer

            def reg_rate = 0,first_speech_rate = 0, speech_rate = 0, first_cost_rate = 0
            if (active != null && active != 0 && reg != null && reg != 0) {
                reg_rate = reg / active
            }
            obj.put("reg_rate", reg_rate)

            // 发言率
            if (login_count != null && login_count != 0 && speechs != null && speechs != 0) {
                speech_rate = speechs / login_count
            }
            obj.put("speech_rate", speech_rate)

            // 新增发言率
            if (active != null && active != 0 && first_speechs != null && first_speechs != 0) {
                first_speech_rate = first_speechs / active
            }
            obj.put("first_speech_rate", first_speech_rate)

            // 新增消费率
            if (active != null && active != 0 && first_cost != null && first_cost != 0) {
                first_cost_rate = first_cost / active
            }
            obj.put("first_cost_rate", first_cost_rate)
        }
    }

    //注册收益
    def reg_pay_list(HttpServletRequest req) {
        logger.debug('Received reg_pay_list params is {}', req.getParameterMap())
        def channel = adminMongo.getCollection('channels')

        def qid = req[_id]
        def child_qd = req["child_qd"]
        def parent_qd = req["parent_qd"]
        def qdQuery = $$('client', req['client'])
        def type = req['type'] ?: 0//0:全部；1：父渠道；2：子渠道
        if ('1'.equals(type)) {
            qdQuery.append("parent_qd", null)
        }
        if ('2'.equals(type)) {
            qdQuery.append("parent_qd", [$ne: null])
        }
        if (parent_qd != null) {
            qdQuery.append("parent_qd", parent_qd)
        }
        def query = new BasicDBObject('qd', [$in: channel.find(qdQuery).toArray()
                .collect { it.getAt(_id) }])
        BasicDBObject desc = $$(reg: -1)
        if (qid) { // 按照id查询  日期排序
            def my_query = Web.fillTimeBetween(req).and('qd').is(qid)
            if (child_qd)
                my_query.and('child_qd').is(child_qd)
            query = my_query.get()
            desc = $$(timestamp, -1);
        } else {
            //按注册数排序
            def day = req[stime]
            if (day) {
                query[timestamp] = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss').parse(day as String).clearTime().getTime()
            }
        }
        reg_pay_list_service(query, desc, req)
    }

    def reg_pay_export(HttpServletRequest req, HttpServletResponse res) {
        def query = new BasicDBObject()
        def day = req[stime]
        def client = [1: 'PC', 2: 'Android', 4: 'iOS', 5: 'H5', 6: 'RIA'].get(req["client"] as Integer)
        Date date = new SimpleDateFormat('yyyy-MM-dd').parse(day as String).clearTime()
        query[timestamp] = date.getTime();

        def qdQuery = $$('client', req['client'])
        query.put('qd', [$in: adminMongo.getCollection('channels').find(qdQuery).toArray()
                .collect { it.getAt(_id) }])

        List<BasicDBObject> qd_list = table().find(query, $$(pays: 0, regs: 0)).sort($$(reg: -1)).toArray().collect {
            it as BasicDBObject
        }
        fillQdList(qd_list);
        for (BasicDBObject obj : qd_list) {
            obj.put("pay", obj.get("pay") ?: 0);

            Integer avg_cny = obj.getInt("pay") == 0 ? 0 : obj.getInt("cny") / obj.getInt("pay") as Integer;
            obj.put("avg_cny", avg_cny);
            Integer avg_cny_month_cny = obj.getInt("month_pay") == 0 ? 0 : obj.getInt("month_cny") / obj.getInt("month_pay") as Integer;
            obj.put("avg_cny_month_cny", avg_cny_month_cny);
            Integer _1_day_rate = obj.getInt("reg") == 0 ? 0 : obj.getInt("1_day") / obj.getInt("reg") as Integer;
            obj.put("1_day_rate", Math.round(_1_day_rate * 10000) / 100);
            Integer _3_day_rate = obj.getInt("reg") == 0 ? 0 : obj.getInt("3_day") / obj.getInt("reg") as Integer;
            obj.put("3_day_rate", Math.round(_3_day_rate * 10000) / 100);
            Integer _7_day_rate = obj.getInt("reg") == 0 ? 0 : obj.getInt("7_day") / obj.getInt("reg") as Integer;
            obj.put("7_day_rate", Math.round(_7_day_rate * 10000) / 100);
            Integer _30_day_rate = obj.getInt("reg") == 0 ? 0 : obj.getInt("30_day") / obj.getInt("reg") as Integer;
            obj.put("30_day_rate", Math.round(_30_day_rate * 10000) / 100);

            obj.put("reg_rate", obj.getInt("reg_rate") * 100);
        }


        def bodyBuf = new StringBuffer()
        ExportUtils.render(qd_list, ExportType.REG_PAY_LIST(client), bodyBuf, true)

        StringBuffer buf = ExportUtils.generateTitle([date, date] as Date[], String.valueOf(qd_list.size()), null)
        String filename = ExportUtils.generateFilename([date, date] as Date[], client + "注册和收益");
        ExportUtils.response(res, filename, buf.append(bodyBuf).toString())

        //return qd_list;
    }


    Map reg_list_service(DBObject query, HttpServletRequest req) {
        Crud.list(req, users(), query, new BasicDBObject(timestamp: 1, nick_name: 1), SJ_DESC)
    }

    def reg_list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).and('qd').is(req[_id] as String).get()
        reg_list_service(query, req)
    }


    def pay_list(HttpServletRequest req) {
        QueryBuilder query = Web.fillTimeBetween(req)
        query = query.and('qd').is(req[_id] as String)
        pay_list_service(query.get(), req)

    }
    //已优化
    Map pay_list_service(DBObject query, HttpServletRequest req) {
        query.put('via', [$ne: 'Admin'])
        //优化后
        Crud.list(req, adminMongo.getCollection('finance_log'), query, ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            def users = users()
            //待优化
            for (BasicDBObject obj : list) {
                def user = users.findOne(obj['user_id'], $$(nick_name: 1, timestamp: 1))
                obj.put('nick_name', user?.get('nick_name'))
                obj.put('reg_time', user?.get('timestamp'))
            }
        }
    }

    final Long DAY_MILLON = 24 * 3600 * 1000L
    static final BasicDBObject REG_DESC = new BasicDBObject('reg', -1);

    def pay_rate(HttpServletRequest req) {
        def channel = adminMongo.getCollection('channels')

        def qid = req[_id]
        def query
        BasicDBObject desc = REG_DESC
        if (qid) { // 按照id查询
            query = Web.fillTimeBetween(req).and('qd').is(qid).get()
            desc = new BasicDBObject('timestamp', -1);
        } else {
            query = new BasicDBObject('qd', [$in: channel.find($$('client', req['client'])).toArray()
                    .collect { it.getAt(_id) }])
            def day = req[stime]
            if (day) {
                query[timestamp] = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss').parse(day as String).clearTime().getTime()
            }
        }
        pay_rate_service(query, desc, req)

    }
    //待优化

    Map pay_rate_service(DBObject query, BasicDBObject desc, HttpServletRequest req) {
        Crud.list(req, table(), query, new BasicDBObject(pays: 0, cny: 0, count: 0), desc) { List<BasicDBObject> qd_list ->
            def finance_log = adminMongo.getCollection('finance_log')
            def channel = adminMongo.getCollection('channels')
            for (BasicDBObject obj : qd_list) {
                long begin = obj[timestamp] as long
                def allUids = obj.remove('regs') as Collection
                obj.put('name', channel.findOne(obj['qd'], $$('name', 1))?.get('name'))
                if (allUids && allUids.size() > 0)
                    [1, 3, 7, 30].each { Integer d ->
                        def timeBetween = [$gte: begin, $lt: begin + d * DAY_MILLON]
                        def iter = finance_log.aggregate(
                                $$('$match', [via: [$ne: 'Admin'], user_id: [$in: allUids], timestamp: timeBetween]),
                                $$('$project', [cny: '$cny', user_id: '$user_id']),
                                $$('$group', [_id: null, cny: [$sum: '$cny'], count: [$sum: 1], pays: [$addToSet: '$user_id']])
                        ).results().iterator()
                        if (iter.hasNext()) {
                            def finaObj = iter.next()
                            finaObj['pay'] = (finaObj.removeField('pays') as Collection).size()
                            finaObj.removeField(_id)
                            obj.put("${d}_day".toString(), finaObj)
                        }
                    }
            }
        }
    }

    //
    def login_rate(HttpServletRequest req) {
        def channel = adminMongo.getCollection('channels')
        def query
        def qid = req[_id]
        BasicDBObject desc = REG_DESC
        if (qid) { // 按照id查询
            query = Web.fillTimeBetween(req).and('qd').is(qid).get()
            desc = new BasicDBObject('timestamp', -1);
        } else {
            query = $$('qd', [$in: channel.find($$('client': req['client'], _id: [$ne: 'wfk'])).toArray().collect {
                it[_id]
            }])
            def day = req[stime]
            if (day) {
                query[timestamp] = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss').parse(day as String).clearTime().getTime()
            }
        }
        login_rate_service(query, desc, req)

    }

    //待优化
    Map login_rate_service(DBObject query, BasicDBObject desc, HttpServletRequest req) {
        Crud.list(req, table(), query, new BasicDBObject(pays: 0, cny: 0, count: 0), desc) { List<BasicDBObject> qd_list ->

            def day_login = logMongo.getCollection('day_login')
            def channel = adminMongo.getCollection('channels')
            for (BasicDBObject obj : qd_list) {
                long begin = obj[timestamp] as long
                def allUids = obj.remove('regs') as Collection
                obj.put('name', channel.findOne(obj['qd'], $$('name', 1))?.get('name'))
                if (allUids && allUids.size() > 0)
                    [1, 3, 7, 30].each { Integer d ->
                        Long gt = begin + d * DAY_MILLON
                        def count = day_login.count($$(user_id: [$in: allUids], timestamp:
                                [$gte: gt, $lt: gt + DAY_MILLON]
                        ))
                        obj.put("${d}_day".toString(), count)
                    }
            }
        }
    }


    def login_total(HttpServletRequest req) {
        login_total_service((String) req[_id], req)
    }

    private static
    final Map<String, String> CHANNEL_TYPS = ['1': 'PC渠道', '2': 'Android渠道', '3': 'Android pad', '4': 'IOS渠道', '5': 'H5渠道', '6': 'RIA渠道']
    private static
    final Map<String, String> CHANNEL_LOGIN_KEYS = ['1': 'pc_login', '2': 'mobile_login', '3': 'mobile_login', '4': 'ios_login', '5': 'h5_login', '6': 'ria_login']
    //已优化
    Map login_total_service(String qdId, HttpServletRequest req) {
        Map<Integer, Integer> map
        def query = Web.fillTimeBetween(req)
        if (StringUtils.isNotBlank(qdId)) { // _id client 互斥
            query.put('qd').is(qdId)
            def list = Crud.list(req, adminMongo.getCollection('stat_channels'), query.get(),
                    $$([daylogin: 1, day7login: 1, day30login: 1, timestamp: 1, qd: 1]),
                    $$([timestamp: -1, qd: -1])) { List<BasicDBObject> data ->
                for (BasicDBObject obj : data) {
                    def qd = obj.get('qd') as String
                    //查渠道名称
                    def qdObj = adminMongo.getCollection("channels").findOne($$(_id, qd))
                    obj.put("name", qdObj?.getAt("name"))
                    obj.put("1_day", obj.remove("daylogin"))
                    obj.put("7_day", obj.remove("day7login"))
                    obj.put("30_day", obj.remove("day30login"))
                }
            }
            return list
        } else {
            def stat_daily = adminMongo.getCollection('stat_daily')
            def client = req['client'] as String
            query.put("type").is("alllogin")
            Crud.list(req, stat_daily, query.get(), ALL_FIELD, $$(timestamp, -1)) { List<BasicDBObject> data ->
                for (BasicDBObject obj : data) {
                    def item = obj.remove(CHANNEL_LOGIN_KEYS[client])
                    def name = CHANNEL_TYPS[client]
                    obj.put("name", name)
                    obj.put("1_day", item?.getAt("daylogin"))
                    obj.put("7_day", item?.getAt("day7login"))
                    obj.put("30_day", item?.getAt("day30login"))
                }
            }
        }
    }

    /**
     * 渠道投入回报表
     * @param req
     * @return
     */
    def reg_pay_static(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        def _id = req.getParameter('_id') as String
        if (StringUtils.isNotBlank(_id)) {
            def ids = _id.split(',') as String[]
            query.and('qd').in(ids)
        }
        def client = req.getParameter('client') as String
        if (StringUtils.isNotBlank(client)) {
            query.and('client').is(client)
        }
        def parent_qd = req.getParameter('parent_qd') as String
        if (StringUtils.isNotBlank(parent_qd)) {
            query.and('parent_qd').is(parent_qd)
        }
        def type = req['type'] ?: 0//0:全部；1：父渠道；2：子渠道
        if ('1'.equals(type as String)) {
            query.and($$("parent_qd", null))
        }
        if ('2'.equals(type as String)) {
            query.and($$("parent_qd", [$ne: null]))
        }
        query.and('type').is('channel')
        Crud.list(req, adminMongo.getCollection('stat_month'),
                query.get(), ALL_FIELD, $$(timestamp: -1, 'pay_retention.cny': -1)) { List<BasicDBObject> data ->
            def channel = adminMongo.getCollection('channels')
            for (BasicDBObject obj : data) {
                def qd = obj.get('qd') as String
                obj.put('name', channel.findOne($$(_id: qd))?.get('name') ?: '')
            }
        }
    }

    def reg_pay_period(HttpServletRequest req) {

        def uQ = Web.fillTimeBetween(req)
        def qd = req[_id]

        if (qd.isNotBlank() && qd.equals("f101")) {
            return km_reg_pay_period(req)
        }
        if (qd.isNotBlank()) {
            uQ.and('qd').is(qd)
        } else {
            def client = req['client']
            if (client.isNotBlank()) {
                def qds = adminMongo.getCollection('channels').find($$('client', client),
                        $$(_id, 1)).toArray().collect { DBObject it -> it.get(_id) }
                uQ.and('qd').in(qds)
            }
        }
        def userQuery = (BasicDBObject) uQ.get()
        if (userQuery.isEmpty()) {
            return [code: 0, msg: 'user_query  empty']
        }

        def uids = users().find(userQuery, $$(_id, 1)).collect(new HashSet(10240)) { it[_id] }

        def payQ = Web.fillTimeBetween(Web.getTime(req, 'stime1'), Web.getTime(req, 'etime1')).and('via').notEquals('Admin')

        def payQuery = ((BasicDBObject) payQ.get()).append($or, [[user_id: [$in: uids]], [to_id: [$in: uids]]])

        def cur = adminMongo.getCollection('finance_log').find(payQuery)
        def uset = new HashSet(uids.size())
        def coin = new AtomicLong()
        def cny = new BigDecimal(0)
        def times = new AtomicInteger()
        while (cur.hasNext()) {
            def row = cur.next()
            uset.add(row['to_id'] ?: row['user_id'])
            if (row['coin'] != null) {
                coin.addAndGet((Long) row['coin'])
            }
            if (row['cny'] != null) {
                cny = cny.add(new BigDecimal(((Double) row['cny']).doubleValue()))
            }
            times.incrementAndGet()
        }
        [code: 1, data: [reg_user: uids.size(), pay_user: uset.size(), pay_cny: cny.doubleValue(), pay_coin: coin.longValue(), pay_times: times.get()]]
    }

    private km_reg_pay_period(HttpServletRequest req) {

        def uQ = Web.fillTimeBetween(req)
        def qd = req[_id]
        if (qd.isNotBlank()) {
            uQ.and('qd').is(qd)
        } else {
            def client = req['client']
            if (client.isNotBlank()) {
                def qds = adminMongo.getCollection('channels').find($$('client', client),
                        $$(_id, 1)).toArray().collect { DBObject it -> it.get(_id) }
                uQ.and('qd').in(qds)
            }
        }
        def userQuery = (BasicDBObject) uQ.get()
        if (userQuery.isEmpty()) {
            return [code: 0, msg: 'user_query  empty']
        }

        def payQ = Web.fillTimeBetween(Web.getTime(req, 'stime1'), Web.getTime(req, 'etime1')).and('via').notEquals('Admin')

        Long reg_users = 0
        Long pay_users = 0
        Long pay_cnys = 0
        Long pay_coins = 0
        Long pay_times = 0

        def count = users().count(userQuery)
        def size = 100000
        def allPage = (int) ((count + size - 1) / size);
        while (allPage > 0) {
            List uids = users().find(userQuery, $$(_id, 1)).skip((allPage - 1) * size).limit(size).toArray()*._id;
            def payQuery = ((BasicDBObject) payQ.get()).append($or, [[user_id: [$in: uids]], [to_id: [$in: uids]]])
            def result = reduceRegPay(uids, payQuery)
            reg_users += result["reg_user"] as Long
            pay_users += result["pay_user"] as Long
            pay_cnys += result["pay_cny"] as Long
            pay_coins += result["pay_coin"] as Long
            pay_times += result["pay_times"] as Long
            allPage--
        }
        [code: 1, data: [reg_user: reg_users, pay_user: pay_users, pay_cny: pay_cnys, pay_coin: pay_coins, pay_times: pay_times]]
    }

    private Map reduceRegPay(List uids, BasicDBObject payQuery) {
        def cur = adminMongo.getCollection('finance_log').find(payQuery).batchSize(5000)
        def uset = new HashSet(uids.size())
        def coin = new AtomicLong()
        def cny = new BigDecimal(0)
        def times = new AtomicInteger()
        while (cur.hasNext()) {
            def row = cur.next()
            uset.add(row['to_id'] ?: row['user_id'])
            coin.addAndGet((Long) row['coin'])
            cny = cny.add(new BigDecimal(((Double) row['cny']).doubleValue()))
            times.incrementAndGet()
        }

        return [reg_user: uids.size(), pay_user: uset.size(), pay_cny: cny.doubleValue(), pay_coin: coin.longValue(), pay_times: times.get()]
    }

    /**
     * ASO优化统计 (App Store Optimization)
     * @param req
     * @return
     */
    def aso_list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        Crud.list(req, adminMongo.getCollection('stat_aso'), query.get(), ALL_FIELD, SJ_DESC)
    }
}
