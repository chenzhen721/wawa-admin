package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.common.util.KeyUtils

import com.ttpod.star.model.OpType
import org.apache.commons.lang.StringUtils

import javax.servlet.http.HttpServletRequest
import java.util.regex.Pattern

import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.groovy.CrudClosures.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * date: 13-3-28 下午2:31
 * @author: yangyang.cong@ttpod.com
 */
@RestWithSession
class GiftController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(GiftController.class)
    @Delegate
    Crud crud = new Crud(adminMongo.getCollection('gifts'), true,
            [_id        : Int, name: Str, pic_url: Str, app_swf_url: Str, swf_url: Str, pic_pre_url: Str, sale: Ne0, isNew: Ne0, isHot: Ne0,
             ratio      : { String str -> (str == null || str.isEmpty()) ? null : str as Double }, desc: Str,star_award: Int,
             category_id: Int, order: Int, coin_price: Int, star_limit: Int, star: Eq1, status: Ne0, isHide: Ne0, star_count: Int,
             is_mark    : Ne0, mark_pic_url: Str, is_all: Ne0 , mark_pc: Str, mark_app: Str,is_diamond_gift:Bool,diamond_count:Int],
            new Crud.QueryCondition() {
                public DBObject query(HttpServletRequest req) {
                    QueryBuilder query = QueryBuilder.start()
                    def id = req[_id]
                    if (id.isNotBlank()) {
                        query.and('_id').is(id as Integer)
                    }
                    //礼物名称
                    def name = req.getParameter("name")
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

        if (id == null || adminMongo.getCollection('gifts').count($$("_id", id)) > 0) {
            return ['code': 30442]
        }
        def result = crud.add(req)
        this.cleanCache()
        return result

    }

    def edit(HttpServletRequest req) {
        def result = crud.edit(req)
        this.cleanCache()
        return result
    }

    public void cleanCache() {
        mainRedis.delete(KeyUtils.all_gifts())
    }

    static final String NEXT_WEEK_STAR_GIFT_LIST = 'next_week_star_gift_list'
    static final Map GIFTS_LIST_FIELDS = [gift_ids: { String str ->
        str.split(',').collect {
            Integer.valueOf(it.toString())
        }
    }]
    /**
     * 下周周星礼物列表  每周一凌晨脚本自动替换周星
     * @param req
     * @return
     */
    def next_week_gifts(HttpServletRequest req) {
        def star = req['gift_ids']
        def config = adminMongo.getCollection('config')
        def info = new BasicDBObject(_id, NEXT_WEEK_STAR_GIFT_LIST)
        if (StringUtils.isEmpty(star)) {
            config.remove(info)
            Crud.opLog(OpType.next_week_star_gift_list, info)
            return OK();
        }

        GIFTS_LIST_FIELDS.each { String field, Closure v ->
            info.put(field, v.call(req.getParameter(field)))
        }
        if (config.save(info).getN() == 1) {
            Crud.opLog(OpType.next_week_star_gift_list, info)
        }
        OK()
    }

    def show_next_week_gifts(HttpServletRequest req) {
        def config = adminMongo.getCollection('config')
        [code: 1, data: config.findOne(NEXT_WEEK_STAR_GIFT_LIST)]
    }

}
