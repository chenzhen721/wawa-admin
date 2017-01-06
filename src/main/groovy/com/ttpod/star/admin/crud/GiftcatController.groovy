package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.groovy.CrudClosures.*

/**
 * date: 13-3-28 下午2:31
 * @author: yangyang.cong@ttpod.com
 */
@RestWithSession
class GiftcatController extends BaseController{

    @Resource
    GiftController giftController

    @Delegate Crud crud = new Crud(adminMongo.getCollection('gift_categories'),true,
            [_id:IntNotNull,name:Str,order:Int,lucky:Eq1,status:Ne0,vip:Eq1,ratio:{it as Double}],
            new Crud.QueryCondition(){
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject("order",-1);
                }
            }
    )

    def add(HttpServletRequest req) {
        def result = crud.add(req)
        giftController.cleanCache();
        return result
    }

    def edit(HttpServletRequest req) {
        def result = crud.edit(req);
        giftController.cleanCache();
        return result
    }

    def del(HttpServletRequest req) {
        def result = crud.del(req);
        giftController.cleanCache();
        return result
    }

}
