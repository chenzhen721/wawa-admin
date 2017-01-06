package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.MongoKey
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.crud.MessageController
import com.ttpod.star.model.MsgType
import com.ttpod.star.model.NestStatusType
import com.ttpod.star.model.OpType
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.util.HtmlUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.$set
import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * 小窝相关
 */
@RestWithSession
class NestController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(NestController.class)

    @Resource
    MessageController messageController

    def list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        intQuery(query, req, "_id")//小窝ID
        intQuery(query, req, "user_id")//用户id
        stringQuery(query, req, "name")//小窝名称
        Crud.list(req, nests(), query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> data ->
            def users = users()
            for (BasicDBObject obj : data) {
                obj.put("nick_name", users.findOne(obj['user_id'], new BasicDBObject("nick_name", 1))?.get("nick_name"))
            }
        }
    }

    def nest_photos(){return nestMongo.getCollection('photos')}

    /**
     * 照片列表
     * @param req
     * @return
     */
    def photo_list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        def userId = req['user_id'] as Integer
        def nest_id = req['_id'] as Integer
        def path = req['path'] as String
        if (userId != null) {
            query.put('user_id').is(userId)
        }
        if (nest_id != null) {
            query.put('nest_id').is(nest_id)
        }
        if (StringUtils.isNotBlank(path)) {
            query.put(_id).is(path)
        }
        Crud.list(req, nest_photos(), query.get(), MongoKey.ALL_FIELD,
                $$(['status': 1, 'timestamp': -1]))
    }

    /**
     * 照片审核
     * @param req
     * @return
     */
    def ban_photo(HttpServletRequest req) {
        def path = req['_id']
        //是否为小窝成员
        def obj = nest_photos().findAndRemove($$(_id :path))
        if(obj != null){
            obj.put("del_time",System.currentTimeMillis())
            obj.put("upai_bucket",UpaiController.NEST_PHOTO_BUCKET)
            obj.put("s",UpaiController.USER_DELETE)
            mainMongo.getCollection("ban_photos").save(obj)
            Crud.opLog(OpType.nest_photo_ban, obj)
        }
        return [code:  (null == obj  ? 0 : 1)]
    }

    def edit(HttpServletRequest req) {
        def update = new HashMap()
        Integer nestId = req.getInt(_id)
        String v = req.getParameter("pic")
        if (StringUtils.isNotBlank(v))
            update.put('pic', v)

        String name = req.getParameter("name")
        if (StringUtils.isNotBlank(name) && name.length() < 21 && !name.contains(" ")) {
            name = HtmlUtils.htmlEscape(name)
            update.put("name", name)
        }
        if (update.size() > 0) {
            if (1 == nests().update(new BasicDBObject(_id, nestId), new BasicDBObject($set, update), false, false, writeConcern).getN())
                return [code: 1]
        }
        return [code: 0]
    }

    def handle(HttpServletRequest req) {
        def status = req.getInt('status') as Integer
        def _id = req[_id] as Integer
        if (_id != null && status != null) {
            BasicDBObject queryObj = new BasicDBObject('_id', _id)
            if (status == NestStatusType.有效.ordinal()) {
                queryObj.put("status", NestStatusType.封禁.ordinal())
            }
            if (status == NestStatusType.封禁.ordinal()) {
                queryObj.put("status", NestStatusType.有效.ordinal())
            }
            if (status == NestStatusType.封禁.ordinal() || status == NestStatusType.有效.ordinal()) {
                Long time = System.currentTimeMillis()
                def record = nests().findAndModify(queryObj,
                        new BasicDBObject('$set': [status: status, lastmodif: time]))
                if (record) {
                    def user_id = record.get("user_id") as Integer
                    def nest_id = record.get("_id") as Integer
                    if (status == NestStatusType.封禁.ordinal()) {
                        //发送消息
                        messageController.sendSingleMsg(user_id, '小窝已封禁', '您的小窝已被封禁，有问题请联系客服', MsgType.系统消息);
                    }
                    Crud.opLog(OpType.nest_handle, [user_id: user_id, nest_id: nest_id, status: status])
                }
            }
        }
        OK()
    }

    def nest_log(HttpServletRequest req){
        def query = Web.fillTimeBetween(req).and('status').is(2)
        def nest_id = req.getParameter('nest_id') as String
        def user_id = req.getParameter('user_id') as String
        if(StringUtils.isNotBlank(nest_id)) query.and('nest_id').is(nest_id as Integer)
        if(StringUtils.isNotBlank(user_id)) query.and('user_id').is(user_id as Integer)
        Crud.list(req,nestMongo.getCollection('red_packets'),query.get(),ALL_FIELD,SJ_DESC)
    }

}
