package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.ttpod.rest.common.doc.MongoKey
import com.ttpod.rest.common.doc.ParamKey
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.rest.web.StaticSpring
import com.ttpod.star.common.util.KeyUtils
import com.ttpod.star.model.ApplyType
import com.ttpod.star.model.OpType
import com.ttpod.star.model.RoomType
import com.ttpod.star.model.UserType
import org.apache.commons.lang.StringUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * 主播间列表排序
 */
@RestWithSession
class RoomRankController extends BaseController{

    @Resource
    StarController starController

    DBCollection table(){rankMongo.getCollection('rooms')}

    //查询
    def list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        [_id].each { String field ->
            intQuery(query, req, field)
        }
        Crud.list(req, table(), query.get(), MongoKey.ALL_FIELD, $$(total : -1)) { List<BasicDBObject> data ->
            def users = users()
            for (BasicDBObject obj : data) {
                def user = users.findOne(obj[_id], $$("nick_name":1, "finance.bean_count_total" : 1))
                if(user){
                    user.removeField(_id)
                    obj.putAll(user)
                }

            }
        }
    }

    /**
     * 运营手动增加分数
     * @param req
     * @return
     */
    def add_points(HttpServletRequest req){
        def star_id = req.getInt(_id)
        def points = req.getInt('points')
        if(table().update($$(_id: star_id), $$($set:[op_points:points]), true, false, writeConcern).getN() == 1){
            Crud.opLog(OpType.add_room_rank_points,[star_id:star_id,points:points])
        }
        OK()
    }

}
