package com.wawa.admin.web.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.wawa.base.anno.RestWithSession
import com.wawa.base.persistent.KGS
import com.wawa.base.Crud
import com.wawa.base.BaseController
import com.wawa.api.Web
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.wawa.groovy.CrudClosures.Int
import static com.wawa.groovy.CrudClosures.Str
import static com.wawa.groovy.CrudClosures.Timestamp

/**
 * 类目修改
 */
@RestWithSession
class CategoryController extends BaseController
{
    Logger logger = LoggerFactory.getLogger(CategoryController.class)

    DBCollection table(){return adminMongo.getCollection('category')}

    //status 0-上线 1-下线，onshow true显示在首页 false不显示在首页
    //type 0-房间 1-标签
    @Resource
    KGS seqKGS
    @Delegate Crud crud = new Crud(table(),true,
            [_id:{seqKGS.nextId()}, status:Int, name:Str, img:Str, type:Int, timestamp:Timestamp, lastModif:Timestamp,
             stime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()},
             etime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()}
            ],
            new Crud.QueryCondition(){
                public DBObject query(HttpServletRequest req) {
                    if (req['status']){
                        return QueryBuilder.start().and('status').is(req['status'] as Integer).get()
                    }
                    return super.query(req)
                }
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject([status: 1, "stime":-1, "etime": 1])
                }
            }
    )

}
