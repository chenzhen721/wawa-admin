package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.common.util.JSONUtil
import com.ttpod.rest.common.util.MsgDigestUtil
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.persistent.KGS
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import java.util.regex.Pattern

import static com.ttpod.rest.groovy.CrudClosures.*

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
                    if (req['nick_name']){
                        return QueryBuilder.start().and('nick_name').regex(Pattern.compile("/"+req['nick_name']+"/")).get()
                    }
                    return super.query(req)
                }
                public DBObject field(HttpServletRequest req) {
                    return new BasicDBObject("password",0);
                }
            }
    )

    def show(HttpServletRequest req){
        table().findOne(req.getInt(_id),new BasicDBObject("password",0))
    }

}
