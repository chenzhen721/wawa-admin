package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.groovy.CrudClosures.*

@RestWithSession
class GameController extends BaseController{

    @Delegate Crud crud = new Crud(adminMongo.getCollection('games'),
            [_id:Str,name:Str,pic_url:Str,status:Bool,timestamp:Timestamp],
            new Crud.QueryCondition(){
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject("timestamp",-1);
                }
            }
    )

}
