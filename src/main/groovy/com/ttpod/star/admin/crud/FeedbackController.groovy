package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.groovy.CrudClosures.*
import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * 用户反馈
 */
@RestWithSession
class FeedbackController extends BaseController{

    @Delegate Crud crud = new Crud(logMongo.getCollection('feedbacks'),false,
            [_id:Str,status:Int],
            new Crud.QueryCondition(){
                public DBObject query(HttpServletRequest req) {
                    if ('1' == req['status']){
                       return new BasicDBObject('status',1)
                    }
                    return new BasicDBObject('status',$$($ne,1))
                }
                public DBObject sortby(HttpServletRequest req) {ID_DESC}
            }
    )

    public Map add(HttpServletRequest req) {
        throw new UnsupportedOperationException()
    }
}
