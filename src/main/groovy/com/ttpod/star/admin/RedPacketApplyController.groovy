package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.persistent.KGS
import com.ttpod.rest.web.Crud
import com.ttpod.rest.web.StaticSpring
import com.ttpod.star.admin.BaseController
import com.ttpod.star.admin.Web
import com.ttpod.star.model.ApplyType
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.web.bind.ServletRequestUtils
import static com.ttpod.rest.common.util.WebUtils.$$

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.groovy.CrudClosures.*


/**
 * 2016/10/12
 * 用户特权列表
 */
@Rest
class RedPacketApplyController extends BaseController {
    @Resource
    KGS productKGS
    /**
     * day_apply_limit 每日限制申请提现次数
     * withdraw_min_cash 最小提现金额 单位 分
     * level_condition 提现等级限制
     * count_condition 提现次数
     * _id 随便用个id计数器
     * tariff 税率填写0.2 则需要 扣除20%的提现金额，填写0 则不扣税
     */
    @Delegate
    Crud crud = new Crud(gameLogMongo.getCollection('red_packet_withdraw_conditions'), Boolean.TRUE,
            [_id                  : {productKGS.nextId()}, desc: Str,day_apply_limit : Int, order: Int, withdraw_min_cash: Int,tariff:Str,
             level_condition          : Int, status: Bool, timestamp: Timestamp, count_condition: Int],
            new Crud.QueryCondition() {
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject('order', 1);
                }
            }
    )

    /**
     * 提现日志
     * @param req
     * @return
     */
//    def apply_logs(HttpServletRequest req){
//        def log =gameLogMongo.getCollection('red_packet_apply_logs')
//        def userId = ServletRequestUtils.getIntParameter(req,'user_id',0)
//        def query = Web.fillTimeBetween(req)
//        if(userId != 0){
//            query.and('user_id').is(userId)
//        }
//        Crud.list(req, log, query.get(), ALL_FIELD, SJ_DESC)
//    }
//
//    /**
//     * 批量通过
//     * @param req
//     * @return
//     */
//    def batch_pass(HttpServletRequest req){
//        List<Integer> applyIds = new ArrayList<Integer>(ids.length)
//        def ids = req.getParameterValues('_ids')
//        checkParamsValid(req,applyIds)
//        if(ids.length == 0){
//            return Web.missParam()
//        }
//        List<Integer> applyIds = new ArrayList<Integer>(ids.length)
//        ids.each {
//            it ->
//                applyIds.add(Integer.valueOf(it.toString()))
//        }
//
//        def apply_logs =gameLogMongo.getCollection('red_packet_apply_logs')
//        def query = $$('_id':['$in':applyIds],'status':ApplyType.未处理.ordinal())
//        def apply = apply_logs.find(query).toArray()
//        if(apply.size() != ids.length){
//            return Web.notAllowed()
//        }
//        def update = $$('status':ApplyType.通过.ordinal(),'last_modify':new Date().getTime())
//        apply_logs.update(query,update,false,true,writeConcern)
//    }
//
//    /**
//     * 批量拒绝
//     * @param req
//     */
//    def batch_refuse(HttpServletRequest req){
//
//    }
//
//    private Boolean checkParamsValid(HttpServletRequest req,applyIds){
//        def ids = req.getParameterValues('_ids')
//        if(ids.length == 0){
//            return  Boolean.FALSE
//        }
//        ids.each {
//            it ->
//                applyIds.add(Integer.valueOf(it.toString()))
//        }
//
//        def apply_logs =gameLogMongo.getCollection('red_packet_apply_logs')
//        def query = $$('_id':['$in':applyIds],'status':ApplyType.未处理.ordinal())
//        def apply = apply_logs.find(query).toArray()
//        if(apply.size() != ids.length){
//            return  Boolean.FALSE
//        }
//        return Boolean.TRUE
//    }
}
