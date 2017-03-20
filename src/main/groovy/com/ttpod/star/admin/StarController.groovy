package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.MongoKey
import com.ttpod.rest.web.Crud
import com.ttpod.rest.web.StaticSpring
import com.ttpod.star.common.util.ExportUtils
import com.ttpod.star.common.util.KeyUtils
import com.ttpod.star.model.ApplyType
import com.ttpod.star.model.ExportType
import com.ttpod.star.model.OpType
import com.ttpod.star.model.TerminateType
import com.ttpod.star.model.UserType
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.ServletRequestUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * date: 13-3-28 下午2:31
 * @author: yangyang.cong@ttpod.com
 */
//@Rest
@RestWithSession
class StarController extends BaseController {
    Logger logger = LoggerFactory.getLogger(StarController.class)

    DBCollection table() { users() }

    static long DAY_MILLON = 24 * 3600 * 1000L

    static long zeroMill = new Date().clearTime().getTime()

    def list(HttpServletRequest req) {
        QueryBuilder query = QueryBuilder.start();
        //签约时间
        query = Web.fillTimeBetween(query, 'star.timestamp', req, "stime", "etime").and('priv').is(UserType.主播.ordinal())

        if (!intQuery(query, req, _id)) { //没有id处理下面内容
//            if (req['nick_name')!=null]{
//                query.and('nick_name').regex(Pattern.compile("/"+Pattern.quote(req['nick_name'))+"/")]
//            }
            stringQuery(query, req, 'nick_name')
            if (req['status']) {
                query.and('status').is(!'0'.equals(req['status']))
            }
            intQuery(query, req, "star.broker")
            def start = req['sbean']
            def end = req['ebean']
            if (start || end) {
                query.and('finance.bean_count_total')
                if (start) {
                    query.greaterThanEquals(start as Long)
                }
                if (end) {
                    query.lessThan(end as Long)
                }
            }
        }

        //最后开播时间
        Date livestime = Web.getTime(req, "livestime")
        Date liveetime = Web.getTime(req, "liveetime")
        String never_live = req['never_live']
        String warning = req['warning']
        QueryBuilder liveQuery = QueryBuilder.start();
        List<Integer> starIds = null;
        Boolean needRoomQuery = Boolean.FALSE;
        if (livestime != null || liveetime != null){
            liveQuery.and(timestamp);
            if(livestime != null){
                liveQuery.greaterThanEquals(livestime.getTime());
            }
            if (liveetime != null){
                liveQuery.lessThan(liveetime.getTime());
            }
            needRoomQuery = Boolean.TRUE;
        }
        if(never_live != null && !'0'.equals(never_live)){
            liveQuery.and(timestamp).exists(Boolean.FALSE);
            needRoomQuery = Boolean.TRUE;
        }

        if(warning != null && !'0'.equals(warning)){
            liveQuery.and('warning').is(Boolean.TRUE);
            needRoomQuery = Boolean.TRUE;
        }

        String tag = req['tag'] as String
        if(StringUtils.isNotEmpty(tag)){
            liveQuery.and('tags').is(tag)
            needRoomQuery = Boolean.TRUE;
        }

        starIds = rooms().find(liveQuery.get(), $$(_id : 1)).toArray()*._id
        //logger.debug("starIds : {}", starIds);
        if(needRoomQuery && starIds != null && starIds.size() > 0)
            query.and(_id).in(starIds)

        //主播类型：对私，对公
        def partnership = req['partnership'] as Integer
        if (partnership != null) {
            query.and('star.partnership').is(partnership == 1 ? $$($ne, 2) : 2)
        }
        //主播是否特殊
        def special = req['special'] as Integer;
        if (special != null) {
            query.and('star.special').is(special == 0 ? $$($ne, 1) : 1)
        }

        def userQuery = query.get()
        //logger.debug("userQuery : {}", userQuery)
        Crud.list(req, table(), userQuery, ALL_FIELD, $$('star.timestamp', -1)) { List<BasicDBObject> data ->
            def rooms = rooms()
            def applys = adminMongo.getCollection('applys')
            for (BasicDBObject user : data) {
                def myroom = rooms.findOne($$('xy_star_id': user.get(_id)))
                def apply = applys.findOne($$('xy_user_id': user.get(_id), status:ApplyType.通过.ordinal()))
                user.put('last_live', myroom?.get(timestamp))
                user.put('baidu_active', myroom?.get("baidu_active"))
                user.put('time_slot', myroom?.get("time_slot"))
                user.put('real_sex', myroom?.get("real_sex"))
                user.put('test', myroom?.get("test"))
                user.put('warning', myroom?.get("warning"))
                user.put('tel', apply?.get("tel"))
                user.put('qq', apply?.get("qq"))
                user.put('live_type', myroom?.get("apply_type"))
                user.put('tags', myroom?.get("tags"))
                user.put('broker_recomm', myroom?.get("broker_recomm"))
                if (myroom?.get("address"))
                    user.putAll(myroom?.get("address") as Map)
            }
        }
    }

    /**
     * 解约
     * @param req
     * @return
     */
    def terminate(HttpServletRequest req) {
        def id = req.getInt(_id)
        def remark = req.getInt('remark')
        terminateStar(id, remark)
        return OK()
    }

    /**
     * 批量解约
     * @param req
     * @return
     */
    def batch_terminate(HttpServletRequest req) {
        def ids = req['ids']
        List<Integer> starids = strSplit2List(ids)
        def remark = req.getInt('remark')
        starids.each {Integer id ->
            terminateStar(id, remark)
        }
        return OK()
    }

    private void terminateStar(Integer id, Integer remark){
        def user = table().findAndModify(new BasicDBObject(_id: id, priv: UserType.主播.ordinal()),
                new BasicDBObject('$unset': [star: 1], '$set': [priv: UserType.普通用户.ordinal()])
        )
        if (user) {
            Map star = (Map) user.get('star')
            if (star) {
                Integer roomId = star.get("room_id") as Integer
                rooms().remove(new BasicDBObject(_id, roomId))

                Integer broker = star.get("broker") as Integer
                if (broker) {
                    users().update(new BasicDBObject(_id: broker, 'broker.star_total': [$gt: 0]),
                            new BasicDBObject('$inc': ['broker.star_total': -1], $pull:['broker.stars': id]))
                }

                def applys = adminMongo.getCollection('applys')
                def apply = applys.findOne(new BasicDBObject('xy_user_id': id, status: ApplyType.通过.ordinal()), new BasicDBObject('status', 1), SJ_DESC)
                if (apply) {
                    def tmp = System.currentTimeMillis()
                    applys.update(apply, new BasicDBObject('$set',
                            [status: ApplyType.解约.ordinal(), lastModif: tmp, remark: getTerminateType(remark)]))
                    logMongo.getCollection('member_applys').findAndModify(new BasicDBObject(xy_user_id: id, status: ApplyType.通过.ordinal()),
                            new BasicDBObject('$set': [status: ApplyType.关闭.ordinal(), lastmodif: tmp, msg: 'terminate']))
                    familyMongo.getCollection('members').updateMulti($$(uid:id), $$($set,$$(user_priv:UserType.普通用户.ordinal())))
                }
                Crud.opLog(OpType.star_terminate, [user_id: id, roomId: roomId])
                //清除粉丝关注
                removeFollow(roomId)
                Web.api('java/flushuser?id1=' + id)
            }
        }
    }

    /**
     * 清除关注关系
     */
    private void removeFollow(Integer star_id){
        def followers = mainRedis.opsForSet().members(KeyUtils.USER.followers(star_id))
        if(followers != null && followers.size() > 0){
            followers.each {String userId ->
                mainRedis.opsForSet().remove(KeyUtils.USER.following(userId),star_id.toString())
            }
        }
        mainRedis.delete(KeyUtils.USER.followers(star_id))
        mainRedis.delete(KeyUtils.USER.historyFollowers(star_id))
    }

    private String getTerminateType(Integer i) {
        switch (i) {
            case TerminateType.换代理.ordinal():
                return TerminateType.换代理.toString()
                break
            case TerminateType.大号换小号.ordinal():
                return TerminateType.大号换小号.toString()
                break
            case TerminateType.未完成任务开除.ordinal():
                return TerminateType.未完成任务开除.toString()
                break
            case TerminateType.违规解约.ordinal():
                return TerminateType.违规解约.toString()
                break
            case TerminateType.工作室要求解约.ordinal():
                return TerminateType.工作室要求解约.toString()
                break
            case TerminateType.形象不达标.ordinal():
                return TerminateType.形象不达标.toString()
                break
            default: return ''
        }
    }


    /**
     * 清理直播间观众
     * @param req
     */
    def clean_viewer(HttpServletRequest req){
        def room_id = req.getInt(_id)
        userRedis.delete(KeyUtils.ROOM.users(room_id))
        Crud.opLog(OpType.clean_viewer, [ room_id : room_id])
        [code : 1]
    }


    def live_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).and('type').is('live_on')
        if (req[_id]) {
            query.and('session._id').is(req[_id])
        }
        Crud.list(req, logMongo.getCollection('room_edit'), query.get(), ALL_FIELD, SJ_DESC)
    }

    /**
     * 火拼日志
     * @param req
     * @return
     */
    def pk_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        if (req[_id]) {
            query.and('pk_ids').is(req[_id] as Integer)
        }
        if (req['fid']) {
            query.and('f_id').is(req['fid'] as Integer)
        }
        if (req['tid']) {
            query.and('t_id').is(req['tid'] as Integer)
        }
        Crud.list(req, logMongo.getCollection('pk_logs'), query.get(), $$(pk_ids:0), SJ_DESC){ List<BasicDBObject> data ->
            def users = users()
            for (BasicDBObject obj : data) {
                obj.remove(_id)
                obj.put("from_nick_name", users.findOne(obj['f_id'], new BasicDBObject("nick_name", 1))?.get("nick_name"))
                obj.put("to_nick_name", users.findOne(obj['t_id'], new BasicDBObject("nick_name", 1))?.get("nick_name"))
            }
        }
    }


    /**
     * 直播间连麦日志
     * @param req
     * @return
     */
    def room_mic_log(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        if (req['user_id']) {
            query.and('user_id').is(req['user_id'] as Integer)
        }
        if (req[_id]) {
            query.and('room_id').is(req[_id] as Integer)
        }
        Crud.list(req, logMongo.getCollection('room_mic_log'), query.get(), ALL_FIELD, SJ_DESC){ List<BasicDBObject> data ->
            def users = users()
            for (BasicDBObject obj : data) {
                obj.remove(_id)
                obj.put("nick_name", users.findOne(obj['user_id'], new BasicDBObject("nick_name", 1))?.get("nick_name"))
                obj.put("star_nick_name", users.findOne(obj['room_id'], new BasicDBObject("nick_name", 1))?.get("nick_name"))
            }
        }
    }

    def gift_log(HttpServletRequest req) {
        stars_log(req, 'send_gift')
    }

    def song_log(HttpServletRequest req) {
        stars_log(req, 'song')
    }

    //抢沙发记录
    def sofa_log(HttpServletRequest req) {
        stars_log(req, 'grab_sofa')
    }
    //接生记录
    def level_up_log(HttpServletRequest req) {
        stars_log(req, 'level_up')
    }

    def vip_log(HttpServletRequest req) {
        stars_log(req, 'buy_vip')
    }

    private stars_log(HttpServletRequest req, String type) {
        QueryBuilder q = Web.fillTimeBetween(req)
        q.and('type').is(type)
        stringQuery(q, req, 'live')
        if (req[_id]) {
            def room_id = req.getInt(_id)
            q.and('room').is(room_id)
            if ('send_gift'.equals(type)) {
                q.and('session.data.xy_star_id').is(room_id)
            }
        }

        def room_db = logMongo.getCollection('room_cost')
        Crud.list(req, room_db, q.get(), ALL_FIELD, SJ_DESC)
    }

    def stars_log_history(HttpServletRequest req) {
        QueryBuilder q = Web.fillTimeBetween(req)
        stringQuery(q, req, 'type')

        if (req[_id]) {
            def room_id = req.getInt(_id)
            q.and('star_id').is(room_id)

        }
        def room_db = logMongo.getCollection('room_cost_day_star')
        Crud.list(req, room_db, q.get(), ALL_FIELD, SJ_DESC)
    }

    static String[] apply_props = [
            'real_name', "bank", "bank_location", "bank_name", "bank_id", "bank_user_name",
            "tel", "qq", "address", 'sfz', "sex", "baidu_active", "time_slot"
    ]

    def edit(HttpServletRequest req) {
        Integer star_id = req.getInt(_id)
        def query = new BasicDBObject('xy_user_id', star_id)
        def update = new BasicDBObject()

        for (String prop : apply_props) {
            String v = req.getParameter(prop)
            if (StringUtils.isNotEmpty(v)) {
                update.put(prop, v)
            }
        }
        def update_romm = new BasicDBObject()
        String sex = req.getParameter("sex")
        if (StringUtils.isNotEmpty(sex)) {
            update_romm.put("real_sex", Integer.parseInt(sex))
        }
        String province = req.getParameter("province")
        if (StringUtils.isNotEmpty(province)) {
            update_romm.put("address.province", province)
        }
        String city = req.getParameter("city")
        if (StringUtils.isNotEmpty(city)) {
            update_romm.put("address.city", city)
        }
        String region = req.getParameter("region")
        if (StringUtils.isNotEmpty(region)) {
            update_romm.put("address.region", region)
        }
        String baidu_active = req.getParameter("baidu_active")
        if (StringUtils.isNotEmpty(baidu_active)) {
            update_romm.put("baidu_active", Integer.parseInt(baidu_active))
        }
        String time_slot = req.getParameter("time_slot")
        if (StringUtils.isNotEmpty(time_slot)) {
            update_romm.put("time_slot", time_slot)
        }
        String test = req.getParameter("test")
        if (StringUtils.isNotEmpty(test)) {
            update_romm.put("test", '1'.equals(test))
        }
        //标记违规主播
        String warning = req.getParameter("warning")
        if (StringUtils.isNotEmpty(warning)) {
            update_romm.put("warning", '1'.equals(warning))
        }
        //是否允许代理推荐主播
        String broker_recomm = req.getParameter("broker_recomm")
        if (StringUtils.isNotEmpty(broker_recomm)) {
            update_romm.put("broker_recomm", broker_recomm as Integer)
        }
        //主播标签
        edit_star_tag(req)

        if (!update.isEmpty()) {
            def res = adminMongo.getCollection('applys').findAndModify(query, SJ_DESC, new BasicDBObject('$set', update))
            if(res == null){
                return [code: 0, msg: 'apply not found']
            }
        }
        if (!update_romm.isEmpty()) {
            if(mainMongo.getCollection("rooms").update($$('xy_star_id', star_id), new BasicDBObject('$set', update_romm)).getN() != 1){
                return [code:0]
            }
        }
        Crud.opLog(OpType.edit_star_info, [xy_user_id:star_id])


        //对公对私，是否特殊
        def starUpdate = new BasicDBObject()
        Integer partnership = req["partnership"] as Integer
        Integer special = req['special'] as Integer
        starUpdate.put("star.partnership", partnership)
        starUpdate.put("star.special", special)
        users().update($$(_id, star_id).append("priv", UserType.主播.ordinal()), new BasicDBObject('$set', starUpdate))

        [code: 1]
    }

    def add_union_pic(HttpServletRequest req) {
        def map = new HashMap()

        String v = req.getParameter("union_pic")
        String unionKey = req.getParameter("union_key")
        String field = "union_pic"

        if ("km".equals(unionKey))
            field = field + ".km"
        else if ("bd".equals(unionKey))
            field = field + ".bd"

        if (StringUtils.isNotBlank(v)) {
            map.put(field, v)
            // if(v.startsWith(pic_domain))
        }
        if (map.size() > 0)
            users().update(new BasicDBObject(_id, req.getInt(_id)), new BasicDBObject($set, map), false, false, writeConcern)
        [code: 1]
    }

    //多张图片
    def add_union_pics(HttpServletRequest req) {
        def map = new HashMap()

        String v = req.getParameter("union_pic")
        String unionKey = req.getParameter("union_key")
        String field = "union_pic"

        if ("km".equals(unionKey))
            field = field + ".kms"
        else if ("bd".equals(unionKey))
            field = field + ".bds"

        if (StringUtils.isNotBlank(v)) {
            map.put(field, v)
            // if(v.startsWith(pic_domain))
        }
        if (map.size() > 0)
            users().update(new BasicDBObject(_id, req.getInt(_id)), new BasicDBObject($addToSet, map), false, false, writeConcern)
        [code: 1]
    }

    def del_union_pic(HttpServletRequest req) {
        def map = new HashMap()
        String v = req.getParameter("union_pic")
        String unionKey = req.getParameter("union_key")
        String field = "union_pic"

        if ("km".equals(unionKey))
            field = field + ".kms"
        else if ("bd".equals(unionKey))
            field = field + ".bds"
        if (StringUtils.isNotBlank(v)) {
            map.put(field, v)
        }
        if (map.size() > 0)
            users().update(new BasicDBObject(_id, req.getInt(_id)), new BasicDBObject($pull, map), false, false, writeConcern)
        [code: 1]
    }

    def change_broker(HttpServletRequest req) {
        String input = req[auth_code]
        /*if (codeVerifError(req, input)) {
            return [code: 30419, msg: '验证码错误']
        }*/
        def star_id = req.getInt(_id)
        def broker_id = req.getInt('broker')
        def star_query = new BasicDBObject(_id: star_id, priv: UserType.主播.ordinal(), 'star.broker': [$ne: broker_id])
        def broker_query = new BasicDBObject(_id: broker_id, priv: UserType.经纪人.ordinal())
        def users = users()
        if (users.count(star_query) + users.count(broker_query) != 2) {
            return [code: 0, msg: 'star or broker NOT exist,OR star.broker = current']
        }
        adminMongo.getCollection('applys').findAndModify(new BasicDBObject('xy_user_id': star_id, status: ApplyType.通过.ordinal()), SJ_DESC,
                new BasicDBObject('$set': [broker: broker_id.toString()]))
        def old_broker = users.findOne(new BasicDBObject(_id, star_id), $$('star': 1)).get('star')?.getAt('broker')
        users.findAndModify(new BasicDBObject(_id, star_id), new BasicDBObject('$set', ['star.broker': broker_id]))
        if (old_broker) {
            users.update(new BasicDBObject(_id: old_broker, 'broker.star_total': [$gt: 0]), new BasicDBObject('$inc': ['broker.star_total': -1], $pull:['broker.stars': star_id]))
        }
        println "old_broker : ${old_broker}  broker : ${broker_id}"
        users.update(new BasicDBObject(_id, broker_id),
                new BasicDBObject($addToSet: ['broker.stars': star_id], $inc: ['broker.star_total': 1]))
        Crud.opLog(OpType.star_change_broker, [star: star_id, old_broker: old_broker, broker: broker_id])
        OK()
    }


    def history_special_gift(HttpServletRequest req) {
        QueryBuilder q = QueryBuilder.start();
        Date stime = Web.getStime(req);
        Date etime = Web.getEtime(req);
        def ql = []
        for (int i = 1; i < 5; i++) {
            def qstar = QueryBuilder.start()
            if (stime != null || etime != null) {
                qstar.and("star${i}.bonus_time".toString());
                if (stime != null) {
                    qstar.greaterThanEquals(stime.getTime());
                }
                if (etime != null) {
                    qstar.lessThan(etime.getTime());
                }
            }

            if(req[_id]) qstar.and("star${i}._id".toString()).is(req[_id] as Integer)
            ql.add(qstar.get())
        }
        q.or((DBObject[]) ql.toArray())
        Crud.list(req, logMongo.getCollection('special_gifts'), q.get(), ALL_FIELD, ID_DESC, history_special_gift_closure())
    }

    def history_special_gift_export(HttpServletRequest req, HttpServletResponse res) {
        Date[] dates = ExportUtils.checkDate(Web.getStime(req), Web.getEtime(req))
        QueryBuilder query = QueryBuilder.start();
        Date stime = dates[0];
        Date etime = dates[1];
        def ql = []
        for (int i = 1; i < 5; i++) {
            def qstar = QueryBuilder.start()
            if (stime != null || etime != null) {
                qstar.and("star${i}.bonus_time".toString());
                if (stime != null) {
                    qstar.greaterThanEquals(stime.getTime());
                }
                if (etime != null) {
                    qstar.lessThan(etime.getTime());
                }
            }

            ql.add(qstar.get())
        }
        query.or((DBObject[]) ql.toArray())
        def special_db = logMongo.getCollection('special_gifts')
        def bodyBuf = new StringBuffer()
        def title = Boolean.TRUE
        def result = ExportUtils.list(req, special_db, query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> data ->
            history_special_gift_closure().call(data)
            ExportUtils.render(data, ExportType.SPECIAL_GIFT_LIST, bodyBuf, title)
            if (Boolean.TRUE.equals(title)) title = Boolean.FALSE
        } as Map
        def count = result['count'] as String
        def page = result['all_page'] as String
        StringBuffer buf = ExportUtils.generateTitle(dates, count, page)
        String filename = ExportUtils.generateFilename(dates, "奇迹礼物-道具销售流水")
        //获得值列表
        ExportUtils.response(res, filename, buf.append(bodyBuf).toString())
    }

    def history_special_gift_closure() {
        { List<BasicDBObject> data ->
            def users = users()
            def rooms = rooms()
            def liveField = new BasicDBObject("live", 1)
            for (BasicDBObject giftLog : data) {
                for (int i = 1; i < 5; i++) {
                    fillBasicInfo(users, giftLog, i)
                    def star1 = (Map) giftLog.get("star${i}".toString())
                    if (null != star1) { // 增加主播是否开播字段
                        star1.put("live", Boolean.TRUE.equals(rooms.findOne(star1.get(_id), liveField)?.get("live")))
                    }
                }
            }
        }
    }

    static final String[] gift_log_props = ['star', 'fan']

    private static DBObject fillBasicInfo(DBCollection users, DBObject giftLog,int i) {
        for (String field : gift_log_props) {
            def user = (Map) giftLog.get(field + i)
            if (user != null && user.size() > 0) {
                Map map = (Map) users.findOne(user.get("_id") as Integer,
                        new BasicDBObject('finance.coin_spend_total': 1, 'finance.bean_count_total': 1, nick_name: 1, pic: 1))
                user.putAll(map)
            }
        }
        return giftLog
    }

    def promotion_list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).and('via').is('System')
        def room_id = req.getParameter("_id")
        if (room_id)
            query.and("star_id").is(req.getInt(_id))
        def field = $$(bean: 1, coin: 1, nick_name: 1, star_id: 1, timestamp: 1, user_id: 1)
        return Crud.list(req, adminMongo.getCollection('bean_ops'), query.get(), field, SJ_DESC)
    }


    @Value("#{application['pic.domain']}")
    String pic_domain = "https://aiimg.sumeme.com/"

    File pic_folder

    public static final BasicDBObject ROOM_QUERY = new BasicDBObject(pic_url: [$exists: true])

    @Value("#{application['pic.folder']}")
    void setPicFolder(String folder) {
        pic_folder = new File(folder)
        pic_folder.mkdirs()
        println "初始化图片上传目录 : ${folder}"
    }

    @Resource
    UnionPicController unionPicController
    /**
     * 批量切割为百度推广图片要求格式
     * @param request
     * @return
     */
    def batchPic(HttpServletRequest request) {
        StaticSpring.execute(
                new Runnable() {
                    public void run() {
                        def users = mainMongo.getCollection("users");
                        def rooms = mainMongo.getCollection("rooms");
                        Long b = System.currentTimeMillis()
                        rooms.find(ROOM_QUERY, new BasicDBObject("pic_url", 1)).toArray().each {
                            String url = it["pic_url"] as String
                            Integer _id = it["_id"] as Integer
                            unionPicController.generateUnionPic(_id, url)
                        }
                        println "batchPic cost time ${System.currentTimeMillis() - b}"
                    }
                }
        );
        Crud.opLog(OpType.batch_uion_pic, [])
        return [code: 1]
    }

    /**
     * 主播标签
     * @param req
     */
    def tags(HttpServletRequest req) {
        def tags = new ArrayList();
        adminMongo.getCollection('tags').find().toArray().each { DBObject obj ->
            tags.add(obj['tags'])
        }
        [code: 1, data: tags]
    }

    /**
     * 主播标签
     * @param req
     * @return
     */
    def star_tag(HttpServletRequest req) {
        String star_id = req.getParameter(_id)
        def room = rooms().findOne($$(xy_star_id: star_id as Integer), $$('tags': 1))
        [code: 1, data: [tags: room?.get('tags')]]
    }

    /**
     * 编辑主播标签
     * @param req
     */
    def edit_star_tag(HttpServletRequest req) {
        def star_id = req['_id'] as Integer
        def tags = req['tags']
        if(add_star_tag(star_id, tags)){
            return [code: 1]
        }
        return [code: 0]
    }

    public Boolean add_star_tag(Integer star_id, String tags){
        if (StringUtils.isNotEmpty(tags)) {
            def tags_table = adminMongo.getCollection('tags')
            //检查tags有效性
            Boolean valid = Boolean.TRUE
            tags.split(',').each { String tag ->
                if (tags_table.count($$('tags': tag)) <= 0)
                    valid = Boolean.FALSE
            }
            if (valid && rooms().update($$(xy_star_id: star_id),
                    $$($addToSet, $$("tags", $$('$each', tags.split(','))))
                    , true, false).getN() == 1) {
                return Boolean.TRUE
            }
        }
        return Boolean.FALSE
    }

    public Boolean remove_star_tag(Integer star_id, String tag){
        if (rooms().update($$('xy_star_id': star_id),
                $$($pull , $$("tags", tag)), true, false).getN() == 1) {
            return Boolean.TRUE
        }
        return Boolean.FALSE
    }
    /**
     * 编辑主播标签
     * @param req
     */
    def edit_room_tag(HttpServletRequest req) {
        def room_id = req['_id'] as Integer
        def tags = req['tags']
        if (rooms().update($$(_id: room_id),
                $$($unset, $$("tags", 1)), true, false).getN() == 1) {

            if (StringUtils.isNotEmpty(tags)) {
                def tags_table = adminMongo.getCollection('tags')
                //检查tags有效性
                Boolean valid = Boolean.TRUE
                tags.split(',').each { String tag ->
                    if (tags_table.count($$('tags': tag)) <= 0)
                        valid = Boolean.FALSE
                }
                if (valid && rooms().update($$(_id: room_id),
                        $$($addToSet, $$("tags", $$('$each', tags.split(','))))
                        , true, false).getN() == 1) {
                    return [code: 1]
                }
                return [code: 0]
            }
            return [code: 1]
        }
        return [code: 0]
    }

    private static List<Integer> strSplit2List(String str){
        return str.trim().replace('，',',',).split(',').collect { Integer.valueOf(it.toString())}
    }

    /**
     * 即构录像回放地址
     * @param req
     * @return
     */
    def replay_list(HttpServletRequest req){
        QueryBuilder query = Web.fillTimeBetween(req);
        def roomId = req['_id'] as Integer
        if(roomId != null){
            query.and('roomId').is(roomId)
        }
        query.and('type').is('replay')
        Crud.list(req, unionMongo.getCollection('room_cdn'), query.get(), ALL_FIELD, SJ_DESC) {List<BasicDBObject> datas ->
            for (BasicDBObject obj : datas) {
                def data = obj['data'] as Map
                def replay_urls =  data['replay_url'] as List<String>
                logger.debug("replay_urls : {}", replay_urls)
                obj.put('replay_url', replay_urls[0]);
                obj.remove('data');
                obj.remove('_id')
            }
        }
    }

    /**
     * 关闭主播直播播间操作记录
     * @param req
     * @return
     */
    def room_terminate_log(HttpServletRequest req){
        QueryBuilder query = Web.fillTimeBetween(req);
        def roomId = req['_id'] as Integer
        if(roomId != null){
            query.and('room_id').is(roomId)
        }
        Crud.list(req, adminMongo.getCollection("room_terminate_ops"), query.get(), ALL_FIELD, SJ_DESC) {List<BasicDBObject> datas ->
            def users = users()
            def rooms = rooms()
            for (BasicDBObject obj : datas) {
                obj.put("star_nick_name", users.findOne(obj['star_id'], new BasicDBObject("nick_name", 1))?.get("nick_name"))
                obj.put("t_nick_name", users.findOne(obj['t_uid'], new BasicDBObject("nick_name", 1))?.get("nick_name"))
            }
        }
    }

    /**
     * 主播分成
     * @param req
     */
    def star_award_logs(HttpServletRequest req){
        def query = Web.fillTimeBetween(req)
        def roomId = ServletRequestUtils.getIntParameter(req,'room_id',0)
        if(roomId != 0 ){
            query.and('room_id').is(roomId)
        }

        def star_award_logs = gameLogMongo.getCollection("star_award_logs")
        return Crud.list(req, star_award_logs, query.get(), null, SJ_DESC)
    }
}
