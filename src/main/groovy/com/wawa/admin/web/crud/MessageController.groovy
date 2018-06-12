package com.wawa.admin.web.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.wawa.base.anno.RestWithSession
import com.wawa.common.doc.IMessageCode
import com.wawa.common.util.JSONUtil
import com.wawa.base.persistent.KGS
import com.wawa.base.Crud
import com.wawa.base.StaticSpring
import com.wawa.admin.web.PushController
import com.wawa.base.BaseController
import com.wawa.api.Web
import com.wawa.common.util.HttpClientUtils
import com.wawa.common.util.IMUtil
import com.wawa.model.MsgType
import com.wawa.model.OpType
import com.wawa.model.UserType
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import java.text.SimpleDateFormat

import static com.wawa.common.util.WebUtils.$$
import static com.wawa.groovy.CrudClosures.*
import static com.wawa.common.doc.MongoKey.*

/**
 * 普通消息， 消息推送
 * @author: jiao.li@ttpod.com
 * Date: 13-11-12 上午11:54
 */
//@Rest
@RestWithSession
class MessageController extends BaseController{

    static final Logger logger = LoggerFactory.getLogger(MessageController.class)

    DBCollection messages(){adminMongo.getCollection("messages")}
    DBCollection push_msgs(){adminMongo.getCollection("push_msgs")}
    DBCollection sys_push(){adminMongo.getCollection("sys_push")}

    DBCollection msgs(){mainMongo.getCollection("msgs")}

    DBCollection msgs_global(){mainMongo.getCollection("msgs_global")}

    @Resource
    KGS   msgKGS
    @Resource
    KGS   sysPushKGS

    @Delegate Crud crud = new Crud(messages(),true,
            [_id:{msgKGS.nextId()},title:Str,content:Str,type:Int,s_type:Int,
                    t_users:{def users = it as String; if(users != null && !users.isEmpty()){;users.split(',').collect { it as Integer}}},
                    user_type:Int,timestamp:Timestamp,status:{0},
                    stime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()},
                    etime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()}
                    ],
            new Crud.QueryCondition(){
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject("timestamp",-1);
                }
            }
    )

    public add(HttpServletRequest req) {
        Map map = new HashMap();
        def id = msgKGS.nextId();
        Map props = [_id:{id},title:Str,content:Str,type:Int,s_type:Int,
                t_users:{def users = it as String; if(users != null && !users.isEmpty()){;users.split(',').collect { it as Integer}}},
                user_type:Int,timestamp:Timestamp,status:{0},
                stime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()},
                etime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()}
                ];
        for (Map.Entry<String, Closure> entry : props.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue().call(req.getParameter(key));
            if (val != null) {
                map.put(key, val);
            }
        }
        if(messages().save(new BasicDBObject(map)).getN() == 1){
            crud.opLog(messages().getName() + "_add", map);
            if(req.getParameter('send') && req.getParameter('send').equals("1")){
                sendMsgById(id);
            }
        }
        return IMessageCode.OK;
    }

    public edit(HttpServletRequest req) {
        if(crud.edit(req) == IMessageCode.OK){
            if(req.getParameter('send') && req.getParameter('send').equals("1")){
                def id = req.getParameter(_id);
                sendMsgById(id);
            }
            return IMessageCode.OK;
        }
        return IMessageCode.CODE0;
    }

    def send(HttpServletRequest req){
        def id = req.getParameter(_id);
        return sendMsgById(id)
    }

    private def sendMsgById(id){
        if(id == null){
            return [code : 0]
        }
        def msg = messages().findAndModify($$(_id, id as Integer).append("status" , 0), $$($set, $$(status : 1)))
        if(msg == null){
            return [code : 0]
        }
        def user_ids = null
        if(msg.get("s_type") == 0){//发送方式 用户ID
            user_ids = msg.get("t_users");
            if(user_ids != null)
                sendMsgs(user_ids, msg["title"], msg["content"], msg["type"])
        }
        else if(msg.get("s_type") == 1){//发送方式 用户群组
            Integer user_type = msg["user_type"] as Integer
            if(user_type == UserType.普通用户.ordinal()){
                //插入全局消息
                sendGlobalMsgs( msg["title"], msg["content"], msg["type"],msg["stime"], msg["etime"])
            }else{
                def query = $$("priv", user_type);
            /* def count = users().count(query)
            def size = 500
            def allPage = (int)((count + size - 1) / size);
            while(allPage > 0){
                Long l = System.currentTimeMillis()
                user_ids = users().find(query, $$(_id ,1)).skip((allPage - 1) * size).limit(size).toArray()*._id;
                println "skip: ${(allPage - 1) * size} query cost time: ${System.currentTimeMillis() - l}"
                allPage--
                sendMsgs(user_ids, msg["title"], msg["content"], msg["type"])

                Thread.sleep(500) //降低批量写入的压力
            }*/
                def count = users().count(query)
                Integer first_ids = 1024000
                Integer step = 50000
                Integer index = 0
                while(count > 0 && index++ <= 5000){
                    //println "index: "+ index
                    Long l = System.currentTimeMillis()
                    query.append(_id, $$($gt:first_ids, $lte:(first_ids+step)))
                    user_ids = users().find(query, $$(_id ,1)).sort($$(_id:1)).limit(step).toArray().collect{it.get(_id)}
                    first_ids = first_ids+step

                    if(user_ids != null && user_ids.size() > 0){
                        //println "ids query cost time: ${System.currentTimeMillis() - l}"
                        count = count - user_ids.size()
                        //println "count: "+ count
                        sendMsgs(user_ids, msg["title"], msg["content"], msg["type"])
                    }

                    Thread.sleep(500)
                }
            }


        }
        Crud.opLog(OpType.message_send,  [type: msg["type"],title:msg["title"],content:msg["content"],timestamp:System.currentTimeMillis()])
        cleanHistoryMsg()
        return [code : 1]
    }

    @Resource
    PushController pushController

    /**
     * 发送多条消息
     * @param user_ids
     * @param title
     * @param content
     * @param type
     */
     def sendMsgs(final user_ids,  final title,  final content,  final type){
        StaticSpring.execute(
                new Runnable() {
                    public void run() {
                        Long l = System.currentTimeMillis()
                        List<DBObject> msgList = new ArrayList<DBObject>();
                        user_ids.each{
                            Long tmp = System.currentTimeMillis()
                            Integer id = it as Integer
                            def m = $$(_id: id+"_"+tmp,
                                    type: type, t:id,
                                    tdel:0,tread:0,
                                    title:title,
                                    content:content,
                                    timestamp:tmp)
                            msgList.add(m);
                        }
                        msgs().insert(msgList);
                        //发送通知
                        user_ids.each{Integer user_id ->
                            pushController.push_sign_user(user_id, title as String, content as String)
                        }

                        println "sendMsgs cost time: ${System.currentTimeMillis() - l}"
                    }
                }
        );

    }

    /**
     * 发送全局系统消息
     * @param title
     * @param content
     * @param type
     * @param stime
     * @param etime
     * @return
     */
    def sendGlobalMsgs(final title,  final content,  final type, def stime, def etime){
        Long tmp = System.currentTimeMillis()
        def m = $$(_id: tmp,
            type: type,
            title:title,
            content:content,
            stime:stime,
            etime:etime,
            timestamp:tmp)
        msgs_global().insert(m);
    }

    /**
     * 清除2个月之前，且未读的消息
     */
    private final static long MONTH = 2 * 30 *24 * 3600 * 1000L
    private def cleanHistoryMsg(){
        StaticSpring.execute(
                new Runnable() {
                    public void run() {
                        // 已经被用户删除的
                        //msgs().remove($$(tdel:1))
                        def time = System.currentTimeMillis() - MONTH
                        //2个月之前，且未读的消息
                        int size =1
                        while(size > 0){
                            List lst = msgs().find($$([timestamp : $$($lt , time), tread : 0]), $$(_id, 1))
                                    .limit(1000).toArray().collect{it.get(_id)}
                            size = lst.size();
                            msgs().remove($$(_id , $$($in, lst)))
                            Thread.sleep(500)
                        }

                    }
                }
        );
    }

    /**
     * 发送单条消息
     * @param uid
     * @param title
     * @param content
     * @param type
     * @return
     */
    def sendSingleMsg(Integer uid, String title, String content, MsgType type){
        List<Integer> userList = new ArrayList<Integer>();
        userList.add(uid)
        sendMsgs(userList,  title,  content,  type.ordinal())
    }


    /** 系统活动消息推送 支持小窝助手 */

    def msg_logs(HttpServletRequest req){
        def query = Web.fillTimeBetween(req)
        return Crud.list(req, push_msgs(), query.get(), ALL_FIELD, SJ_DESC);
    }

    private final static List<String> FIELDS = ['title', 'content', 'img_url', 'link']
    def push_msg(HttpServletRequest req){
        def stime = req.getParameter('stime')  as String
        def etime = req.getParameter('etime')  as String
        def msg = new HashMap();
        FIELDS.each {String field ->
            if(StringUtils.isNotEmpty(req.getParameter(field) as String)){
                msg.put(field, req.getParameter(field))
            }
        }
        if(StringUtils.isNotEmpty(stime)){
            msg.put('stime', Web.getTime(stime).getTime())
        }
        if(StringUtils.isNotEmpty(etime)){
            msg.put('etime', Web.getTime(etime).getTime())
        }
        if(msg.size() == 0){
            return Web.missParam()
        }
        msg.put(_id,System.currentTimeMillis())
        msg.put('timestamp', System.currentTimeMillis())

        //推送到消息系统
        def pushMsg = new HashMap();
        pushMsg.put('expire',  Web.getTime(etime).getTime().toString())
        pushMsg.put('msg', JSONUtil.beanToJson([action:'notify.sys', data:msg]))
        try{
            String json = HttpClientUtils.post(Web.IM_DOMAIN + '/api/pub', pushMsg, null)
            logger.debug("im api response : {}", json)
            if(JSONUtil.jsonToMap(json).get("code").toString().equals("1")){
                push_msgs().insert($$(msg))
                Crud.opLog(OpType.push_msg, msg)
                return [code : 1]
            }
        }catch (Exception e){
            logger.error("push to msg error: {}", e)
        }
        return [code : 0]
    }

    def listSysPush(HttpServletRequest req) {
        def q = Web.fillTimeBetween(req)
        intQuery(q, req, "user_id")
        stringQuery(q, req, _id)
        stringQuery(q, req, 'via')
        intQuery(q, req, 'to_id')
        stringQuery(q, req, 'rid')
        stringQuery(q, req, '_id')
        String gte = req.getParameter('coingte')
        if (StringUtils.isNotBlank(gte)) {
            q.and('coin').greaterThanEquals(Integer.valueOf(gte))
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

    def list_push(HttpServletRequest req) {
        Crud.list(req, sys_push(), null, null, null);
    }

    def save_push(HttpServletRequest req) {
        /*title	Sting	true	-	消息标题
        image	String	false	-	消息图片
        link	String	false	-	消息外链
        ts	Number	true	-	消息的时间戳(毫秒)
        expireTime	Number	true	-	过期时间
        text*/
//        Integer platform = req.getParameter('platform'] as Integer ?: 0
        String filter = req.getParameter('filter')
        String userIds = req.getParameter('userIds') as String
        String title = req.getParameter('title')
        String text = req.getParameter('title')
        String link = req.getParameter('link')
        String image = req.getParameter('image')
        Date stime = new Date();

        if (StringUtils.isEmpty(userIds) && StringUtils.isEmpty(filter)) {
            return Web.missParam();
        }

        if (StringUtils.isEmpty(title)) {
            return Web.missParam();
        }

        def sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //stime = sf.parse(stime)
        //etime = sf.parse(etime)

        def obj = [
                //platform: platform,
                filter: filter,
                userIds: userIds,
                title: title,
                text: text,
                link: link,
                image: image,
                stime: stime,
                status: 0,
                timestamp: new Date()
        ]

        //update
        if(req.getParameter("_id")) {
            def id = req.getParameter("_id") as Integer
            def one = sys_push().findOne($$("_id": id))
            if (one) {
                sys_push().update(one, $$(obj))
            }
        } else {
            obj.put("_id", msgKGS.nextId())
            sys_push().save($$(obj))
        }

        Crud.opLog(OpType.push_save, obj);

        return OK();
    }

    def push(HttpServletRequest req){
        Integer id = req.getParameter("_id") as Integer;

        def one = sys_push().findOne($$("_id": id))

        if(one["status"] == 1) {
            return [code: 0, msg: "此消息已发送"]
        }

        def resultUids;
        if (one['userIds'] && !StringUtils.isEmpty(one['userIds'].toString())) {
            resultUids = one['userIds'].toString().split(",").collect {it as Integer};
        } else {
            //按条件推送
            Calendar now = Calendar.getInstance()
            now.add(Calendar.DATE, -7);
            Date left7 = now.getTime();
            now.add(Calendar.DATE, -8);
            Date left15 = now.getTime();
            now.add(Calendar.DATE, -15);
            Date left30 = now.getTime();

            def query1 = [:]
            def query2 = [:]
            /*if (one['platform'] != 0) {
                query1.put("platform", one['platform'])
            }*/

            /*（1）7日内活跃用户，15日内有过付费
            （2）7日内活跃用户，15日内无付费
            （3）7日内不活跃，15日内有付费
            （4）30日内有活跃7日内无活跃，无付费
            （5）7日内有活跃
            （6）30日内有活跃7日内无活跃 15日内有活跃*/
            switch (one['filter']) {
                case "1":
                    query1.put("timestamp", [$gte: left7.getTime()]);
                    query2.put("timestamp", [$gte: left15.getTime()]);
                    break;
                case "2":
                    query1.put("timestamp", [$gte: left7.getTime()]);
                    query2.put("timestamp", [$lte: left15.getTime()]);
                    break;
                case "3":
                    query1.put("timestamp", [$lte: left7.getTime()]);
                    query2.put("timestamp", [$gte: left15.getTime()]);
                    break;
                case "4":
                    query1.put("timestamp", [$gte: left30.getTime(), $lte: left7.getTime()]);
                    query2.put("timestamp", [$lte: left30.getTime()]);
                    break;
                case "5":
                    query1.put("timestamp", [$gte: left7.getTime()]);
//                  query2.put("timestamp", [$lte: left15.getTime()]);
                    break;
                case "6":
                    query1.put("timestamp", [$gte: left30.getTime(), $lte: left7.getTime()]);
//                    query2.put("timestamp", [$lte: left15.getTime()]);
                    break;
                case "7":
                    query1.put("timestamp", [$gte: left30.getTime()]);
//                  query2.put("timestamp", [$lte: left15.getTime()]);
                    break;
            }

            def uids =  logMongo.getCollection("day_login").distinct("user_id", $$(query1));
            if (query2.isEmpty()) {
                resultUids = uids;
            } else {
                resultUids = adminMongo.getCollection("finance_log").distinct("user_id", $$(query2).append("user_id", $$($in: uids)));
            }
        }

        def message = [
                "action": 'system.operation',
                "data": [
                        "title": one['title'],
                        "image": one['image'],
                        "link": one['link'],
                        "text": one['text'],
                        "ts": System.currentTimeMillis(),
                        "expireTime": System.currentTimeMillis() + 2 * 60 * 60 * 1000
                ]
        ]
        IMUtil.sendToUsers([
                "message": message, "userIds": resultUids, "isNotify": 1, "isSave": 1,
                extra: [
                        event: StringUtils.isEmpty(one['link'] as String) ? "system_operation" : "system_linkurl",
                        linkUrl: one['link']
                ]
        ])
        sys_push().update(one, $$($set : $$("status": 1, "count": resultUids.size())))
        Crud.opLog(OpType.push_send, one);

        return OK();
    }

    def del(HttpServletRequest req) {
        Integer id = req.getParameter("_id") as Integer;

        sys_push().remove($$("_id", id));
        Crud.opLog(OpType.push_del, ['_id': id]);

        return OK();
    }
}