package com.ttpod.star.admin
import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.MongoKey
import com.ttpod.rest.common.util.MsgDigestUtil
import com.ttpod.rest.web.Crud
import com.ttpod.rest.web.StaticSpring
import com.ttpod.star.admin.BaseController
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.common.util.WebUtils.$$

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