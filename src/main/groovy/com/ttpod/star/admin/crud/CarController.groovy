package com.ttpod.star.admin.crud
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.persistent.KGS
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.groovy.CrudClosures.*
import com.ttpod.star.common.util.KeyUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * date: 13-3-28 下午2:31
 * @author: yangyang.cong@ttpod.com
 */
//@Rest
@RestWithSession
class CarController extends BaseController{

//    @Resource
//    KGS    giftKGS

    // 车 id 自己指定，大于0 ， 越贵越大
    /**
     * 是否续费
     * 是否自命名
     * 是否等级解锁
     * 解锁等级 最小0，最大29
     * 赠送天数
     * 加成比例
     */
    static final  Logger logger = LoggerFactory.getLogger(CarController.class)
    @Delegate Crud crud = new Crud(adminMongo.getCollection('cars'),true,
            [_id:IntNotNull,name:Str,pic_url:Str,app_swf_url: Str, swf_url:Str,pic_pre_url:Str,enter_info:Str,
                    cat:Str,order:Int,coin_price:Int,status:Ne0,type:Int, category_id: Int, desc:Str, effect:Bool,
                    is_renew:Bool,is_custom:Bool, is_unlock:Bool, unlock_level:Int, unlock_level_desc:Str,award_days:Int,
                    exp_ratio : { String str -> (str == null || str.isEmpty()) ? null : str as Double }],
            new Crud.QueryCondition(){
                public DBObject query(HttpServletRequest req) {
                    def q = QueryBuilder.start()
                    stringQuery(q,req,'cat')
                    intQuery(q,req,'type')
                    q.get()
                }
                public DBObject sortby(HttpServletRequest req) {new BasicDBObject("order",-1)}
            }
    )

    def add(HttpServletRequest req)
    {
        def id = req[_id] as Integer
        if(adminMongo.getCollection('cars').count($$(_id, id)) > 0){
            return [code: 30442]
        }
        def result = crud.add(req)
        this.cleanCache()
        return result
    }

    def edit(HttpServletRequest req)
    {
        def result = crud.edit(req)
        this.cleanCache()
        return result
    }

    public void cleanCache(){
        String cars_key = "all:ttxiuchang:cars"
        mainRedis.delete(cars_key)
    }

}
