package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.model.OpType
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.common.doc.MongoKey.$setOnInsert
import static com.ttpod.rest.common.doc.MongoKey.timestamp
import static com.ttpod.rest.common.doc.MongoKey.$unset

/**
 * 垃圾信息相关内容
 * date: 13-10-11 下午2:31
 * @author: haigen.xiong@ttpod.com
 */
@RestWithSession
class SpamController extends BaseController
{
    Logger logger = LoggerFactory.getLogger(SpamController.class)

    def list(HttpServletRequest req){

        QueryBuilder query = QueryBuilder.start();

        Crud.list(req,adminMongo.getCollection('spam_info'),query.get(),ALL_FIELD,SJ_DESC)

    }

    def add(HttpServletRequest req)
    {
         def field = req['field_id']

         def field_content = req['field_content']

         def tmp =   System.currentTimeMillis()
         def update = new BasicDBObject(field,field_content).append("modify",tmp)

        adminMongo.getCollection('spam_info').update(new BasicDBObject(_id, 'field_id_field_content_20131121'),
             new BasicDBObject($set:update))

        Crud.opLog(OpType.spam_add,update)

        [code:1]
    }

    def edit(HttpServletRequest req){

        def field = req['field_id']
        adminMongo.getCollection('spam_info').update(new BasicDBObject(_id, 'field_id_field_content_20131121'),
                new BasicDBObject($unset,new BasicDBObject(field,1)))

        Crud.opLog(OpType.spam_del,[del:field])
        OK()
    }










}
