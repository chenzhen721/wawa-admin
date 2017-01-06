package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.util.WebUtils
import com.ttpod.rest.web.Crud
import org.apache.commons.lang.StringUtils

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * Created by Administrator on 2014/11/5.
 */
@RestWithSession
class TradeController extends BaseController {

    DBCollection table() { logMongo.getCollection('trade_logs') }

    /**
     * 之前为水果乐园统计接口via：kunbo，现在改为德州统计接口via:德州 texasholdem，捕鱼 fishing，捕鱼 niuniu
     */
    def kunbo_list(HttpServletRequest req) {
        def queryBuilder = new QueryBuilder()
        def start = Web.getStime(req) as Date
        def end = Web.getEtime(req) as Date
        def _id = req.getParameter("_id") as String
        def res = req.getParameter("result") as String//0-查询成功，1-查询失败
        def type = req.getParameter("type") as String//1-兑换游戏币，2-兑换柠檬
        def via = (req.getParameter("via") as String) ?: "texasholdem"//德州-texasholdem，捕鱼-fishing，捕鱼-niuniu
        if (start != null) {
            queryBuilder.put("time").greaterThanEquals(start.getTime())
        }
        if (end != null) {
            queryBuilder.lessThan(end.getTime())
        }
        if (StringUtils.isNotBlank(_id)) {
            queryBuilder.put("uid").is(_id as Integer)
        }
        if (StringUtils.isNotBlank(res)) {
            queryBuilder.put("resp.result")
            if ("0".equals(res)) {
                queryBuilder.is(res)
            } else {
                queryBuilder.notEquals("0")
            }
        }
        if (StringUtils.isNotBlank(type)) {
            queryBuilder.put("reqs.type").is(type)
        }
        //订单是否成功
        //兑入兑出
        queryBuilder.put("via").is(via)
        def map = Crud.list(req, table(), queryBuilder.get(), $$([resp: 1, uid: 1, time: 1]), $$(time: -1)) { List<BasicDBObject> data ->
            for (BasicDBObject obj : data) {
                def uid = obj.get('uid') as Integer
                obj.put('nick_name', users().findOne($$(_id: uid), $$(nick_name: 1))?.get('nick_name'))
                if (obj.get('time') != null) {
                    obj.put('time', obj.remove('time') as Long)
                }
                def resp = obj.remove('resp') as Map
                def result = resp?.get("result")
                def coin = resp?.get('coin') as Integer
                if ("0".equals(result)) {
                    obj.put("result", "成功")
                } else {
                    obj.put("result", "失败")
                }
                if (coin != null) {
                    if (coin > 0) {
                        obj.put('type', '2')//加柠檬
                    } else {
                        obj.put('type', '1')//减柠檬
                    }
                    obj.put('coin', Math.abs(coin))
                }
            }
        }
        def range = new HashMap<String, Integer>(2)
        range.put("in", 0)
        range.put("out", 0)
        if (!"1".equals(res)) {
            table().aggregate(
                    $$($match, queryBuilder.get()),
                    $$($project, [_id: '$reqs.type', coin: '$resp.coin']),
                    $$($group, [_id: '$_id', coin: [$sum: '$coin']])
            ).results().each { BasicDBObject obj ->
                def cat = obj.get("_id") as String
                def coin = obj.get("coin") as Integer
                if ("1".equals(cat)) {
                    range.put("out", Math.abs(coin))
                }
                if ("2".equals(cat)) {
                    range.put("in", Math.abs(coin))
                }
            }
        }
        map.put("range", range)
        def total = new HashMap<String, Integer>(2)
        table().aggregate(
                $$($match, [via: via, "resp.result": "0"]),
                $$($project, [_id: '$reqs.type', coin: '$resp.coin']),
                $$($group, [_id: '$_id', coin: [$sum: '$coin']])
        ).results().each { BasicDBObject obj ->
            def cat = obj.get("_id") as String
            def coin = obj.get("coin") as Integer
            if ("1".equals(cat)) {
                total.put("out", Math.abs(coin))
            }
            if ("2".equals(cat)) {
                total.put("in", Math.abs(coin))
            }
        }
        map.put("total", total)
        return map
    }

    def dianle_list(HttpServletRequest req) {
        def queryBuilder = new QueryBuilder()
        def start = Web.getStime(req) as Date
        def end = Web.getEtime(req) as Date
        def _id = req.getParameter("_id") as String
        if (start != null) {
            queryBuilder.put("time").greaterThanEquals(start.getTime())
        }
        if (end != null) {
            queryBuilder.lessThan(end.getTime())
        }
        if (StringUtils.isNotBlank(_id)) {
            queryBuilder.put("uid").is(_id as Integer)
        }
        def via = "baidu_jf"
        //queryBuilder.put("via").is("dianle")
        queryBuilder.put("via").is(via)
        def map = Crud.list(req, table(), queryBuilder.get(), $$([resp: 1, uid: 1, time: 1]), $$(time: -1)) { List<BasicDBObject> data ->
            for (BasicDBObject obj : data) {
                def uid = obj.get('uid') as Integer
                obj.put('nick_name', users().findOne($$(_id: uid), $$(nick_name: 1))?.get('nick_name'))
                if (obj.get('time') != null) {
                    obj.put('time', obj.remove('time') as Long)
                }
                def resp = obj.remove('resp') as Map
                def coin = resp?.get('coin') as Integer
                if (coin != null) {
                    obj.put('coin', Math.abs(coin))
                }
            }
        }
        table().aggregate(
                $$($match, [via: via, "resp.coin": [$ne: null]]),
                $$($project, [_id: '$via', coin: '$resp.coin']),
                $$($group, [_id: '$_id', coin: [$sum: '$coin']])
        ).results().each { BasicDBObject obj ->
            def coin = obj.get("coin") as Integer
            map.put("total", coin)
        }
        return map
    }

    def letu_list(HttpServletRequest req) {
        def queryBuilder = new QueryBuilder()
        def start = Web.getStime(req) as Date
        def end = Web.getEtime(req) as Date
        def _id = req.getParameter("_id") as String
        if (start != null) {
            queryBuilder.put("time").greaterThanEquals(start.getTime())
        }
        if (end != null) {
            queryBuilder.lessThan(end.getTime())
        }
        if (StringUtils.isNotBlank(_id)) {
            queryBuilder.put("uid").is(_id as Integer)
        }
        queryBuilder.put("via").is("letu").put("resp.amount").exists(true)
        def map = Crud.list(req, table(), queryBuilder.get(), $$([resp: 1, uid: 1, time: 1]), $$(time: -1)) { List<BasicDBObject> data ->
            for (BasicDBObject obj : data) {
                def uid = obj.get('uid') as String
                if (uid != null && uid.isInteger()) {
                    obj.put('nick_name', users().findOne($$(_id: uid.toInteger()), $$(nick_name: 1))?.get('nick_name'))
                } else {
                    obj.put('nick_name', uid)
                }
                if (obj.get('time') != null) {
                    obj.put('time', obj.remove('time') as Long)
                }
                def resp = obj.remove('resp') as Map
                def coin = resp?.get('amount') as Double
                if (coin != null) {
                    obj.put('amount', coin)
                }
            }
        }
        table().aggregate(
                $$($match, [via: "letu", "resp.amount": [$ne: null]]),
                $$($project, [_id: '$via', coin: '$resp.amount']),
                $$($group, [_id: '$_id', coin: [$sum: '$coin']])
        ).results().each { BasicDBObject obj ->
            def coin = obj.get("coin") as Integer
            map.put("total", coin)
        }
        return map
    }

    def letu_static(HttpServletRequest req) {
        int p = WebUtils.getPage(req);
        int size = WebUtils.getPageSize(req);
        def query = new QueryBuilder()
        def stime = WebUtils.getStime(req) as Date
        def etime = WebUtils.getEtime(req) as Date
        if (stime || etime) {
            query.put('time')
            if (stime) query.greaterThanEquals(stime.getTime())
            if (etime) query.lessThan(etime.getTime())
        }
        query.put("via").is("letu").put("resp.amount").exists(true)
        def all_page = 0, count = 0
        def qd = req.getParameter("qd") as String
        if (StringUtils.isNotBlank(qd)) {
            query.put("qd").is(qd)
            count = 1
        } else {
            count = table().distinct('qd', query.get())?.size()
        }
        if (count == null || count == 0) {
            return [code: 1, data: [], count: 0, all_page: all_page]
        }
        all_page = new BigDecimal((double) (count / size)).toInteger()
        if (count % size != 0) all_page += 1
        if (p > all_page) p = all_page
        def skip = (p <= 1) ? 0 : (p - 1) * size
        List list = table().aggregate(
                $$($match, query.get()),
                $$($project, [via: '$via', qd: '$qd', amount: '$resp.amount', uid: '$uid']),
                $$($group, [_id: '$qd', amounts: [$push: '$amount'], uids: [$addToSet: '$uid']]),
                $$($skip, skip),
                $$($limit, size)
        ).results().toList()
        list.each { BasicDBObject obj ->
            def uids = obj.remove("uids") as List
            obj.put("users", uids == null ? 0 : uids.size())
            def amounts = obj.remove("amounts") as List
            def amount = 0 as Double
            if (amounts != null) {
                amounts.each { String am ->
                    amount += new BigDecimal(am).doubleValue()
                }
            }
            obj.put("amount", amount)
        }
        return [code: 1, data: list, count: count, all_page: all_page]
    }

}
