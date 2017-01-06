package com.ttpod.star.admin.crud
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.persistent.KGS
import com.ttpod.rest.web.Crud
import com.ttpod.rest.web.StaticSpring
import com.ttpod.star.admin.BaseController
import com.ttpod.star.admin.Web
import com.ttpod.star.model.MadelType
import com.ttpod.star.model.Medal

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.groovy.CrudClosures.*
import static com.ttpod.rest.common.doc.MongoKey.*
import com.ttpod.star.common.util.KeyUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * @author: jiao.li@ttpod.com
 * Date: 14-2-17 上午10:23
 */
//@Rest
@RestWithSession
class MedalController extends BaseController{

    static final  Logger logger = LoggerFactory.getLogger(MedalController.class)

    private final static Long YEAR_MILLON = 10 * 365 * 24 * 3600 * 1000L
    /**
     * type:1用户 2主播
     * medal_type: 1:礼物 2:活动 3:系统 4:称号
     * status: 1 上线 0 下线
     * expiry_days:有效天数
     * sum_days :累计天数
     * is_mvip:是否为M特权徽章
     */
    @Delegate Crud crud = new Crud(adminMongo.getCollection('medals'),true,

            [_id:IntNotNull,medal_type:Int,type:Int,name:Str,grey_pic:Str,pic_url:Str,small_pic:Str,expiry_days:Int,
                    sum_days:Int,desc:Str, is_mvip:Bool,
                    gift_ids:{String str-> (str == null || str.isEmpty()) ? null : str.split(',').collect {Integer.valueOf(it.toString())}},
                    order:Int,coins:Int,status:Ne0,timestamp:Timestamp,
                    stime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()},
                    etime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()}
            ],
            new Crud.QueryCondition(){
                public DBObject query(HttpServletRequest req) {
                    def q = QueryBuilder.start()
                    stringQuery(q,req,'status')
                    intQuery(q,req,'type')
                    q.get()
                }
                public DBObject sortby(HttpServletRequest req) {new BasicDBObject("order",-1)}
            }
    )

    def add(HttpServletRequest req)
    {
        def id = req[_id] as Integer
        if(adminMongo.getCollection('medals').count($$(_id, id)) > 0){
            return [code: 30442]
        }
        this.cleanCache()
        Map result =  crud.add(req)
        if(result.get("code") == 1){
            setSysMedals(req);
        }
        return result
    }

    def edit(HttpServletRequest req)
    {
        //this.unsetMedal(req)
        this.cleanCache()
        Map data = crud.edit(req) as Map
        if(data.get("code") == 1){
            setSysMedals(req);
        }
        return data
    }


    private static final Long SYS_MEDAL_MILLS = 5 * 365 * 24 * 3600 * 1000L

    //设置系统徽章
    private void setSysMedals(HttpServletRequest req){
        final Integer medal_type = req['medal_type'] as Integer
        final Integer mid = req['_id'] as Integer
        if(MadelType.系统.ordinal() == medal_type){
            StaticSpring.execute(
                    new Runnable() {
                        public void run() {
                            def users = mainMongo.getCollection('users')
                            Long now = System.currentTimeMillis()
                            String entryKey = "medals." + mid
                            Long l = System.currentTimeMillis()
                            BasicDBObject query = $$(entryKey,[$not: [$gte:now]]);
                            Boolean flag = Boolean.FALSE
                            if(Medal.羽毛徽章.getId() == mid){
                                query.append('finance.feather_send_total', $$($gte:1000))
                                flag = Boolean.TRUE
                            }
                            /*else if(Medal.签到徽章.getId() == mid){
                                query.append('finance.sign_daily_total', $$($gte:100))
                                flag = Boolean.TRUE
                            }*/
                            if(flag){
                                users.update(query, $$($set,$$(entryKey,now+SYS_MEDAL_MILLS)), false, true)
                            }

                            println "setSysMedals cost time: ${System.currentTimeMillis() - l}"
                        }
                    }
            );
        }

    }

    private void unsetMedal(HttpServletRequest req){
        def id = req.getParameter('_id')
        def status = !'0'.equals(req.getParameter('status'))
        def medal = adminMongo.getCollection('medals').findOne($$(_id, id as Integer))
        if(medal == null) return
        Boolean old_status = medal.get('status') as Boolean
        if(old_status != status){
            def entryKey = 'medals.'+id
            if(status)
                users().update($$(entryKey,$$($gt,0)), $$($inc,$$(entryKey,YEAR_MILLON)), false , true)
            else
                users().update($$(entryKey,$$($gt,0)), $$($inc,$$(entryKey,-YEAR_MILLON)), false , true)
        }
    }

    private void cleanCache()
    {
        String medals_key = "all:ttxiuchang:medals"
        mainRedis.delete(medals_key)
        /*def ids = adminMongo.getCollection('medals').find($$('status', Boolean.TRUE),$$(_id,1)).toArray()*._id
        ids.each {
            mainRedis.opsForSet().add("all:medals:ids", it as String)
        }*/

    }
}