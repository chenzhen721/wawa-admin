package com.ttpod.star.admin.crud
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.groovy.CrudClosures.*
/**
 * date: 13-3-28 下午2:31
 * @author: yangyang.cong@ttpod.com
 */
@RestWithSession
class MissionController extends BaseController{

    @Delegate Crud crud = new Crud(adminMongo.getCollection('missions'),
            [_id:Str,title:Str,pic_url:Str,icon_url:Str,coin_count:Int,order:Int,status:Bool,level:Int,total:Int,remark:Str,
                    type:Int],
            new Crud.QueryCondition(){
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject("order",-1);
                }
            }
    )
}
