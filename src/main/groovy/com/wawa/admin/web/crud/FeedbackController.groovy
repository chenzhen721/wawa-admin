package com.wawa.admin.web.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.wawa.base.anno.RestWithSession
import com.wawa.base.Crud
import com.wawa.base.BaseController

import javax.servlet.http.HttpServletRequest

import static com.wawa.groovy.CrudClosures.*
import static com.wawa.common.doc.MongoKey.*
import static com.wawa.common.util.WebUtils.$$

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
