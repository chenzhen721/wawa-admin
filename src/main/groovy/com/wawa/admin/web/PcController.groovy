package com.wawa.admin.web
import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.wawa.base.BaseController
import com.wawa.base.anno.RestWithSession
import com.wawa.common.doc.MongoKey
import com.wawa.base.Crud
import com.wawa.api.Web
import org.apache.commons.lang.StringUtils

import javax.servlet.http.HttpServletRequest

import static com.wawa.common.doc.MongoKey.ALL_FIELD

/**
 * @author: jiao.li@ttpod.com
 * Date: 14-05-27 上午9:46
 */
//@Rest
@RestWithSession
class PcController extends BaseController{
    DBCollection table(){adminMongo.getCollection('config')}
    DBCollection client_logs(){logMongo.getCollection('pc_client_logs')}

    static final String PC_PARTNER_DOWNLOAD = 'pc_partner_download'
    static final String[] PC_PARTNER_DOWNLOAD_FIELDS=["version","down_url"]
    def save_download_info(HttpServletRequest req){
        String id = req[_id]
        if (StringUtils.isBlank(id))
            id =  PC_PARTNER_DOWNLOAD

        def info  = new BasicDBObject(_id,id)
        for(String field : PC_PARTNER_DOWNLOAD_FIELDS){
            info.put(field,req.getParameter(field))
        }
        info.put(timestamp,System.currentTimeMillis())
        if(table().save(info).getN() == 1){
            Crud.opLog("sys_pc_partner_download_info",info)
        }
        OK()
    }

    def show_download_info(HttpServletRequest req){
        def config = table().findOne(PC_PARTNER_DOWNLOAD)
        [code:1,data:config]
    }

    def pc_client_logs(HttpServletRequest req){
        def query = Web.fillTimeBetween(req)
        Crud.list(req, client_logs(), query.get(), ALL_FIELD, MongoKey.SJ_DESC)
    }
}