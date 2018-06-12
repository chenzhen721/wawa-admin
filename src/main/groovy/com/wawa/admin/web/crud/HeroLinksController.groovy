package com.wawa.admin.web.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.wawa.base.anno.RestWithSession
import com.wawa.base.persistent.KGS
import com.wawa.base.Crud
import com.wawa.base.BaseController
import com.wawa.api.Web

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.wawa.groovy.CrudClosures.*

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
                    if (req.getParameter('status')){
                        return QueryBuilder.start().and('status').is(req.getParameter('status') as Boolean).get()
                    }
                    return super.query(req)
                }
                public DBObject sortby(HttpServletRequest req) {
                 return new BasicDBObject([status: -1, "stime":-1, "etime": -1, order: 1])
                }
            }
    )

}
