package com.ttpod.star.admin
import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.model.OpType

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.*
import com.ttpod.star.model.ApplyType

/**
 * date: 13-3-28 下午2:31
 * @author: yangyang.cong@ttpod.com
 */
//@Rest
@RestWithSession
class WithdrawController extends BaseController{
    DBCollection table(){adminMongo.getCollection('withdrawl_log')}

    static final Integer WITH_DRAW_BROKER_TYPE = 2  //代理提现

    def list(HttpServletRequest req){
        def query = Web.fillTimeBetween(req)
        ['status','user_id'].each {String field->
            intQuery(query,req,field)
        }
        def exchange_total = 0

        def q = query.and("type").notIn([WITH_DRAW_BROKER_TYPE]).get()
        def map = Crud.list(req,table(),q,ALL_FIELD,SJ_DESC){List<BasicDBObject> data ->
            def applys = adminMongo.getCollection('applys')
            def users = users()
            for(BasicDBObject obj: data){
                def applylist = applys.find(new BasicDBObject('xy_user_id': obj.get('user_id') as Integer, status: ApplyType.通过.ordinal()), new BasicDBObject(
                        tel: 1, real_name: 1, bank_id: 1, bank: 1, bank_location: 1, bank_name: 1, bank_user_name: 1, sfz: 1
                )).sort(new BasicDBObject(lastmodif: -1))
                if (applylist?.hasNext()) {
                    def apply = applylist.next()
                    apply.removeField(_id)
                    obj.putAll(apply)
                }
                def user = users.findOne(new BasicDBObject(_id,obj.get('user_id') as Integer),new BasicDBObject(['star.broker':1,'nick_name':1]))
                obj.put('nick_name',user?.get('nick_name'))
                Map star  = user?.get('star') as Map
                obj.put('broker',star?.get('broker'))
//                //调整后的金额
//                obj.put("real_exchange", obj.get("real_exchange")?:obj.get("exchange"))
            }
            exchange_total = table().aggregate(
                    new BasicDBObject('$match',q),
                    new BasicDBObject('$project', [exchange:'$exchange']),
                    new BasicDBObject('$group', [_id:null, exchange: [$sum: '$exchange']])
            ).results().first().get('exchange')
        }
        map.put('exchange_total',exchange_total)
        return map
    }


    def edit(HttpServletRequest req){
        Integer status = req["status"] as Integer
        Integer real_exchange = req["real_exchange"] as Integer
        String remark = req['remark']
        //更新状态
        if(status) {
            def record = table().findAndModify(new BasicDBObject(_id,req[_id]),
                    new BasicDBObject('$set':[status:status,modif:System.currentTimeMillis()]))
            if (record){
                Crud.opLog(OpType.withdraw_edit,[status: status,withdrawl_log_id:req[_id]])
                if (status == 2 ){ //拒绝
                    users().update(new BasicDBObject(_id,record.get('user_id')),
                            new BasicDBObject('$inc',['finance.bean_count' : 100 * ((Number) record.get('exchange')).intValue()])
                    )
                }
            }
        } else if(real_exchange) {//调整
            def record = table().findAndModify(new BasicDBObject(_id,req[_id]),
                    new BasicDBObject('$set':[real_exchange: real_exchange, remark: remark,modif:System.currentTimeMillis()]))
            if (record){
                Crud.opLog(OpType.withdraw_edit,[real_exchange: real_exchange, remark: remark, withdrawl_log_id:req[_id]])
            }
        }

        OK()
    }

    def list_broker(HttpServletRequest req){
        def query = Web.fillTimeBetween(req)
        ['status','user_id'].each {String field->
            intQuery(query,req,field)
        }
        def exchange_total = 0
        def q = query.and("type").is(WITH_DRAW_BROKER_TYPE).get()
        def map = Crud.list(req,table(),q,ALL_FIELD,SJ_DESC){List<BasicDBObject> data ->

            def users = users()
            for(BasicDBObject obj: data)
            {
                obj.put('nick_name',users.findOne(new BasicDBObject(_id,obj.get('user_id')),new BasicDBObject('nick_name',1))
                        ?.get('nick_name'))
            }

            exchange_total = table().aggregate(
                    new BasicDBObject('$match',q),
                    new BasicDBObject('$project', [exchange:'$exchange']),
                    new BasicDBObject('$group', [_id:null, exchange: [$sum: '$exchange']])
            ).results().first().get('exchange')
        }
        map.put('exchange_total',exchange_total)
        return map
    }

}
