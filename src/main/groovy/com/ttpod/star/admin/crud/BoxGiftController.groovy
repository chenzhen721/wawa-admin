package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.groovy.CrudClosures.*

/**
 * Created on 2014/8/27.
 */
@RestWithSession
class BoxGiftController extends BaseController {
    static final Logger logger = LoggerFactory.getLogger(BoxGiftController.class)

    @Delegate
    Crud crud = new Crud(adminMongo.getCollection('box_gifts'), true,
            [_id        : Int, name: Str, coin_price: Int, order: Int, desc: Str, pic_url: Str, swf_url: Str, pic_pre_url: Str, status: Ne0,
             ratio      : { String str -> (str == null || str.isEmpty()) ? null : str as Double },
             boxer_ratio: { String str -> (str == null || str.isEmpty()) ? null : str as Double },
            ],
            new Crud.QueryCondition() {
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject("order", -1);
                }

//                DBObject query(HttpServletRequest req) {
//                    return super.query(req)
//                }
            }
    )

    def add(HttpServletRequest req) {
        String id = req.getParameter("_id")
        if (id == null || adminMongo.getCollection('box_gifts').count($$("_id", id)) > 0) {
            return ["code": 30442]
        } else {
            this.cleanCache()
            return crud.add(req)
        }
    }

    def edit(HttpServletRequest req) {
        this.cleanCache()
        return crud.edit(req)
    }

    private void cleanCache() {
        String gifts_key = "all:ttxiuchang:box:gifts"
        mainRedis.delete(gifts_key)
    }

}
