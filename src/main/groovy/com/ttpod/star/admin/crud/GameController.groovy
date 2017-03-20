package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.admin.Web
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.groovy.CrudClosures.*

@RestWithSession
class GameController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(GameController.class)

    def rounds() { return gameLogMongo.getCollection('game_round') }

    def bets() { return gameLogMongo.getCollection('user_bet') }

    def lotterys() { return gameLogMongo.getCollection('user_lottery') }

    def star_award_logs() { return gameLogMongo.getCollection('star_award_logs') }

    static final BasicDBObject TIMESTAMP_SORT_DESC = $$('timestamp': -1)

    @Delegate
    Crud crud = new Crud(adminMongo.getCollection('games'), true,
            [_id: Int, name: Str, pic_url: Str, status: Bool, timestamp: Timestamp, order: Int, icon_pic_url: Str],
            new Crud.QueryCondition() {
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject("timestamp", -1);
                }
            }
    )

    /**
     * 游戏日志
     * @param req
     * @return
     */
    def rounds_logs(HttpServletRequest req) {
        logger.debug('Received rounds_logs params is {}', req.getParameterMap())
        def query = Web.fillTimeBetween(req)
        def roomId = ServletRequestUtils.getIntParameter(req, 'room_id', 0)
        def roundId = ServletRequestUtils.getIntParameter(req, '_id', 0)
        def gameId = ServletRequestUtils.getIntParameter(req, 'game_id', 0)
        def liveId = ServletRequestUtils.getStringParameter(req, 'live_id', '')

        if (StringUtils.isNotBlank(liveId)) {
            query.and('live_id').is(liveId)
        }
        if (roomId != 0) {
            query.and('room_id').is(roomId)
        }
        if (roundId != 0) {
            query.and('_id').is(roundId)
        }

        if (gameId != 0) {
            query.and('game_id').is(gameId)
        }

        Crud.list(req, rounds(), query.get(), null, TIMESTAMP_SORT_DESC)
    }

    /**
     * 用户下注信息
     * @param req
     * @return
     */
    def user_bet_logs(HttpServletRequest req) {
        logger.debug('Received user_bet_logs params is {}', req.getParameterMap())
        def query = Web.fillTimeBetween(req)
        def userId = ServletRequestUtils.getIntParameter(req, 'user_id', 0)
        def roomId = ServletRequestUtils.getIntParameter(req, 'room_id', 0)
        def roundId = ServletRequestUtils.getIntParameter(req, '_id', 0)
        def gameId = ServletRequestUtils.getIntParameter(req, 'game_id', 0)
        def liveId = ServletRequestUtils.getStringParameter(req, 'live_id', '')
        if (StringUtils.isNotBlank(liveId)) {
            query.and('live_id').is(liveId)
        }
        if (roomId != 0) {
            query.and('room_id').is(roomId)
        }
        if (roundId != 0) {
            query.and('_id').is(roundId)
        }

        if (gameId != 0) {
            query.and('game_id').is(gameId)
        }
        if (userId != 0) {
            query.and('user_id').is(userId)
        }

        Crud.list(req, bets(), query.get(), null, TIMESTAMP_SORT_DESC)
    }

    /**
     * 用户输赢信息
     * @param req
     * @return
     */
    def user_lottery_logs(HttpServletRequest req) {
        logger.debug('Received user_lottery_logs params is {}', req.getParameterMap())
        def query = Web.fillTimeBetween(req)
        def userId = ServletRequestUtils.getIntParameter(req, 'user_id', 0)
        def roomId = ServletRequestUtils.getIntParameter(req, 'room_id', 0)
        def roundId = ServletRequestUtils.getIntParameter(req, '_id', 0)
        def gameId = ServletRequestUtils.getIntParameter(req, 'game_id', 0)
        def liveId = ServletRequestUtils.getStringParameter(req, 'live_id', '')

        if (StringUtils.isNotBlank(liveId)) {
            query.and('live_id').is(liveId)
        }
        if (roomId != 0) {
            query.and('room_id').is(roomId)
        }
        if (roundId != 0) {
            query.and('_id').is(roundId)
        }

        if (gameId != 0) {
            query.and('game_id').is(gameId)
        }
        if (userId != 0) {
            query.and('user_id').is(userId)
        }

        Crud.list(req, lotterys(), query.get(), null, TIMESTAMP_SORT_DESC)
    }

    /**
     * 主播分成日志
     * @param req
     * @return
     */
    def star_award_logs(HttpServletRequest req) {
        logger.debug('Received star_award_logs params is {}', req.getParameterMap())
        def query = Web.fillTimeBetween(req)
        def id = ServletRequestUtils.getIntParameter(req, '_id', 0)
        def roomId = ServletRequestUtils.getIntParameter(req, 'room_id', 0)

        if (id != 0) {
            query.and('_id').is(id)
        }

        if (roomId != 0) {
            query.and('room_id').is(roomId)
        }
        Crud.list(req, star_award_logs(), query.get(), null, TIMESTAMP_SORT_DESC)
    }


}
