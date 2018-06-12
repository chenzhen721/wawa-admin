package com.wawa.admin.web.crud

import com.wawa.base.anno.RestWithSession
import com.wawa.base.Crud
import com.wawa.base.BaseController
import com.wawa.model.AppPropType
import com.wawa.model.OpType
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.servlet.http.HttpServletRequest

import static com.wawa.common.doc.MongoKey.ALL_FIELD
import static com.wawa.common.doc.MongoKey.SJ_DESC
import static com.wawa.common.util.WebUtils.$$

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
        def id = req.getParameter(_id) as String
        if(StringUtils.isEmpty(id))
            return [code:0]

        def desc =  req.getParameter('desc')
        def content =  req.getParameter('content')
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
        String id = req.getParameter(_id)
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
