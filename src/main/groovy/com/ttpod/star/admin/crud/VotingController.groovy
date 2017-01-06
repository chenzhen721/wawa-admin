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

/**
 * date: 13-10-11 下午2:31
 * @author: haigen.xiong@ttpod.com
 */
@RestWithSession
class VotingController extends BaseController{
    Logger logger = LoggerFactory.getLogger(VotingController.class)
    def list(HttpServletRequest req){

        QueryBuilder query = QueryBuilder.start();

        String id = req[_id]
        if (StringUtils.isNotBlank(id))
            query.and("_id").is(Long.parseLong(id))

        Crud.list(req,adminMongo.getCollection('voting'),query.get(),ALL_FIELD,SJ_DESC)

    }

    def add(HttpServletRequest req)
    {
        String title = req['title']
        String title_image = req['title_image']

        def prop = [
                _id:System.currentTimeMillis(),
                title:title,
                title_image:title_image,
                timestamp:System.currentTimeMillis(),
                voting_num:0
        ]
        adminMongo.getCollection('voting').save(new BasicDBObject((Map)prop))
        Crud.opLog(OpType.voting_add,prop)
        [code:1]
    }


    def del(HttpServletRequest req){

        String id = req[_id]

        adminMongo.getCollection('voting').remove(new BasicDBObject(_id,Long.parseLong(id)))

        Crud.opLog(OpType.voting_del,[del:id])
        OK()
    }








}
