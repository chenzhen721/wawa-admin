package com.wawa.admin.web.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.wawa.base.anno.RestWithSession
import com.wawa.common.doc.MongoKey
import com.wawa.base.Crud
import com.wawa.base.BaseController
import com.wawa.model.OpType
import org.apache.commons.lang.StringUtils

import javax.servlet.http.HttpServletRequest

import static com.wawa.common.doc.MongoKey.ALL_FIELD

/**
 * 敏感字管理
 */
//@Rest
@RestWithSession
class BlackwordController extends BaseController{

       DBCollection table(){adminMongo.getCollection('blackwords')}

    def list(HttpServletRequest req){
        def q= QueryBuilder.start()
        intQuery(q,req,'type')
        //super.list(req,q.get())
        def dataList = table().find(q.get(),ALL_FIELD).sort(MongoKey.SJ_DESC).toArray()
        [code: 1,data:dataList]
    }


    def add(HttpServletRequest req){
        String words = req[_id]
        if(StringUtils.isBlank(words)){
            return OK()
        }
        def tmp = System.currentTimeMillis()
        def coll = table()
        def type = req['type'] as Integer
        words.split('\\s+').collect {
            new BasicDBObject(_id,it).append(timestamp,tmp).append('type',type)
        }.each {
            DBObject word = (DBObject)it
            if(StringUtils.isNotBlank(word.get(_id) as String))
                coll.save(word)
        }
        Crud.opLog(OpType.blackwords_add,words)
        this.cleanCache(type)
        [code:1]
    }

    def del(HttpServletRequest req){
        table().remove(new BasicDBObject(_id,[$in:req[_id].toString().split('\\s+')]));
        Crud.opLog(OpType.blackwords_del,[del:req[_id]])
        this.cleanCache(0)
        this.cleanCache(1)
        OK()
    }

    private void cleanCache(Integer type)
    {
        mainRedis.delete("blackblist:0:blacklists")
        mainRedis.delete("blackblist:1:blacklists")
    }

}
