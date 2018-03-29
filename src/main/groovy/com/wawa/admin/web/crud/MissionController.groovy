package com.wawa.admin.web.crud
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.wawa.base.anno.RestWithSession
import com.wawa.base.Crud
import com.wawa.base.BaseController

import javax.servlet.http.HttpServletRequest

import static com.wawa.groovy.CrudClosures.*
/**
 * 任务管理
 */
@RestWithSession
class MissionController extends BaseController{

    @Delegate Crud crud = new Crud(adminMongo.getCollection('missions'),Boolean.FALSE,
            [_id:Str,title:Str,pic_url:Str,icon_url:Str,coin_count:Int,order:Int,status:Bool,level:Int,total:Int,remark:Str,
                    type:Int],
            new Crud.QueryCondition(){
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject("order",-1);
                }
            }
    )
}
