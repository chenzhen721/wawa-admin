package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.common.doc.IMessageCode
import com.ttpod.rest.common.util.MsgDigestUtil
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.admin.Web
import com.ttpod.star.model.OpType
import groovy.json.JsonSlurper
import org.apache.commons.lang.math.NumberUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.$set
import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.groovy.CrudClosures.*
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import org.apache.commons.lang.StringUtils

/**
 * 渠道管理
 */
//@Rest
@RestWithSession
class ChannelController extends BaseController {

    DBCollection table() { adminMongo.getCollection('channels') }
    public static final Logger logger = LoggerFactory.getLogger(ChannelController.class)

    private Closure NoneBlankStr = { StringUtils.isBlank(it as String) ? null : it };
    private Map<String, Closure> props = [_id      : Str, name: Str, comment: Str, client: Str, type: Str, third_id: Str,
                                          parent_qd: NoneBlankStr, is_close: Eq1, timestamp: Timestamp]

    private BasicDBObject query(HttpServletRequest req) {
        logger.debug("Recv event_callback params : {}", req.getParameterMap())

        def query = new BasicDBObject()
        def id = req[_id]
        if (id.isNotBlank()) {
            query.put(_id, id)
        }
        def client = req['client']
        if (client.isNotBlank()) {
            query.put("client", client)
        }

        def name = req['name']
        if (name.isNotBlank()) {
            query.put("name", name)
        }

        def appId = req['app_id']
        if (name.isNotBlank()) {
            query.put("app_id", appId)
        }

        def appSecret = req['app_secret']
        if (appSecret.isNotBlank()) {
            query.put("app_secret", appSecret)
        }

        def appName = req['app_name']
        if (appName.isNotBlank()) {
            query.put("app_name", appName)
        }

        def parent_flag = req['parent_flag']
        if (StringUtils.isNotBlank(parent_flag)) {
            if ("1".equals(parent_flag))//查子渠道
                query.append("parent_qd", new BasicDBObject($ne: null))
            else if ("-1".equals(parent_flag))//查父渠道
                query.append("parent_qd", null)
        }

        def parent_qd = req['parent_qd']//根据父渠道ID查询子渠道
        if (StringUtils.isNotBlank(parent_qd)) {
            query.append("parent_qd", parent_qd)
        }
        //查询是否关闭同步
        def is_close = req['is_close'] as String
        if (StringUtils.isNotBlank(is_close)) {
            if ('0'.equals(is_close)) {//未关闭
                query.append('is_close', false)
            } else if ('1'.equals(is_close)) {//已关闭
                query.append('is_close', true)
            }
        }
        return query
    }

    @Delegate
    Crud crud = new Crud(table(), props, new Crud.QueryCondition() {
        public DBObject query(HttpServletRequest req) {
            def query = new BasicDBObject()
            def id = req[_id]
            if (id.isNotBlank()) {
                query.put(_id, id)
            }
            def client = req['client']
            if (client.isNotBlank()) {
                query.put("client", client)
            }

            def name = req['name']
            if (name.isNotBlank()) {
                query.put("name", name)
            }

            def parent_flag = req['parent_flag']
            if (StringUtils.isNotBlank(parent_flag)) {
                if ("1".equals(parent_flag))
                    query.append("parent_qd", new BasicDBObject($ne: null))//查子渠道
                else if ("-1".equals(parent_flag))
                    query.append("parent_qd", null)//查父渠道
            }

            def parent_qd = req['parent_qd']//根据父渠道ID查询子渠道
            if (StringUtils.isNotBlank(parent_qd)) {
                query.append("parent_qd", parent_qd)
            }
            return query
        }
    })

    def list(HttpServletRequest req) {
        Crud.list(req, table(), query(req), null, null) { List<BasicDBObject> list ->
            for (BasicDBObject obj : list) {
                def regMap = obj.remove('reg_discount') as Map
                def acMap = obj.remove('active_discount') as Map
                if (regMap != null && regMap.size() > 0) {
                    def keyList = regMap.keySet().toArray().sort()
                    obj.put('reg_discount', regMap.get(keyList[-1]))
                }
                if (acMap != null) {
                    def keyList = acMap.keySet().toArray().sort()
                    obj.put('active_discount', acMap.get(keyList[-1]))
                }
            }
        }
    }

    def add(HttpServletRequest req) {
        String id = req.getParameter("_id") as String
        logger.debug("Recv event_callback params : {}", req.getParameterMap())
        String appId = req['app_id']
        String appSecret = req['app_secret']
        String appName = req['app_name']

        if (id == null || table().count($$("_id", id)) != 0)
            return ['code': 30442]

        Map map = new HashMap();
        for (Map.Entry<String, Closure> entry : props.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue().call(req.getParameter(key));
            if (val != null) {
                map.put(key, val);
            }
        }

        //设置自动扣量（注册扣量，激活扣量）
        def reg_discount = req.getParameter('reg_discount') as String
        def active_discount = req.getParameter('active_discount') as String
        def currentDay = new Date().clearTime().getTime()
        if (StringUtils.isNotBlank(reg_discount) && reg_discount.isInteger()) {
            def disMap = new HashMap()
            disMap.put("${currentDay}".toString(), reg_discount as Integer)
            map.put('reg_discount', disMap)
        }
        if (StringUtils.isNotBlank(reg_discount) && reg_discount.isInteger()) {
            def disMap = new HashMap()
            disMap.put("${currentDay}".toString(), active_discount as Integer)
            map.put('active_discount', disMap)
        }
        if (StringUtils.isNotBlank(appId)) {
            map.put('app_id', appId)
        }
        if (StringUtils.isNotBlank(appSecret)) {
            if (appSecret.length() != 32) {
                logger.debug('app secret 不合法')
                return Web.missParam();
            }
            map.put('app_secret', appSecret)
        }
        if (StringUtils.isNotBlank(appName)) {
            map.put('app_name', appName)
        }
        if (table().save(new BasicDBObject(map)).getN() == 1) {
            Crud.opLog(table().getName() + "_add", map);
        }
        return IMessageCode.OK;
    }

    def edit(HttpServletRequest req) {
        logger.debug("Recv event_callback params : {}", req.getParameterMap())
        String appId = req['app_id']
        String appSecret = req['app_secret']
        String appName = req['app_name']
        Object id = parseId(req);
        if (null == id) {
            return IMessageCode.CODE0;
        }
        Map map = new HashMap();
        for (Map.Entry<String, Closure> entry : props.entrySet()) {
            String key = entry.getKey();
            if (key.equals(_id)) {
                continue;
            }
            String strValue = req.getParameter(key);
            if (strValue != null) {
                Object val = entry.getValue().call(strValue);
                if (val != null) {
                    map.put(key, val);
                }
            }
        }

        def reg_discount = req['reg_discount'] as String
        def active_discount = req['active_discount'] as String
        def currentDay = new Date().clearTime().getTime()
        if (StringUtils.isNotBlank(reg_discount) && reg_discount.isInteger()) {
            map.put("reg_discount.${currentDay}".toString(), reg_discount as Integer)
        }
        if (StringUtils.isNotBlank(active_discount) && active_discount.isInteger()) {
            map.put("active_discount.${currentDay}".toString(), active_discount as Integer)
        }
        if (StringUtils.isNotBlank(appId)) {
            map.put('app_id', appId)
        }
        if (StringUtils.isNotBlank(appSecret)) {
            if (appSecret.length() != 32) {
                logger.debug('app secret 不合法')
                return Web.missParam();
            }
            map.put('app_secret', appSecret)
        }
        if (StringUtils.isNotBlank(appName)) {
            map.put('app_name', appName)
        }
        if (map.size() > 0 && table().update(new BasicDBObject(_id, id), new BasicDBObject($set, map)).getN() == 1) {
            map.put(_id, id);//just For Log
            Crud.opLog(table().getName() + "_edit", map);
        }
        return IMessageCode.OK;
    }

    def add_user(HttpServletRequest req) {
        String pwd = req['password']
        def prop = [
                //_id:seqKGS.nextId(),
                nick_name : req['nick_name'],
                _id       : req['name'],
                timestamp : System.currentTimeMillis(),
                qd        : req['qd'],
                permission: "1".equals(req['permission'] as String)
        ]
        if (pwd) {
            prop.put('password', MsgDigestUtil.SHA.digest2HEX(pwd.toString()))
        }
        adminMongo.getCollection('channel_users').save(new BasicDBObject((Map) prop))
        Crud.opLog(OpType.channel_add_user, prop)
        [code: 1]
    }

    /**
     * 设置渠道属性
     * @param req
     * @return
     */
    def add_properties(HttpServletRequest req) {
        Map properties = req.getParameterMap()
        def prop = new HashMap();
        properties.each { String k, String[] v ->
            if (!_id.equals(k)) {
                def value = NumberUtils.isNumber(v[0]) ? Integer.valueOf(v[0]) : v[0]
                prop.put(k, value);
            } else {
                prop.put(k, v[0]);
            }


        }
        prop.remove('callback')
        String id = prop.remove(_id) as String
        table().update($$(_id, id), $$($set: [properties: prop]))
        Crud.opLog(OpType.channel_add_prop, properties)
        [code: 1]
    }

    //生成微信渠道二维码
    def add_weixin_qrcode(HttpServletRequest req) {
        Object id = parseId(req);
        String appId = req['app_id']
        if (null == id || !StringUtils.endsWith(id.toString(), '_h5')) {
            return IMessageCode.CODE0;
        }

        Integer sence_id = Integer.valueOf((table().findOne($$(sence_id: [$ne: null]), $$(sence_id: 1,), $$(sence_id: -1))?.get('sence_id') ?: 0) as Integer) + 1
        if (sence_id >= 100000) {
            return Web.missParam();
        }
        if (table().count($$(sence_id: sence_id)) == 1) {
            return Web.missParam();
        }
        String url = "weixin/qrcode?sence_id=${sence_id}&app_id=${appId}"
        logger.debug('url is {}', url)
        Map res = Web.api(url) as Map
        if (res == null) {
            return [code: 0, error: 'weixin generate qrcode error']
        }
        String qrcode_img = res['img_url'] as String
        def data = $$(sence_id: sence_id, qrcode_img: qrcode_img)
        if (table().update($$(_id: id, sence_id: [$ne: sence_id]), new BasicDBObject($set, data)).getN() == 1) {
            Crud.opLog(OpType.channel_add_qrcode, data)
            return [code: 1, data: data]
        }
        [code: 0]
    }

    def list_user(HttpServletRequest req) {
        int p = Web.getPage(req)
        int size = Web.getPageSize(req)
        def query = new BasicDBObject()
        if (req['qd']) {
            query['qd'] = req['qd']
        }
        Crud.list(req, adminMongo.getCollection('channel_users'), query, ALL_FIELD, SJ_DESC)
    }


    def del_user(HttpServletRequest req) {
        adminMongo.getCollection('channel_users').remove(new BasicDBObject(_id, req[_id]))
        Crud.opLog(OpType.channel_del_user, [_id: req[_id]])
        [code: 1]
    }

    def talkingdata() {
        String resp = new URL("http://openapi.talkingdata.net:80/api/analytics/data/appprofile/?access_key=6680803e87344730b2e27d95408f7dd7&appkey=07C414F714C601B13A2D955538145C33").getText()
        def data = new JsonSlurper().parseText(resp)
        [code: 1, data: data]
    }

    def syn_umeng_data(HttpServletRequest req) {
        //static_channels(req)
        def channel = req['channel']
        def date_str = req['date']
        if (StringUtils.isEmpty(req['date'])) {
            return [code: 0]
        }
        def date = Date.parse("yyyy-MM-dd", date_str)
        def list = []
        def day = date.format("yyyyMMdd_")
        def coll = adminMongo.getCollection('stat_channels')
        Integer reqs = 0 as Integer
        ['53ab9ff256240b97cf0164a5', '544f71eafd98c5a62b002aa3'].each { String appkey ->
            def page = 1, per_page = 400
            def hasMore = true
            while (hasMore) {
                if (reqs >= 290) {
                    reqs = 0
                    Thread.sleep(16 * 60 * 1000L)
                }
                reqs++
                def data = new JsonSlurper().parseText(
                        new URL("http://api.umeng.com/channels?appkey=${appkey}&auth_token=wLL2nMK8Lcn0NhmJxxlU&per_page=${per_page}&page=${page++}&date=${date.format('yyyy-MM-dd')}").getText()
                ) as List
                if (data != null && data.size() > 0) {
                    // def mongo  = new Mongo(new com.mongodb. MongoURI('mongodb://10.0.5.32:10000,10.0.5.33:10000,10.0.5.34:10000/?w=1&slaveok=true'))
                    data.each { Map row ->
                        if (StringUtils.isEmpty(req['channel']) || channel.equals(row['channel'] as String)) {
                            //查询umeng自定义事件三日发言
                            def update = new BasicDBObject([active     : row['install'] as Integer,//新增用户
                                                            active_user: row['active_user'] as Integer,//日活
                                                            duration   : row['duration'] as String,//平均使用时长
                                                            timestamp  : date.clearTime().getTime()
                            ])
                            row.put('update', update)
                            list.add(row)
                        }
                    }
                }
                if (data == null || data.size() < per_page) {
                    hasMore = false
                }
            }
        }
        list.each { Map row ->
            def update = row['update'] as BasicDBObject
            try {
                if (row['id'] != null) {
                    if (reqs >= 290) {
                        reqs = 0
                        Thread.sleep(16 * 60 * 1000L)
                    }
                    reqs++
                    def content = new URL("http://api.umeng.com/events/parameter_list?appkey=53ab9ff256240b97cf0164a5" +
                            "&auth_token=wLL2nMK8Lcn0NhmJxxlU&period_type=daily&event_id=543ce217e8af9ceaa72f3847" +
                            "&start_date=${date.format('yyyy-MM-dd')}&end_date=${date.format('yyyy-MM-dd')}" +
                            "&channels=${row['id']}").getText("UTF-8")
                    def count = 0
                    if (StringUtils.isNotBlank(content) && content.length() >= 2) {
                        content = content.substring(1, content.length() - 1)
                        if (StringUtils.isNotBlank(content)) {
                            def listObj = new JsonSlurper().parse(new StringReader(content)) as List
                            if (listObj != null) {
                                for (Object item : listObj) {
                                    if ("新注册用户数发言率".equals(((Map) item).get('label') as String)) {
                                        count = ((Map) item).get('num') as Integer
                                    }
                                }
                            }
                        }
                    }
                    update.put("speechs", count)
                }
            } catch (Exception e) {

            }
            coll.update(new BasicDBObject('_id', "${day}${row['channel']}".toString()), new BasicDBObject('$set', update))
        }
        def before = new Date(date.getTime() - 24 * 3600 * 1000L)
        list.each { Map row ->
            def appkey = row['appkey']
            def beforeStr = before.format('yyyy-MM-dd')
            try {
                if (row['id'] != null) {
                    if (reqs >= 290) {
                        reqs = 0
                        Thread.sleep(16 * 60 * 1000L)
                    }
                    reqs++
                    def content = new URL("http://api.umeng.com/retentions?appkey=${appkey}&auth_token=wLL2nMK8Lcn0NhmJxxlU" +
                            "&start_date=${beforeStr}&end_date=${beforeStr}&period_type=daily" +
                            "&channels=${row['id']}").getText("UTF-8")
                    def rate = 0 as Double
                    if (StringUtils.isNotBlank(content)) {
                        def listObj = new JsonSlurper().parse(new StringReader(content)) as List
                        if (listObj != null && listObj.size() > 0) {
                            def obj = listObj[0] as Map
                            def retentionList = obj.get('retention_rate') as List
                            if (retentionList != null && retentionList.size() > 0) {
                                rate = new BigDecimal(retentionList[0] as String).toDouble()
                            }
                        }
                    }
                    coll.update(new BasicDBObject('_id', "${before.format("yyyyMMdd_")}${row['channel']}".toString()), new BasicDBObject('$set', ["retention": rate]))

                }
            } catch (Exception e) {

            }
        }
        //更新父渠道数据
        static_parent(req)
    }

    /**
     * 同步某天的数据,只支持单个渠道的同步操作
     */
    def static_channels(HttpServletRequest req) {
        def channelId = req['channel']
        def date_str = req['date']
        if (StringUtils.isEmpty(date_str) || StringUtils.isBlank(channelId)) {
            return [code: 0]
        }
        def date = Date.parse("yyyy-MM-dd", date_str)
        def users = mainMongo.getCollection('users')
        def finance_log = adminMongo.getCollection('finance_log')
        def coll = adminMongo.getCollection('stat_channels')

        Long begin = date.clearTime().getTime()
        def timeBetween = [$gte: begin, $lt: begin + 24 * 3600 * 1000L]
        def query = new BasicDBObject()
        if (StringUtils.isNotBlank(channelId)) {
            query = new BasicDBObject("_id", channelId)
        }
        adminMongo.getCollection('channels').find(query).toArray().each { BasicDBObject channnel ->
            def cId = channnel.removeField("_id")
            def user_query = new BasicDBObject(qd: cId, timestamp: timeBetween)
            def YMD = new Date(begin).format("yyyyMMdd")
            def st = new BasicDBObject(_id: "${YMD}_${cId}" as String, qd: cId, timestamp: begin)
            def regUsers = users.find(user_query, new BasicDBObject('status', 1)).toArray()*._id
            if (regUsers != null && regUsers.size() > 0) {
                st.append("regs", regUsers).append("reg", regUsers.size())
            }

            //优化后
            def iter = finance_log.aggregate(
                    new BasicDBObject('$match', [via: [$ne: 'Admin'], qd: cId, timestamp: timeBetween]),
                    new BasicDBObject('$project', [cny: '$cny', user_id: '$user_id']),
                    new BasicDBObject('$group', [_id: null, cny: [$sum: '$cny'], count: [$sum: 1], pays: [$addToSet: '$user_id']])
            ).results().iterator()

            if (iter.hasNext()) {
                def obj = iter.next()
                obj.removeField('_id')
                st.putAll(obj)
                st['pay'] = (st['pays'] as List).size()
                if (!channnel.isEmpty()) {
                    st.putAll(channnel)
                }
            }
            coll.update(new BasicDBObject('_id', st.removeField('_id')), new BasicDBObject('$set', st), true, false)
        }
        return [code: 1]
    }

    /**
     * 同步父渠道数据信息，支持单个父渠道更新
     * @param req
     * @return
     */
    def static_parent(HttpServletRequest req) {
        def stat_channels = adminMongo.getCollection('stat_channels')
        def channelId = req['channel']
        def date_str = req['date']
        if (StringUtils.isEmpty(date_str)) {
            return [code: 0]
        }
        def date = Date.parse("yyyy-MM-dd", date_str)

        Map<String, DBObject> parentMap = new HashMap<String, DBObject>()
        def channels = table().find(new BasicDBObject(parent_qd: [$ne: null]), new BasicDBObject(parent_qd: 1)).toArray()
        for (DBObject obj : channels) {
            String parent_id = obj.get("parent_qd") as String
            if ((channelId && parent_id.equals(channelId)) || !channelId) {
                parentMap.put(parent_id, obj)
            }
        }

        for (String key : parentMap.keySet()) {
            DBObject obj = parentMap.get(key)
            Long begin = date.clearTime().getTime()
            def parent_id = obj.get("parent_qd") as String
            def childqds = table().find(new BasicDBObject(parent_qd: parent_id), new BasicDBObject(_id: 1)).toArray()
            DBObject query = new BasicDBObject('qd', [$in: childqds.collect {
                ((Map) it).get('_id').toString()
            }]).append("timestamp", begin)
            def stat_child_channels = stat_channels.find(query).toArray()
            Integer payNum = 0
            Integer regNum = 0
            Integer cny = 0
            Integer count = 0
            Integer daylogin = 0
            Integer day7login = 0
            Integer day30login = 0
            Integer stay1 = 0
            Integer stay3 = 0
            Integer stay7 = 0
            Integer stay30 = 0
            Integer cpa2 = 0
            Integer cpa1 = 0
            Integer cpa3 = 0
            Integer visitors = 0
            Integer active = 0
            Integer active_user = 0
            Integer speechs = 0
            Boolean hasVisitor = Boolean.FALSE
            int size = stat_child_channels.size()
            //println "stat_child_channels.size-------------->:$size"
            for (DBObject myObj : stat_child_channels) {
                Integer currentPayNum = (myObj.get("pay") != null) ? myObj.get("pay") as Integer : 0
                payNum += currentPayNum
                Integer currentRegNum = (myObj.get("reg") != null) ? myObj.get("reg") as Integer : 0
                regNum += currentRegNum
                Integer currentCny = (myObj.get("cny") != null) ? myObj.get("cny") as Integer : 0
                cny += currentCny
                Integer currentCount = (myObj.get("count") != null) ? myObj.get("count") as Integer : 0
                count += currentCount
                Integer currentDaylogin = (myObj.get("daylogin") != null) ? myObj.get("daylogin") as Integer : 0
                daylogin += currentDaylogin
                Integer currentDay7login = (myObj.get("day7login") != null) ? myObj.get("day7login") as Integer : 0
                day7login += currentDay7login
                Integer currentDay30login = (myObj.get("day30login") != null) ? myObj.get("day30login") as Integer : 0
                day30login += currentDay30login
                def myStay = myObj.get("stay") as Map
                if (myStay != null) {
                    Integer currentStay1 = (myStay.get("1_day") != null) ? myStay.get("1_day") as Integer : 0
                    stay1 += currentStay1
                    Integer currentStay3 = (myStay.get("3_day") != null) ? myStay.get("3_day") as Integer : 0
                    stay3 += currentStay3
                    Integer currentStay7 = (myStay.get("7_day") != null) ? myStay.get("7_day") as Integer : 0
                    stay7 += currentStay7
                    Integer currentStay30 = (myStay.get("30_day") != null) ? myStay.get("30_day") as Integer : 0
                    stay30 += currentStay30
                }
                Integer currentCpa3 = (myObj.get("cpa3") != null) ? myObj.get("cpa3") as Integer : 0
                cpa3 += currentCpa3
                Integer currentCpa2 = (myObj.get("cpa2") != null) ? myObj.get("cpa2") as Integer : 0
                cpa2 += currentCpa2
                Integer currentCpa1 = (myObj.get("cpa1") != null) ? myObj.get("cpa1") as Integer : 0
                cpa1 += currentCpa1
                Integer currentActive = (myObj.get("active") != null) ? myObj.get("active") as Integer : 0
                active += currentActive
                Integer currentActive_user = (myObj.get("active_user") != null) ? myObj.get("active_user") as Integer : 0
                active_user += currentActive_user
                Integer currentSpeechs = (myObj.get("speechs") != null) ? myObj.get("speechs") as Integer : 0
                speechs += currentSpeechs
                if (myObj.containsField("visitors")) {
                    hasVisitor = Boolean.TRUE
                    visitors += (myObj.get("visitors") != null) ? myObj.get("visitors") as Integer : 0
                }

            }
            def YMD = new Date(begin).format("yyyyMMdd")
            def st = new BasicDBObject(_id: "${YMD}_${parent_id}" as String, qd: parent_id, timestamp: begin)
            def setObject = new BasicDBObject(qd: parent_id, timestamp: begin, cpa1: cpa1, active: active, active_user: active_user, speechs: speechs)
            if (hasVisitor) setObject.append('visitors', visitors)
            setObject.append('pay', payNum).append('reg', regNum).append('cny', cny).append('count', count)
            stat_channels.findAndModify(st, null, null, false, new BasicDBObject($set: setObject), false, false)
        }
    }

}
