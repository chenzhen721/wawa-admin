package com.ttpod.star.web.api.notify;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.ttpod.rest.AppProperties;
import com.ttpod.rest.common.util.JSONUtil;
import com.ttpod.star.common.util.HttpClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * 消息推送
 */
public class MessageSend {

    static final Logger logger = LoggerFactory.getLogger(MessageSend.class);

    // ws服务器地址
    static final String WS_SERVER_URL = AppProperties.get("ws.domain");

    // 推送用户信息api
    static final String PUBLIISH_USER_API = "/api/publish/user/ID";

    // 推送房间信息api
    static final String PUBLIISH_ROOM_API = "/api/publish/room/ID";

    // 全局推送api
    static final String PUBLIISH_GLOBAL_API = "/api/publish/global";

    // 送礼action
    static final String SEND_ACTION = "room.gift.rc";

    // 全局推送action
    static final String GLOBAL_ACTION = "global.marquee";

    // 聊天推送action
    static final String CHAT_ACTION = "room.pm.rc";

    // 关注推送action
    static final String FOLLOW_ACTION = "room.follow.rc";

    // 任务推送action
    static final String MISSION_ACTION = "user.mission.rc";

    // 开播关播action
    static final String LIVE_ACTION = "room.live.rc";

    // GM关闭房间通知房间action
    static final String GM_CLOSE_ACTION = "room.gm.close.rc";

    // 强制关闭推送主播信息action
    static final String PUBLISH_STAR_CLOSE_ACTION = "user.gm.close.rc";


    /**
     * 新的推送消息接口
     */
    public static void publish(String url, String body) {
        logger.info("url is " + url + ",body is " + body);
        try {
            String resp = HttpClientUtils.postJson(url, body);
            Map result = JSONUtil.jsonToMap(resp);
            if (!result.get("code").toString().equals("1")) {
                logger.error("publish error ,resp is {}", resp);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getUserPublishUrl(String userId) {
        return String.format("%s%s", WS_SERVER_URL, PUBLIISH_USER_API.replace("ID", userId));
    }

    public static String getRoomPublishUrl(String roomId) {
        return String.format("%s%s", WS_SERVER_URL, PUBLIISH_ROOM_API.replace("ID", roomId));
    }

    public static String getGlobalPublishUrl() {
        return String.format("%s%s", WS_SERVER_URL, PUBLIISH_GLOBAL_API);
    }

    /**
     * 发送用户消息
     *
     * @param from
     * @param to
     * @param content
     */
    public static void publishUserMessageEvent(DBObject from, DBObject to, String content) {
        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("from", from);
        data.put("to", to);
        data.put("content", content);
        params.put("action", CHAT_ACTION);
    }

    /**
     * 推送主播关注信息
     *
     * @param from
     * @param to
     */
    public static void publishFollowEvent(DBObject from, DBObject to) {
        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> data = new HashMap<String, Object>();
        params.put("action", FOLLOW_ACTION);
        data.put("from", from);
        data.put("to", to);
        params.put("data", data);
        String roomId = String.valueOf(to.get("_id"));
        String url = getRoomPublishUrl(roomId);
        String postJson = JSONUtil.beanToJson(params);
        publish(url, postJson);
    }

    /**
     * 向房间推送礼物
     *
     * @param from
     * @param to
     * @param gift
     * @param count
     */
    public static void publishSendGiftEvent(DBObject from, DBObject to, DBObject gift, int earned) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("from", from);
        data.put("to", to);
        data.put("gift", gift);
        data.put("earned", earned);
        data.put("t", new Date().getTime());
        params.put("action", SEND_ACTION);
        params.put("template_id", "send_gift");
        params.put("data", data);
        String roomId = String.valueOf(to.get("_id"));
        String postJson = JSONUtil.beanToJson(params);
        String url = getRoomPublishUrl(roomId);
        publish(url, postJson);
    }

    /**
     * 任务完成通知
     *
     * @param data
     * @param userId
     */
    public static void publishMissionCompleteEvent(Map data, int userId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("action", MISSION_ACTION);
        params.put("data", data);
        params.put("template_id", "mission_complete");
        String url = getUserPublishUrl(String.valueOf(userId));
        String postJson = JSONUtil.beanToJson(params);
        publish(url, postJson);
    }

    /**
     * 发送全局信息
     */
    public static void publishGlobalEvent(Map body) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("action", GLOBAL_ACTION);
        params.put("data", body);
        params.put("template_id", "global_send_gift");
        String url = getGlobalPublishUrl();
        String postJson = JSONUtil.beanToJson(params);
        publish(url, postJson);
    }

    /**
     * 推送房间开播信息
     */
    public static void publishLiveEvent(Map body) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("action", LIVE_ACTION);
        params.put("data", body);
        String roomId = String.valueOf(body.get("room_id"));
        String url = getRoomPublishUrl(roomId);
        String postJson = JSONUtil.beanToJson(params);
        publish(url, postJson);
    }

    /**
     * gm关闭房间
     *
     * @param body
     */
    public static void publishCloseRoomByManagerEvent(Map body, int roomId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("action", GM_CLOSE_ACTION);
        params.put("data", body);
        String url = getRoomPublishUrl(String.valueOf(roomId));
        String postJson = JSONUtil.beanToJson(params);
        publish(url, postJson);
    }

    /**
     * 房间被关闭通知主播
     *
     * @param body
     */
    public static void publishStarCloseEvent(Map body, int starId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("action", PUBLISH_STAR_CLOSE_ACTION);
        params.put("data", body);
        String url = getUserPublishUrl(String.valueOf(starId));
        String postJson = JSONUtil.beanToJson(params);
        publish(url, postJson);
    }

    /**
     * 玩游戏赢一定奖励的信息
     * 上跑道
     */
    public static void publishGameEvent(String roomId, String award) {
        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("room_id", roomId);
        data.put("award", award);
        params.put("action", GLOBAL_ACTION);
        params.put("data", data);
        String url = getGlobalPublishUrl();
        String postJson = JSONUtil.beanToJson(params);
        publish(url, postJson);
    }

    public static void main(String[] args) throws IOException {
        DBObject from = new BasicDBObject("a", 1);
        DBObject to = new BasicDBObject("_id", 1201393);
        DBObject gift = new BasicDBObject("c", 3);
        publishSendGiftEvent(from, to, gift, 1);
    }
}


