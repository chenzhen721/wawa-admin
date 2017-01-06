package com.ttpod.star.admin
import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.MongoKey
import com.ttpod.rest.common.util.JSONUtil
import com.ttpod.rest.common.util.Regex

import com.ttpod.rest.web.Crud
import com.ttpod.rest.web.StaticSpring
import com.ttpod.star.common.util.KeyUtils
import com.ttpod.star.model.OpType
import com.ttpod.star.model.RecommendType
import com.ttpod.star.model.UserType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest
import java.sql.Array

import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM
import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.groovy.CrudClosures.*
import static com.ttpod.rest.common.util.WebUtils.$$
import freemarker.template.utility.StringUtil
import org.apache.commons.lang.StringUtils
import com.mongodb.DBObject

/**
 * 主播推荐
 * date: 14-528 下午2:31
 */
//@Rest
@RestWithSession
class RecommController extends BaseController{

    static final Logger logger = LoggerFactory.getLogger(RecommController.class)

    DBCollection table(){adminMongo.getCollection('config')}
    DBCollection star_recommends(){adminMongo.getCollection('star_recommends')}

    public static final String index_recommend_list = 'index_star_recommend_list'
    public static final String index_new_star_field = 'stars' //首页新人推荐 field
    public static final String index_top_star_field = 'top_stars' //首页么么推荐 field
    public static final String index_wait_star_field = 'wait_stars' //首页么么等待推荐主播 field
    public static final String recommend_star_group = 'star_group' //推荐主播分组

    static final Map index_recommend_list_fields=[stars:{String str->str.split(',').collect {Integer.valueOf(it.toString())}  }]

    def save_index_star_recommend(HttpServletRequest req){
        def star = req['stars']
        def info  = new BasicDBObject(_id,index_recommend_list)
        if(StringUtils.isEmpty(star)){
            table().remove(info)
            Crud.opLog("sys_index_star_recommend_list",info)
            return OK();
        }

        index_recommend_list_fields.each{String field,Closure v->
            info.put(field,v.call(req.getParameter(field)))
        }
        if(table().save(info).getN() == 1){
            Crud.opLog("sys_index_star_recommend_list",info)
        }
        OK()
    }

    def add_index_star_recommend(HttpServletRequest req){
        Integer starId = req['starId'] as Integer
        def week = req['dayOfweek']
        def info  = new BasicDBObject(_id,index_recommend_list)
        if(table().update($$(_id,index_recommend_list),$$($addToSet , $$("week_"+week, starId)), true, false).getN() == 1){
            Crud.opLog("sys_index_star_recommend_list",info)
        }
        OK()
    }

    def edit_index_star_recommend(HttpServletRequest req){
        def week = req['dayOfweek']
        def stars = req['stars']
        def info  = new BasicDBObject(_id,index_recommend_list)
        table().update($$(_id,index_recommend_list),$$($set, $$("week_"+week, [])))
        if(table().update($$(_id,index_recommend_list),
                $$($addToSet , $$("week_"+week, $$('$each',stars.split(',').collect{Integer.valueOf(it.toString())}))),
                true, false).getN() == 1){
        }
        Crud.opLog("sys_index_star_recommend_list",info)
        OK()
    }

    def del_index_star_recommend(HttpServletRequest req){
        Integer starId = req['starId'] as Integer
        def week = req['dayOfweek']
        def info  = new BasicDBObject(_id,index_recommend_list)
        if(table().update($$(_id,index_recommend_list),$$($pull , $$("week_"+week, starId))).getN() == 1){
            Crud.opLog("sys_index_star_recommend_list",info)
        }
        OK()
    }

    def show_index_star_recommend(HttpServletRequest req){
        def config = table().findOne(index_recommend_list)
        def Map<String, Object> stars = new HashMap()
        if(config){
            List<Integer> broker_stars = config?.get('stars') as List
            if(broker_stars != null && broker_stars.size() > 0)
                stars.put('broker_stars', getStarInfo(broker_stars));
            def weeks = 1..7
            weeks.each{day ->
                def week = "week_${day}".toString()
                List<Integer> starIds = config?.get(week) as List
                if(starIds != null && starIds.size() > 0)
                    stars.put(week, getStarInfo(starIds));
            }
        }
        [code:1,data:stars]
    }

    /**
     * 获取主播和经纪人信息
     * @param starIds
     * @return
     */
    private List<DBObject> getStarInfo(List<Integer> starIds){
        def starList = users().find($$(_id, $$($in:starIds)), $$(nick_name : 1, star : 1)).toArray()
        starList.each {DBObject user ->
            def star = user.removeField('star') as Map;
            if(star){
                def broker = users().findOne($$(_id, star?.get('broker') as Integer), $$(nick_name : 1))
                user.put('broker' , broker)
            }
        }
        return starList;
    }

    /**
     * 分组列表
     * @param req
     * @return
     */
    def list_group(HttpServletRequest req){
        def config = table().findOne($$(_id,index_recommend_list),$$(recommend_star_group,1))
        List<Integer> groups = (config?.get(recommend_star_group) as List) ?: Collections.emptyList();
        return [code : 1, data : groups]
    }

    /**
     * 添加主播推荐分组
     * @param req
     * @return
     */
    def add_group(HttpServletRequest req){
        String groups = req['groups']
        if(StringUtils.isEmpty(groups)) return  Web.missParam();
        String[] gs = groups.trim().replace('，',',',).split(',')
        if(gs == null || gs.length == 0) return  Web.missParam();
        Crud.opLog(OpType.add_recomm_group,gs)
        return [code : table().update($$(_id,index_recommend_list),
                $$($addToSet , $$(recommend_star_group, $$('$each',gs))), true, false).getN()]
    }

    /**
     * 删除分组
     * @param req
     * @return
     */
    def del_group(HttpServletRequest req){
        String groups = req['groups']
        if(StringUtils.isEmpty(groups)) return  Web.missParam();
        String[] gs = groups.trim().replace('，',',',).split(',')
        if(gs == null || gs.length == 0) return  Web.missParam();

        gs.each {String group ->
            table().update($$(_id,index_recommend_list),
                    $$($pull , $$(recommend_star_group, group)), true, false)
        }
        Crud.opLog(OpType.del_recomm_group,gs)
        return [code : 1]
    }

    /**
     * 设置主播分组
     * @param req
     * @return
     */
    def set_star_group(HttpServletRequest req){
        def ids = req['ids'] as String
        def group = req['group'] as String
        if(StringUtils.isEmpty(ids) || StringUtils.isEmpty(group)) return Web.missParam();
        List<Integer> star_ids = strSplit2List(ids)
        star_ids.each {Integer star_id ->
            star_recommends().update($$(_id, star_id), $$($set:[group:group]))
        }
        Crud.opLog(OpType.set_recomm_star_group,[star_ids:star_ids, group : group])
        return [code : 1]
    }

    //么么推荐列表
    def list(HttpServletRequest req) {
        def star_ids = req['ids'] as String;
        String group = req['group']
        def query = $$(type : RecommendType.普通.ordinal())
        if(StringUtils.isNotEmpty(star_ids)){
            List<Integer> search_ids = strSplit2List(star_ids)
            query = $$(_id: [$in: search_ids])
        }
        if(StringUtils.isNotEmpty(group)){
            query.append('group', group)
        }
        star_list(req, query)
    }

    //添加推荐主播 (多个逗号分隔)
    def recommend(HttpServletRequest req){
        def ids = req['ids'] as String
        if(StringUtils.isEmpty(ids)) return Web.missParam();
        List<Integer> star_ids = strSplit2List(ids)
        def expire_date = Web.getTime(req, 'expire_date')//失效时间
        def day_begin = req['day_begin']  //每日开始时间段
        def day_end = req['day_end']  //每日结束时间段
        def weeks =  req['weeks'] as String //每周
        Map weekFileds = new HashMap();
        if(StringUtils.isNotEmpty(weeks)){
            if(StringUtils.isEmpty(day_begin) || StringUtils.isEmpty(day_end)) return Web.missParam();
            Integer begin = Integer.valueOf(StringUtils.remove(day_begin, ":"))
            Integer end = Integer.valueOf(StringUtils.remove(day_end, ":"))
            List<Integer> dayOfweeks = strSplit2List(weeks)
            dayOfweeks.each {Integer dayOfweek ->
                weekFileds.put(dayOfweek, $$(begin:begin, end:end))
            }
        }

        def expire_time = null
        if(expire_date != null){
            expire_time = expire_date.getTime()
        }

        star_recommends().updateMulti($$(_id : [$in: star_ids]),
                                                $$($set : [is_recomm:Boolean.TRUE,expires:expire_time,
                                                      weeks:weeks, day_begin:day_begin, day_end:day_end ,week:weekFileds]))

        Crud.opLog(OpType.index_top_recommend_add,[ids : star_ids, expire_date:expire_date,weeks:weeks,day_begin:day_begin,day_end:day_end])
        return [code:1]

    }

    //取消推荐 (多个逗号分隔)
    def cancel(HttpServletRequest req){
        String ids = req['ids'] as String
        if(StringUtils.isEmpty(ids)) return Web.missParam();
        List<Integer> star_ids = strSplit2List(ids)
        star_recommends().updateMulti($$(_id : [$in: star_ids]), $$($set : [is_recomm:Boolean.FALSE]))
        Crud.opLog(OpType.index_top_recommend_cancel,[ids:star_ids])
        OK()
    }

    //添加候选推荐主播
    def add(HttpServletRequest req){
        def ids = req['ids'] as String
        String group = req['group'] as String
        if(StringUtils.isEmpty(ids)) return Web.missParam();
        if(addWaitStars(strSplit2List(ids), group)){
            Crud.opLog(OpType.index_top_recommend_add_waiting,[ids:strSplit2List(ids)])
            return [code:1]
        }
        return [code: 0]
    }

    private Boolean addWaitStars(List<Integer> ids, String group){
        List<Integer> star_ids = new ArrayList<>();
        ids.each {Integer star_id ->
            def user =  users().findOne($$(_id : star_id, priv : UserType.主播.ordinal()), $$(_id:1))
            if(user == null)
                user =  users().findOne($$(mm_no : star_id, priv : UserType.主播.ordinal()), $$(_id:1))
            if(user != null){
                star_ids.add(user[_id] as Integer);
            }
        }
        if(star_ids == null || star_ids.size() == 0) return Boolean.FALSE;

        star_ids.each {Integer star_id ->
            def info  = $$(_id : star_id, type: RecommendType.普通.ordinal(), is_recomm:Boolean.FALSE, timestamp : System.currentTimeMillis(), expires:null)
            if(StringUtils.isNotEmpty(group)){
                info.append('group', group)
            }
            star_recommends().update($$(_id : star_id), $$($set : info), true ,false)
            //star_recommends().save(info)
        }
        return Boolean.TRUE;
    }

    //删除(多个逗号分隔)
    def del(HttpServletRequest req){
        def ids = req['ids'] as String
        if(StringUtils.isEmpty(ids)) return Web.missParam();
        strSplit2List(ids).each {Integer star_id ->
            star_recommends().remove($$(_id : star_id))
        }
        Crud.opLog(OpType.index_top_recommend_del,[ids:strSplit2List(ids)])
        OK()
    }

    //新人推荐列表
    def list_new(HttpServletRequest req){
        def config = table().findOne(index_recommend_list)
        def star_ids = req['ids'] as String;

        def query = $$(type : RecommendType.新人.ordinal())
        if(StringUtils.isNotEmpty(star_ids)){
            List<Integer> search_ids = strSplit2List(star_ids)
            query = $$(_id: [$in: search_ids])
        }
        star_list(req, query)
    }


    //添加新人 (多个逗号分隔)（代理）推荐
    def add_new(HttpServletRequest req){
        def ids = req['ids'] as String
        if(StringUtils.isEmpty(ids)) return Web.missParam();
        def expire_date = Web.getTime(req, 'expire_date')
        def day_begin = req['day_begin']  //每日开始时间段
        def day_end = req['day_end']  //每日结束时间段
        def weeks =  req['weeks'] as String //每周
        Map weekFileds = new HashMap();
        if(StringUtils.isNotEmpty(weeks)){
            if(StringUtils.isEmpty(day_begin) || StringUtils.isEmpty(day_end)) return Web.missParam();
            Integer begin = Integer.valueOf(StringUtils.remove(day_begin, ":"))
            Integer end = Integer.valueOf(StringUtils.remove(day_end, ":"))
            List<Integer> dayOfweeks = strSplit2List(weeks)
            dayOfweeks.each {Integer dayOfweek ->
                weekFileds.put(dayOfweek, $$(begin:begin, end:end))
            }
        }

        List<Integer> star_ids = strSplit2List(ids)
        def expires = null
        if(expire_date != null){
            expires = expire_date.getTime()
        }

        star_ids.each {Integer star_id ->
            def info  = $$(_id : star_id,  type: RecommendType.新人.ordinal(),
                        is_recomm:Boolean.TRUE, timestamp : System.currentTimeMillis(), expires : expires,
                            weeks:weeks, day_begin:day_begin, day_end:day_end ,week:weekFileds)
            star_recommends().update($$(_id : star_id), $$($set : info), true ,false)
        }
        Crud.opLog(OpType.index_new_recommend_add,[ids:strSplit2List(ids)])
        return [code:1]
    }

    //删除(多个逗号分隔)
    def del_new(HttpServletRequest req){
        def ids = req['ids'] as String
        if(StringUtils.isEmpty(ids)) return Web.missParam();
        strSplit2List(ids).each {Integer star_id ->
            star_recommends().remove($$(_id : star_id))
        }
        Crud.opLog(OpType.index_new_recommend_del,[ids:strSplit2List(ids)])
        OK()
    }


    private static List<Integer> strSplit2List(String str){
        return str.trim().replace('，',',',).split(',').collect { Integer.valueOf(it.toString())}
    }

    private star_list(HttpServletRequest req,  BasicDBObject query){
        Crud.list(req, star_recommends(), query, $$(week:0), SJ_DESC) { List<BasicDBObject> data ->
            def users = users()
            def rooms = rooms()
            for (BasicDBObject obj : data) {
                Integer xy_star_id = obj[_id] as Integer
                def star = rooms.findOne($$(_id : xy_star_id),$$(pic_url : 1, live : 1))
                if(star == null){//移除被解约主播
                    star_recommends().remove($$(_id : xy_star_id))
                    continue;
                }
                star.removeField(_id)
                obj.putAll(star);
                def user = users.findOne(xy_star_id, $$("nick_name":1, "finance.bean_count_total" : 1, 'mm_no' : 1))
                if(user){
                    user.removeField(_id)
                    obj.putAll(user)
                }
                //主播面试视频
                def video_path = adminMongo.getCollection('applys').findOne($$(xy_user_id:xy_star_id),
                        $$(xy_user_id: 1, 'video_path': 1))?.get("video_path")
                if (StringUtils.isNotEmpty(video_path as String)) {
                    obj.put('video_path', UpaiController.STAR_VIDEO_DOMAIN + video_path)
                }
            }
        }
    }
}
