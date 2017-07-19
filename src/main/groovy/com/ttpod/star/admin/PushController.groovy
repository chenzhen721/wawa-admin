package com.ttpod.star.admin

import com.gexin.rp.sdk.template.NotificationTemplate
import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.WriteConcern
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.IMessageCode
import com.ttpod.rest.web.Crud
import com.ttpod.star.common.util.ExportUtils
import com.ttpod.star.common.util.IMUtil
import com.ttpod.star.common.util.StrUtils
import com.ttpod.star.model.IMType
import com.ttpod.star.model.SysMsgType
import com.ttpod.star.model.UmengEventType
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.CollectionUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.groovy.CrudClosures.Timestamp

/**
 * 官方运营推送，官方来吼消息+友盟APP通知栏消息
 */
@RestWithSession
class PushController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(PushController.class)
    private static Long MESSAGE_EXPIRE = 60 * 60 * 1000L
    private static final Long DAYMILLI = 24 * 60 * 60 * 1000L
    //单次推送的最大用户数
    private static final int max = 5
    private static int pos = 0
    private static final Lock lock = new ReentrantLock()
    @Resource
    public WriteConcern writeConcern
    Closure Str = {it}
    public final Map<String, Closure> props = [
            _id: { it == null ? "${Web.currentUserId}_${System.currentTimeMillis()}" as String: it as String},
            user_ids: {String ids-> if (StringUtils.isNotBlank(ids)) { ids.split(",").collect { it as Integer}}},
            push_type: { StringUtils.isBlank(it as String) ? 0 : it as Integer }, //默认为：0，推送制定用户；1，全部用户
            text: Str, link_url:Str, img_url: Str, is_notify: {Boolean.valueOf((String)it)}, umeng_title: Str, umeng_text: Str,
            umeng_event: Str, umeng_event_room_id: {StringUtils.isBlank(it as String) ? null : it as Integer}, umeng_event_url: Str,timestamp:Timestamp, status:{StringUtils.isBlank(it as String) ? 0 : it as Integer},
            stime:{String str->  StringUtils.isBlank(str) ? null : Web.getTime(str).getTime()},
            etime:{String str->  StringUtils.isBlank(str) ? null : Web.getTime(str).getTime()}
    ]

    DBCollection table() { logMongo.getCollection("app_info") }

    DBCollection finance_log = adminMongo.getCollection('finance_log')
    DBCollection push_log = logMongo.getCollection('push_log')
    final Crud crud = new Crud(push_log, props)

    def list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        Crud.list(req, push_log, query.get(), ALL_FIELD, SJ_DESC)
    }

    def add(HttpServletRequest req) {
        def map = getParam(req)
        def result = check(map)
        if (result != null) {
            return result
        }
        if (1 == push_log.save($$(map), writeConcern).getN()) {
            Crud.opLog(push_log.getName() + "_add", map)
            if (map.get("status") == 1) {
                //推送信息
                def message = buildMessage(map)
                sendToUser(map, message)
            }
            return OK()
        }
        logger.error("推送信息写入错误")
        return [code: 0]
    }

    def edit(HttpServletRequest req) {
        def map = getParam(req)
        def id = map.remove(_id)
        if (id == null) {
            return IMessageCode.CODE0
        }

        if(map.size() > 0 && push_log.update($$(_id: id, status: 0), $$($set,map), false, false, writeConcern).getN() == 1){
            map.put(_id, id)
            Crud.opLog(push_log.getName() + "_edit", map)
            def result = push_log.findOne($$(_id, id))?.toMap()
            if (result.get("status") == 1) {
                //推送信息
                def message = buildMessage(result)
                sendToUser(map, message)
            }
        }
        return IMessageCode.OK
    }

    def change_status(HttpServletRequest req) {
        def id = req.getParameter("_id") as String
        def status = req.getParameter("status") as String
        if (StringUtils.isBlank(id) || StringUtils.isBlank(status)) {
            return Web.missParam()
        }
        if ("0".equals(status)) {
            return [code: 0, msg: "不支持更新至当前状态"]
        }
        return edit(req)
    }

    private Map getParam(HttpServletRequest req) {
        //判断是add还是edit
        Boolean isAdd = Boolean.FALSE
        Object id = req.getParameter(_id)
        if(null == id){
            isAdd = Boolean.TRUE
        }


        Map map = new HashMap()
        for (Map.Entry<String, Closure> entry : props.entrySet()) {
            String key = entry.getKey()
            if (isAdd) {
                Object val = entry.getValue().call(req.getParameter(key))
                if (val != null) {
                    map.put(key, val)
                }
            } else {
                if(key.equals(_id)){
                    continue
                }
                String strValue = req.getParameter(key)
                if(strValue != null){
                    Object val = entry.getValue().call(strValue)
                    if (val != null) {
                        map.put(key, val)
                    }
                }
            }
        }
        if (!isAdd) {
            map.put(_id, id)
        }
        return map
    }

    private Map check(Map map) {
        def msg = ""
        if (StringUtils.isBlank(map.get("text") as String)
                && StringUtils.isBlank(map.get("img_url") as String)
                && StringUtils.isNotBlank(map.get("link_url") as String)) {
            msg = "文字或图片至少填一个"
        }
        if (map.get('push_type') == null) {
            msg = "推送类型为空"
        }
        if (0 == map.get('push_type') && CollectionUtils.isEmpty(map.get("user_ids") as Collection)) {
            msg = "发送用户ID为空"
        }
        if (map.get("is_notify") as Boolean) {
            if (UmengEventType.打开房间.getEventType().equals(map.get("umeng_event")) && StringUtils.isBlank(map.get("umeng_event_room_id") as String)) {
                msg = "房间ID为空"
            }
            if (UmengEventType.跳至页面.getEventType().equals(map.get("umeng_event")) && StringUtils.isBlank(map.get("umeng_event_url") as String)) {
                msg = "H5页面链接地址为空"
            }
        }
        if (StringUtils.isNotBlank(msg)) {
            return [code:0, msg: msg]
        }
        return null
    }

    private Map buildMessage(Map map) {
        def result = [
                "message": [
                    "action": IMType.系统消息.getAction(),
                    "data": [
                            "text": StrUtils.defaultIfBlank(map.get("text") as String, ""),
                            "type": SysMsgType.官方运营.ordinal(),
                            "link_url": StrUtils.defaultIfBlank(map.get("link_url") as String, ""),
                            "img_url": StrUtils.defaultIfBlank(map.get("img_url") as String, ""),
                            "ts"         : System.currentTimeMillis(),
                            "expire_time": System.currentTimeMillis() + MESSAGE_EXPIRE
                    ]
                ],
                "user_ids": map.get("user_ids")
        ] as Map
        if (map.get("is_notify")) {
            result.putAll([
                    "umeng": [
                            "params": [
                                "title": map.get("umeng_title"),
                                "text": map.get("umeng_text"),
                                "expire_time": System.currentTimeMillis() + MESSAGE_EXPIRE,
                            ],
                            "extra": [
                                "event": map.get("umeng_event"),
                                "room_id": map.get("umeng_event_room_id"),
                                "url": map.get("umeng_event_url")
                            ]
                    ]
            ] as Map)
        }
        //支持不发送APP内消息
        if (StringUtils.isBlank(map.get("text") as String) && StringUtils.isBlank(map.get("img_url") as String)) {
            result.remove('message')
        }
        if (result['umeng'] != null) {
            if (map.get("umeng_event_room_id") == null) {
                (result["umeng"]["extra"] as Map).remove("room_id")
            }
            if (map.get("umeng_event_url") == null) {
                (result["umeng"]["extra"] as Map).remove("url")
            }
        }
        return result
    }

    // 推送信息
    private void sendToUser(Map map, Object body) {
        String path = IMUtil.SEND_TO_GROUP
        if (1 == map.get('push_type')) {
            path = IMUtil.SEND_TO_ALL
        }
        IMUtil.sendToUser(path, body)
    }

    // ---------------------友盟消息推送
    def push_sign_user(Integer userId, String title, String text) {
        /*def register_info = table().findOne(userId)
        if(register_info == null) return [code : 0];
        Integer type = register_info.get('type') as Integer
        if(type){
            String device_token = register_info.get('device_token')
            if(StringUtils.isEmpty(device_token)) return [code : 0];
            def obj = $$(_id, "single_${userId}_".toString() + System.currentTimeMillis())
            try{
                if(type.equals(1)){
                    MsgPushUmengUtils.sendAndroidUnicast(device_token, title, title, text)
                }else{
                    if(isTest){
                        MsgPushUmengUtils.sendTestIOSUnicast(device_token, title, text)
                    }else{
                        MsgPushUmengUtils.sendIOSUnicast(device_token, title, text)
                    }
                }
                //推送信息持久化

                obj.put('timestamp', System.currentTimeMillis())
                obj.put('type', type)//推送类型
                obj.put('user_id', userId)
                obj.put('device_token', device_token)
                obj.put('text', text)
                obj.put('title', title)
            }catch (Exception e){
                logger.error("push_sign_user Exception : {}", e)
            }
            push_log.insert(obj)
        }*/

        IMUtil.sendToUsers([
                "message" : [
                        "action": "msg.system",
                        "data"  : [
                                "title"      : title,
                                "type":2,
//                                "image": one['image'],
//                                "link": one['link'],
                                "text"       : text,
                                "ts"         : System.currentTimeMillis(),
                                "expire_time": System.currentTimeMillis() + MESSAGE_EXPIRE
                        ]
                ],
                "user_ids" : [userId],
                "isNotify": 1,
                "isSave"  : 1,
                extra     : [
                        event: "msg_system"
                ]
        ]);

        return [code: 1, msg: '成功']
    }

    // ---------------------------个推消息  BEGIN

    //针对单个用户发送推送信息
    def single_user(HttpServletRequest req) {
        /*def map = getMessage(req)
        if (map == null) {
            return [code: 0, msg: '请输入正确的参数']
        }
        Integer userId = req.getParameter('_id') as Integer
        def cid = table().findOne($$(_id: userId))?.get('cid') as String
        if (StringUtils.isBlank(cid)) {
            return [code: 0, msg: '无个推参数']
        }
        IGtPush push = new IGtPush(HOST, APPKEY, MASTERSECRET);
        push.connect();

        NotificationTemplate template = NotificationTemplate(map);

        SingleMessage message = new SingleMessage();
        message.setOffline(true);
        message.setOfflineExpireTime(72 * 1000 * 3600);
        message.setData(template);

        Target target1 = new Target();
        target1.setAppId(APPID);
        target1.setClientId(cid);

        IPushResult ret = push.pushMessageToSingle(message, target1);
        def result = 'ok'.equals(ret.getResponse().get('result')) ?: false
        def cId = ret.getResponse().get('taskId')
        //推送信息持久化
        def obj = new BasicDBObject(map)
        obj.put('_id', "single_${userId}_".toString() + System.currentTimeMillis())
        obj.put('timestamp', System.currentTimeMillis())
        obj.put('type', 1)//推送类型
        obj.put('result', result ? 1 : 0)
        if (result) obj.put('cid', cId)
        obj.put('users', 1)
        obj.put('count', 1)
        push_log.save(obj)
        return [code: 1, msg: '成功']*/
    }

    //针对某一类用户发送推送信息
    def group_user(HttpServletRequest req) {
       /* def map = getMessage(req)
        if (map == null) {
            return [code: 0, msg: '请输入正确的参数']
        }
        Map userMap = fetchUser(req) as Map
        if (userMap.get('code') as Integer == 0) {
            return userMap
        }
        userMap.remove('userlist')
        def cidList = userMap.remove('data') as List
        pos = 0
        final int size = cidList.size()
        final String _id = 'group' + System.currentTimeMillis()
//        final String groupId = "toList_Push_" + System.currentTimeMillis()
        final IGtPush push = new IGtPush(HOST, APPKEY, MASTERSECRET);
        push.connect();
        NotificationTemplate template = NotificationTemplate(map);
        ListMessage message = new ListMessage();
        message.setData(template);
        message.setOffline(true);
        message.setOfflineExpireTime(72 * 1000 * 3600);
        final String taskId = push.getContentId(message);

        def obj = new BasicDBObject(map)
        obj.put('_id', _id)
        obj.put('timestamp', System.currentTimeMillis())
        obj.put('type', 2)
        obj.put('users', size)
        obj.put('result', size > 0 ? 2 : 0)
        obj.put('cid', taskId)
        push_log.update($$(_id: obj.remove('_id')), obj, true, false)
        for (int i = 0; i < 10 && size > 0; i++) {
            new Thread() {
                private Boolean hasMore = true
                private int count = 0

                @Override
                void run() {
                    while (hasMore) {
                        int start = 0, end = 0
                        lock.lock()
                        try {
                            if (size <= pos) {
                                hasMore = false
                            } else if (size > pos + max) {
                                start = pos
                                end = pos + max
                                pos = pos + max
                            } else if (size <= pos + max) {
                                start = pos
                                end = size
                                pos = size
                                hasMore = false
                            }
                        } finally {
                            lock.unlock()
                        }
                        def list = cidList.subList(start, end)
                        if (list.size() > 0) {
                            List<Target> targets = new ArrayList<Target>();
                            list.each { String cid ->
                                Target target = new Target();
                                target.setAppId(APPID);
                                target.setClientId(cid);
                                targets.add(target);
                            }
                            if (targets.size() > 0) {
                                IPushResult ret = push.pushMessageToList(taskId, targets);
                                def result = 'ok'.equals(ret.getResponse().get('result')) ?: false
                                if (result) {
                                    push_log.update($$(_id: _id), $$($inc: [count: targets.size()]))
                                    count = count + targets.size()
                                }
                                logger.debug('push taskId:' + taskId + ' group user push result:' +
                                        ret.getResponse().get('result') + ' contentId:' + ret.getResponse().get('contentId'))
                            }
                        }
                    }
                    push_log.update($$(_id: _id), $$($inc: [count: count]))
                    push_log.findAndModify($$(_id: _id, result: 2), $$($set: [result: 1]))
                }
            }.start()
        }
        return userMap*/
    }

    //针对激活用户发送推送信息
    def all_user(HttpServletRequest req) {
        /*def map = getMessage(req)
        if (map == null) {
            return [code: 0, msg: '请输入正确的参数']
        }
        IGtPush push = new IGtPush(HOST, APPKEY, MASTERSECRET);
        push.connect();
        NotificationTemplate template = NotificationTemplate(map);
        AppMessage message = new AppMessage();
        message.setData(template);

        message.setOffline(true);
        message.setOfflineExpireTime(72 * 1000 * 3600);

        List<String> appIdList = new ArrayList<String>();
        List<String> phoneTypeList = new ArrayList<String>();

        appIdList.add(APPID);
        phoneTypeList.add("ANDROID");

        message.setAppIdList(appIdList);
        message.setPhoneTypeList(phoneTypeList);
        IPushResult ret = push.pushMessageToApp(message, "");
        def result = 'ok'.equals(ret.getResponse().get('result')) ?: false
        def cId = ret.getResponse().get('contentId')
        //推送信息持久化
        def obj = new BasicDBObject(map)
        obj.put('_id', 'all' + System.currentTimeMillis())
        obj.put('timestamp', System.currentTimeMillis())
        obj.put('type', 3)
        obj.put('result', result ? 1 : 0)
        if (result) obj.put('cid', cId)
        push_log.save(obj)
        return [code: 1, msg: '成功']*/
    }


    private static NotificationTemplate NotificationTemplate(Map map)
            throws Exception {
        /*NotificationTemplate template = new NotificationTemplate();
        template.setAppId(APPID);
        template.setAppkey(APPKEY);
        template.setTitle(map.get('title') as String);
        template.setText(map.get('text') as String);
        template.setLogo("app_icon.png");
        template.setIsRing(false);
        template.setIsVibrate(true);
        template.setIsClearable(true);
        template.setTransmissionType(2);
        template.setTransmissionContent(map.get('content') as String);
        return template;*/
    }

    /*private Map getMessage(HttpServletRequest req) {
        def title = req.getParameter('title') as String
        def text = req.getParameter('text') as String
        def type = req.getParameter('type') as String
        def activity_url = req.getParameter('activity_url') as String
        def activity_title = req.getParameter('activity_title') as String
        def star_room_id = req.getParameter('star_room_id') as String
        def live_room_id = req.getParameter('live_room_id') as String
        def ring = req.getParameter('ring') as String//是否响铃
        def vibrate = req.getParameter('vibrate') as String//是否震动
        if (StringUtils.isBlank(title) || StringUtils.isBlank(text)) {
            return null
        }
        if (StringUtils.isBlank(type)) {
            type = '0'
        }
        Boolean success = Boolean.FALSE
        def str = new HashMap<String, String>()
        switch (type) {
            case '1':
                if (StringUtils.isNotBlank(activity_title) && StringUtils.isNotBlank(activity_url)) {
                    success = Boolean.TRUE
                    str.put('action_type', type)
                    str.put('activity_title', activity_title)
                    str.put('activity_url', activity_url)
                }
                break
            case '2':
                if (StringUtils.isNotBlank(star_room_id)) {
                    success = Boolean.TRUE
                    str.put('action_type', type)
                    str.put('star_room_id', star_room_id)
                }
                break
            case '3':
                if (StringUtils.isNotBlank(live_room_id) && StringUtils.isNotBlank(activity_url)) {
                    success = Boolean.TRUE
                    str.put('action_type', type)
                    str.put('live_room_id', live_room_id)
                    str.put('activity_url', activity_url)
                }
                break
            case '4':
                str.put('action_type', type)
                success = Boolean.TRUE
                break
            case '5':
                str.put('action_type', type)
                success = Boolean.TRUE
                break
            default:
                success = Boolean.TRUE
        }
        if (!success) {
            return null
        }
        def content = JSONUtil.beanToJson(str)
        return [title: title, text: text, content: content]
    }*/

    def fetchUser(HttpServletRequest req) {
        def end = new Date().clearTime().getTime()
        def payType = req.getParameter('payType') as Integer//查询是否付费用户(0:未付费(1月内)，1:付费)
        //查询登录用户（0:全部，1:三日登录，2:三日未登录，3:七日登录，4:七日未登录）
        def loginType = req.getParameter('loginType') as Integer
        if (payType == null || loginType == null) {
            return [code: 0, msg: '请输入正确的参数']
        }
        def payList = new HashSet<Integer>(10000)
        def payTypeList = new HashSet<Integer>(200000)
        //查询出所有付费用户ID
        finance_log.aggregate(
                $$('$match', ['via': [$ne: 'Admin']]),
                $$('$project', [_id: '$user_id']),
                $$('$group', [_id: '$_id'])
        ).results().each { BasicDBObject obj ->
            def userId = obj.get('_id') as Integer
            payList.add(userId)
        }
        def query = new BasicDBObject()
//        if (payType == 0) {
//            query.append('_id', [$nin: payList])
//        }
//        if (payType == 1 && loginType != 0) {
//            query.append('_id', [$in: payList])
//        }
        switch (loginType) {
            case 0:
                if (payType == 0) query.append('last_login', [$gte: end - 30 * DAYMILLI, $lt: end])
                break;
            case 1://三日登录
                query.append('last_login', [$gte: end - 3 * DAYMILLI, $lt: end])
                break;
            case 2://三日未登录
                def map = [$lt: end - 3 * DAYMILLI]
                if (payType == 0) {
                    map.put('$gte', end - 30 * DAYMILLI)
                }
                query.put('last_login', map)
                break;
            case 3://七日登录
                query.append('last_login', [$gte: end - 7 * DAYMILLI, $lt: end])
                break;
            case 4://七日未登录
                def map = [$lt: end - 7 * DAYMILLI]
                if (payType == 0) {
                    map.put('$gte', end - 30 * DAYMILLI)
                }
                query.put('last_login', map)
                break;
        }
        if (query.size() > 0) {
            //查询数据量在30w条以上，需分页提取数据
            ExportUtils.list(req, users(), query, $$(_id: 1), NATURAL_DESC) { List<BasicDBObject> list ->
                for (BasicDBObject obj : list) {
                    def id = obj.get('_id') as Integer
                    if ((payType == 0 && !payList.contains(id)) || (payType == 1 && loginType != 0 && payList.contains(id))) {
                        payTypeList.add(id)
                    }
                }
            }
//            users().find(query, $$(_id: 1)).toArray().each { BasicDBObject obj ->
//                payTypeList.add(obj.get('_id') as Integer)
//            }
        } else {
            payTypeList = payList
        }
        //查询用户对应的cid
        def cids = new HashSet()
        def size = payTypeList.size()
        def count = 50000
        def page = size / count as Integer
        for (int i = 0; i < page + 1; i++) {
            def len = count
            def pos = i * count
            if (pos + len > size) len = size - i * count
            if (len > 0) {
                def subArray = new Integer[len]
                System.arraycopy(payTypeList.toArray() as Integer[], pos, subArray, 0, len)
                table().find($$(_id: ['$in': subArray]), $$(cid: 1)).toArray().each { BasicDBObject obj ->
                    cids.add(obj.get('cid'))
                }
            }
        }
        logger.debug('push the number of pushed person is cids=' + cids.size() + " userlist=" + payTypeList.size())
        return [code: 1, data: cids, userlist: payTypeList]
    }
    // ---------------------------个推消息  END

}
