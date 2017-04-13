package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.persistent.KGS
import com.ttpod.rest.web.Crud
import com.ttpod.rest.web.StaticSpring
import com.ttpod.star.admin.BaseController
import org.springframework.data.mongodb.core.MongoTemplate

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.groovy.CrudClosures.*


/**
 * 2016/10/12
 * 用户特权列表
 */
@Rest
class RedPacketController extends BaseController {

    @Delegate
    Crud crud = new Crud(gameLogMongo.getCollection('red_packet_conditions'), Boolean.TRUE,
            [_id                  : Int, desc: Str, count_down: Int, order: Int, type: Int,
             award_limit          : Int, status: Bool, timestamp: Timestamp, cost_coin_condition: Int, check_in_condition: Int,
             game_number_condition: Int, send_gift_number_condition: Int, reward_coin_min: Int, reward_coin_max: Int,
             reward_cash_amount   : Str, reward_cash_ratio: Str,lock:Bool],
            new Crud.QueryCondition() {
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject('order', -1);
                }
            }
    )
}
