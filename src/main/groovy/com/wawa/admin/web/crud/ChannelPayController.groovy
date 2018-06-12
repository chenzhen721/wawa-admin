package com.wawa.admin.web.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.wawa.base.anno.RestWithSession
import com.wawa.base.Crud
import com.wawa.base.BaseController
import org.apache.commons.lang.StringUtils

import javax.servlet.http.HttpServletRequest

import static com.wawa.groovy.CrudClosures.Ne0
import static com.wawa.groovy.CrudClosures.Str
import static com.wawa.groovy.CrudClosures.Timestamp

/**
 * date: 13-4-19 下午2:31
 */
//@Rest
@RestWithSession
class ChannelPayController extends BaseController {

    /**
     * client:1-pc端充值,2-手机端充值
     * charge_type:1-直充,2-代充
     */
    @Delegate
    Crud crud = new Crud(adminMongo.getCollection('channel_pay'),
            [_id: Str, name: Str, comment: Str, client: Str, charge_type : Str, status: Ne0, timestamp: Timestamp],
            new Crud.QueryCondition() {
                public DBObject query(HttpServletRequest req) {
                    def query = new BasicDBObject()
                    def id = req.getParameter(_id) as String
                    if (StringUtils.isNotBlank(id)) {
                        query.put(_id, id)
                    }
                    def client = req.getParameter('client') as String
                    if (StringUtils.isNotBlank(client)) {
                        query.put("client", client)
                    }

                    def name = req.getParameter('name') as String
                    if (StringUtils.isNotBlank(name)) {
                        query.put("name", name)
                    }
                    return query
                }
            })
}
