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
//            String day = new Date().format('yyyyMMdd_')
            def ret = new BasicDBObject(ip: 1, uid: 1, timestamp: 1)
            for (BasicDBObject obj : data) { // 更新昵称 http://192.168.1.181/redmine/issues/4086
                //def login = logins.findOne("${day}${obj[_id]}".toString(),ret)
                def login = logins.findOne(new BasicDBObject('user_id', obj[_id]), ret, ID_DESC)
                if (login) {
                    login.removeField(_id)
                    obj.put('login', login)
                }
                obj.put("ban_status", 0)
                /*性能差
                def keys = liveRedis.keys(KeyUtils.USER.blackClient("*"))
                def valOp = liveRedis.opsForValue()
                // def pre = KeyUtils.USER.blackClient("").length()

                for (String key : keys) {
                    String value = (String) valOp.get(key)
                    String[] tmp = value.split("_")
                    String sUserId = tmp[0]
                    if (sUserId.equals(user_id))
                        obj.put("ban_status", 1)
                }*/
                String user_id = obj[_id].toString()
                if (bannedUsers.contains(user_id))
                    obj.put("ban_status", 1)
                if (priv.equals(UserType.经纪人.ordinal())) {
                    def timeQuery = new BasicDBObject()
                    def queryfield = [:]
                    if (stime != null) queryfield.put($gte, stime.getTime())
                    if (etime != null) queryfield.put($lt, etime.getTime())
                    if (queryfield.size() > 0) timeQuery.put('timestamp', queryfield)
                    def beanStatIter = adminMongo.getCollection('stat_brokers').aggregate(
                            $$($match, timeQuery.append('user_id', user_id as Integer)),
                            $$($project, [bean_count: '$star.bean_count']),
                            $$($group, [_id: null, bean_total: [$sum: '$bean_count']])
                    ).results().iterator()
                    if (beanStatIter.hasNext()) {
                        obj.put("bean_period", beanStatIter.next().get("bean_total"))
                    }
                    //旗下主播数量
                    def broker = obj?.get('broker') as Map
                    def star_total = obj?.get('star_total') as Integer
                    if (broker) {
                        def real_star_count = users().count($$('star.broker', user_id as Integer))
                        if (star_total != real_star_count) {//同步主播数量
                            broker['star_total'] = real_star_count
                            users().update($$(_id: user_id as Integer), $$($set: ['broker.star_total': real_star_count]))
                        }

                    }
                }

            }
        }

    }

    /**
     * 经纪人接口管理
     * @param req
     * @return
     */
    def broker_list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).get()
        query.put('priv', UserType.经纪人.ordinal())

        def stime = Web.getTime(req, 'pstime')
        def etime = Web.getTime(req, 'petime')

        //签约时间
        def astime = Web.getTime(req, 'astime')
        def aetime = Web.getTime(req, 'aetime')

        //经纪人类型：对私，对公
        if (req['partnership'] as Integer == 2) {
            query.put('broker.partnership', 2)
        }
        //经纪人是否特殊
        if (req['special'] as Integer == 1) {
            query.put('broker.special', 1)
        }

        Crud.list(req, table(), query, ALL_FIELD, SJ_DESC) { List<BasicDBObject> data ->
            for (BasicDBObject obj : data) {
                Integer user_id = obj[_id] as Integer
                def timeQuery = new BasicDBObject()
                def queryfield = [:]
                if (stime != null) queryfield.put($gte, stime.getTime())
                if (etime != null) queryfield.put($lt, etime.getTime())
                if (queryfield.size() > 0) timeQuery.put('timestamp', queryfield)
                def beanStatIter = adminMongo.getCollection('stat_brokers').aggregate(
                        $$($match, timeQuery.append('user_id', user_id as Integer)),
                        $$($project, [bean_count: '$star.bean_count']),
                        $$($group, [_id: null, bean_total: [$sum: '$bean_count']])
                ).results().iterator()
                if (beanStatIter.hasNext()) {
                    // 脚本统计出来的能量 room_cost + game_award

                    obj.put("bean_period", beanStatIter.next().get("bean_total"))
                }
                //旗下主播数量
                if (obj.containsField('broker')) {
                    def star_total = 0
                    def broker = obj['broker'] as Map
                    if (broker.containsKey('star_total')) {
                        star_total = broker['star_total'] as Integer
                    }
                    //同步主播数量
                    def real_star_count = users().count($$('star.broker', user_id)) as Integer
                    if (real_star_count != star_total) {
                        users().update($$(_id: user_id), $$($set: ['broker.star_total': real_star_count]))
                    }
                    //一段时间内签约主播,被解约的也不会被查询出来
                    List<Integer> stars = getUidsByApplyTime(astime, aetime, user_id);
                    logger.debug("broker {} stars: {}", user_id, stars)
                    broker.put('apply_period_stars', stars.size())
                    //有效签约数量 (代理在某段签约时间内签约主播一段时间内收益达到10W维C的
                    broker.putAll(caculateStars(stars, timeQuery, null))
                }
            }
        }
    }

    /**
     * 经纪人接口管理
     * @param req
     * @return
     */
    def broker_salary(HttpServletRequest req) {
        if (req['time'] == null) {
            return [code: 0]
        }
        String month = req['time']
        def stime = new SimpleDateFormat("yyyyMM").parse(month);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(stime);
        calendar.add(Calendar.MONTH, 1);
        //月末
        Date etime = calendar.getTime();

        calendar.setTime(stime); calendar.add(Calendar.MONTH, -1);
        calendar.set(Calendar.DAY_OF_MONTH, 21);
        //上上月21号
        Date stime21 = calendar.getTime();

        Integer broker_partnership = req['broker_partnership'] as Integer//经纪人类型：对私，对公
        Integer broker_special = req['broker_special'] as Integer//经纪人是否特殊

        def query = $$("priv", UserType.经纪人.ordinal())

        if (StringUtils.isNotBlank(req[_id])) {
            query.put('_id', req[_id] as Integer)
        }
        //经纪人类型：对私，对公
        if (broker_partnership != null) {
            query.put('broker.partnership', broker_partnership == 1 ? $$($ne, 2) : 2)
        }
        //经纪人是否特殊
        if (broker_special != null) {
            query.put('broker.special', broker_special == 0 ? $$($ne, 1) : 1)
        }

        /*在上月21日-本月末这个时间段内
        新签约的主播中有3名及以上当月维C收益各达到20W
        及以上或新签约主播总收益当月达到100W维C及以上*/
        Crud.list(req, users(), query, ALL_FIELD, SJ_DESC) { List<BasicDBObject> data ->
            for (BasicDBObject obj : data) {
                Integer user_id = obj[_id] as Integer
                def timeQuery = new BasicDBObject()
                def queryfield = [:]
                queryfield.put($gte, stime.getTime())
                queryfield.put($lt, etime.getTime())
                if (queryfield.size() > 0) timeQuery.put('timestamp', queryfield)
                def beanStatIter = adminMongo.getCollection('stat_brokers').aggregate(
                        $$($match, timeQuery.append('user_id', user_id as Integer)),
                        $$($project, [bean_count: '$star.bean_count']),
                        $$($group, [_id: null, bean_total: [$sum: '$bean_count']])
                ).results().iterator()
                if (beanStatIter.hasNext()) {
                    obj.put("bean_period", beanStatIter.next().get("bean_total"))
                }

                //经纪人收益有调整
                def basicSalary = basicSalary().findOne($$(_id, month + "_" + user_id))
                if (basicSalary != null) {
                    obj.put("new_earned", basicSalary.get("new_earned"))
                    obj.put("remark", basicSalary.get("remark"))
                }

                //旗下主播数量
                def broker = obj?.get('broker') as Map
                def star_total = obj?.get('star_total') as Integer
                if (broker) {
                    def real_star_count = users().count($$('star.broker', user_id))
                    if (star_total != real_star_count) {//同步主播数量
                        broker['star_total'] = real_star_count
                        users().update($$(_id: user_id), $$($set: ['broker.star_total': real_star_count]))
                    }
                    //一段时间内签约主播
                    List<Integer> stars = getUidsByApplyTime(stime21, etime, user_id);
                    logger.debug("broker {} stars: {}", user_id, stars)
                    broker.put('apply_period_stars', stars.size())
                    //有效签约数量 (代理在某段签约时间内签约主播一段时间内收益达到20W维C的
                    def caculate = caculateStars(stars, timeQuery, month);
                    broker.putAll(caculate)

                    def commission = 0.17;

                    //签约前两月结算18%
                    Long timestamp = broker.get("timestamp") as Long;
                    calendar.setTime(stime); calendar.add(Calendar.MONTH, 2);
                    Date month2etime = calendar.getTime()
                    if (timestamp >= stime.getTime() && timestamp < month2etime.getTime()) {
                        commission = 0.18;
                        //有3名及以上当月维C收益各达到20W，或总收益当月达到100W维C及以上
                    } else if ((caculate.get("apply_period_effective_stars") as Integer) > 2
                            || (caculate.get("apply_period_bean_total") as Integer) > 1000000) {
                        commission = 0.18;
                    }

                    broker.put("commission", commission)
                }
                obj.put("time", month)
            }
        }
    }

    /**
     * 经纪人底薪修改
     * @param req
     * @return
     */
    def broker_salary_modify(HttpServletRequest req) {
        if (req['_id'] == null || req['time'] == null || req['new_earned'] == null) {
            return [code: 0]
        }

        Integer uid = req['_id'] as Integer;
        String month = req['time'];
        String id = month + "_" + uid;

        basicSalary().update($$("_id", id),
                $$($set, ["new_earned": req["new_earned"] as Integer, "remark": req["remark"]]), true, false)

        return OK();
    }

    /**
     * 一段时间签约主播
     */
    private List<Integer> getUidsByApplyTime(Date applyStime, Date applyEtime, Integer brokerId) {
        def timeQuery = $$(priv: UserType.主播.ordinal(), 'star.broker': brokerId)
        def queryfield = [:]
        if (applyStime != null) queryfield.put($gte, applyStime.getTime())
        if (applyEtime != null) queryfield.put($lt, applyEtime.getTime())
        if (queryfield.size() > 0) {
            timeQuery.put('star.timestamp', queryfield)
            //logger.debug("broker ApplyTime timeQuery: ${timeQuery}")
            return users().find(timeQuery, $$(_id: 1)).toArray()*._id
        }
        return Collections.emptyList();
    }

    private static final Long EFFECTIVE_LIMIT = 200000
    /**
     * 统计一段签约时间内收益 以及达到达到10W维C收益的主播数量
     * @return
     */
    private Map caculateStars(List<Integer> stars, BasicDBObject timeQuery, String month) {
        Map result = new HashMap()
        Integer count = 0;
        Long bean_total = 0;
        result.put('apply_period_effective_stars', count)
        result.put('apply_period_bean_total', bean_total)
        if (stars == null || stars.size() == 0) return result;

        timeQuery.put('user_id', $$($in: stars))
        logger.debug("broker caculateStars query: {}", timeQuery)
        Iterator records = adminMongo.getCollection("stat_lives").aggregate(
                new BasicDBObject('$match', timeQuery),
                new BasicDBObject('$project', [_id: '$user_id', earned: '$earned']),
                new BasicDBObject('$group', [_id: '$_id', earned: [$sum: '$earned']])
        ).results().iterator()
        while (records.hasNext()) {
            def obj = records.next()
            def uid = obj.get(_id) as Integer
            Long earned = obj.get('earned') as Long

            if (StringUtils.isNotEmpty(month)) {
                def basicSalary = basicSalary().findOne($$(_id, month + "_" + uid))
                if (basicSalary != null) {
                    earned = basicSalary.get("new_earned") as Integer;
                }
            }

            logger.debug("broker star : {}, earned: {}", uid, earned)
            if (earned >= EFFECTIVE_LIMIT) {
                count++;
            }
            bean_total += earned
        }
        result.put('apply_period_effective_stars', count)
        result.put('apply_period_bean_total', bean_total)
        return result;
    }

    static final String FREEZE_TITLE = '冻结账户'
    static final String FREEZE_CONTENT = '管理员冻结账户,请联系爱玩客户人员'

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
                def live = room.containsField('live') ? room['live'] as Boolean : Boolean.FALSE
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
    static final String BAN_CONTENT = '管理员封杀设备,请联系爱玩直播客服人员'

    def ban(HttpServletRequest req) {
        def id = req.getInt(_id)
        String uid = getClientId(req, id)
        def comment = req['comment']
        if (uid) {
            Integer hour = Math.max(1, ServletRequestUtils.getIntParameter(req, 'hour', 48))
            String token = userRedis.opsForValue().get(KeyUtils.USER.token(id))
            def room = rooms().findOne($$(_id: id),$$('live':1))
            def live = room.containsField('live') ? room['live'] as Boolean : Boolean.FALSE
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

        def reason = '管理员封杀设备,如有疑问,请联系爱玩客服人员'
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

    /**
     * 完善经纪人信息
     * @param req
     */
    def info_complete(HttpServletRequest req) {
        logger.debug('Received set_cash_info params is {}', req.getParameterMap())

        def userId = req['user_id']

        if (StringUtils.isBlank(userId)) {
            return Web.missParam()
        }

        // 身份证
        def sfz = req['sfz']
        // 真实姓名
        def realName = req['real_name']
        // 卡号
        def bankId = req['bank_id']
        // 所在地
        def bankLocation = req['bank_location']
        // 支行名称
        def bankName = req['bank_name']
        // 所属银行
        def bank = req['bank']

        def map = [real_name: realName, bank_id: bankId, bank_location: bankLocation, bank_name: bankName, bank: bank, sfz: sfz]
        def query = $$('_id': userId as Integer, 'priv': UserType.经纪人.ordinal())
        def update = $$('$set': $$('broker.cash': map))
        users().update(query, update)
    }

    /**
     * 设置经济人推荐主播个数
     * @return
     */
    def set_broker_recomm(HttpServletRequest req) {
        def id = req.getInt(_id)
        def count = req.getInt('count')
        Integer type = ServletRequestUtils.getIntParameter(req, "type", RecommendType.新人.ordinal())
        //初始化之前代理推荐的主播 TODO 运营到手台手动取消推荐
        /*def stars = users().find($$('star.broker':id), $$(_id:1)).toArray()
        if(stars != null && stars.size() > 0){
            def star_ids = stars.collect{it[_id] as Integer}
                star_ids.each {Integer star_id ->
                    adminMongo.getCollection('config').update($$(_id,RecommController.index_recommend_list)
                            ,$$($pull , $$(RecommController.index_new_star_field,star_id)))
                }
        }*/
        def updateInfo = null
        def updateRecommCount = null
        if (RecommendType.新人.ordinal().equals(type)) {
            updateInfo = $$('broker.recomm_limit': count)
            updateRecommCount = $$('broker.recomm_count': 0)
        } else if (RecommendType.普通.ordinal().equals(type)) {
            updateInfo = $$('broker.meme_recomm_limit': count)
            updateRecommCount = $$('broker.meme_recomm_count': 0)
        }

        if (users().update(new BasicDBObject(_id, id).append("priv", UserType.经纪人.ordinal()),
                $$($set: updateInfo), false, false, writeConcern
        ).getN() == 1) {
            users().update($$(_id: id), $$($set: updateRecommCount), false, false, writeConcern)
            Crud.opLog(OpType.broker_recomm_set, [type: type, user_id: id, count: count])
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


    def broker_show(HttpServletRequest req) {
        def id = req[_id]
        DBObject query = Web.fillTimeBetween(req).get()
        if (id) {
            query['user_id'] = id as Integer
        }
        Crud.list(req, adminMongo.getCollection('stat_brokers'), query, ALL_FIELD, SJ_DESC) { List<BasicDBObject> data ->
            def users = users()
            for (BasicDBObject obj : data) {
                obj.putAll(users.findOne(obj['user_id'], new BasicDBObject(nick_name: 1, broker: 1)))
            }
        }
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

    def send_gift_export(HttpServletRequest req, HttpServletResponse res) {
        def _id = req.getParameter(_id) as String
        if (StringUtils.isBlank(_id)) {
            return [code: 0, msg: '请输入用户ID']
        }
        String type = 'send_gift'
        Date[] dates = ExportUtils.checkDate(Web.getStime(req), Web.getEtime(req))
        def query = Web.fillTimeBetween(dates[0], dates[1]).and('type').is(type)
        query.put('session._id').is(_id)
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
        String filename = ExportUtils.generateFilename(dates, "${_id}-${typeName}-道具销售流水".toString())
        ExportUtils.response(res, filename, titleBuf.append(bodyBuf).toString())
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

    def gift_rec(HttpServletRequest req) {
        QueryBuilder q = Web.fillTimeBetween(req)
        q.and('type').is("send_gift")
        if (req[_id]) {
            def user_id = req.getInt(_id)
            q.and('session.data.xy_user_id').is(user_id)
        }
        def room_db = logMongo.getCollection('room_cost')
        Crud.list(req, room_db, q.get(), ALL_FIELD, NATURAL_DESC)
    }

    def gift_rec_export(HttpServletRequest req, HttpServletResponse res) {
        Date[] dates = ExportUtils.checkDate(Web.getStime(req), Web.getEtime(req))
        def query = Web.fillTimeBetween(dates[0], dates[1]).and('type').is('send_gift')
        def room_db = logMongo.getCollection('room_cost')
        def bodyBuf = new StringBuffer()
        def title = Boolean.TRUE
        def result = ExportUtils.list(req, room_db, query.get(), ALL_FIELD, NATURAL_DESC) { List<BasicDBObject> list ->
            ExportUtils.render(list, ExportType.GIFT_REC_LIST, bodyBuf, title)
            if (Boolean.TRUE.equals(title)) title = Boolean.FALSE
        } as Map
        def count = result['count'] as String
        def page = result['all_page'] as String
        StringBuffer buf = ExportUtils.generateTitle(dates, count, page)
        String filename = ExportUtils.generateFilename(dates, "收礼-道具销售流水")
        ExportUtils.response(res, filename, buf.append(bodyBuf).toString())
    }

    def football_log(HttpServletRequest req) {
        QueryBuilder q = Web.fillTimeBetween(req)
        q.and('type').is("football_shoot")
        if (req[_id]) {
            q.and('session._id').is(req[_id])
        }
        Crud.list(req, logMongo.getCollection('room_cost'), q.get(), ALL_FIELD, SJ_DESC)
    }

    def card_log(HttpServletRequest req) {
        QueryBuilder q = Web.fillTimeBetween(req)
        q.and('type').is("open_card")
        if (req[_id]) {
            q.and('session._id').is(req[_id])
        }
        Crud.list(req, logMongo.getCollection('room_cost'), q.get(), ALL_FIELD, SJ_DESC)
    }

    def egg_log(HttpServletRequest req) {
        QueryBuilder q = Web.fillTimeBetween(req)
        q.and('type').is("open_egg")
        if (req[_id]) {
            q.and('session._id').is(req[_id])
        }
        Crud.list(req, logMongo.getCollection('room_cost'), q.get(), ALL_FIELD, NATURAL_DESC)
    }

    def bingo_egg_log(HttpServletRequest req) {
        QueryBuilder q = Web.fillTimeBetween(req)
        q.and('type').is("open_bingo_egg")
        if (req[_id]) {
            q.and('session._id').is(req[_id])
        }
        Crud.list(req, logMongo.getCollection('room_cost'), q.get(), ALL_FIELD, NATURAL_DESC)
    }

    def car_race_log(HttpServletRequest req) {
        QueryBuilder q = Web.fillTimeBetween(req)
        q.and('type').is("car_race")
        if (req[_id]) {
            q.and('session._id').is(req[_id])
        }
        Crud.list(req, logMongo.getCollection('room_cost'), q.get(), ALL_FIELD, NATURAL_DESC)
    }

    def prettynum_log(HttpServletRequest req) {
        QueryBuilder q = Web.fillTimeBetween(req)
        q.and('type').is("buy_prettynum")
        if (req[_id]) {
            q.and('session._id').is(req[_id])
        }
        Crud.list(req, logMongo.getCollection('room_cost'), q.get(), ALL_FIELD, SJ_DESC)
    }

    def luck_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        if (req[_id]) {
            query.and('session._id').is(req[_id])
        }
        Crud.list(req, logMongo.getCollection('room_luck'), query.get(), ALL_FIELD, SJ_DESC)
    }

    def bell_log(HttpServletRequest req) {
        QueryBuilder q = Web.fillTimeBetween(req)
        q.and('type').is("send_bell")
        if (req[_id]) {
            q.and('session._id').is(req[_id])
        }
        Crud.list(req, logMongo.getCollection('room_cost'), q.get(), ALL_FIELD, NATURAL_DESC)
    }

    def reward_log(HttpServletRequest req) {
        QueryBuilder q = Web.fillTimeBetween(req)
        if (req[_id]) {
            q.and('user_id').is(req[_id] as Integer)
        }
        Crud.list(req, rewards(), q.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> data ->
            def users = users()
            for (BasicDBObject obj : data) {
                obj.put('user_nick_name', users.findOne(obj['user_id'] as Integer, new BasicDBObject(nick_name: 1))?.get('nick_name'))
                if (obj['star_id']) {
                    obj.put('star_nick_name', users.findOne(obj['star_id'] as Integer, new BasicDBObject(nick_name: 1))?.get('nick_name'))
                }

            }
        }
    }

    def luck_log_export(HttpServletRequest req, HttpServletResponse res) {
        Date[] dates = ExportUtils.checkDate(Web.getStime(req), Web.getEtime(req))
        def query = Web.fillTimeBetween(dates[0], dates[1])
        def luck_db = logMongo.getCollection('room_luck')
        def bodyBuf = new StringBuffer()
        def title = Boolean.TRUE
        def result = ExportUtils.list(req, luck_db, query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            ExportUtils.render(list, ExportType.LUCK_LIST, bodyBuf, title)
            if (Boolean.TRUE.equals(title)) title = Boolean.FALSE
        } as Map
        def count = result['count'] as String
        def page = result['all_page'] as String
        StringBuffer titleBuf = ExportUtils.generateTitle(dates, count, page)
        String filename = ExportUtils.generateFilename(dates, "中奖记录-道具销售流水")

        ExportUtils.response(res, filename, titleBuf.append(bodyBuf).toString())
    }

    def exchange_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        if (req[_id]) {
            query.and('user_id').is(req.getInt(_id))
        }
        Crud.list(req, logMongo.getCollection('exchange_log'), query.get(), ALL_FIELD, SJ_DESC)
    }

    def lottery_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        if (req[_id]) {
            query.and('user_id').is(req.getInt(_id))
        }

        String active_name = req.getParameter("active_name")
        if (StringUtils.isNotBlank(active_name))
            query.and("active_name").is(active_name)

        String lottery_type = req.getParameter("lottery_type")

        logger.info("lottery_type---------->:" + lottery_type)
        if (StringUtils.isNotBlank(lottery_type)) {
            Integer iLotteryType = Integer.parseInt(lottery_type)
            query.and("lottery_type").is(iLotteryType)
        }
        Crud.list(req, logMongo.getCollection('lottery_logs'), query.get(), ALL_FIELD, NATURAL_DESC)
    }

    def lottery_log_export(HttpServletRequest req, HttpServletResponse res) {
        Date[] dates = ExportUtils.checkDate(Web.getStime(req), Web.getEtime(req))
        def query = Web.fillTimeBetween(dates[0], dates[1])
        def lottery_db = logMongo.getCollection('lottery_logs')
        SoftReference<StringBuffer> softBodyBuf = new SoftReference<StringBuffer>(new StringBuffer());
        def bodyBuf = softBodyBuf.get()
        def title = Boolean.TRUE
        def result = ExportUtils.list(req, lottery_db, query.get(), ALL_FIELD, NATURAL_DESC) { List<BasicDBObject> list ->
            ExportUtils.render(list, ExportType.LOTTERY_LIST, bodyBuf, title)
            if (Boolean.TRUE.equals(title)) title = Boolean.FALSE
        } as Map
        def count = result['count'] as String
        def page = result['all_page'] as String
        StringBuffer buf = ExportUtils.generateTitle(dates, count, page)
        String filename = ExportUtils.generateFilename(dates, "抽奖记录-道具销售流水")
        ExportUtils.response(res, filename, buf.append(bodyBuf).toString())
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

    def broker_edit(HttpServletRequest req) {
        Integer userId = req.getInt("_id")
        Integer partnership = req["partnership"] as Integer
        Integer special = req['special'] as Integer

        def update = new BasicDBObject()
        update.put("broker.partnership", partnership)
        update.put("broker.special", special)

        users().update($$(_id, userId).append("priv", UserType.经纪人.ordinal()), new BasicDBObject('$set', update))

        return [code: 1]
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

    /**
     * 删除用户自定义背景
     * @param req
     * @return
     */
    def del_bg(HttpServletRequest req) {
        table().update($$(_id: req.getInt(_id)), $$($unset: [bg_url: 1]))
        Crud.opLog(OpType.del_bg, [user_id: req.getInt(_id)])
        return [code: 1]
    }

}
