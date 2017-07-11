package com.ttpod.star.common.util;

import com.ttpod.rest.AppProperties;
import com.ttpod.rest.common.util.JSONUtil;
import com.ttpod.rest.web.StaticSpring;
import com.ttpod.star.model.IMType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wqh on 2016/11/11.
 */
public class IMUtil {

    static final Logger logger = LoggerFactory.getLogger(IMUtil.class);


    private static final String IM_DOMAIN = AppProperties.get("im.domain", "http://test-aiim.memeyule.com:6070");

    public static void sendToUser(Object body) {
        send("user", body);
    }
    public static void sendToUsers(Object body) {
        send("batch", body);
    }

    private static void send(final String path, final Object body) {
        StaticSpring.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = HttpClientUtils.postJson(IM_DOMAIN + "/api/publish/" + path, JSONUtil.beanToJson(body));
                    logger.info("result: {}", result);
                    if (result != null && JSONUtil.jsonToMap(result).get("code") != 1) {
                        logger.error("push error" + result);
                    }
                    logger.debug("push result:" + result);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    static final Long MESSAGE_EXPIRE = 60 * 60 * 1000L;

    /**
     * 构建系统消息
     *
     * @param title
     * @param text
     * @param userIds
     * @param isNotify
     * @param isSave
     * @return
     */
    public static Map<String, Object> buildSystemMessageBody(String title, String text, List<Integer> userIds, Integer isNotify, Integer isSave) {
        Map<String, Object> body = new HashMap<String, Object>();
        Map<String, Object> message = new HashMap<String, Object>();
        Long now = System.currentTimeMillis();
        Long expireTime = now + MESSAGE_EXPIRE;
        Map<String, Object> extra = new HashMap<String, Object>();
        extra.put("event", IMType.系统消息.getEvent());

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("title", title);
        data.put("text", text);
        data.put("ts", now);
        data.put("expire_time", expireTime);
        data.put("type", 2);
        message.put("action", IMType.系统消息.getAction());
        message.put("data", data);

        body.put("user_ids", userIds);
        body.put("isNotify", isNotify);
        body.put("isSave", isSave);
        body.put("extra", extra);
        body.put("message", message);
        logger.debug("body is " +  body);
        return body;
    }
}
