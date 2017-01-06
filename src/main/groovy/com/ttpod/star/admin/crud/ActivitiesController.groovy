package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.util.JSONUtil
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.common.util.KeyUtils
import com.ttpod.star.admin.StarController
import com.ttpod.star.model.OpType
import com.ttpod.star.model.UserType
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import java.util.regex.Pattern

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.doc.MongoKey.$addToSet
import static com.ttpod.rest.common.doc.MongoKey.$pull
import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import com.ttpod.star.admin.Web

import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.groovy.CrudClosures.Int

import static com.ttpod.rest.groovy.CrudClosures.Ne0

import static com.ttpod.rest.groovy.CrudClosures.Str
import static com.ttpod.rest.groovy.CrudClosures.Timestamp

/**
 * 户外活动相关的内容
 */
//@Rest
@RestWithSession
class ActivitiesController extends BaseController{

    static final Logger logger = LoggerFactory.getLogger(ActivitiesController.class)

    DBCollection table(){adminMongo.getCollection('activities')}

    @Resource
    StarController starController

    //活动标题 关联链接 首页小图 直播间小图 Banner图 排序 状态 开始时间 结束时间 活动礼物 参赛主播列表 操作
    @Delegate
    Crud crud = new Crud(table(), true,
            [_id : Int, title: Str, link_url: Str, pic_url: Str, room_pic_url: Str,  banner_pic_url: Str,
             stime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()},
             etime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()},
             order: Int,  status: Ne0, gift_id : Int, process: Int,timestamp:Timestamp],
            new Crud.QueryCondition() {
                public DBObject query(HttpServletRequest req) {
                    QueryBuilder query = QueryBuilder.start()
                    def id = req[_id]
                    if (id.isNotBlank()) {
                        query.and('_id').is(id as Integer)
                    }
                    //礼物名称
                    def name = req.getParameter("title")
                    if (StringUtils.isNotBlank(name)) {
                        Pattern pattern = Pattern.compile("^" + name + ".*\$", Pattern.CASE_INSENSITIVE);
                        query.and("name").regex(pattern)
                    }
                    return query.get();
                }

                public DBObject sortby(HttpServletRequest req) {
                    BasicDBObject sortObj = new BasicDBObject("status", -1)
                    sortObj.put("order", -1)
                    return sortObj
                }
            }
    )

    def add(HttpServletRequest req) {
        Integer id = req.getParameter("_id") as Integer
        if (id == null || table().count($$("_id", id)) > 0) {
            return ['code': 30442]
        }
        def result = crud.add(req)
        renewCache(id)
        return result
    }

    def edit(HttpServletRequest req) {
        Integer id = req.getParameter("_id") as Integer
        def result = crud.edit(req)
        renewCache(id)
        return result
    }

    /**
     * 活动报名主播列表
     * @param req
     * @return
     */
    def star_list(HttpServletRequest req){
        Integer id = req.getParameter("_id") as Integer
        def activity = table().findOne($$(_id, id), $$(stars:1))
        List<Integer> stars = (activity?.get('stars') ?: Collections.emptyList() )as List
        if(stars == null || stars.size() == 0) return [code : 1, data:[]];
        Crud.list(req, users(), $$(_id : [$in : stars]), $$('nick_name':1, "finance":1), $$('star.timestamp', -1))
    }

    private final static String tag = "比赛"
    /**
     * 添加活动主播
     * @param req
     * @return
     */
    def add_star(HttpServletRequest req){
        Integer id = req.getParameter("_id") as Integer
        Integer star_id = req.getParameter("star_id") as Integer
        if(id == null || star_id == null) return Web.missParam();
        //判断是否已经参加其他活动
        def redis_star_list = KeyUtils.Actives.starList();
        if(mainRedis.opsForHash().hasKey(redis_star_list, star_id.toString())){
            return [code : 30443]
        }
        //判断是否为主播
        if(users().count($$(_id :star_id, priv : UserType.主播.ordinal())) != 1)
            return Web.missParam();
        if(table().update($$(_id : id), $$($addToSet, $$(stars:star_id)), false, false,writeConcern).getN()==1){
            def activity = table().findOne($$(_id, id), $$(gift_id:1,stime:1,etime:1,process:1,status:1,title:1,room_pic_url:1,link_url:1))
            Boolean status = activity['status'] as Boolean
            if(status){
                renewStarCache(activity, star_id, Boolean.TRUE)
            }
            Crud.opLog(OpType.activities_add_star, [_id : id, star: star_id])
            return [code : 1]
        }
        return [code : 0]
    }

    /**
     * 删除主播
     * @param req
     * @return
     */
    def del_star(HttpServletRequest req){
        Integer id = req.getParameter("_id") as Integer
        Integer star_id = req.getParameter("star_id") as Integer
        if(id == null || star_id == null) return Web.missParam();
        if(table().update($$(_id : id), $$($pull, $$(stars:star_id)), false, false,writeConcern).getN()==1){
            renewStarCache(null, star_id, Boolean.FALSE)
            Crud.opLog(OpType.activities_del_star, [_id : id, star: star_id])
            return [code : 1]
        }
        return [code : 0]

    }

    private void renewCache(Integer activityId){
        def activity = table().findOne($$(_id, activityId), $$(stars:1,gift_id:1,stime:1,etime:1,process:1,status:1,title:1,room_pic_url:1,link_url:1))
        if(activity){
            Boolean status = activity['status'] as Boolean
            def redis_info = KeyUtils.Actives.info(activityId);
            List<Integer> stars = (activity?.get('stars') ?: Collections.emptyList() )as List

            if(status){//上线状态设置主播缓存，设置活动信息缓存
                /*Map<String, String> info = new HashMap<>();
                info.put('_id', activity['_id'] as String)
                info.put('title', activity['title'] as String)
                info.put('room_pic_url', activity['room_pic_url'] as String)
                info.put('link_url', activity['link_url'] as String)
                info.put('gift_id', activity['gift_id'] as String)
                info.put('stime', activity['stime'] as String)
                info.put('etime', activity['etime'] as String)
                info.put('process', activity['process'] as String)*/
                //mainRedis.opsForHash().putAll(redis_info, info)
                if(stars != null && stars.size() > 0){
                    activity.removeField('stars');
                    stars.each {Integer starId ->
                        renewStarCache(activity, starId, Boolean.TRUE)
                    }
                }

            }else{
                //下线状态，清除主播缓存，清除活动信息缓存
                stars.each {Integer starId ->
                    renewStarCache(null, starId, Boolean.FALSE)
                }
                //mainRedis.delete(redis_info);
            }
        }
    }

    private void renewStarCache(DBObject activity, Integer star_id, Boolean isAdd){
        def redis_star_list = KeyUtils.Actives.starList();
        if(isAdd && activity != null){
            mainRedis.opsForHash().put(redis_star_list, star_id.toString(), activity['_id'].toString());
            rooms().update($$(_id : star_id), $$($set, $$(activity:activity)))
            //starController.add_star_tag(star_id, tag)
        }else if (!isAdd){
            mainRedis.opsForHash().delete(redis_star_list, star_id.toString());
            rooms().update($$(_id : star_id), $$($unset, $$(activity:1)))
            //starController.remove_star_tag(star_id, tag)
        }
    }
}
