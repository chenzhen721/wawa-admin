package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.model.OpType
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.$addToSet
import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.common.doc.MongoKey.$setOnInsert
import static com.ttpod.rest.common.doc.MongoKey.timestamp
import static com.ttpod.rest.common.doc.MongoKey.$unset

/**
 * 主播标签内容
 */
//@Rest
@RestWithSession
class TagController extends BaseController
{
    Logger logger = LoggerFactory.getLogger(TagController.class)

    def DBCollection table(){return adminMongo.getCollection('tags')}

    def list(HttpServletRequest req){
        QueryBuilder query = QueryBuilder.start();
        Crud.list(req,table(),query.get(),ALL_FIELD,SJ_DESC)
    }

    def add(HttpServletRequest req){
        def cat = req['cat']
        def tags = req['tags']
        if(table().count($$(cat:cat)) > 0){
            return [code: 30442]
        }
        def tmp =   System.currentTimeMillis()
        def insert = $$(cat:cat,timestamp:tmp)
        if(table().update($$(_id,seqKGS.nextId()),
                                $$($addToSet , $$("tags", $$('$each',tags.split(','))))
                                   .append($set,insert), true, false).getN() == 1){
            Crud.opLog(OpType.tag_add,insert)
            return [code:1]
        }
        [code:0]
    }

    def edit(HttpServletRequest req){
        def _id = req['_id'] as Integer
        def cat = req['cat']
        def tags = req['tags']
        if(_id <= 0){
            return [code: 30441]
        }
        def tmp =   System.currentTimeMillis()
        def insert = $$(cat:cat,timestamp:tmp)
        if(table().update($$(_id:_id),$$($unset , $$("tags", 1)), false, false).getN() == 1){
            if(table().update($$(_id:_id),
                    $$($addToSet , $$("tags", $$('$each',tags.split(','))))
                            .append($set,insert), false, false).getN() == 1){
                Crud.opLog(OpType.tag_edit,insert.append('_id',_id))
                return [code:1]
            }
        }
        return [code:0]
    }


    def del(HttpServletRequest req){
        def _id = req['_id'] as Integer
        if(_id <= 0){
            return [code: 30441]
        }
        if(table().remove($$(_id:_id)).getN() == 1){
            Crud.opLog(OpType.tag_del,$$(_id:_id))
            return [code:1]
        }
        return [code:0]
    }








}
