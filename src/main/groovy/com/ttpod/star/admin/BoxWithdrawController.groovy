package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.model.OpType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * Created on 2014/8/27.
 */
@RestWithSession
class BoxWithdrawController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(BoxWithdrawController.class)

    DBCollection table() { adminMongo.getCollection('box_withdrawl_log') }


    def list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        def exchange_total = 0
        intQuery(query, req, "status")
        intQuery(query, req, "user_id")//房主id
        Map<String, Object> userMap = new HashMap<String, Object>()
        def map = Crud.list(req, table(), query.get(), ALL_FIELD, SJ_DESC) { List<BasicDBObject> data ->
            def boxes = boxes()
            def users = users()
            for (BasicDBObject obj : data) {
                def userId = obj.get("user_id") as Integer
                def box = userMap.get(String.valueOf(userId))
                if (box == null) {
                    box = boxes.findOne($$("user_id", userId), $$(['withdraw': 1]))
                    if (box != null) {
                        box.removeField("_id")
                        userMap.put(String.valueOf(userId), box)
                    } else {
                        userMap.put(String.valueOf(userId), -1)
                    }
                }
                if (box != null && (-1 != box)) {
                    obj.putAll((Map)box)
                }
                def user = userMap.get(userId + '_info')
                if (user == null) {
                    user = users.findOne($$(_id, userId), $$(['nick_name': 1, 'user_name': 1]))
                    if (user != null) {
                        userMap.put(userId + '_info', user)
                    } else {
                        userMap.put(userId + '_info', -1)
                    }
                }
                if (user != null && (-1 != user)) {
                    user = user as Map
                    obj.put('nick_name', user?.get('nick_name'))
                    obj.put('user_name', user?.get('user_name'))
                }
            }
            exchange_total = table().aggregate(
                    new BasicDBObject('$match',query.get()),
                    new BasicDBObject('$project', [exchange:'$exchange']),
                    new BasicDBObject('$group', [_id:null, exchange: [$sum: '$exchange']])
            ).results().first().get('exchange')
        }
        map.put('exchange_total',exchange_total)
        return map
    }

    def edit(HttpServletRequest req){
        Integer  status = req.getInt("status")
        def record = table().findAndModify(new BasicDBObject(_id,req[_id]),
                new BasicDBObject('$set':[status:status,modif:System.currentTimeMillis()]))
        if (record){
            Crud.opLog(OpType.box_withdraw_edit,[status: status,box_withdrawl_log_id:req[_id]])
            if ( status == 2 ){ //拒绝
                users().update(new BasicDBObject(_id,record.get('user_id')),
                        new BasicDBObject('$inc',['finance.bean_count' : 100 * ((Number) record.get('exchange')).intValue()])
                )
            }
        }
        OK()
    }


}
