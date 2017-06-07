package com.ttpod.star.admin

import com.gexin.rp.sdk.base.IPushResult
import com.gexin.rp.sdk.base.impl.AppMessage
import com.gexin.rp.sdk.base.impl.ListMessage
import com.gexin.rp.sdk.base.impl.SingleMessage
import com.gexin.rp.sdk.base.impl.Target
import com.gexin.rp.sdk.http.IGtPush
import com.gexin.rp.sdk.template.NotificationTemplate
import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.common.util.JSONUtil
import com.ttpod.rest.web.Crud
import com.ttpod.star.common.util.ExportUtils
import com.ttpod.star.common.util.IMUtil
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * @author: zhen.chen@2339.com
 * Date: 2014/12/1 16:18
 */
@Rest
class PushController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(PushController.class)

    private static final String APPID = "cW2tDclAjXAQF3vRWfUMV6"
    private static final String APPKEY = "h32bcmADuU6NONXO9qDti9"
    private static final String APPSECRET = "3ls2xUi7ke6CFOEww5ABD3"
    private static final String MASTERSECRET = "RzVnVeRJE68yK1uEtAIZm"
    private static String HOST = "http://sdk.open.api.igexin.com/apiex.htm";
    private static Long MESSAGE_EXPIRE = 60 * 60 * 1000L

    private static final Long DAYMILLI = 24 * 60 * 60 * 1000L
    //单次推送的最大用户数
    private static final int max = 5
    private static int pos = 0
    private static final Lock lock = new ReentrantLock()

    DBCollection table() { logMongo.getCollection("app_info") }

    DBCollection finance_log = adminMongo.getCollection('finance_log')
    DBCollection push_log = logMongo.getCollection('push_log')

    def push_list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        Crud.list(req, push_log, query.get(), ALL_FIELD, SJ_DESC)
    }

    //TODO ---------------------友盟消息推送
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

    // TODO ---------------------------个推消息  BEGIN

    //针对单个用户发送推送信息
    def single_user(HttpServletRequest req) {
        def map = getMessage(req)
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
        return [code: 1, msg: '成功']
    }

    //针对某一类用户发送推送信息
    def group_user(HttpServletRequest req) {
        def map = getMessage(req)
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
        return userMap
    }

    //针对激活用户发送推送信息
    def all_user(HttpServletRequest req) {
        def map = getMessage(req)
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
        return [code: 1, msg: '成功']
    }


    private static NotificationTemplate NotificationTemplate(Map map)
            throws Exception {
        NotificationTemplate template = new NotificationTemplate();
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
        return template;
    }

    private Map getMessage(HttpServletRequest req) {
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
    }

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
    // TODO ---------------------------个推消息  END


}
