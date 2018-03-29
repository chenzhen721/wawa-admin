package com.wawa.admin.web.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.wawa.base.anno.RestWithSession
import com.wawa.base.persistent.KGS
import com.wawa.base.Crud
import com.wawa.base.BaseController
import com.wawa.api.Web
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.wawa.common.util.WebUtils.$$
import static com.wawa.groovy.CrudClosures.*

/**
 * date: 13-3-28 下午2:31
 * @author: yangyang.cong@ttpod.com
 */
@RestWithSession
class NoticeController extends BaseController{
    static final Logger logger = LoggerFactory.getLogger(MessageController.class)


    @Resource
    KGS noticeKGS

    @Delegate Crud crud = new Crud(adminMongo.getCollection('notices'),true,
            [_id:{noticeKGS.nextId()},title:Str,content:Str,click_url:Str,order:Int,
                    type:Int,status:Ne0,timestamp:Timestamp,
                    stime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()},
                    etime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()}
            ],
            new Crud.QueryCondition(){
                public DBObject query(HttpServletRequest req) {
                    def q = QueryBuilder.start()
                    intQuery(q,req,'type')
                    q.get()
                }
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject(status:-1,order:-1,timestamp:-1);
                }
            }
    )

    /**
     * 重写add方法
     * @param req
     * @return
     */
    def add(HttpServletRequest req){
        logger.debug('Received add params  is {}',req.getParameterMap())
        def id = ServletRequestUtils.getIntParameter(req,'_id',0)
        if(id == 0){
            id = noticeKGS.nextId();
        }
        def notice = adminMongo.getCollection('notices')
        long count = notice.count($$('_id',id))
        if(count > 0){
            return [code: 0,msg:'id不能重复']
        }
        def order = ServletRequestUtils.getIntParameter(req,'order',0)
        def status = ServletRequestUtils.getIntParameter(req,'status',0)
        def title = ServletRequestUtils.getStringParameter(req,'title','')
        def type = ServletRequestUtils.getIntParameter(req,'type',0)
        def content = ServletRequestUtils.getStringParameter(req,'content','')
        def click_url = ServletRequestUtils.getStringParameter(req,'click_url','')
        def timestamp = new Date().getTime()
        def query = Web.fillTimeBetween(req)

        query.and('_id').is(id)
        query.and('order').is(order)
        if(status == 0){
            query.and('status').is(Boolean.FALSE)
        }else{
            query.and('status').is(Boolean.TRUE)
        }
        query.and('title').is(title)
        query.and('content').is(content)
        query.and('click_url').is(click_url)
        query.and('timestamp').is(timestamp)
        query.and('type').is(type)

        notice.insert(query.get())

        return [code:1]
    }
    /*def add(HttpServletRequest req) {
        Integer id = req.getParameter("_id") as Integer

        if (id == null || adminMongo.getCollection('gifts').count($$("_id", id)) > 0) {
            return ['code': 30442]
        } else {
            this.cleanCache()
            return crud.add(req)
        }
    }

    private Integer generateId(){
        Integer id = noticeKGS.nextId()
        if (table().count($$("_id", id)) > 0) {
            return generateId()
        }
        return id
    }*/
}
