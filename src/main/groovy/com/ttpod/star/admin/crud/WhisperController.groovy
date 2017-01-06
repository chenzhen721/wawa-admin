package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.admin.Web
import com.ttpod.star.model.OpType
import org.apache.commons.lang.StringUtils

import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC

/**
 * date: 13-10-11 下午2:31
 * @author: haigen.xiong@ttpod.com
 */
@RestWithSession
class WhisperController extends BaseController {


    def list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        intQuery(query, req, "from_id")
        intQuery(query, req, "to_id")
        intQuery(query, req, "room_id")

        def msg = req.getParameter("msg");
        if (StringUtils.isNotBlank(msg)) {
            Pattern pattern = Pattern.compile("^.*" + msg + ".*\$", Pattern.CASE_INSENSITIVE)
            query.and("msg").regex(pattern)
        }

        Crud.list(req, adminMongo.getCollection("rooms_whisper"), query.get(), ALL_FIELD, SJ_DESC)
    }




    def del(HttpServletRequest req) {

        String id = req[_id]

        adminMongo.getCollection('rooms_whisper').remove(new BasicDBObject(_id, id))

        Crud.opLog(OpType.blacklist_del, [del: id])
        OK()
    }


}
