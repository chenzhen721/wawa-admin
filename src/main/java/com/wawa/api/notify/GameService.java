package com.wawa.api.notify;

import com.wawa.AppProperties;
import com.wawa.common.util.HttpClientUtils;
import com.wawa.common.util.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 游戏推送
 */
public class GameService {

    static final Logger logger = LoggerFactory.getLogger(GameService.class);
    // 游戏服务器服务器地址
    static final String SERVER_DOMAIN = AppProperties.get("api.domain");

    /**
     * 开始游戏
     * @param roomId
     * @param gameId
     * @param liveId
     * @return
     */
    public static Boolean starGame(Object roomId, Object gameId, String liveId) {
        try {
            String open_game_server_url = SERVER_DOMAIN + "/api/room/open?room_id="+roomId+"&game_id="+gameId+"&live_id="+liveId;
            String resp = HttpClientUtils.get(open_game_server_url, null);
            Map result = JSONUtil.jsonToMap(resp);
            if (!result.get("code").toString().equals("1")) {
                return Boolean.FALSE;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * 结束游戏
     * @param roomId
     * @param gameId
     * @param liveId
     * @return
     */
    public static Boolean closeGame(Object roomId, Object gameId, String liveId) {
        try {
            String open_game_server_url = SERVER_DOMAIN + "/api/room/close?room_id="+roomId+"&game_id="+gameId+"&live_id="+liveId;
            String resp = HttpClientUtils.get(open_game_server_url, null);
            Map result = JSONUtil.jsonToMap(resp);
            if (!result.get("code").toString().equals("1")) {
                return Boolean.FALSE;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }
}
