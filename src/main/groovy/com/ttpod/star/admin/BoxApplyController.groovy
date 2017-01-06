package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.common.doc.MongoKey
import com.ttpod.rest.common.doc.ParamKey
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.star.admin.crud.MessageController
import com.ttpod.rest.web.Crud
import com.ttpod.rest.web.StaticSpring
import com.ttpod.star.model.BoxApplyStatus
import com.ttpod.star.model.BoxStatusType
import com.ttpod.star.model.BoxWithdrawStatus
import com.ttpod.star.model.MsgType
import com.ttpod.star.model.OpType
import com.ttpod.star.model.UserType
import org.apache.commons.lang.StringUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.web.Crud.list

/**
 * 包厢
 */
//@Rest
@RestWithSession
class BoxApplyController extends BaseController{

    DBCollection table(){adminMongo.getCollection('box_applys')}
    @Resource
    MessageController messageController

    // 签约申请查询
    def list(HttpServletRequest req){
        def query = Web.fillTimeBetween(req)
        query = Web.fillTimeBetween(query, "timestamp", req)
        ['status','user_id','box_id'].each {String field->
            intQuery(query,req,field)
        }
        def desc = $$(status:1,timestamp:-1)
        list(req,table(),query.get(),ALL_FIELD,desc){List<BasicDBObject> data->
            def users = users()
            for(BasicDBObject obj: data){
                obj.put("nick_name", users.findOne(obj['user_id'],new BasicDBObject("nick_name",1))?.get("nick_name") )
            }
        }
    }

    private static final Long DAY_MILL = 24*3600*1000L

    def handle(HttpServletRequest req){

        def status = req.getInt('status')
        if (status == BoxApplyStatus.通过.ordinal() || status == BoxApplyStatus.未通过.ordinal()){
            Long time = System.currentTimeMillis()
            def record =  table().findAndModify(new BasicDBObject(_id:req[_id],status:BoxApplyStatus.未处理.ordinal()),
                    new BasicDBObject('$set':[status:status,lastmodif:time]))
            if (record){
                def box_id = record.get('box_id') as Integer
                def cost_coin = record.get('cost_coin') as Integer
                def user_id = record.get('user_id') as Integer
                if (status == BoxApplyStatus.通过.ordinal()){
                    //获得包厢天数 从审核通过开始倒计时
                    def days = boxes().findOne($$(_id: box_id), $$(days:1))?.get('days') as Integer
                    Long delta_mills = days * DAY_MILL
                    if(boxes().update($$(_id: box_id), $$($set:$$(status: BoxStatusType.开启.ordinal(),expires:delta_mills+System.currentTimeMillis())), false ,false ,writeConcern).getN() == 1){
                        //发送消息
                        messageController.sendSingleMsg(user_id, '申请包厢审核通过', '您申请的包厢审核通过，赶快与朋友们一起嗨起来吧!', MsgType.系统消息);
                    }

                }
                else if(status == BoxApplyStatus.未通过.ordinal()){
                    //删除房间
                    if(boxes().remove($$('_id':box_id, user_id:user_id), writeConcern).getN() == 1){
                        //退款
                        if(users().update($$(_id:user_id), $$($inc:$$('finance.coin_count':cost_coin), $unset:$$('box':1))).getN() == 1){
                            //发送消息
                            messageController.sendSingleMsg(user_id, '申请包厢审核不通过', '您申请的包厢审核不通过，有问题请联系客服', MsgType.系统消息);
                        }
                    }

                }
                Crud.opLog(OpType.apply_box_handle,[user_id:box_id,status:status])
            }
        }
        OK()
    }

    /**
     * 审核房主修改的银行卡信息
     * @param req
     * @return
     */
    def withdarw_list(HttpServletRequest req){
        def query = Web.fillTimeBetween(req)
        query = Web.fillTimeBetween(query, "timestamp", req)
        ['status','user_id'].each {String field->
            intQuery(query,req,field)
        }
        String box_id = req.getParameter('box_id');
        if (StringUtils.isNotBlank(box_id)){
            query.and('_id').is(Integer.valueOf(box_id));
        }
        Crud.list(req,boxes(),query.get(),$$('pwd',0),$$('withdraw.status':1,'withdraw.timestamp':-1)){List<BasicDBObject> data->
            def users = users()
            for(BasicDBObject obj: data){
                obj.put("nick_name", users.findOne(obj['user_id'],$$("nick_name",1))?.get("nick_name") )
            }
        }
    }

    /**
     * 审核房主修改的银行卡信息
     * @param req
     * @return
     */
    def withdarw_handle(HttpServletRequest req){
        def status = req.getInt('status')
        if (status == BoxWithdrawStatus.通过.ordinal() || status == BoxWithdrawStatus.未通过.ordinal()){
            Long time = System.currentTimeMillis()
            def record =  boxes().findAndModify(new BasicDBObject(_id:req[_id] as Integer,'withdraw.status':BoxWithdrawStatus.未处理.ordinal()),
                    new BasicDBObject('$set':['withdraw.status':status]))
            if (record){
                def box_id = record.get('_id') as Integer
                def user_id = record.get('user_id') as Integer
                if (status == BoxWithdrawStatus.通过.ordinal()){
                    //发送消息
                    messageController.sendSingleMsg(user_id, '银行卡信息审核通过', '您修改的银行卡信息审核通过!', MsgType.系统消息);
                }
                else if(status == BoxWithdrawStatus.未通过.ordinal()){
                    //发送消息
                    messageController.sendSingleMsg(user_id, '银行卡信息审核不通过', '您修改的银行卡信息审核不通过，有问题请联系客服', MsgType.系统消息);
                }
                Crud.opLog(OpType.apply_withdarw_box_handle,[user_id:box_id,status:status])
            }
        }
        OK()
    }

    def show(HttpServletRequest req){
        table().findOne(req[_id])
    }

}
