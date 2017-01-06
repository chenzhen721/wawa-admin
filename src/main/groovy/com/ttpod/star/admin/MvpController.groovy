package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.IMessageCode
import com.ttpod.rest.web.Crud
import com.ttpod.star.common.util.ExportUtils
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicLong

import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.doc.MongoKey.timestamp
import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.groovy.CrudClosures.Int
import static com.ttpod.rest.groovy.CrudClosures.Str
import static com.ttpod.rest.groovy.CrudClosures.Timestamp

@RestWithSession
class MvpController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(MvpController.class)

    DBCollection table() { adminMongo.getCollection('mvps') }

    DBCollection stat_mvps() { adminMongo.getCollection('stat_mvps') }

    private final Map<String, Closure> props = [_id: Str, user_id: Int, order: Int, type: Int, timestamp: Timestamp]

    /**
     * type:0-大额用户，1-运营监控用户
     */
    @Delegate
    Crud crud = new Crud(table(), false, props, new Crud.QueryCondition() {
        public DBObject sortby(HttpServletRequest req) {
            BasicDBObject sortObj = $$("order", -1)
            return sortObj
        }
    }
    )


    def list(HttpServletRequest req) {
        def type = req.getParameter('type') as Integer
        def query = new BasicDBObject()
        if (type) query.put('type', type)
        crud.list(req, table(), query, ALL_FIELD, $$('order', 1)) { List<BasicDBObject> list ->
            for (BasicDBObject obj : list) {
                def id = obj['user_id'] as Integer
                def user = users().findOne($$('_id', id), $$('nick_name', 1))
                def nick_name = user?.get('nick_name')
                obj.put('nick_name', nick_name == null ? '' : nick_name)
            }
        }
    }

    def add(HttpServletRequest req) {
        def user_id = req.getParameter('user_id') as Integer
        def type = req.getParameter('type') as Integer
        if (user_id == null || users().findOne($$(_id: user_id)) == null) {
            return ['code': 30442]
        } else {
            Map map = new HashMap();
            for (Map.Entry<String, Closure> entry : props.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue().call(req.getParameter(key));
                if (val != null) {
                    map.put(key, val);
                }
            }
            map.put('_id', "${user_id}_${type}".toString())
            if (table().save(new BasicDBObject(map)).getN() == 1) {
                Crud.opLog(table().getName() + "_add", map);
            }
            return IMessageCode.OK
        }
    }

    def edit(HttpServletRequest req) {
        return crud.edit(req)
    }

    /**
     * 大额用户统计信息
     * @param req
     * @return
     */
    def pay_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        pay_log_service(req, query)
    }

    /**
     * 每日付费金额超过一定金额的大额用户信息
     * @param req
     */
    def pay_most_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).and('type').is('mvp_pay_most')
        def user_id = req.getParameter('user_id') as String
        if (StringUtils.isNotBlank(user_id)) query.put('user_id').is(user_id as Integer)
        Crud.list(req, stat_mvps(), query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            for (BasicDBObject obj : list) {
                def uid = obj.get('user_id') as Integer
                def user = users().findOne($$(_id: uid), $$(nick_name: 1, timestamp: 1))
                def nick_name = user?.get('nick_name')
                def reg_time = user?.get('timestamp') as Long
                reg_time = reg_time ? new Date(reg_time).format('yyyy-MM-dd') : ''
                obj.put('nick_name', nick_name)
                obj.put('reg_time', reg_time)
            }
        }
    }

    /**
     * 运用特殊用户
     * @param req
     * @return
     */
    def cost_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).and('type').is('mvp_cost')
        def user_id = req.getParameter('user_id') as String
        if (StringUtils.isNotBlank(user_id)) query.put('user_id').is(user_id as Integer)
        Crud.list(req, stat_mvps(), query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            for (BasicDBObject obj : list) {
                def uid = obj.get('user_id') as Integer
                def starId = obj.get('star_id') as Integer
                def unick = users().findOne($$(_id: uid), $$(nick_name: 1))?.get('nick_name')
                def snick = users().findOne($$(_id: starId), $$(nick_name: 1))?.get('nick_name')
                obj.put('user_nick', unick)
                obj.put('star_nick', snick)
            }
        }
    }

    /**
     * 导出特殊用户管理中添加的大额用户的充值信息（以天为单位）
     */
    def pay_log_export(HttpServletRequest req, HttpServletResponse res) {
        Date[] dates = ExportUtils.checkDate(Web.getStime(req), Web.getEtime(req))
        def query = Web.fillTimeBetween(dates[0], dates[1])
        def result = pay_log_service(req, query) as Map
        def count = result['count'] as String
        def page = result['all_page'] as String
        StringBuffer buf = ExportUtils.generateTitle(dates, count, page)
        String filename = ExportUtils.generateFilename(dates, "大额用户跟踪");
        def titles = ["交易日期", "用户ID", "昵称", "充值金额（元）", "充值柠檬"]
        buf.append(titles.join(",")).append(ExportUtils.ls)
        //获得值列表
        List<BasicDBObject> list = result['data'] as List
        for (BasicDBObject obj : list) {
            ['time', 'user_id', 'nick_name', 'cny', 'coin'].each { String key ->
                def value = obj[key] as String
                if (StringUtils.isBlank(value)) {
                    value = ''
                }
                buf.append(value).append(',')
            }
            buf.append(ExportUtils.ls)
        }
        ExportUtils.response(res, filename, buf.toString())
    }

    /**
     * 导出该月充值金额大于1000的用户信息（以月为单位）
     */
    def mvp_pay_export(HttpServletRequest req, HttpServletResponse res) {
        def start = Web.getStime(req)
        if (start == null) {
            return [code: 0, msg: '请输入开始时间']
        }
        Calendar cal = getCalendar(start)
        long firstDayOfCurrentMonth = cal.getTimeInMillis()  //当月第一天
        cal.add(Calendar.MONTH, +1);
        long firstDayOfNextMonth = cal.getTimeInMillis()  //下月第一天
        def dates = [new Date(firstDayOfCurrentMonth), new Date(firstDayOfNextMonth)] as Date[]
        String ym = new Date(firstDayOfCurrentMonth).format("yyyyMM")
        StringBuffer buf = ExportUtils.generateTitle(dates, null, null)
        String filename = ExportUtils.generateFilename(dates, "大额充值统计");
        def titles = ["日期", "用户ID", "充值金额（元）", "注册日期", "渠道", "昵称"]
        buf.append(titles.join(",")).append(ExportUtils.ls)
        adminMongo.getCollection('finance_log').aggregate(
                new BasicDBObject('$match', [timestamp: [$gte: firstDayOfCurrentMonth, $lt: firstDayOfNextMonth], via: [$ne: 'Admin']]),
                new BasicDBObject('$project', [user_id: '$user_id', cny: '$cny', qd: '$qd']),
                new BasicDBObject('$group', [_id: '$user_id', cny: [$sum: '$cny'], qd: [$addToSet: '$qd']]),
                new BasicDBObject('$match', [cny: [$gte: 1000]]),
                new BasicDBObject('$sort', [cny: -1])
        ).results().each { BasicDBObject obj ->
            def user = users().findOne(new BasicDBObject('_id': obj.get('_id') as Integer), new BasicDBObject('nick_name': 1, timestamp: 1)) as BasicDBObject
            def nickName = user?.get('nick_name') as String
            if (StringUtils.isNotBlank(nickName)) {
                nickName = "\"" + nickName.replaceAll("\"", "\"\"") + "\""
            }
            def timestamp = user?.get('timestamp') as Long
            def qd = "\"" + obj.get('qd') + "\""
            buf.append(ym).append(',').append(obj.get('_id')).append(',').append(obj.get('cny')).append(',')
                    .append(new Date(timestamp).format('yyyyMMdd')).append(',').append(qd).append(',')
                    .append(nickName).append(System.lineSeparator())
        }
        ExportUtils.response(res, filename, buf.toString())
    }

    private pay_log_service(HttpServletRequest req, QueryBuilder query) {
        def user = req.getParameter('user_id')
        List<Integer> userIds = new ArrayList<Integer>()
        def userMap = new HashMap()
        def userTimestamp = new HashMap()
        def userPayStat = new HashMap()
        if (StringUtils.isBlank(user)) {
            (table().find(new BasicDBObject(type: 0), $$('user_id', 1)).sort($$('order', 1))).toArray().each {
                def user_id = it?.getAt('user_id')
                if (user_id != null) {
                    userIds.add(user_id as Integer)
                }
            }
        } else {
            userIds = [user as Integer]
        }
        //获取用户信息
        users().find($$('_id': ['$in': userIds]), $$([_id: 1, nick_name: 1])).toArray().each { obj ->
            userMap.put(obj['_id'] as String, obj['nick_name'])
        }
        query.and('user_id').in(userIds).and('via').notEquals('Admin')
        def list = adminMongo.getCollection('finance_log').find(query.get(),
                $$(user_id: 1, cny: 1, coin: 1, timestamp: 1)).sort($$(timestamp, -1)).toArray() as List
        list.each { BasicDBObject obj ->
            def user_id = obj['user_id'] as String
            def cny = 0 as BigDecimal
            if (obj.get('cny') != null) {
                cny = new BigDecimal(((Number) obj.get('cny')).doubleValue())
            }
            def coin = obj.get('coin') as Long
            def timestamp = obj.get(timestamp) as Long
            String thatday = new Date(timestamp).format('yyyyMMdd')
            def timeArray = userTimestamp.get(user_id) as List
            if (timeArray == null) {
                timeArray = [thatday]
                userTimestamp.put(user_id, timeArray)
            } else {
                if (!timeArray.contains(thatday)) {
                    timeArray.add(thatday)
                }
            }
            def payStat = userPayStat.get("${user_id}[]${thatday}") as PayStat
            if (payStat == null) {
                payStat = new PayStat()
                userPayStat.put("${user_id}[]${thatday}", payStat)
            }
            payStat.add(cny, coin)
        }
        def data = []
        userIds.each {
            def id = it as Number
            def nick_name = userMap.get(String.valueOf(id))
            def times = userTimestamp.get(String.valueOf(id))
            if (times != null) {
                times.each {
                    def payStat = userPayStat.get("${id}[]${it}") as PayStat
                    def coin = payStat.getCoin()
                    def cny = payStat.getCny()
                    data.add([user_id: id, nick_name: nick_name == null ? '' : nick_name, time: it, coin: coin, cny: cny])
                }
            }
        }
        [code: 1, data: data]
    }

    private class PayStat {
        final AtomicLong coin = new AtomicLong()
        def BigDecimal cny = new BigDecimal(0)

        def toMap() { [coin: coin.get(), cny: cny.doubleValue()] }

        def add(BigDecimal deltaCny, Long deltaCoin) {
            cny = cny.add(deltaCny)
            coin.addAndGet(deltaCoin)
        }
    }

    private static Calendar getCalendar(Date date) {
        Calendar cal = Calendar.getInstance()//获取当前日期
        cal.setTime(date)
        cal.set(Calendar.DAY_OF_MONTH, 1)//设置为1号,当前日期既为本月第一天
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return cal
    }

}
