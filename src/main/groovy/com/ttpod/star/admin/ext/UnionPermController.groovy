package com.ttpod.star.admin.ext

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.common.util.MsgDigestUtil
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.admin.Web
import com.ttpod.star.admin.crud.ChannelController
import com.ttpod.star.model.OpType
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.$set
import static com.ttpod.rest.common.doc.MongoKey.$setOnInsert
import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.util.WebUtils.$$

@Rest
class UnionPermController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(UnionPermController.class)
    @Resource
    UnionController unionController
    @Resource
    ChannelController channelController

    DBCollection channels() { adminMongo.getCollection('channels') }

    DBCollection channel_users() { adminMongo.getCollection('channel_users') }

    final Long DAY_MILLON = 24 * 3600 * 1000L

    //必须有权限的用户才能使用此api
    private permCheck(HttpServletRequest req, Closure closure) {
        def user = req.getSession().getAttribute('union_user')
        if (null == user) {
            return [code: 0, msg: "请登录"]
        }
        def timestamp = user['timestamp'] as Long
        //若session中的信息保存超过一天,强制更新session
        def channel = null
        if (timestamp == null || (System.currentTimeMillis() - timestamp > DAY_MILLON)) {
            user = adminMongo.getCollection("channel_users").findOne(
                    new BasicDBObject(_id: user['name']), new BasicDBObject('password', 0))
            logger.debug("doInQd user : {}", user)
            if (null == user) {
                return [code: 0, msg: '用户失效，请退出后重新登录']
            }
            if (!user['qd']) {
                return [code: 0, msg: '权限不足']
            }
            channel = adminMongo.getCollection("channels").findOne(user['qd'], new BasicDBObject('comment', 0))
            if (!channel) {
                logger.debug("channel: {}", channel)
                return [code: 0, msg: '权限不足']
            }
            def qds = [user['qd']] as List
            //添加子渠道信息
            def query = new BasicDBObject("parent_qd": user['qd'])
            adminMongo.getCollection("channels").find(query, new BasicDBObject('comment', 0))
                    .toArray().each { BasicDBObject obj ->
                qds.add(obj['_id'])
            }
            user.put("qdinfo", channel)
            user.put("qdall", qds)
            user.put("timestamp", System.currentTimeMillis())//加时间戳
            req.getSession().setAttribute("union_user", user)
        }
        def childQd = user['qdall'] as List
        def qdId = user['qd']

        logger.debug("permCheck : {}", user)
        def permission = user['permission'] as Boolean
        if (!permission) {
            return [code: 0, msg: '权限不足']
        }
        closure.call(qdId, childQd, user)
    }

    def set_cpa(HttpServletRequest req) {
        permCheck(req) { String qid, List<String> ids, Map user ->
            def _id = req[_id]
            logger.debug("set_cpa params _id: {}", _id)
            if(StringUtils.isBlank(_id)||_id.length() < 9){
                return [code:0,msg:"参数不足"]
            }
            def qd = _id.substring(9)
            
            if (StringUtils.isBlank(qd) || qd.equals(qid) || !ids.contains(qd)) {
                logger.debug("set_cpa params error  qd: {}, qid:{}", qd, qid)
                return [code: 0, msg: "权限不足"]
            }
            def cpa1 = req['cpa1'] as Integer
            def cpa2 = req['cpa2'] as Integer
            def cpa3 = req['cpa3'] as Integer
            def update = new BasicDBObject()
            if (cpa1 != null) {
                update.append('cpa1', cpa1)
            }
            if (cpa2 != null) {
                update.append('cpa2', cpa2)
            }
            if (cpa2 != null) {
                update.append('cpa3', cpa2)
            }
            if (update.size() > 0) {
                return [code: adminMongo.getCollection('stat_channels').update($$(_id: _id),
                        $$($set, update)).getN()]
            }
            return [code: 1]
        }
    }

    def channel_list(HttpServletRequest req) {
        permCheck(req) { String qid, List<String> ids, Map user ->
            def query = new BasicDBObject()
            def inId = []
            ids.collect { if (!qid.equals(it)) inId.add(it) }
            query.append('_id', [$in: inId])
            def id = req[_id]
            if (id.isNotBlank()) {
                query.put(_id, id)
            }
            def name = req['name']
            if (name.isNotBlank()) {
                query.put("name", name)
            }
            Crud.list(req, channels(), query, ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
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
    }

    /**
     * 子渠道创建
     * @param req
     * @param closure
     * @return
     */
    def add_channel(HttpServletRequest req) {
        permCheck(req) { String qid, List<String> ids, Map user ->
            def _id = req['_id'] as String
            def name = req['name'] as String
            def comment = req['comment'] as String
            def type = req['type'] as String
            def reg_discount = req['reg_discount'] as String
            def active_discount = req['active_discount'] as String
            def client = (req['client'] ?: "2") as String
            //只能添加子渠道用户
            if (StringUtils.isBlank(_id)) {
                return [code: 0, msg: "请输入渠道号"]
            }
            if (StringUtils.isBlank(type)) {
                return [code: 0, msg: "请输入渠道类型"]
            }
            if (StringUtils.isBlank(name)) name = _id
            def prop = [_id      : _id, name: name, type: type, client: client, comment: comment,
                        timestamp: System.currentTimeMillis(), parent_qd: qid
            ] as Map
            def currentDay = new Date().clearTime().getTime()
            if (StringUtils.isNotBlank(reg_discount) && reg_discount.isInteger()) {
                def map = new HashMap()
                map.put(currentDay, reg_discount as Integer)
                prop.put('reg_discount', map)
            }
            if (StringUtils.isNotBlank(active_discount) && active_discount.isInteger()) {
                def map = new HashMap()
                map.put(currentDay, active_discount as Integer)
                prop.put('active_discount', map)
            }
            DBObject newUser = channels().findAndModify($$(_id: _id), null, null, false,
                    new BasicDBObject($setOnInsert, prop), false, true);
            if (newUser != null) {
                return [code: 0, msg: "渠道号重复"]
            }
            ids.add(_id)
            operation(OpType.channel_add_user.name(), prop,
                    [_id: user['_id'], nick_name: user['nick_name'], qd: user['qd']])
            [code: 1]
        }
    }

    /**
     * 子渠道更新
     * @param req
     * @param closure
     * @return
     */
    def update_channel(HttpServletRequest req) {
        permCheck(req) { String qid, List<String> ids, Map user ->
            def _id = req['_id'] as String
            if (StringUtils.isBlank(_id) || _id.equals(qid) || ids == null || !ids.contains(_id)) {
                return [code: 0, msg: "权限不足"]
            }
            def name = req['name'] as String
            def comment = req['comment'] as String
            def type = req['type'] as String
            def reg_discount = req['reg_discount'] as String
            def active_discount = req['active_discount'] as String
            def prop = new HashMap()
            if (StringUtils.isNotBlank(name)) prop.put('name', name)
            if (StringUtils.isNotBlank(type)) prop.put('type', type)
            if (StringUtils.isNotBlank(comment)) prop.put('comment', comment)
            def currentDay = new Date().clearTime().getTime()
            if (StringUtils.isNotBlank(reg_discount) && reg_discount.isInteger()) {
                prop.put("reg_discount.${currentDay}".toString(), reg_discount as Integer)
            }
            if (StringUtils.isNotBlank(active_discount) && active_discount.isInteger()) {
                prop.put("active_discount.${currentDay}".toString(), active_discount as Integer)
            }
            if (prop.size() == 0) {
                return [code: 0, msg: "请输入更新内容"]
            }
            DBObject newUser = channels().findAndModify($$(_id: _id), null, null, false,
                    new BasicDBObject('$set', prop), false, true);
            if (newUser == null) {
                return [code: 0, msg: "渠道号重复"]
            }
            operation(OpType.channel_add_user.name(), prop,
                    [_id: user['_id'], nick_name: user['nick_name'], qd: user['qd']])
            [code: 1]
        }
    }

    /**
     * 子渠道用户设置
     * @param req
     * @param closure
     * @return
     */
    def add_user(HttpServletRequest req) {
        permCheck(req) { String qid, List<String> ids, Map user ->
            def _id = req['name'] as String
            def nick_name = req['nick_name'] as String
            def pwd = req['password'] as String
            def qd = req['qd'] as String
            //只能添加子渠道用户
            if (StringUtils.isBlank(qd) || qd.equals(qid) || ids == null || !ids.contains(qd)) {
                return [code: 0, msg: '权限不足']
            }
            if (StringUtils.isBlank(pwd)) {
                return [code: 0, msg: '密码为空']
            }

            def prop = [nick_name: nick_name ?: _id, _id: _id, qd: qd, timestamp: System.currentTimeMillis()] as Map

            def newUser = channel_users().findAndModify($$(_id: _id), null, null, false,
                    new BasicDBObject($setOnInsert, $$(prop).append('password', MsgDigestUtil.SHA.digest2HEX(pwd.toString()))),
                    false, true
            )
            if (newUser != null) {
                return [code: 0, msg: "用户已存在"]
            }
            operation(OpType.channel_add_user.name(), prop,
                    [_id: user['_id'], nick_name: user['nick_name'], qd: user['qd']])
            [code: 1]
        }
    }

    /**
     * 查询子渠道用户信息
     * @param req
     * @return
     */
    def list_user(HttpServletRequest req) {
        permCheck(req) { String qid, List<String> ids, Map user ->
            def _id = req['qd'] as String
            def query = new BasicDBObject()
            //默认查询渠道下所有用户信息
            if (StringUtils.isNotBlank(_id)) {
                if (ids == null || !ids.contains(_id)) {
                    return [code: 0, msg: '权限不足']
                }
                query['qd'] = _id
            } else {
                def inId = []
                ids.collect { if (!qid.equals(it)) inId.add(it) }
                query.append('qd', [$in: inId])
            }
            def sort = new BasicDBObject('qd', 1).append('timestamp', -1)
            Crud.list(req, channel_users(), query, $$('password', 0), sort)
        }
    }


    def del_user(HttpServletRequest req) {
        permCheck(req) { String qid, List<String> ids, Map user ->
            def _id = req[_id] as String
            if (StringUtils.isBlank(_id)) {
                return [code: 0, msg: '渠道号为空']
            }
            def inId = []
            ids.collect { if (!qid.equals(it)) inId.add(it) }
            def remove = channel_users().findAndModify(new BasicDBObject([_id: _id, qd: [$in: inId]]),
                    null, null, true, null, false, false)
            if (remove == null) {
                return [code: 0, msg: "无此用户"]
            }
            operation(OpType.channel_del_user.name(), [_id: req[_id]],
                    [_id: user['_id'], nick_name: user['nick_name'], qd: user['qd']])
            [code: 1]
        }
    }

    private static final String LOG_COLL_NAME = "ops"

    private static void operation(String type, Object data, Map session) {

        BasicDBObject obj = new BasicDBObject()
        Long tmp = System.currentTimeMillis()
        obj.put(_id, tmp)
        obj.put("type", type)
        obj.put("session", session)
        obj.put("data", data)
        obj.put(timestamp, tmp)
        logger.info('==============>:' + obj)
        adminMongo.getCollection(LOG_COLL_NAME).save(obj)
    }

}
