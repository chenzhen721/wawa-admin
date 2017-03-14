package com.ttpod.star.common.util;

import com.ttpod.rest.AppProperties;
import com.ttpod.rest.common.util.JSONUtil;
import com.ttpod.rest.web.StaticSpring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by wqh on 2016/11/11.
 */
public class IMUtil {

    static final Logger logger = LoggerFactory.getLogger(IMUtil.class);


    private static final String IM_DOMAIN = AppProperties.get("im.domain", "http://test-aiim.memeyule.com:6060");

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
}
