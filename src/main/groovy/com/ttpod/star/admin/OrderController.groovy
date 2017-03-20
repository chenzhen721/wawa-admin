package com.ttpod.star.admin

import com.mongodb.DBCollection
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.util.WebUtils.$$

@Rest
class OrderController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(OrderController.class)

    DBCollection orders() { shopMongo.getCollection('orders') }

    /**
     * 订单查询
     * @param req
     */
    def list(HttpServletRequest req) {
        logger.debug('Received order list params is {}', req.getParameterMap())
        def userId = req['user_id']
        def orderId = req['_id']
        def status = req['status']

        def query = Web.fillTimeBetween(req)
        if (StringUtils.isNotBlank(userId)) {
            query.and('user_id').is(userId)
        }
        if (StringUtils.isNotBlank(orderId)) {
            query.and('_id').is(orderId)
        }

        if (StringUtils.isNotBlank(status)) {
            query.and('status').is(status)
        }

        Crud.list(req, orders(), query.get(), null, $$('timestamp': -1))
    }

}
