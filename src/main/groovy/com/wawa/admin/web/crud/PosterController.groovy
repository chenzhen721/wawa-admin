package com.wawa.admin.web.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.wawa.base.anno.RestWithSession
import com.wawa.base.persistent.KGS
import com.wawa.base.Crud
import com.wawa.base.BaseController
import com.wawa.api.Web

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.wawa.groovy.CrudClosures.*

/**
 * date: 13-3-28 下午2:31
 * @author: yangyang.cong@ttpod.com
 */
@RestWithSession
class PosterController extends BaseController {

    @Resource
    KGS posterKGS

    Closure timeformat = { String dateStr ->
        return Web.getTime(dateStr)?.getTime()
    }

    /**
     * type: 0:海报 1:网站预设背景 2:安卓广告页面 3:ios广告页面 4:ios启动页面 5:首页活动 6:个人主页背景 7:安卓启动页 8:搜索栏目
     * goto_type: 0:url, 1:直播间 2:活动专题
     * goto_val: url地址/直播间id/活动id
     */
    @Delegate
    Crud crud = new Crud(adminMongo.getCollection('posters'), true,
            [_id : { posterKGS.nextId() }, title: Str, pic_url: Str, small_pic_url: Str, click_url: Str, goto_type : Int, goto_val : Str,
             type: Int, status: Ne0, timestamp: Timestamp, order: Int, stime: timeformat, etime: timeformat],
            new Crud.QueryCondition() {
                public DBObject query(HttpServletRequest req) {
                    def query = QueryBuilder.start()
                    intQuery(query, req, 'type')
                    return query.get()
                }

                public DBObject sortby(HttpServletRequest req) {
                    new BasicDBObject(status: -1, order: 1, timestamp: -1)
                }
            }
    )
}
