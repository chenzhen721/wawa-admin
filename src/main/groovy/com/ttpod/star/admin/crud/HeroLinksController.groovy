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

import static com.ttpod.rest.groovy.CrudClosures.*

/**
 *
 */
@RestWithSession
class HeroLinksController extends BaseController{

    @Resource
    KGS    seqKGS
    @Delegate Crud crud = new Crud(adminMongo.getCollection('hero_links'),true,
            [_id:{seqKGS.nextId()},title:Str,content:Str,img_url:Str,click_url:Str,order:{String str->  (str == null || str.isEmpty()) ? 1 : Integer.valueOf(str)  },
             status:Bool,timestamp:Timestamp,
             stime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()},
             etime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()}
            ],
             new Crud.QueryCondition(){
                public DBObject query(HttpServletRequest req) {
                    if (req['status']){
                        return QueryBuilder.start().and('status').is(req['status'] as Boolean).get()
                    }
                    return super.query(req)
                }
                public DBObject sortby(HttpServletRequest req) {
                 return new BasicDBObject([status: -1, "stime":-1, "etime": -1, order: 1])
                }
            }
    )

}
