package com.wawa.admin.web.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.wawa.base.anno.RestWithSession
import com.wawa.base.Crud
import com.wawa.base.BaseController
import com.wawa.model.AccuseStatus
import com.wawa.model.MsgType
import com.wawa.model.OpType

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.wawa.groovy.CrudClosures.*
import static com.wawa.common.doc.MongoKey.*
import static com.wawa.common.util.WebUtils.$$

/**
 * date: 14-5-16 下午5:20
 */
//@Rest
@RestWithSession
class AccuseController extends BaseController{

    @Resource
    MessageController messageController

    @Delegate Crud crud = new Crud(adminMongo.getCollection('accuse'),false,
            [_id:Str,status:Int],
            new Crud.QueryCondition(){
                public DBObject query(HttpServletRequest req) {
                    if(req.getParameter('status'))
                        return new BasicDBObject('status',req.getParameter('status') as Integer)
                }
                public DBObject sortby(HttpServletRequest req) {ID_DESC}
            }
    )

    private static final Integer ACCUSE_COST = 0

    private static final String[] TYPES = ["教唆传销","低俗色情","政治违规","暴力犯罪","外站拉人"]

    public Map edit(HttpServletRequest req) {
        Long _id =  req.getParameter('_id') as Long
        Integer status =  req.getParameter('status') as Integer
        def accuse = adminMongo.getCollection('accuse').findAndModify($$(_id:_id, status: AccuseStatus.未处理.ordinal()),
                $$($set,$$('status':status)));
        if(accuse != null){
            Integer uid =  accuse.get('uid') as Integer
            Integer roomId =  accuse.get('roomId') as Integer
            Integer type =  accuse.get('type') as Integer
            def nick_name = users().findOne(roomId,$$(nick_name:1))?.get('nick_name')
            Integer coin = 0;
            String msg = "尊敬的用户,你于(${new Date().format("yyyy-MM-dd HH:mm:ss")})在(${nick_name})的直播间举报了(${TYPES[type]}),"
            //奖励用户200星币
            if(status == AccuseStatus.通过.ordinal()){
                //coin = 200
                //msg += "经核实举报信息真实有效且为首位举报的用户，获得200柠檬的奖励，请再接再励。"
                msg += "经核实举报信息真实有效，我们已对主播做出相应惩处，么么直播感谢你的支持。"
            }
            //返还用户星币
            else if(status == AccuseStatus.重复.ordinal()){
                //coin = 100
                //msg += "经核实验证举报信息真实有效但非首位举报的用户，退还100柠檬举报费用，请再接再厉。"
                msg += "经核实验证举报信息真实有效但非首位举报的用户，请再接再厉。"
            }else if(status == AccuseStatus.未通过.ordinal()){
                msg += "经核实举报信息无法真实验证，请参考《么么直播举报系统规则》进行违规行为的举报。"
            }
            def curr = System.currentTimeMillis()
            if(users().update($$(_id:uid), $$($inc:$$('finance.coin_count':ACCUSE_COST))).getN() == 1){
                logMongo.getCollection("accuse_logs").insert($$(_id: uid + "_" + curr,
                        aid:_id,
                        uid:uid,
                        type:'award',
                        coin:coin,
                        timestamp:curr
                ))

                Crud.opLog(OpType.accuse_audit,[uid:uid,status:status,coin:coin])
                //发送消息
                messageController.sendSingleMsg(uid, '举报结果反馈', msg, MsgType.系统消息);
                return [ code : 1]
            }
        }

        return [ code : 0]


    }
}
