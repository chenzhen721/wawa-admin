package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.MongoKey
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.model.OpType
import org.apache.commons.lang.StringUtils

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD

/**
 * date: 13-3-28 下午2:31
 * @author: yangyang.cong@ttpod.com
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
        if(words.isBlank()){
            return OK()
        }
        def tmp = System.currentTimeMillis()
        def coll = table()
        def type = req.getInt('type')
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
