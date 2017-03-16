package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.groovy.CrudClosures.*

/**
 * 2016/10/12
 * 用户特权列表
 */
//@Rest
@RestWithSession
class ProductController extends BaseController{


    @Delegate Crud crud = new Crud(shopMongo.getCollection('products'),Boolean.TRUE,
            [_id:Int,name:Str,in_stock:Str, desc:Str, price:Int,status:Bool,order:Int,delivery_type:Int,
            timestamp:Timestamp,last_modify:Timestamp,img_url:Str],
            new Crud.QueryCondition(){
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject('order',-1);
                }
            }
    )

}
