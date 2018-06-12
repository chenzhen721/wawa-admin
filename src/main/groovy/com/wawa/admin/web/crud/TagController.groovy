package com.wawa.admin.web.crud

import com.mongodb.DBCollection
import com.mongodb.QueryBuilder
import com.wawa.base.anno.RestWithSession
import com.wawa.base.Crud
import com.wawa.base.BaseController
import com.wawa.model.OpType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

import static com.wawa.common.doc.MongoKey.*
import static com.wawa.common.util.WebUtils.$$

/**
 * 主播标签内容
 */
@RestWithSession
class TagController extends BaseController
{
    Logger logger = LoggerFactory.getLogger(TagController.class)

    DBCollection table(){return adminMongo.getCollection('tags')}

    def list(HttpServletRequest req){
        QueryBuilder query = QueryBuilder.start();
        Crud.list(req,table(),query.get(),ALL_FIELD,SJ_DESC)
    }

    def add(HttpServletRequest req){
        def cat = req.getParameter('cat') as String
        def tags = req.getParameter('tags') as String
        if(table().count($$(cat:cat)) > 0){
            return [code: 30442]
        }
        def tmp =   System.currentTimeMillis()
        def insert = $$(cat:cat,timestamp:tmp)
        def update = $$($set, insert)
        if (tags != null) {
            update.append($addToSet , [tags: [$each:tags?.split(',')]])
        }
        if(table().update($$(_id,seqKGS.nextId()), update, true, false).getN() == 1){
            Crud.opLog(OpType.tag_add,insert)
            return [code:1]
        }
        [code:0]
    }

    def edit(HttpServletRequest req){
        def _id = req.getParameter('_id') as Integer
        def cat = req.getParameter('cat') as String
        def tags = req.getParameter('tags') as String
        if(_id <= 0){
            return [code: 30441]
        }
        def tmp =   System.currentTimeMillis()
        def insert = $$(cat:cat,timestamp:tmp)
        if(table().update($$(_id:_id),$$($unset , $$("tags", 1)), false, false).getN() == 1){
            def update = $$($set, insert)
            if (tags != null) {
                update.append($addToSet , [tags: [$each:tags?.split(',')]])
            }
            if(table().update($$(_id:_id), update, false, false).getN() == 1){
                Crud.opLog(OpType.tag_edit,insert.append('_id',_id))
                return [code:1]
            }
        }
        return [code:0]
    }

    def del(HttpServletRequest req){
        def _id = req.getParameter('_id') as Integer
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
