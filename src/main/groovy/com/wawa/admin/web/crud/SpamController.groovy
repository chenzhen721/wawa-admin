package com.wawa.admin.web.crud

import com.mongodb.BasicDBObject
import com.mongodb.QueryBuilder
import com.wawa.base.anno.RestWithSession
import com.wawa.base.Crud
import com.wawa.base.BaseController
import com.wawa.model.OpType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

import static com.wawa.common.doc.MongoKey.ALL_FIELD
import static com.wawa.common.doc.MongoKey.SJ_DESC
import static com.wawa.common.doc.MongoKey.$unset

/**
 * 垃圾信息相关内容
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
         def field = req['field_id'] as String

         def field_content = req['field_content']

         def tmp =   System.currentTimeMillis()
         def update = new BasicDBObject(field,field_content).append("modify",tmp)

        adminMongo.getCollection('spam_info').update(new BasicDBObject(_id, 'field_id_field_content_20131121'),
             new BasicDBObject($set:update))

        Crud.opLog(OpType.spam_add,update)

        [code:1]
    }

    def edit(HttpServletRequest req){

        def field = req['field_id'] as String
        adminMongo.getCollection('spam_info').update(new BasicDBObject(_id, 'field_id_field_content_20131121'),
                new BasicDBObject($unset,new BasicDBObject(field,1)))

        Crud.opLog(OpType.spam_del,[del:field])
        OK()
    }










}
