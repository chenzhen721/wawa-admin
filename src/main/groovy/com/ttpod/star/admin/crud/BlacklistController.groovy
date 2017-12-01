package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.model.OpType
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.math.NumberUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * 黑名单管理
 */
@RestWithSession
class BlacklistController extends BaseController{

    Logger logger = LoggerFactory.getLogger(BlacklistController.class)

    def list(HttpServletRequest req){

        QueryBuilder query = QueryBuilder.start();

        String _id = req[_id]
        if (StringUtils.isNotBlank(_id))
            query.and("_id").is(_id)

        def user_id = req['user_id']
        if (StringUtils.isNotBlank(user_id))
            query.and("user_id").is(Integer.parseInt(user_id))

        String type = req['type']
        if (StringUtils.isNotBlank(type))
            query.and("type").is(Integer.parseInt(type))

        Crud.list(req,adminMongo.getCollection('blacklist'),query.get(),ALL_FIELD,SJ_DESC){List<BasicDBObject> list ->
            for (BasicDBObject blk : list) {
                if(blk.get('user_id') != null){
                    def user = users().findOne(blk.get('user_id') as Integer, $$(_id: 0, nick_name: 1, finance:1, priv:1))
                    if(user)
                        blk.putAll(user)
                }
            }
        }

    }

    def add(HttpServletRequest req){
        def _id = req[_id]
        def txt_id = req['txt_id']
        def type =  Integer.parseInt(req['type'])
        Integer user_id = null;
        if(NumberUtils.isNumber(_id) && users().count($$(_id : Integer.parseInt(_id))) == 1){
            user_id = Integer.parseInt(_id)
        }
         def id = "${txt_id}_${type}".toString()
        def prop = [
                _id:id,
                type:type,
                user_id:user_id,
                timestamp:System.currentTimeMillis()
        ]
        adminMongo.getCollection('blacklist').save(new BasicDBObject((Map)prop))
        Crud.opLog(OpType.blacklist_add,prop)
        [code:1]
    }

    def del(HttpServletRequest req){
        String id = req[_id]
        adminMongo.getCollection('blacklist').remove(new BasicDBObject(_id,id))
        Crud.opLog(OpType.blacklist_del,[del:id])
        OK()
    }








}
