package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.IMessageCode
import com.ttpod.rest.common.util.WebUtils
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.crud.MessageController
import com.ttpod.star.common.util.ExportUtils
import com.ttpod.star.common.util.KeyUtils
import com.ttpod.star.model.*
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.math.NumberUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * 钻石的统计
 */
@Rest
//@RestWithSession
class DiamondController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(DiamondController.class)

    /**
     * 钻石流水明细
     * @param req
     */
    def logs(HttpServletRequest req){
        def userId = ServletRequestUtils.getIntParameter(req,'_id',0)
        def query = Web.fillTimeBetween(req)
        if(userId != 0){
            query.and('user_id').is(userId)
        }
        def diamond_logs = adminMongo.getCollection("diamond_logs")
        return Crud.list(req, diamond_logs, query.get(), null, SJ_DESC)
    }

    /**
     * 钻石聚合
     * @param req
     */
    def daily_stat(HttpServletRequest req){
        def query = Web.fillTimeBetween(req)
        def diamond_reports = adminMongo.getCollection('diamond_daily_stat')
        return Crud.list(req, diamond_reports, query.get(), null, SJ_DESC)
    }


}
