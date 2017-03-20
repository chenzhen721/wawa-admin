package com.ttpod.star.admin.crud

import com.mongodb.DBCollection
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.admin.Web
import com.ttpod.star.model.OrderType
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.util.WebUtils.$$

@RestWithSession
class OrderController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(OrderController.class)

    def orders() { shopMongo.getCollection('orders') }

    /**
     * 订单查询
     * @param req
     */
    def list(HttpServletRequest req) {
        logger.debug('Received order list params is {}', req.getParameterMap())
        def orderId = req['_id']
        def userId = ServletRequestUtils.getIntParameter(req, 'user_id', 0)
        def status = ServletRequestUtils.getStringParameter(req, 'status', '')


        def query = Web.fillTimeBetween(req)
        if (userId != 0) {
            query.and('user_id').is(userId)
        }
        if (StringUtils.isNotBlank(orderId)) {
            query.and('_id').is(orderId)
        }

        if (StringUtils.isNotBlank(status)) {
            query.and('status').is(status as Integer)
        }

        Crud.list(req, orders(), query.get(), null, $$('timestamp': -1))
    }

    /**
     * 订单完成
     * @param req
     * @return
     */
    def finish(HttpServletRequest req) {
        logger.debug('Received order edit params is {}', req.getParameterMap())
        def orderId = req['_id']
         if (StringUtils.isBlank(orderId)) {
            return Web.missParam()
        }
        def query = $$('_id': orderId, 'status': OrderType.已发货.ordinal())
        def update = $$('status': OrderType.完成.ordinal(), 'last_modify': System.currentTimeMillis())
        if (orders().update(query, update).getN() != 1) {
            return [code: 0]
        }
        return [code: 1]
    }

}
