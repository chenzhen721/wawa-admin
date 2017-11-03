package com.ttpod.star.admin.crud

import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.model.AppPropType
import com.ttpod.star.model.OpType
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * date: 14-05-30 下午2:31
 */
//@Rest
@RestWithSession
class AppPropController extends BaseController{

    Logger logger = LoggerFactory.getLogger(AppPropController.class)

    def list(HttpServletRequest req){
        def type = ServletRequestUtils.getIntParameter(req, 'type', AppPropType.android.ordinal())
        def query = $$(type:type);
        Crud.list(req,adminMongo.getCollection('properties'),query,ALL_FIELD,SJ_DESC)
    }

    def add(HttpServletRequest req){
        def id = req[_id]
        if(StringUtils.isEmpty(id))
            return [code:0]

        def desc =  req['desc']
        def content =  req['content']
        def type = ServletRequestUtils.getIntParameter(req, 'type', AppPropType.android.ordinal())
        def prop = [
                _id:id,
                content:content,
                type:type,
                desc:desc,
                timestamp:System.currentTimeMillis()
        ]
        adminMongo.getCollection('properties').save($$((Map)prop))
        Crud.opLog(OpType.properties_add,prop)
        cleanCache();
        [code:1]
    }

    def del(HttpServletRequest req){
        String id = req[_id]
        if(StringUtils.isEmpty(id))
            return [code:0]

        def pro = adminMongo.getCollection('properties').findOne($$(_id,id))
        adminMongo.getCollection('properties').remove($$(_id,id))
        Crud.opLog(OpType.properties_del,[del:id, data:pro])
        cleanCache();
        OK()
    }

    private void cleanCache()
    {
        String props_key = "all:ttxiuapp:props"
        mainRedis.delete(props_key);
        AppPropType.values().each {AppPropType type ->
            props_key = "all:ttxiuapp:props:"+type.ordinal()
            mainRedis.delete(props_key);
        }
    }
}
