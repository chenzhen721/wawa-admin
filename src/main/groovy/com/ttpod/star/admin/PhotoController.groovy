package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.MongoKey
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.crud.MessageController
import com.ttpod.star.model.MsgType
import com.ttpod.star.model.OpType
import com.ttpod.star.model.PhotoStatusType
import org.apache.commons.lang.StringUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.$set
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * Created by Administrator on 2014/10/20.
 */
@RestWithSession
class PhotoController extends BaseController {

    public DBCollection table() { return mainMongo.getCollection("photos"); }
    public DBCollection audit_pic() { return adminMongo.getCollection("audit_pic"); }

    @Resource
    MessageController messageController

    def list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        def userId = req['user_id'] as Integer
        def status = req['status'] as Integer
        def type = req['type'] as Integer
        def path = req['path'] as String
        if (userId != null) {
            query.put('user_id').is(userId)
        }
        if (status != null) {
            query.put('status').is(status)
        }
        if (type != null) {
            query.put('type').is(type)
        }
        if (StringUtils.isNotBlank(path)) {
            query.put(_id).is(path)
        }
        Crud.list(req, table(), query.get(), MongoKey.ALL_FIELD,
                $$(['status': 1, 'timestamp': -1]))
    }

    def edit(HttpServletRequest req) {
        def status = req['status'] as String
        def path = req['path'] as String
        if (StringUtils.isBlank(status) || StringUtils.isBlank(path)) {
            return [code: 0]
        }
        status = Integer.valueOf(status)
        QueryBuilder query = QueryBuilder.start().and(_id).is(path);
        if (PhotoStatusType.通过.ordinal().equals(status)) {
            query.and('status').notEquals(PhotoStatusType.通过.ordinal())
        } else {
            query.and('status').is(PhotoStatusType.通过.ordinal())
        }
        def obj = table().findAndModify(query.get(), $$('$set', [status: status, modify: System.currentTimeMillis()]))
        if (obj != null) {
            def userId = obj['user_id'] as Integer
            obj.put("modify_time", System.currentTimeMillis())
            obj.put('modify_status', status)

            if (status == PhotoStatusType.通过.ordinal()) {
                //发送消息
                messageController.sendSingleMsg(userId, '照片审核通过', "您有照片未审核通过", MsgType.系统消息);

            } else if (status == PhotoStatusType.未通过.ordinal()) {
                //发送消息
                obj.put("del_time",System.currentTimeMillis())
                obj.put("upai_bucket",UpaiController.STAR_PHOTO_BUCKET)
                obj.put("s",UpaiController.USER_DELETE)
                mainMongo.getCollection("ban_photos").save(obj)
                messageController.sendSingleMsg(userId, '照片审核未通过', "您有照片未审核通过", MsgType.系统消息);
            }
            mainRedis.delete("room:" + userId + ":photo:count:string")
            Crud.opLog(OpType.photo_ban,[starId:userId, path:path, status: status])
        }
        return [code: (null == obj ? 0 : 1)]
    }

    /**
     * 用户头像审核
     */
    def user_pic_list(HttpServletRequest req){
        def query = Web.fillTimeBetween(req)
        def userId = req['user_id'] as Integer
        if (userId != null) {
            query.put('user_id').is(userId)
        }
        Crud.list(req, audit_pic(), query.get(), MongoKey.ALL_FIELD, $$(['status': 1, 'timestamp': -1]))
    }


    private final static String DEFAULT_PIC = "https://aiimg.sumeme.com/22/6/1403510731734.jpg";

    def handle_user_pic(HttpServletRequest req) {
        Integer status = req['status'] as Integer
        def _id = req['_id'] as String
        if (status == null || StringUtils.isBlank(_id)) {
            return Web.missParam();
        }
        QueryBuilder query = QueryBuilder.start().and('_id').is(_id);
        if (PhotoStatusType.通过.ordinal().equals(status)) {
            query.and('status').notEquals(PhotoStatusType.通过.ordinal())
        } else {
            query.and('status').is(PhotoStatusType.未处理.ordinal())
        }
        def obj = audit_pic().findAndModify(query.get(), $$('$set', [status: status, modify: System.currentTimeMillis()]))
        if (obj != null) {
            def userId = obj['user_id'] as Integer
            def pic_url = obj['pic_url'] as String
            obj.put("modify_time", System.currentTimeMillis())
            obj.put('modify_status', status)
            if (status == PhotoStatusType.未通过.ordinal()) {
                //重置用户头像
                if (1 == users().update($$(_id : userId, pic : pic_url), $$($set, $$('pic', DEFAULT_PIC)), false, false, writeConcern).getN()){
                    //发送消息
                    messageController.sendSingleMsg(userId, '头像审核未通过', "您的头像照片未审核通过", MsgType.系统消息);
                }

            }
            Crud.opLog(OpType.user_pic_ban, obj)
        }
        return [code: (null == obj ? 0 : 1)]
    }

}
