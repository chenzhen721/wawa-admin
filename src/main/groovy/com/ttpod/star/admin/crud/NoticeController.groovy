package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.persistent.KGS
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.admin.Web

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.groovy.CrudClosures.*

/**
 * date: 13-3-28 下午2:31
 * @author: yangyang.cong@ttpod.com
 */
@RestWithSession
class NoticeController extends BaseController{


    @Resource
    KGS noticeKGS

    @Delegate Crud crud = new Crud(adminMongo.getCollection('notices'),true,
            [_id:{noticeKGS.nextId()},title:Str,content:Str,click_url:Str,order:Int,
                    type:Int,status:Ne0,timestamp:Timestamp,
                    stime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()},
                    etime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()}
            ],
            new Crud.QueryCondition(){
                public DBObject query(HttpServletRequest req) {
                    def q = QueryBuilder.start()
                    intQuery(q,req,'type')
                    q.get()
                }
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject(status:-1,order:-1,timestamp:-1);
                }
            }
    )

    /*def add(HttpServletRequest req) {
        Integer id = req.getParameter("_id") as Integer

        if (id == null || adminMongo.getCollection('gifts').count($$("_id", id)) > 0) {
            return ['code': 30442]
        } else {
            this.cleanCache()
            return crud.add(req)
        }
    }

    private Integer generateId(){
        Integer id = noticeKGS.nextId()
        if (table().count($$("_id", id)) > 0) {
            return generateId()
        }
        return id
    }*/
}
