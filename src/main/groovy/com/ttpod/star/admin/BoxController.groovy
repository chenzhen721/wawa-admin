package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.crud.MessageController
import com.ttpod.star.model.BoxStatusType
import com.ttpod.star.model.MsgType
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

/**
 * Created on 2014/8/27.
 */
@RestWithSession
class BoxController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(BoxController.class)

    @Resource
    MessageController messageController

    def list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        intQuery(query, req, "box_id")//房间号
        intQuery(query, req, "user_id")//房主id
        stringQuery(query, req, "name")//房间名称
        if (req[_id]) {
            query.and("_id").is(req.getInt("_id"))
        }
        Crud.list(req, boxes(), query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> data ->
            def users = users()
            for (BasicDBObject obj : data) {
                obj.put("nick_name", users.findOne(obj['user_id'], new BasicDBObject("nick_name", 1))?.get("nick_name"))
            }
        }
    }

    def edit(HttpServletRequest req) {
        def update = new HashMap()
        Integer boxId = req.getInt(_id)
        String v = req.getParameter("pic")
        if (StringUtils.isNotBlank(v))
            update.put('pic', v)

        String name = req.getParameter("name")
        if (StringUtils.isNotBlank(name) && name.length() < 21 && !name.contains(" ")) {
            name = HtmlUtils.htmlEscape(name)
            update.put("name", name)
        }
        if (update.size() > 0) {
            if (1 == boxes().update(new BasicDBObject(_id, boxId), new BasicDBObject($set, update), false, false, writeConcern).getN())
                return [code: 1]
        }
        return [code: 0]
    }

    def handle(HttpServletRequest req) {
        def status = req.getInt('status') as Integer
        def _id = req[_id] as Integer
        if (_id != null && status != null) {
            BasicDBObject queryObj = new BasicDBObject('_id', _id)
            if (status == BoxStatusType.开启.ordinal()) {
                queryObj.put("status", BoxStatusType.封禁.ordinal())
            }
            if (status == BoxStatusType.封禁.ordinal()) {
                queryObj.put("status", BoxStatusType.开启.ordinal())
            }
            if (status == BoxStatusType.封禁.ordinal() || status == BoxStatusType.开启.ordinal()) {
                Long time = System.currentTimeMillis()
                def record = boxes().findAndModify(queryObj,
                        new BasicDBObject('$set': [status: status, lastmodif: time]))
                if (record) {
                    def user_id = record.get("user_id") as Integer
                    def box_id = record.get("box_id") as Integer
                    if (status == BoxStatusType.开启.ordinal()) {
                        //发送消息
                        messageController.sendSingleMsg(user_id, '包厢已开启', '您的包厢已开启，赶快与朋友们一起嗨起来吧!', MsgType.系统消息);

                    } else if (status == BoxStatusType.封禁.ordinal()) {
                        //发送消息
                        messageController.sendSingleMsg(user_id, '包厢已封禁', '您的包厢已封禁，有问题请联系客服', MsgType.系统消息);
                    }
                    Crud.opLog(OpType.ban_box_handle, [user_id: user_id, box_id: box_id, status: status])
                }
            }
        }
        OK()
    }

    /**
     * 收益流水
     * @param req
     * @return
     */
    def costlist(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        stringQuery(query, req, "type")
        intQuery(query, req, 'box')
        def f_id = req.getParameter("f_id") as String
        if (f_id != null) {
            query.put("session._id").is(f_id)
        }
        Crud.list(req, logMongo.getCollection("box_cost"), query.get(), ALL_FIELD, SJ_DESC)
    }

    /**
     * 收益统计
     * @param req
     * @return
     */
    def costcount(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        intQuery(query, req, "box")
        query.and("type").is("send_gift")
        def boxCost = logMongo.getCollection("box_cost")
        def cost_total = boxCost.aggregate(
                new BasicDBObject('$match', query.get()),
                new BasicDBObject('$group', [_id: '$box', total: [$sum: '$session.data.box_earned']])
        ).results().toList()
        return [code: 1, data: cost_total]
    }

    /**
     * 转移维C流水
     * @param req
     * @return
     */
    def transferlist(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        ['f_id', 't_id', 'boxId'].collect { intQuery(query, req, it as String) }
        def transfer = logMongo.getCollection("bean_transfer")
        Crud.list(req, transfer, query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> list ->
            for (BasicDBObject obj : list) {
                def f_id = obj.get("f_id") as Number
                def t_id = obj.get("t_id") as Number
                def boxId = obj.get("boxId") as Number
                def cursor = users().find(new BasicDBObject('_id', ['$in': [f_id, t_id]]), new BasicDBObject('nick_name', 1))
                while (cursor.hasNext()) {
                    def user = cursor.next()
                    if (f_id.equals(user.get('_id') as Number)) {
                        obj.put('f_nick_name', user.get('nick_name'))
                    } else if (t_id.equals(user.get('_id') as Number)) {
                        obj.put('t_nick_name', user.get('nick_name'))
                    }
                }
                def box = boxes().findOne(new BasicDBObject('_id', boxId), new BasicDBObject('name', 1))
                obj.put('box_nick_name', box?.get('name'))
            }
        }
    }

}
