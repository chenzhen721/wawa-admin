package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.rest.web.StaticSpring
import com.ttpod.star.common.doc.Param
import com.ttpod.star.common.util.ExportUtils
import com.ttpod.star.model.ApplyType
import com.ttpod.star.model.ExportType
import com.ttpod.star.model.FundingProcess
import com.ttpod.star.model.FundingType
import com.ttpod.star.model.OpType
import com.ttpod.star.model.UserType
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.ServletRequestUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * 众筹管理
 */
//@Rest
@RestWithSession
class CrowdfundingController extends BaseController {

    DBCollection fundings() { activeMongo.getCollection('fundings') }
    DBCollection fundings_logs() { activeMongo.getCollection('fundings_logs') }
    DBCollection fundings_purchase_logs() { activeMongo.getCollection('fundings_purchase_logs') }
    DBCollection fundings_award_logs() { activeMongo.getCollection('fundings_award_logs') }

    def list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        Crud.list(req, fundings(), query.get(), ALL_FIELD, SJ_DESC)
    }

    def add(HttpServletRequest req) {
        Integer price = ServletRequestUtils.getIntParameter(req, 'price', 100);
        Integer quantity = ServletRequestUtils.getIntParameter(req, 'quantity', 1);
        Integer amount = ServletRequestUtils.getIntParameter(req, 'amount', 1);
        Integer limit = ServletRequestUtils.getIntParameter(req, 'limit', quantity);
        Integer type = ServletRequestUtils.getIntParameter(req, 'type', FundingType.礼物.ordinal());
        Boolean status = Boolean.FALSE
        String desc = req.getParameter("desc") as String

        String title = req.getParameter("title") as String
        String pic = req.getParameter("pic") as String
        Integer cid = req.getParameter("cid") as Integer
        String swf_url = req.getParameter("swf_url") as String
        Integer total = quantity * price
        if(StringUtils.isEmpty(title))
            return Web.missParam()
        def info = $$(_id:System.currentTimeMillis(), price : price, status:status,timestamp:System.currentTimeMillis(),limit:limit,
                quantity:quantity,total : total, type : type, title : title, desc : desc, pic : pic, cid : cid, amount:amount, swf_url:swf_url)
        if(fundings().save(info, writeConcern).getN() == 1){
            Crud.opLog(OpType.funding_add, info)
            return [code : 1]
        }
       return [code : 0]
    }

    def edit(HttpServletRequest req) {
        Long _id = req.getParameter("_id") as Long
        Integer price = ServletRequestUtils.getIntParameter(req, 'price', 100);
        Integer quantity = ServletRequestUtils.getIntParameter(req, 'quantity', 1);
        Integer amount = ServletRequestUtils.getIntParameter(req, 'amount', 1);
        Integer type = ServletRequestUtils.getIntParameter(req, 'type', FundingType.礼物.ordinal());
        Integer limit = ServletRequestUtils.getIntParameter(req, 'limit', quantity);
        String desc = req.getParameter("desc") as String
        String title = req.getParameter("title") as String
        String pic = req.getParameter("pic") as String
        Integer cid = req.getParameter("cid") as Integer
        String swf_url = req.getParameter("swf_url") as String
        Integer total = quantity * price
        if(_id == null)
            return Web.missParam()
        def info = $$(price : price, quantity:quantity, limit:limit,total : total, type : type, title : title, desc : desc, pic : pic, cid : cid, amount:amount, swf_url:swf_url)
        if(fundings().update($$(_id:_id),$$($set:info),false, false, writeConcern).getN() == 1){
            Crud.opLog(OpType.funding_edit, info)
            return [code : 1]
        }
        [code : 0]
    }

    /**
     * 上线和下线
     * @param req
     */
    def change_status(HttpServletRequest req){
        Long _id = req.getParameter("_id") as Long
        Boolean status = ServletRequestUtils.getBooleanParameter(req, 'status', Boolean.FALSE)
        if(fundings().update($$(_id:_id,status:!status),$$($set:[status:status]),false, false, writeConcern).getN() == 1){
            Crud.opLog(OpType.funding_edit, [_id:_id,status:status])
            //上线 开启一期众筹
            if(status == Boolean.TRUE){
                open(_id)
            }else{//线下，将反回当前正在众筹中用户柠檬
                close(_id)
            }
            return [code : 1]
        }
        return [code : 1]
    }

    /**
     * 开启新一期众筹
     */
    private void open(Long fid){
        def fund = fundings().findAndModify($$(_id:fid,status: Boolean.TRUE), null, null, false,$$($inc:[round : 1]), true, false)
        if(fund != null){
            Integer round = fund.get('round') as Integer
            def fundings_log = $$(_id: fid+"_"+round)
            fundings_log.put('fid', fid)
            fund.removeField(_id)
            fundings_log.putAll(fund)
            fundings_log.put(timestamp, System.currentTimeMillis())
            fundings_log.put('process_status', FundingProcess.进行.ordinal())
            fundings_log.put('sold', 0)
            if(fundings_logs().save(fundings_log, writeConcern).getN() == 1){
            }
        }

    }

    /**
     * 关闭众筹 返还用户柠檬
     */
    private void close(Long fid){
        def fund = fundings_logs().findAndModify($$(fid:fid, 'process_status': FundingProcess.进行.ordinal()),
                null, null, false,$$($set:['process_status': FundingProcess.关闭.ordinal()]), true, false)
        if(fund != null){//返还用户柠檬
            List<Integer> user_ids = fund.get('users') as List
            Integer coins = fund['price'] as Integer
            user_ids.each {Integer userId ->
                users().update(new BasicDBObject(_id, userId),
                        $$($inc, ['finance.coin_count': coins, "finance.coin_spend_total": -coins]))
            }

        }

    }

    def del(HttpServletRequest req){
        Long _id = req.getParameter("_id") as Long

        if(_id == null)
            return Web.missParam()
        //只能删除下线的众筹
        if(fundings().remove($$(_id:_id, status:false),writeConcern).getN() == 1){
            Crud.opLog(OpType.funding_del, [_id:_id])
            return [code : 1]
        }
        [code : 0]
    }

    /**
     * 众筹轮次
     * @param req
     * @return
     */
    def round_logs(HttpServletRequest req){
        Long _id = req.getParameter("_id") as Long
        def fund = fundings().findOne(_id)
        if(fund == null){
            return Web.missParam()
        }
        def query = Web.fillTimeBetween(req)
        query.and('fid').is(_id)
        Crud.list(req, fundings_logs(), query.get(), $$(users:0), SJ_DESC){List<BasicDBObject> list->
            def users = users()
            for(BasicDBObject fundlog : list){
                fundlog.put('award_user', users.findOne(fundlog['award_id'] as Integer,$$(nick_name:1)))
            }
        }
    }

    /**
     * 购买记录
     * @param req
     * @return
     */
    def purchase_logs(HttpServletRequest req){
        Long _id = req.getParameter("_id") as Long
        def fund = fundings().findOne(_id)
        if(fund == null){
            return Web.missParam()
        }
        def query = Web.fillTimeBetween(req)
        query.and('fid').is(_id)
        Crud.list(req, fundings_purchase_logs(), query.get(), $$(fid:0,rid:0), SJ_DESC){List<BasicDBObject> list->
            def users = users()
            for(BasicDBObject fundlog : list){
                fundlog.put('user', users.findOne(fundlog['user_id'] as Integer,$$(nick_name:1)))
            }
        }
    }

    /**
     * 获奖记录
     * @param req
     * @return
     */
    def award_logs(HttpServletRequest req){
        Long _id = req.getParameter("_id") as Long
        def fund = fundings().findOne(_id)
        if(fund == null){
            return Web.missParam()
        }
        def query = Web.fillTimeBetween(req)
        query.and('fid').is(_id)
        Crud.list(req, fundings_award_logs(), query.get(), $$(fid:0,rid:0), SJ_DESC){List<BasicDBObject> list->
            def users = users()
            for(BasicDBObject fundlog : list){
                fundlog.put('user', users.findOne(fundlog['uid'] as Integer,$$(nick_name:1)))
            }
        }
    }
}
