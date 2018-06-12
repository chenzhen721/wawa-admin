package com.wawa.admin.web.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.wawa.common.util.JSONUtil
import com.wawa.common.util.MsgDigestUtil
import com.wawa.base.anno.RestWithSession
import com.wawa.base.persistent.KGS
import com.wawa.base.Crud
import com.wawa.base.BaseController

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import java.util.regex.Pattern

import static com.wawa.groovy.CrudClosures.*

/**
 * date: 13-3-28 下午2:31
 * @author: yangyang.cong@ttpod.com
 */
@RestWithSession
class AdminController extends BaseController{

    @Resource
    KGS    seqKGS
    @Delegate Crud crud = new Crud(adminMongo.getCollection('admins'),true,
            [_id:{seqKGS.nextId()},nick_name:Str,name:Str,menus:{if(it){JSONUtil.jsonToMap(it as String) }else{null}},
             menus1:{if(it){JSONUtil.jsonToMap(it as String) }else{null}},
            modules: {if(it){JSONUtil.jsonToMap(it as String) }else{null}}, password:{MsgDigestUtil.SHA.digest2HEX(it as String)},timestamp:Timestamp],

            new Crud.QueryCondition(){
                public DBObject query(HttpServletRequest req) {
                    if (req.getParameter('nick_name')){
                        return QueryBuilder.start().and('nick_name').regex(Pattern.compile("/"+req.getParameter('nick_name')+"/")).get()
                    }
                    return super.query(req)
                }
                public DBObject field(HttpServletRequest req) {
                    return new BasicDBObject("password",0);
                }
            }
    )

    def show(HttpServletRequest req){
        table().findOne(req.getParameter(_id) as Integer,new BasicDBObject("password",0))
    }

}
