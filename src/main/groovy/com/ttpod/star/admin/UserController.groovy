package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.AppProperties
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.IMessageCode
import com.ttpod.rest.common.util.MsgDigestUtil
import com.ttpod.rest.web.Crud
import com.ttpod.star.common.doc.Param
import com.ttpod.star.common.util.ExportUtils
import com.ttpod.star.common.util.IMUtil
import com.ttpod.star.common.util.KeyUtils
import com.ttpod.star.model.*
import com.ttpod.star.web.api.notify.GameService
import com.ttpod.star.web.api.notify.MessageSend
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.web.bind.ServletRequestUtils
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.commons.CommonsMultipartResolver
import org.springframework.web.util.HtmlUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.lang.ref.SoftReference
import java.lang.reflect.Array
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.util.MsgDigestUtil.MD5
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * date: 14-8-28 下午2:31
 */

//@Rest
@RestWithSession
class UserController extends BaseController {

    @Resource
    public StringRedisTemplate liveRedis;
    static final Logger logger = LoggerFactory.getLogger(UserController.class)

    def rewards() { return activeMongo.getCollection('rewards') }
    def user_award_logs() { return logMongo.getCollection('user_award_logs') }
    def user_battle_logs() { return gameLogMongo.getCollection('user_battle_logs') }
    def family_event_logs() { return gameLogMongo.getCollection('family_event_logs') }
    DBCollection familys() { return familyMongo.getCollection('familys')}
    def member_contributions() { return familyMongo.getCollection('member_contributions')}
    def stat_mic() { return adminMongo.getCollection('stat_mic')}

    DBCollection table() { users() }

    DBCollection basicSalary() { adminMongo.getCollection('basic_salary') }

    public static
    final BasicDBObject ROOM_LIST_USER_FILED = $$(nick_name: 1, 'finance.coin_spend_total': 1, "finance.bean_count_total": 1, pic: 1, broker: 1, star_total: 1)

    def list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        final Integer priv = (req['priv'] ?: 0) as Integer
        intQuery(query, req, 'priv')
        stringQuery(query, req, 'nick_name')
        stringQuery(query, req, 'qd')
        stringQuery(query, req, 'tuid')
        if (req['status']) {
            query.and('status').is(!'0'.equals(req['status']))
        }

        if (req['mm_no']) {
            query.and('mm_no').is(req.getInt('mm_no'))
        }
        if (req[_id]) {
            query.and(_id).is(req.getInt(_id))
        }

        [bean: 'finance.bean_count_total', coin: "finance.coin_spend_total"].each { String type, String field ->
            def start = req['s' + type]
            def end = req['e' + type]
            if (start || end) {
                query.and(field)
                if (start) {
                    query.greaterThanEquals(start as Long)
                }
                if (end) {
                    query.lessThan(end as Long)
                }
            }
        }

        def keys = liveRedis.keys(KeyUtils.USER.blackClient("*"))
        def valOp = liveRedis.opsForValue()
        Set<String> bannedUsers = new HashSet<String>(keys.size())
        for (String key : keys) {
            String value = (String) valOp.get(key)
            String[] tmp = value.split("_")
            String sUserId = tmp[0]
            bannedUsers.add(sUserId)
        }
        def stime = Web.getTime(req, 'pstime')
        def etime = Web.getTime(req, 'petime')

        Crud.list(req, table(), query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> data ->
            def logins = logMongo.getCollection('day_login')
            def ret = new BasicDBObject(ip: 1, uid: 1, timestamp: 1)
            for (BasicDBObject obj : data) { // 更新昵称 http://192.168.1.181/redmine/issues/4086
                //def login = logins.findOne("${day}${obj[_id]}".toString(),ret)
                def login = logins.findOne(new BasicDBObject('user_id', obj[_id]), ret, ID_DESC)
                if (login) {
                    login.removeField(_id)
                    obj.put('login', login)
                }
                obj.put("ban_status", 0)
                String user_id = obj[_id].toString()
                if (bannedUsers.contains(user_id))
                    obj.put("ban_status", 1)
                obj.put("weixin_bind", obj['account'] != null && obj['account']['open_id'] != null ?: false)

            }
        }

    }

    static final String FREEZE_TITLE = '冻结账户'
    static final String FREEZE_CONTENT = '管理员冻结账户,请联系客户人员'

    def freeze(HttpServletRequest req) {
        def id = req.getInt(_id)
        Boolean status = !'0'.equals(req['status'])
        def reason = req['reason']
        Integer days = ServletRequestUtils.getIntParameter(req, 'days', -1) //封禁日期
        def set = $$(status: status);
        def updateInfo = $$('$set', set)
        if (days > 0 && !status) {
            set.append("unfreeze_time", (new Date() + days).getTime()) //定时任务每天自动解封
        } else if (status) {
            updateInfo.append('$unset', $$("unfreeze_time", 1))
        }
        if (table().update(new BasicDBObject(_id, id), updateInfo, false, false, writeConcern
        ).getN() == 1) {
            Crud.opLog(OpType.user_freeze, [user_id: id, status: status, reason: reason])
            if (!status) {
                String token = userRedis.opsForValue().get(KeyUtils.USER.token(id))
                if (token) {
                    userRedis.delete(KeyUtils.USER.token(id))
                    userRedis.delete(KeyUtils.accessToken(token))
                }
                //是否为主播
                if (StringUtils.isBlank(reason)) {
                    reason = FREEZE_CONTENT
                }
                def body = IMUtil.buildSystemMessageBody(FREEZE_TITLE, reason, [id], 0, 0)
                def room = rooms().findOne($$(_id: id),$$('live':1))
                def live = room == null ? Boolean.FALSE : room['live'] as Boolean
                if (live) {
                    //推送主播房间
                    Long ttl = days == -1 ? 60 * 60 * 24 * 365 * 1000L : 60 * 60 * 24 * days * 1000L
                    live_off(id, ttl)
                }
                IMUtil.sendToUsers(body)
            }
        }
        return OK()
    }

    /**
     * 封杀设备需要关闭直播间
     * @param req
     * @return
     */
    static final String BAN_TITLE = '封杀设备'
    static final String BAN_CONTENT = '管理员封杀设备,请联系客服人员'

    def ban(HttpServletRequest req) {
        def id = req.getInt(_id)
        String uid = getClientId(req, id)
        def comment = req['comment']
        if (uid) {
            Integer hour = Math.max(1, ServletRequestUtils.getIntParameter(req, 'hour', 48))
            String token = userRedis.opsForValue().get(KeyUtils.USER.token(id))
            def room = rooms().findOne($$(_id: id),$$('live':1))
            def live = room == null ? Boolean.FALSE : room['live'] as Boolean
            // 如果是主播关闭直播间
            if (live) {
                Long ttl = hour * 60 * 60 * 1000L
                live_off(id, ttl)
            }
            if (token) {
                userRedis.delete(KeyUtils.USER.token(id))
                userRedis.delete(KeyUtils.accessToken(token))
                if (StringUtils.isBlank(comment)) {
                    comment = BAN_CONTENT
                }
                def body = IMUtil.buildSystemMessageBody(BAN_TITLE, comment, [id], 0, 0)
                IMUtil.sendToUsers(body)
            }

            String value = id
            if (StringUtils.isNotBlank(value)) {
                if (StringUtils.isNotBlank(comment))
                    value = id + "_" + comment

                liveRedis.opsForValue().set(KeyUtils.USER.blackClient(uid), value, hour, TimeUnit.HOURS)
                Crud.opLog(OpType.user_ban, [user_id: id, client: uid, hour: hour, comment: comment])
                return OK()
            }
        }
        [code: 0, msg: '无法获取客户端UID或者IP']
    }

    /**
     * 封杀或者冻结调用关播功能
     * @param roomId
     * @return
     */
    private Boolean live_off(Integer roomId, Long ttl) {
        final time = System.currentTimeMillis()
        final oldRoom = rooms().findAndModify($$("_id": roomId, live: Boolean.TRUE),
                $$($set, [live: false, live_id: '', position: null, live_end_time: time, pull_urls: null])
        )
        logger.debug('oldRoom is {}', oldRoom)
        String live_id = oldRoom?.get("live_id")

        Integer live_type = oldRoom?.get("live_type") as Integer
        if (StringUtils.isBlank(live_id)) {
            return Boolean.FALSE
        }

        userRedis.delete(KeyUtils.ROOM.liveFlag(roomId))
        liveRedis.delete(liveRedis.keys(KeyUtils.LIVE.all(roomId)))
        logMongo.getCollection("room_edit").update($$(type: "live_on", data: live_id, room: roomId), $$('$set': [etime: time]))
        logRoomEdit('live_off', roomId, live_type, live_id)

        def body = ['live': false, room_id: roomId]
        MessageSend.publishLiveEvent(body)

        def zhuboId = oldRoom.get("xy_star_id") as Integer

        def reason = '管理员封杀设备,如有疑问,请联系客服人员'
        // 管理员封杀设备后无法登陆
//        liveRedis.opsForValue().set(KeyUtils.LIVE.blackStar(zhuboId), KeyUtils.MARK_VAL, ttl, TimeUnit.SECONDS)
        def publish_star_close_body = ['star_id': zhuboId, 'reason': reason, 'ttl': ttl]
        MessageSend.publishStarCloseEvent(publish_star_close_body, zhuboId)

        // 记录操作日志
        def userId = Web.getCurrentUserId()
        Crud.opLog(OpType.room_close, [user_id: userId])

        // 关闭直播间通知游戏端
        def game_id = oldRoom['game_id']
        if (game_id != 0) {
            if (!GameService.closeGame(roomId, game_id, live_id)) {
                logger.error("请求关闭游戏失败")
                return Boolean.FALSE
            }
        }

        return Boolean.TRUE
    }

    private void logRoomEdit(String type, Integer roomId, Integer live_type, Object data) {
        Map obj = new HashMap();
        obj.put("type", type);
        obj.put("room", roomId);
        obj.put("data", data);
        obj.put("live_type", live_type);
        obj.put("session", Web.getSession());
        obj.put("timestamp", System.currentTimeMillis());
        logMongo.getCollection("room_edit").save(new BasicDBObject(obj));
    }

    private final String[] clients = ['uid', 'ip']

    private String getClientId(HttpServletRequest req, Integer id) {
        for (String f : clients) {
            String val = (String) req[f]
            if (StringUtils.isNotBlank(val)) {
                return val
            }
        }
        def login = logMongo.getCollection('day_login').findOne("${new Date().format('yyyyMMdd_')}${id}".toString(),
                new BasicDBObject(ip: 1, uid: 1))
        if (login) {
            for (String f : clients) {
                String val = (String) login[f]
                if (StringUtils.isNotBlank(val)) {
                    return val
                }
            }
        }
        return null
    }

    def unban(HttpServletRequest req) {
        def id = req.getInt(_id)
        String uid = getClientId(req, id)
        if (uid) {
            liveRedis.delete(KeyUtils.USER.blackClient(uid))
            Crud.opLog(OpType.user_unban, [user_id: id, client: uid])
            return OK()
        }
        [code: 0, msg: '无法获取客户端UID或者IP']
    }


    def gm(HttpServletRequest req) {
        def id = req.getInt(_id)
        def priv = req.getInt('priv')
        if (priv == UserType.运营人员.ordinal() || priv == UserType.普通用户.ordinal()
                || priv == UserType.经纪人.ordinal()
                || priv == UserType.客服人员.ordinal()
                || priv == UserType.GM.ordinal()) {
            //def old = priv == UserType.运营人员.ordinal() ? UserType.普通用户.ordinal() : UserType.运营人员.ordinal()


            def set = new HashMap()
            set.put("priv", priv)
            if (priv == UserType.经纪人.ordinal()) {
                set.put('broker.' + timestamp, System.currentTimeMillis())

                Integer partnership = req["partnership"] as Integer
                Integer special = req['special'] as Integer
                set.put("broker.partnership", partnership)
                set.put("broker.special", special)
            }

            if (users().update(new BasicDBObject(_id, id).append("priv", [$ne: UserType.主播.ordinal()]), // 不能修改主播
                    new BasicDBObject('$set': set), false, false, writeConcern
            ).getN() == 1) {
                refresh_token(id)
                Crud.opLog(OpType.user_gm, [user_id: id, priv: priv])
            }
        }

        return OK()
    }



    def ban_list(HttpServletRequest req) {
        String uid = req[_id]
        def keys = liveRedis.keys(KeyUtils.USER.blackClient("*"))
        def list = new ArrayList(keys.size())
        def valOp = liveRedis.opsForValue()
        def pre = KeyUtils.USER.blackClient("").length()
        for (String key : keys) {
            String value = (String) valOp.get(key)
            String[] tmp = value.split("_")
            String user_id = tmp[0]
            String comment = tmp.length == 2 ? tmp[1] : ""
            if (StringUtils.isNotEmpty(uid)) {
                if (user_id.equals(uid)) {
                    list.add([client: key.substring(pre), _id: user_id, ttl: liveRedis.getExpire(key), comment: comment])
                    break;
                }
            } else {
                list.add([client: key.substring(pre), _id: user_id, ttl: liveRedis.getExpire(key), comment: comment])
            }
        }
        [code: 1, data: list]
    }


    def show(HttpServletRequest req) {
        def user = table().findOne(req.getInt(_id))
        def myroom = rooms().findOne(new BasicDBObject('xy_star_id', user.get(_id)))
        user.put('baidu_active', myroom?.get("baidu_active"))
        user.put('time_slot', myroom?.get("time_slot"))

        return user
    }


    static long zeroMill = new Date().clearTime().getTime()

    def cost_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        if (req[_id]) {
            query.and('session._id').is(req[_id])
        }
        stringQuery(query, req, 'type')
        def room_db = logMongo.getCollection('room_cost')
        Crud.list(req, room_db, query.get(), ALL_FIELD, NATURAL_DESC, null)
    }



    def cost_log_export(HttpServletRequest req, HttpServletResponse res) {
        def type = req['type']
        if (type == null) {
            return [code: 0, msg: '请输入道具类型type']
        }
        Date[] dates = ExportUtils.checkDate(Web.getStime(req), Web.getEtime(req))
        def query = Web.fillTimeBetween(dates[0], dates[1]).and('type').is(type)
        def room_db = logMongo.getCollection('room_cost')
        def bodyBuf = new StringBuffer()
        def title = Boolean.TRUE
        def result = ExportUtils.list(req, room_db, query.get(), ALL_FIELD, NATURAL_DESC) { List<BasicDBObject> list ->
            ExportUtils.render(list, ExportType.getListByType(type), bodyBuf, title)
            if (Boolean.TRUE.equals(title)) title = Boolean.FALSE
        } as Map
        def count = result['count'] as String
        def page = result['all_page'] as String
        def typeName = ExportType.valueOf(type)?.getDesc();
        StringBuffer titleBuf = ExportUtils.generateTitle(dates, count, page)
        String filename = ExportUtils.generateFilename(dates, "${typeName}-道具销售流水")
        ExportUtils.response(res, filename, titleBuf.append(bodyBuf).toString())
    }

    def cost_log_history(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        if (req[_id]) {
            def user_id = req.getInt(_id)
            query.and('user_id').is(user_id)
        }
        stringQuery(query, req, 'type')
        def room_db = logMongo.getCollection('room_cost_day_usr')

        Crud.list(req, room_db, query.get(), ALL_FIELD, SJ_DESC, null)
    }


    def union_photo(HttpServletRequest req) {
        Crud.list(req, adminMongo.getCollection("union_photos"), $$("user_id", req.getInt(_id)), ALL_FIELD, SJ_DESC)
    }

    static final Long SIX_HUNDRED_SECONDS = 600L

    static final String notify_url = (AppProperties.get('api.domain') + 'upai/notify').replace("/", "\\/")

    static final Pattern URL_PATT = Pattern.compile("/\\d+/\\d{4}/\\w{32}.[a-z]{3,4}")

    static final String HTTP_FORM_KEY = "bl37fSAQyZ0ZMcF/cZMGjwWNuQU="

    def token(HttpServletRequest req) {
        def uid = req.getInt(_id);
        def json = "{\"bucket\":\"showphoto\"," +
                "\"expiration\":${System.unixTime() + SIX_HUNDRED_SECONDS}," +
                "\"save-key\":\"/${uid}/{mon}{day}/{filemd5}{.suffix}\"," +
                "\"allow-file-type\":\"jpg,jpeg,gif,png\"," +
                "\"image-width-range\":\"120,2048\"," +
                "\"image-height-range\":\"120,8192\"," +
                "\"content-length-range\":\"0,3145728\"," + // 0 ~ 3MB
//                    "\"x-gmkerl-type\":\"\","+
                "\"return-url\":\"\"," +
                "\"notify-url\":\"${notify_url}\"}"
        def policy = Base64.encodeBase64String(json.asBytes())
        return [code: 1, data: [
                action   : 'http://v0.api.upyun.com/showphoto/', policy: policy,
                signature: MsgDigestUtil.MD5.digest2HEX("${policy}&${HTTP_FORM_KEY}")
        ]]
    }

    @Value("#{application['pic.domain']}")
    String pic_domain = "https://aiimg.sumeme.com/"

    def add_union(HttpServletRequest req) {
        def path = req['path']
        def pic_url = req['pic_url']
        def uid = req.getInt(_id);
        //if(URL_PATT.matcher(path.clean()).matches() && path.startsWith("/"+uid+"/")){
        if (adminMongo.getCollection("union_photos").count($$('user_id', uid)) <= 5) {
            if (adminMongo.getCollection("union_photos").save(new BasicDBObject(
                    _id: path,
                    user_id: uid as Integer,
                    pic_url: pic_url,
                    timestamp: System.currentTimeMillis(),
                    status: PhotoStatusType.未处理.ordinal()
            )).getN() > 0)
                return IMessageCode.OK

        }
    }

    //图片处理
    File pic_folder

    @Value("#{application['pic.folder']}")
    void setPicFolder(String folder) {
        pic_folder = new File(folder)
        pic_folder.mkdirs()
        println "初始化图片上传目录 : ${folder}"
    }

    /**
     * 上传身份证
     * @param request
     * @return
     */
    def upload_sfz(HttpServletRequest request, HttpServletResponse response) {
        def parse = new CommonsMultipartResolver()
        def req = parse.resolveMultipart(request)

        try {
            Integer id = req['_id'] as Integer  //用户ID
            Integer type = req.getParameter(Param.first) as Integer //1:正面 0:反面 2 手持
            logger.debug("type {} id {}", type, id)
            if (type != 1 && type != 0 && type != 2) {
                return Web.missParam()
            }
            String filePath = "sfz/${id}_${type}.jpg"
            for (Map.Entry<String, MultipartFile> entry : req.getFileMap().entrySet()) {
                MultipartFile file = entry.getValue()
                def target = new File(pic_folder, filePath)
                target.getParentFile().mkdirs()
                file.transferTo(target)
                break
            }
            String iframeCallBack = req["icallback"]
            if (StringUtils.isNotBlank(iframeCallBack)) {
                def out = response.getWriter()
                out.println("<script>top.${iframeCallBack}({\"code\":1,\"data\":{\"pic_url\":\"${pic_domain}${filePath}\"}});</script>")
                out.close()
                return
            }
            return [code: 1, url: "${pic_domain}${filePath}".toString(), error: 0]
        } catch (Exception e) {
            logger.error("upload_sfz Exception:{}", e)
            return [code: 0]
        }
        finally {
            parse.cleanupMultipart(req)
        }
    }

    def edit(HttpServletRequest req) {
        def update = new HashMap()
        Integer userId = req.getInt(_id)
        String v = req.getParameter("pic")
        if (StringUtils.isNotBlank(v))
            update.put('pic', v)

        String nick_name = req.getParameter("nick_name")
        if (StringUtils.isNotBlank(nick_name) && nick_name.length() < 21 && !nick_name.contains(" ")) {
            nick_name = HtmlUtils.htmlEscape(nick_name)
            update.put("nick_name", nick_name)
            String token = userRedis.opsForValue().get(KeyUtils.USER.token(userId))
            Web.putUserInfoToSession(KeyUtils.accessToken(token), "nick_name", nick_name)
            //userRedis.opsForHash().put(KeyUtils.accessToken(token),"nick_name", nick_name)
        }
        String enter_info = req.getParameter("enter_info") ?: ""
        if (StringUtils.isNotBlank(enter_info) && enter_info.length() < 8 && !enter_info.contains(" ")) {
            enter_info = HtmlUtils.htmlEscape(enter_info)
        }

        update.put("enter_info", enter_info)
        if (update.size() > 0) {
            if (1 == users().update(new BasicDBObject(_id, userId), new BasicDBObject($set, update), false, false, writeConcern).getN()) {
                Integer priv = users().findOne($$(_id, userId), $$(priv: 1)).get("priv") as Integer
                if (UserType.主播.ordinal() == priv && StringUtils.isNotBlank(update.get('nick_name') as String))
                    rooms().update(new BasicDBObject(_id, userId), new BasicDBObject($set, $$(nick_name: nick_name)), false, false, writeConcern)
                return [code: 1]
            }
        }
        //String token = userRedis.opsForValue().get(KeyUtils.USER.token(userId))
        //userRedis.opsForHash().put(KeyUtils.accessToken(token), "enter_info", enter_info)
        //Web.putUserInfoToSession(KeyUtils.accessToken(token),"enter_info", enter_info)
        return [code: 0]
    }

    private final static String PRIV_KEY = "meme#*&07071zhibo";
    /**
     * 用户token刷新
     * @param req
     * @return
     */
    def refresh_user_token(HttpServletRequest req) {
        refresh_token(req.getInt(_id))

        /*if (mainRedis.hasKey(old_token_key))
            mainRedis.rename(old_token_key, new_token_key)*/
        Crud.opLog(OpType.refresh_token, [user_id: req.getInt(_id)])

        OK()
    }

    private refresh_token(Integer user_id) {
        def user = table().findOne(user_id, $$(tuid: 1))
        //获得tuid
        String tuid = user['tuid'] as String
        def sign = MD5.digest2HEX("${PRIV_KEY}&userId=${tuid}".toString())
        Map result = Web.userApi("pwd/refresh_token?_id=${tuid}&sign=${sign}")
        if (result == null)
            return [code: 0]
        if (((Number) result.get("code")).intValue() != 1) {
            return [code: result.get("code")]
        }
        final Map data = (Map) result.get("data");

        String newToken = (String) data.get("token");
        String new_token_key = KeyUtils.accessToken(newToken)

        String oldToken = (String) data.get("old_token");
        String old_token_key = KeyUtils.accessToken(oldToken)
        logger.debug("new token is ${newToken},old token is ${oldToken}")

        if (userRedis.hasKey(old_token_key)) {
            userRedis.delete(old_token_key)
        }
        if (userRedis.hasKey(new_token_key)) {
            userRedis.delete(new_token_key)
        }

        Web.api("java/flushuser?id1=${user_id}&access_token=${newToken}".toString())
    }
    /**
     * 绑定用户手机号
     * @param req
     * @return
     */
    def set_user_mobile(HttpServletRequest req) {
        def mobile = req['mobile']
        def sms_code = req["sms_code"]
        //获得tuid
        def user = table().findOne(req.getInt(_id), $$(tuid: 1))
        String tuid = user['tuid'] as String
        def sign = MD5.digest2HEX("${PRIV_KEY}&userId=${tuid}".toString())

        Crud.opLog(OpType.set_user_mobile, [user_id: req.getInt(_id), mobile: mobile])

        //获得用户token
        Map userResult = Web.userApi("pwd/token_by_id?_id=${tuid}&sign=${sign}")
        if (userResult == null)
            return [code: 0]
        if (((Number) userResult.get("code")).intValue() != 1) {
            return [code: userResult.get("code")]
        }

        final Map data = (Map) userResult.get("data");
        String token = (String) data.get("token");

        Map apiResult = Web.api2Map("user/bind_mobile?access_token=${token}&mobile=${mobile}&sms_code=${sms_code}")
        if (apiResult == null)
            return [code: 0]
        if (((Number) apiResult.get("code")).intValue() != 1) {
            return [code: apiResult.get("code")]
        }

        OK()
    }

    /**
     * 绑定用户名密码
     * @param req
     * @return
     */
    def set_user_name(HttpServletRequest req) {
        def user_name = req['user_name'] as String
        def pwd = req["pwd"] as String

        //获得tuid
        def user = table().findOne(req.getInt(_id), $$(tuid: 1))

        if (user == null || StringUtils.isEmpty(user_name) || StringUtils.isEmpty(pwd)) {
            return Web.missParam()
        }
        String tuid = user['tuid'] as String
        def sign = MD5.digest2HEX("${PRIV_KEY}&userId=${tuid}".toString())
        //获得用户token
        Map userResult = Web.userApi("pwd/token_by_id?_id=${tuid}&sign=${sign}")
        if (userResult == null)
            return [code: 0]
        if (((Number) userResult.get("code")).intValue() != 1) {
            return [code: userResult.get("code")]
        }

        final Map data = (Map) userResult.get("data");
        String token = (String) data.get("token");

        Map apiResult = Web.api2Map("user/bind_userName?access_token=${token}&userName=${user_name}&pwd=${pwd}")
        if (apiResult == null)
            return [code: 0]
        if (((Number) apiResult.get("code")).intValue() != 1) {
            return [code: apiResult.get("code")]
        }
        Crud.opLog(OpType.set_user_name_pwd, [user_id: req.getInt(_id), user_name: user_name])
        OK()
    }

    /**
     * 解绑用户手机号
     * @param req
     * @return
     */
    def unbind_mobile(HttpServletRequest req) {
        //获得tuid
        Integer uid = req.getInt(_id)
        def user = table().findOne(uid, $$(tuid: 1))
        String tuid = user['tuid'] as String
        def sign = MD5.digest2HEX("${PRIV_KEY}&userId=${tuid}".toString())
        //获得用户token
        Map userResult = Web.userApi("pwd/unbind_mobile?_id=${tuid}&sign=${sign}")
        if (userResult == null)
            return [code: 0]
        if (((Number) userResult.get("code")).intValue() != 1) {
            return [code: userResult.get("code")]
        }
        users().update($$(_id: uid), $$($set: [mobile_bind: false]), false, false, writeConcern)
        Crud.opLog(OpType.unbind_mobile, [user_id: uid])

        OK()
    }

    /**
     * 解绑用户微信
     * @param req
     * @return
     */
    def unbind_weixin(HttpServletRequest req) {
        //获得tuid
        Integer uid = req.getInt(_id)
        def user = table().findOne(uid, $$(tuid: 1))
        /*String tuid = user['tuid'] as String
        def sign = MD5.digest2HEX("${PRIV_KEY}&userId=${tuid}".toString())*/
        users().update($$(_id: uid), $$($unset: ['account.open_id': true]), false, false, writeConcern)
        Crud.opLog(OpType.unbind_weixin, [user_id: uid])

        OK()
    }

    /**
     * 发送手机验证码
     * @param req
     * @return
     */
    def send_mobile(HttpServletRequest req) {
        def mobile = req['mobile']
        def sign = MD5.digest2HEX("${PRIV_KEY}&mobile=${mobile}".toString())
        Integer type = ServletRequestUtils.getIntParameter(req, "type", 1)
        Map userResult = Web.userApi("pwd/send_mobile?mobile=${mobile}&type=${type}&sign=${sign}")
        if (userResult == null)
            return [code: 0]
        if (((Number) userResult.get("code")).intValue() != 1) {
            return [code: userResult.get("code")]
        }
        Crud.opLog(OpType.send_mobile, [mobile: mobile])
        OK()
    }

    /**
     * 通过手机号获得验证码
     * @param req
     * @return
     */
    def get_mobile_code(HttpServletRequest req) {
        def mobile = req['mobile']
        def sign = MD5.digest2HEX("${PRIV_KEY}&mobile=${mobile}".toString())
        Integer type = ServletRequestUtils.getIntParameter(req, "type", 1)
        //获得用户token
        Map userResult = Web.userApi("pwd/get_code_by_mobile?mobile=${mobile}&type=${type}&sign=${sign}")
        if (userResult == null)
            return [code: 0]
        if (((Number) userResult.get("code")).intValue() != 1) {
            return [code: userResult.get("code")]
        }
        final Map data = (Map) userResult.get("data");
        String auth_code = (String) data.get("auth_code");
        Crud.opLog(OpType.get_mobile_code, [mobile: mobile])
        return [code: 1, data: [auth_code: auth_code]]
    }

    /**
     * 重置用户密码
     * @param req
     * @return
     */
    def reset_pwd(HttpServletRequest req) {
        //获得tuid
        def user = table().findOne(req.getInt(_id), $$(tuid: 1))
        String tuid = user['tuid'] as String
        def sign = MD5.digest2HEX("${PRIV_KEY}&userId=${tuid}".toString())
        Map result = Web.userApi("pwd/reset_pwd?_id=${tuid}&sign=${sign}")
        if (result == null)
            return [code: 0]
        if (((Number) result.get("code")).intValue() != 1) {
            return [code: result.get("code")]
        }
        final Map data = (Map) result.get("data");
        String pwd = (String) data.get("pwd");

        Crud.opLog(OpType.reset_pwd, [user_id: req.getInt(_id)])

        return [code: 1, data: [pwd: pwd]]
    }

    /**
     * 生成重置密码链接
     * @param req
     * @return
     */
    def reset_pwd_url(HttpServletRequest req) {
        //获得tuid
        def user = table().findOne(req.getInt(_id), $$(tuid: 1))
        String tuid = user['tuid'] as String
        def sign = MD5.digest2HEX("${PRIV_KEY}&userId=${tuid}".toString())
        Map result = Web.userApi("pwd/generate_pwd_url?_id=${tuid}&sign=${sign}")
        if (result == null)
            return [code: 0]
        if (((Number) result.get("code")).intValue() != 1) {
            return [code: result.get("code")]
        }
        final Map data = (Map) result.get("data");
        String url = (String) data.get("url");

        Crud.opLog(OpType.reset_pwd_url, [user_id: req.getInt(_id)])

        return [code: 1, data: [url: url]]
    }

    private String getNickName(Integer uid) {
        if (uid != null) {
            def nick_name = users().findOne($$(_id: uid), $$(nick_name: 1))?.get('nick_name')
            return nick_name
        }
        return null
    }

    private String getFamilyName(Integer fid) {
        if (fid != null) {
            def nick_name = familys().findOne($$(_id: fid), $$(name: 1))?.get('name')
            return nick_name
        }
        return null
    }

    /**
     * 查询用户翻卡流水
     * @param req
     */
    def open_card_list(HttpServletRequest req) {
        def userId = req.getParameter('user_id') as Integer
        def query = Web.fillTimeBetween(req)
        query.put('type').is("open_card")
        if (userId != null) {
            query.put('user_id').is(userId)
        }
        Crud.list(req, user_award_logs(), query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            for(BasicDBObject obj : list) {
                obj.put('nick_name', getNickName(obj.get('user_id') as Integer))
            }
        }
    }

    /**
     * 道具使用流水
     */
    def use_item_list(HttpServletRequest req) {
        def userId = req.getParameter('user_id') as Integer
        def query = Web.fillTimeBetween(req)
        if (userId != null) {
            query.put('user_id').is(userId)
        }
        Crud.list(req, user_battle_logs(), query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            for(BasicDBObject obj : list) {
                obj.put('nick_name', getNickName(obj.get('user_id') as Integer))
                obj.put('target_nick_name', getNickName(obj.get('target_uid') as Integer))
                obj.put('type', UserItemType.values()[obj.get('type') as Integer ?: 0])
            }
        }
    }

    /**
     * 挖矿流水
     * @param req
     */
    def family_event_list(HttpServletRequest req) {
        def userId = req.getParameter('user_id') as Integer
        def familyId = req.getParameter('family_id') as Integer
        def query = Web.fillTimeBetween(req).get()
        query.putAll(['users.0': [$exists: true], status: 4])
        if (userId != null) {
            query.put('users', userId)
        }
        if (familyId != null) {
            query.put('family_id', familyId)
        }
        Crud.list(req, family_event_logs(), query, ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            for(BasicDBObject obj : list) {
                obj.put('family_name', getFamilyName(obj.get('family_id') as Integer))
                def rewards = obj.removeField('rewards') as List
                def reward = MapWithDefault.<String, Integer>newInstance(new HashMap<String, Integer>()) { 0 }
                def keys = ['steal', 'defense', 'exp', 'coin', 'diamond', 'cash', 'attack']
                for(String key: keys) {
                    if (rewards != null) {
                        for(int i = 0; i < rewards.size(); i++) {
                            def it = rewards.get(i)
                            reward[key] = (reward[key] as Integer) + ((it[key] ?: 0) as Integer)
                        }
                    }
                }
                obj.put('reward', reward)
            }
        }
    }

    /**
     * 家族贡献流水
     * @param req
     */
    def family_contribution_list(HttpServletRequest req) {
        def userId = req.getParameter('user_id') as Integer
        def familyId = req.getParameter('family_id') as Integer
        def query = Web.fillTimeBetween(req).get()
        if (userId != null) {
            query.put('users', userId)
        }
        if (familyId != null) {
            query.put('family_id', familyId)
        }
        Crud.list(req, member_contributions(), query, ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            for(BasicDBObject obj : list) {
                obj.put('nick_name', getNickName(obj.get('user_id') as Integer))
                obj.put('family_name', getFamilyName(obj.get('family_id') as Integer))
            }
        }
    }

    /**
     * 用户上麦流水
     */
    def on_mic_list(HttpServletRequest req) {
        def userId = req.getParameter('user_id') as Integer
        def familyId = req.getParameter('family_id') as Integer
        def query = Web.fillTimeBetween(req).and('type').is('on_mic_user_log').get()
        if (userId != null) {
            query.put('user_id', userId)
        }
        if (familyId != null) {
            query.put('family_id', familyId)
        }
        Crud.list(req, stat_mic(), query, ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            for(BasicDBObject obj : list) {
                obj.put('nick_name', getNickName(obj.get('user_id') as Integer))
                obj.put('family_name', getFamilyName(obj.get('family_id') as Integer))
            }
        }
    }

}
