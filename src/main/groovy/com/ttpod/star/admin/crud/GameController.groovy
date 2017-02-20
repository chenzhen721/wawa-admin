package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.admin.Web
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.groovy.CrudClosures.*

@RestWithSession
class GameController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(GameController.class)

    def game_logs() { return logMongo.getCollection('game_logs') }

    static final BasicDBObject TIMESTAMP_SORT_DESC = $$('timestamp': -1)

    @Delegate
    Crud crud = new Crud(adminMongo.getCollection('games'),
            [_id: Str, name: Str, pic_url: Str, status: Bool, timestamp: Timestamp,order:Int,icon_pic_url:Str],
            new Crud.QueryCondition() {
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject("timestamp", -1);
                }
            }
    )

    /**
     * 用户玩游戏查询
     * @param req
     * @return
     */
    def log_list(HttpServletRequest req) {
        logger.debug('Received game_log params is {}', req.getParameterMap())
        def userId = req['user_id']
        def roomId = req['room_id']
        def round_id = req['round_id'] // 每一局游戏的id
        def query = Web.fillTimeBetween(req)
        if (StringUtils.isNotBlank(userId)) {
            query.and('user_id').is(userId as Integer)
        }
        if(StringUtils.isNotBlank(roomId)){
            query.and('room_id').is(roomId as Integer)
        }
        if(StringUtils.isNotBlank(round_id)){
            query.and('round_id').is(round_id as Integer)
        }
        Crud.list(req, game_logs(), query.get(), null, TIMESTAMP_SORT_DESC)
    }

}
