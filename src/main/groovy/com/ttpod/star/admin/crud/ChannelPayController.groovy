package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.util.MsgDigestUtil
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.admin.Web
import com.ttpod.star.model.OpType
import groovy.json.JsonSlurper
import org.apache.commons.lang.StringUtils

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.groovy.CrudClosures.Ne0
import static com.ttpod.rest.groovy.CrudClosures.Str
import static com.ttpod.rest.groovy.CrudClosures.Timestamp

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
                    def id = req[_id]
                    if (id.isNotBlank()) {
                        query.put(_id, id)
                    }
                    def client = req['client']
                    if (client.isNotBlank()) {
                        query.put("client", client)
                    }

                    def name = req['name']
                    if (name.isNotBlank()) {
                        query.put("name", name)
                    }
                    return query
                }
            })
}
