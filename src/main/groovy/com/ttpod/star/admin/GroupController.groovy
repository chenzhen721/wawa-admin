package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.model.ApplyType
import com.ttpod.star.model.FamilyType
import com.ttpod.star.model.MsgType
import com.ttpod.star.model.OpType
import com.ttpod.star.admin.crud.MessageController

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.doc.MongoKey._id
import com.ttpod.rest.web.StaticSpring

import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.doc.MongoKey._id

/**
 *  家族相关内容
 * date: 13-10-23 下午2:31
 * @author: haigen.xiong@ttpod.com
 */
@RestWithSession
class GroupController extends BaseController {

    @Resource
    MessageController messageController

    DBCollection table() {adminMongo.getCollection('family_applys')}
    DBCollection familys_table() {familyMongo.getCollection('familys')}

    static final Long REFUND_COIN = 30000   // 申请创建家族 退 30000柠檬

    // 创建家族申请查询
    def list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        ['status', 'xy_user_id'].each {String field ->
            intQuery(query, req, field)
        }
        Crud.list(req, table(), query.get(), ALL_FIELD, SJ_DESC)
    }

    def handle(HttpServletRequest req) {

        def status = req.getInt('status')

        if (status == ApplyType.通过.ordinal() || status == ApplyType.未通过.ordinal()) {

            def users = users()
            Long time = System.currentTimeMillis()
            def family_apply = table().findAndModify(new BasicDBObject(_id: req[_id], status: ApplyType.未处理.ordinal()),
                    new BasicDBObject('$set': [status: status, lastmodif: time]))
            def user_id = family_apply.get('xy_user_id') as Integer
            if (family_apply) {
                if (status == ApplyType.通过.ordinal()) {
                    if (1 == users.update(new BasicDBObject(_id, user_id),
                            new BasicDBObject($set: [family: [family_id: user_id, family_priv: FamilyType.族长.ordinal(), timestamp: time]]), false, false, writeConcern).getN()) {
                        def tmp = System.currentTimeMillis()
                        familys_table().save(new BasicDBObject(
                                _id: user_id, timestamp: tmp, lastmodif: tmp,
                                status: ApplyType.通过.ordinal(),
                                family_name: family_apply.get('family_name'),
                                badge_name: family_apply.get('badge_name'),
                                family_pic: family_apply.get('family_pic'),
                                family_notice: family_apply.get('family_notice'),
                                member_count: 1,
                                rank_cost: 0,
                                rank_support: 0,
                                rank_num: 0,
                                leader_id: user_id
                        ), writeConcern)
                    }
                }
                else if (status == ApplyType.未通过.ordinal()) {
                    def logWithId = new BasicDBObject(
                            _id: "${user_id}_${REFUND_COIN}_Admin_${System.currentTimeMillis()}".toString(),
                            user_id: user_id,
                            coin: REFUND_COIN,
                            via: 'Admin',
                            remark: '家族审核未通过,退:' + REFUND_COIN + '柠檬给用户:' + user_id,
                            session: Web.getSession()

                    )
                    if (refundCoin(user_id, REFUND_COIN, logWithId)) {
                        Crud.opLog(OpType.finance_refund, [family_id: user_id, user_id: user_id, coin: REFUND_COIN])
                    }
                    //发送消息

                }

            }
            Crud.opLog(OpType.family_handle, [family_id: user_id, user_id: user_id, status: status])

        }
        OK()
    }

    def family_list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        ['status', '_id'].each {String field ->
            intQuery(query, req, field)
        }
        Crud.list(req,familys_table(),query.get(), ALL_FIELD, SJ_DESC)
    }

    def close(HttpServletRequest req)
    {
        def status = ApplyType.关闭.ordinal()
        def family_id = req.getInt(_id)

        def family = familyMongo.getCollection('familys').findOne($$(_id, family_id).append("status", ApplyType.通过.ordinal()));
        if (null == family)
            return Web.notAllowed()
       def  user_id = family?.get("leader_id") as Integer
        Long time = System.currentTimeMillis()
        def update = [status: status, lastmodif: time]
        if (1 == adminMongo.getCollection('family_applys').update($$(xy_user_id: user_id, status: ApplyType.通过.ordinal()),
                $$('$set': update)).getN())
            familys_table().remove($$(_id, family_id))

        def logWithId = new BasicDBObject(
                _id: "${user_id}_${REFUND_COIN}_Admin_${System.currentTimeMillis()}".toString(),
                user_id: user_id,
                coin: REFUND_COIN,
                via: 'Admin',
                remark: '家族审核未通过,退:' + REFUND_COIN + '柠檬给用户:' + user_id,
                session: Web.getSession()

        )
        if (refundCoin(user_id, REFUND_COIN, logWithId)) {
            Crud.opLog(OpType.family_handle, [family_id: family_id, user_id: user_id, status: status])
        }

        StaticSpring.execute(new Runnable(){
            void run()
            {
              //删除家族
              familyMongo.getCollection('familys').remove($$(_id, family_id))
              //删除家族直播间
              rooms().remove($$(family_id:family_id))
              //清除解散家族中的所有成员
                def lst = users().find($$('family.family_id', family_id), new BasicDBObject("family": 1)).toArray()
                for (DBObject obj : lst)
                {
                    if (1 == users().update($$(_id, obj.get(_id)), $$($unset, $$("family", 1))).getN())
                    {

                        familyMongo.getCollection('member_applys').findAndModify($$(family_id: family_id, xy_user_id:  obj.get(_id), status: ApplyType.通过.ordinal()),
                                $$('$set': [status: ApplyType.关闭.ordinal(), lastmodif:time]))
                        //给成员发送消息
                        messageController.sendSingleMsg(obj.get(_id) as Integer, '家族解散', '你所在的家族'+family.get('family_name')+'因为家族徽章/家族名称相关词汇违规而被解散。如有任何疑问欢迎联系客服QQ', MsgType.系统消息);
                    }
                }
                familyMongo.getCollection("members").remove($$('fid',family_id),writeConcern)
            }
        })

        //发送消息
        messageController.sendSingleMsg(family.get("leader_id") as Integer, '家族关闭', '你所创建的家族'+family.get('family_name')+'因为家族徽章/家族名称相关词汇违规而被停用,创建费用已退回你的账户。如有任何疑问欢迎联系客服QQ', MsgType.系统消息);

        Crud.opLog(OpType.family_handle, [family_id: family_id, user_id: family.get("leader_id"), status: status])
        OK()
    }

    def show(HttpServletRequest req) {
        table().findOne(req[_id])
    }

}
