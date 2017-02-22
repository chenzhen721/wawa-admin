package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.IMessageCode
import com.ttpod.rest.web.Crud
import com.ttpod.star.common.util.KeyUtils
import com.ttpod.star.model.OpType
import com.ttpod.star.model.UserType
import com.ttpod.star.web.api.notify.GameService
import com.ttpod.star.web.api.notify.MessageSend
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.web.bind.ServletRequestUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import java.util.concurrent.TimeUnit

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * 直播间管理
 */
//@Rest
@RestWithSession
class RoomController extends BaseController {

    @Resource
    StarController starController

    @Resource
    StringRedisTemplate liveRedis;

    DBCollection table() { mainMongo.getCollection('rooms') }

    DBCollection room_candidates() { mainMongo.getCollection('room_candidates') }

    static final Logger logger = LoggerFactory.getLogger(UserController.class)

    //查询
    def list(HttpServletRequest req) {
        QueryBuilder query = QueryBuilder.start();
        [_id].each { String field ->
            intQuery(query, req, field)
        }
        query.and('live').is(Boolean.TRUE).and('temp').is(Boolean.TRUE)
        Crud.list(req, table(), query.get(), ALL_FIELD, $$(timstamp: -1)) { List<BasicDBObject> data ->
            def users = users()
            for (BasicDBObject obj : data) {
                def user = users.findOne(obj['xy_star_id'], $$("nick_name": 1, "finance.bean_count_total": 1))
                if (user) {
                    user.removeField(_id)
                    obj.putAll(user)
                }
            }
        }
    }

    /**
     * 运营手动点赞
     * @return
     */
    def love(HttpServletRequest req) {
        def star_id = req.getInt(_id)
        def room = table().findAndModify($$(_id: star_id, temp: Boolean.TRUE), ALL_FIELD, null, false, $$($inc: [loves: 1]), Boolean.TRUE, false)
        if (room &&
                table().update($$(_id: star_id, temp: Boolean.TRUE), $$($set, [temp: Boolean.FALSE]), false, false, writeConcern).getN() == 1) {
            Integer loves = room['loves'] as Integer
            if (loves < 3) { //前三次只是临时推荐上首页
                //加入到待定正式主播列表 后台定时任务自动恢复到临时
                room.put('love_expires', System.currentTimeMillis() + 24 * 60 * 60 * 1000)
                room.put('lastmodif_user', Web.currentUser())
                room.put('lastmodif', System.currentTimeMillis())
                room_candidates().save(room)
            }
            //
            String content = ""
            if (loves >= 3) {
                content = "恭喜您！由于您的出色表现，您已被评选为优秀主播，今后您的直播将会获得更高人气！"
            } else {
                content = "恭喜您！您的直播非常精彩，已获得官方推荐。"
            }
            publish(KeyUtils.CHANNEL.room(room[_id]), [action: "room.love", data_d: ['content': content, temp: Boolean.FALSE]])
            Crud.opLog(OpType.add_temp_to_candidates, [star_id: star_id])
        }
        OK()
    }

    /**
     * 候选人列表
     * @param req
     * @return
     */
    def candidate_list(HttpServletRequest req) {
        QueryBuilder query = QueryBuilder.start();
        [_id].each { String field ->
            intQuery(query, req, field)
        }
        Crud.list(req, room_candidates(), query.get(), ALL_FIELD, $$(timstamp: -1)) { List<BasicDBObject> data ->
            def users = users()
            for (BasicDBObject obj : data) {
                def user = users.findOne(obj['xy_star_id'], $$("nick_name": 1, "finance.bean_count_total": 1))
                if (user) {
                    user.removeField(_id)
                    obj.putAll(user)
                }
            }
        }
    }

    /**
     * 踢回临时主播
     */
    def kick_down(HttpServletRequest req) {
        def star_id = req.getInt(_id)
        if (room_candidates().remove($$(_id: star_id), writeConcern).getN() == 1) {
            table().update($$(_id: star_id), $$($set, [temp: Boolean.TRUE, loves: 0]), false, false, writeConcern)
            Crud.opLog(OpType.kick_down_candidates, [star_id: star_id])
        }
        OK()
    }

    DBCollection fake_follower_logs() { return adminMongo.getCollection('fake_follower_logs') }

    /**
     * 增加虚假主播关注数量
     */
    def add_fake_follower(HttpServletRequest req) {
        Integer roomId = req['room_id'] as Integer
        Integer followers = req['followers'] as Integer
        String id = "${roomId}_${System.currentTimeMillis()}".toString()
        if (rooms().update($$(_id: roomId), $$($inc, $$('inc_followers': followers, 'followers': followers)),
                false, false, writeConcern).getN() == 1) {
            fake_follower_logs().update($$(_id: id), $$($set, [inc_followers: followers, room_id: roomId, timstamp: System.currentTimeMillis()]),
                    true, false, writeConcern)
            Crud.opLog(fake_follower_logs().getName() + "_add", [inc_followers: followers, room_id: roomId]);
            return IMessageCode.OK;
        }
        return Web.missParam()
    }

    /**
     * 删除虚假主播关注数量
     */
    def del_fake_follower(HttpServletRequest req) {
        if (StringUtils.isEmpty(req['_id'])) {
            return Web.missParam()
        }
        DBObject fake_follower_log = fake_follower_logs().findAndRemove($$(_id, req['_id']));
        if (fake_follower_log != null) {
            Integer inc_followers = fake_follower_log.get('inc_followers') as Integer
            Integer room_id = fake_follower_log.get('room_id') as Integer
            rooms().update($$(_id: room_id, inc_followers: [$gte: inc_followers]), $$($inc, $$('inc_followers': -inc_followers, 'followers': inc_followers)))
            Crud.opLog(fake_follower_logs().getName() + "_del", fake_follower_log);
            return IMessageCode.OK;
        }
        return IMessageCode.CODE0
    }

    /**
     * 虚假关注数量操作日志流水
     */
    def fake_follower_logs(HttpServletRequest req) {
        QueryBuilder query = QueryBuilder.start();
        [_id].each { String field ->
            intQuery(query, req, field)
        }
        Crud.list(req, fake_follower_logs(), query.get(), ALL_FIELD, $$(timstamp: -1)) { List<BasicDBObject> data ->
            def rooms = rooms()
            for (BasicDBObject obj : data) {
                def room = rooms.findOne(obj['room_id'], $$("nick_name": 1, "followers": 1))
                if (room) {
                    room.removeField(_id)
                    obj.putAll(room)
                }
            }
        }
    }

    /**
     * 关播
     * @param req
     */
    def off(HttpServletRequest req) {
        logger.debug('Received off params is {}', req.getParameterMap())
        Integer roomId = ServletRequestUtils.getIntParameter(req, 'room_id', 0)
        def userId = Web.getCurrentUserId()
        def user = users().findOne($$('_id',userId));
        logger.debug('user is {},userId is {}',user,userId)
        def priv = user['priv'] as Integer

        if (roomId == 0) {
            return Web.missParam()
        }
        if (!isRoomManagement(priv)) {
            return Web.notAllowed()
        }

        final time = System.currentTimeMillis()
        final oldRoom = rooms().findAndModify($$("_id": roomId, live: Boolean.TRUE),
                $$($set, [live: false, live_id: '', position: null, live_end_time: time, pull_urls: null])
        )
        logger.debug('oldRoom is {}', oldRoom)
        String live_id = oldRoom?.get("live_id")

        Integer live_type = oldRoom?.get("live_type") as Integer
        if (StringUtils.isBlank(live_id)) {
            return [code: 30415, msg: '已经关闭了']
        }

        userRedis.delete(KeyUtils.ROOM.liveFlag(roomId))
        liveRedis.delete(liveRedis.keys(KeyUtils.LIVE.all(roomId)))
        logMongo.getCollection("room_edit").update($$(type: "live_on", data: live_id, room: roomId), $$('$set': [etime: time]))
        logRoomEdit('live_off', roomId, live_type, live_id)
        // 记录操作日志
        Crud.opLog(OpType.room_close, [user_id: userId])

        def body = ['live': false, room_id: roomId]
        MessageSend.publishLiveEvent(body)

        def zhuboId = oldRoom.get("xy_star_id") as Integer
        liveRedis.opsForValue().set(KeyUtils.LIVE.blackStar(zhuboId), KeyUtils.MARK_VAL, 600L, TimeUnit.SECONDS)

        def reason = req['reason']
        def room_close_body = ['reason': reason, user_id: zhuboId, priv: priv]
        MessageSend.publishCloseRoomByManagerEvent(room_close_body, roomId)
        def publish_star_close_body = ['ttl': 600, 'star_id': zhuboId, 'reason': reason]
        MessageSend.publishStarCloseEvent(publish_star_close_body, zhuboId)
        //记录运营、代理 关闭日志
        adminMongo.getCollection("room_terminate_ops").insert($$(_id: "${roomId}_${userId}_${time}".toString(), room_id: roomId, star_id: zhuboId as Integer,
                t_priv: priv, t_uid: userId, reason: reason, timestamp: time));

        // 关闭直播间通知游戏端
        def game_id = oldRoom['game_id']
        if (game_id != 0) {
            if (!GameService.closeGame(roomId, game_id, live_id)) {
                logger.error("请求关闭游戏失败")
                return [code: 30416,msg: '请求关闭游戏失败']
            }
        }

        return [code: 1]
    }

    /**
     * 判断是否是管理人员
     * @param room_id
     * @return
     */
    private boolean isRoomManagement(int priv) {
        Boolean competence = Boolean.FALSE
        if (priv == UserType.运营人员.ordinal() || priv == UserType.客服人员.ordinal() || priv == UserType.客服人员.ordinal()) {
            competence = Boolean.TRUE
        }
        return competence
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

}
