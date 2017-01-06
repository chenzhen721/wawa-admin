package com.ttpod.star.admin.crud
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.groovy.CrudClosures.*
/**
 * 2016/10/12
 * 用户特权列表
 */
//@Rest
@RestWithSession
class PrivilegeController extends BaseController{

    /**
     *
     ID:_id
     title:特权名称
     type:特权类型（0:功能特权 1:装扮特权）
     order:排序
     icon_url:未激活图片
     active_icon_url:激活图片
     desc:特权详情
     content_pc:特权PC介绍（富文本编辑/地址生成）
     content_h5:特权H5介绍（富文本编辑/地址生成）
     link_url:链接地址
     bg_color:背景色
     status:状态（上线/下线）
     */
    @Delegate Crud crud = new Crud(adminMongo.getCollection('privileges'),Boolean.TRUE,
            [_id:Int,title:Str,icon_url:Str, active_icon_url:Str, order:Int,status:Int,
                    type:Int,timestamp:Timestamp,desc:Str,
                    link_url:Str, bg_color:Str, content_pc:Str,content_h5:Str],
            new Crud.QueryCondition(){
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject("order",-1);
                }
            }
    )
}
