package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import sun.misc.REException

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.groovy.CrudClosures.*

/**
 * 座驾分类
 */
//@Rest
@RestWithSession
class CarCateController extends BaseController{

    @Resource
    CarController carController

    @Delegate Crud crud = new Crud(adminMongo.getCollection('car_categories'),true,
            [_id:IntNotNull,name:Str,order:Int,status:Ne0],
            new Crud.QueryCondition(){
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject("order",-1);
                }
            }
    )

    def add(HttpServletRequest req) {
        def result = crud.add(req)
        carController.cleanCache();
        return result
    }

    def edit(HttpServletRequest req) {
        def result = crud.edit(req);
        carController.cleanCache();
        return result
    }

    def del(HttpServletRequest req) {
        def result = crud.del(req);
        carController.cleanCache();
        return result
    }

}
