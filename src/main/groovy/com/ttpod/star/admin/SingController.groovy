package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.common.doc.IMessageCode
import com.ttpod.rest.common.doc.MongoKey
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.common.util.KeyUtils
import com.ttpod.star.model.OpType
import com.ttpod.star.model.PayType
import com.ttpod.star.model.User
import org.apache.commons.lang.StringUtils

import javax.servlet.http.HttpServletRequest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * date: 14-3-24 下午2:31
 * @author: jiao.li@ttpod.com
 */
@RestWithSession
class SingController extends BaseController{

    def rounds(){return singMongo.getCollection("rounds")}
    def applyLogs(){return singMongo.getCollection("apply_logs")}
    def scoreLogs(){return singMongo.getCollection("score_logs")}
    def rankLogs(){return singMongo.getCollection("rank_logs")}
    def pointLogs(){return singMongo.getCollection("point_logs")}
    def expLogs(){return singMongo.getCollection("exp_logs")}
    def giftLogs(){return singMongo.getCollection("gift_logs")}

    def list(HttpServletRequest req){
        def q = Web.fillTimeBetween(req)
        q.and("type").is(1);
        Crud.list(req,rounds(),q.get(),$$(award:1,award_total:1,timestamp:1,status:1),SJ_DESC){List<BasicDBObject> data->
            for(BasicDBObject obj: data){ //
                def rid = obj['_id'] as String
                //荧光棒
                def gift = giftLogs().findOne($$(_id: rid));
                if(gift != null)
                    obj.putAll(gift)
                //评分人数
                obj.put("score_nums",scoreLogs().count($$(pid:rid)) ?: 0)
                //总积分
                long total_point = 0;
                def query = [pid:rid]
                //待优化，请优化==============================================//
                def res = pointLogs().aggregate(
                        new BasicDBObject('$match', query),
                        new BasicDBObject('$project', [rid: '$rid', points: '$points']),
                        new BasicDBObject('$group', [_id: '$rid',total_point: [$sum: '$points'] ]),
                        new BasicDBObject('$limit', 1)
                )
                //待优化，请优化==============================================//
                if(res.results().iterator().hasNext()){
                    def points = res.results().iterator().next()
                    total_point = (points['total_point'] ?: 0) as Long
                }
                obj.put("total_point",total_point)
            }
        }
    }

    def point_list(HttpServletRequest req){
        def pid = req['rid'] as String
        def query = $$(pid:pid);
        Crud.list(req,pointLogs(),query,ALL_FIELD,SJ_DESC)
    }

    /**
     * 比赛重置
     * @param req
     * @return
     */
    def reset(HttpServletRequest req){
        singMongo.getCollection("rounds").update($$('curr',1),$$('curr',0))
        rooms().update($$(sing:Boolean.TRUE) , $$($set, [sing:Boolean.FALSE]), false ,true)
        def id = new Date().format("yyyyMMddHHmmss")
        singMongo.getCollection("rounds").update($$(["curr":1]),
                $$([ _id:id,
                        curr:1,
                        pid:id,
                        timestamp:System.currentTimeMillis(),
                        status:1,
                        type:1
                ]), true, false)
        mainRedis.delete("singingRoundRunning")
        [code:1]
    }
}
