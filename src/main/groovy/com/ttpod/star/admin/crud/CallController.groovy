package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.IMessageCode
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.admin.Web
import com.ttpod.star.common.util.KeyUtils
import com.ttpod.star.model.CallApply
import com.ttpod.star.model.CallIdentity
import com.ttpod.star.model.CallUserStatus
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.util.WebUtils.$$

//@RestWithSession
@Rest
class CallController extends BaseController {
    static final Logger logger = LoggerFactory.getLogger(CallController.class)

    // 用户表
    def call_users() { return callMongo.getCollection('users') }

    // 甜心审核表
    def call_stars_apply() { return callMongo.getCollection('stars_apply') }

    // 甜心表
    def call_stars() { return callMongo.getCollection('stars') }

    // 订单表
    def call_orders() { return callMongo.getCollection('orders') }

    // 提现表
    def cash_logs() { return callMongo.getCollection('cash_logs') }

    // 流水表
    def finance_logs() { return callMongo.getCollection('finance_logs') }

    static final Long BEGIN = 0L

    // 2026年
    static final Long END = 1767200461000L

    // 玩家表查询默认字段
    static
    final BasicDBObject USER_FIELD = $$('_id': 1, 'nick_name': 1, 'mobile': 1, 'mobile_bind': 1, 'timestamp': 1,
            'sex': 1, 'pic_url': 1, 'enable': 1, 'age': 1)

    // 甜心表查询默认字段
    static final BasicDBObject STAR_FIELD = $$('_id': 1, 'nick_name': 1, 'mobile': 1, 'service_time': 1, 'age': 1,'mobile_bind': 1,
            'timestamp': 1, 'price': 1, 'pic_url': 1, 'photos': 1, 'finance': 1, 'sound_url': 1, 'cash_info': 1, 'enable': 1)

    // 甜心审核表查询默认字段
    static final BasicDBObject STAR_APPLY_FIELD = $$('_id': 1, 'nick_name': 1, 'mobile': 1, 'service_time': 1,
            'timestamp': 1, 'price': 1, 'pic_url': 1, 'photos': 1, 'sound_url': 1, 'status': 1, 'user_id': 1,'sex':1,'age':1)

    // 提现日志审核查询默认字段
    static final BasicDBObject CASH_LOG_FIELD = $$('_id': 1, 'user_id': 1, 'cash_amount': 1,
            'cash_info': 1, 'timestamp': 1, 'status': 1)

    // 订单查询默认字段
    static final BasicDBObject ORDER_FIELD = $$('_id': 1, 'user': 1, 'amount': 1, 'star': 1,
            'timestamp': 1, 'status': 1, 'call_time': 1)

    static final BasicDBObject TIMESTAMP_SORT_DESC = $$('timestamp': -1)

    static final String ALL_DAY = '00:00:00-23:59:59'

    static final Long DEFAULT_PRICE = 500L

    /**
     * 用户列表
     * @param req
     */
    def user_list(HttpServletRequest req) {
        logger.debug('Received user_list params is {}', req.getParameterMap())

        def enable = req['enable']
        def nickName = req['nick_name']
        def userId = req['_id']
        def query = Web.fillTimeBetween(req).and('priv').is(CallIdentity.玩家.ordinal())
        if (StringUtils.isNotBlank(enable)) {
            query.and('enable').is(enable as Integer)
        }
        if (StringUtils.isNotBlank(userId)) {
            query.and('_id').is(userId as Integer)
        }
        if (StringUtils.isNotBlank(nickName)) {
            query.and('nick_name').is(nickName)
        }
        logger.debug('query is {}', query.get())
        Crud.list(req, call_users(), query.get(), USER_FIELD, TIMESTAMP_SORT_DESC)
    }

    /**
     * 甜心列表
     * @param req
     */
    def star_list(HttpServletRequest req) {
        logger.debug('Received star_list params is {}', req.getParameterMap())

        def enable = req['enable']
        def nickName = req['nick_name']
        def starId = req['_id']
        def mobile = req['mobile']
        def query = Web.fillTimeBetween(req).and('priv').is(CallIdentity.甜心.ordinal())

        if (StringUtils.isNotBlank(enable)) {
            query.and('enable').is(enable as Integer)
        }
        if (StringUtils.isNotBlank(starId)) {
            query.and('_id').is(starId as Integer)
        }
        if (StringUtils.isNotBlank(nickName)) {
            query.and('nick_name').is(nickName)
        }
        if (StringUtils.isNotBlank(mobile)) {
            query.and('mobile').is(mobile)
        }
        Crud.list(req, call_stars(), query.get(), STAR_FIELD, TIMESTAMP_SORT_DESC)
    }

    /**
     * 用户详情
     * @param req
     * @return
     */
    def detail(HttpServletRequest req) {
        logger.debug('Received detail params is {}', req.getParameterMap())

        def id = req['_id']
        def priv = req['priv']

        if (StringUtils.isBlank(id)) {
            return Web.missParam()
        }

        def query = $$('_id': id as Integer, 'priv': priv as Integer)

        def user = new HashMap()
        if (Integer.valueOf(priv) == CallIdentity.玩家.ordinal()) {
            user = call_users().findOne(query) as Map
        }

        if (Integer.valueOf(priv) == CallIdentity.甜心.ordinal()) {
            user = call_stars().findOne(query) as Map
        }

        logger.debug('user is {}', user)

        return [code: 1, data: user]
    }

    /**
     * 甜心申请列表
     * @param req
     */
    def apply_list(HttpServletRequest req) {
        logger.debug('Received apply_list params is {}', req.getParameterMap())

        def status = req['status']
        def nickName = req['nick_name']
        def userId = req['user_id']
        def query = Web.fillTimeBetween(req)

        if (StringUtils.isNotBlank(status)) {
            query.and('status').is(status as Integer)
        }
        if (StringUtils.isNotBlank(userId)) {
            query.and('user_id').is(userId as Integer)
        }
        if (StringUtils.isNotBlank(nickName)) {
            query.and('nick_name').is(nickName)
        }

        Crud.list(req, call_stars_apply(), query.get(), STAR_APPLY_FIELD, TIMESTAMP_SORT_DESC)
    }

    /**
     *
     * @param req
     * @return
     */
    def edit(HttpServletRequest req) {
        logger.debug('Received edit_enable params is {}', req.getParameterMap())

        def enable = req['enable']
        def userId = req['user_id']
        def mobile = req['mobile']
        def query = $$('_id', userId as Integer)
        def update_query = $$('last_modify_time': System.currentTimeMillis())
        if (StringUtils.isNotBlank(enable)) {
            update_query.append('enable', enable as Integer)
        }
        if (StringUtils.isNotBlank(mobile)) {
            update_query.append('mobile', mobile).append('mobile_bind',true)
        }

        call_users().update(query, $$('$set':update_query))
        call_stars().update(query, $$('$set':update_query))

        return [code: 1]
    }

    /**
     * 甜心审核结果
     *
     */
    def apply_results(HttpServletRequest req) {
        logger.debug('Received apply_results params is {}', req.getParameterMap())

        def result = req['result']
        def id = req['_id']
        if (StringUtils.isBlank(result) || StringUtils.isBlank(id)) {
            return Web.missParam()
        }

        def starsApply = call_stars_apply().findOne($$('_id': id, 'status': CallApply.待审批.ordinal()))
        if (starsApply == null) {
            logger.warn('user not found in call_stars_apply ...')
            return [code: 0, msg: 'user not found in call_stars_apply ...']
        }

        Integer userId = starsApply['user_id'] as Integer
        Long timestamp = System.currentTimeMillis()
        if (Integer.valueOf(result) == CallApply.审批通过.ordinal()) {
            def update_audit_query = $$('status': CallApply.审批通过.ordinal(), 'last_modify_time': timestamp)
            def update_userPriv_query = $$('priv': CallIdentity.甜心.ordinal(), 'last_modify_time': timestamp, 'nick_name': starsApply['nick_name'], 'sex': starsApply['sex'], 'pic_url': starsApply['pic_url'], 'age': starsApply['age'])
            if (call_stars_apply().update($$('_id', id), $$($set: update_audit_query)) && call_users().update($$('_id', userId), $$($set: update_userPriv_query))) {
                String token = userRedis.opsForValue().get(KeyUtils.USER.token(userId as String))
                if (token) {
                    userRedis.delete(KeyUtils.USER.token(userId as String))
                    userRedis.delete(KeyUtils.accessToken(token))
                }
                Random random = new Random()
                // 大部分甜心距离为3km-300km，小部分 3km - 900km
                Integer distance = random.nextInt(3) + 1 > 2 ? random.nextInt(300) + 3 : random.nextInt(900) + 3
                def starInfo = new HashMap()
                starInfo.putAll(starsApply as Map)
                starInfo.remove('user_id')
                starInfo.remove('status')
                starInfo.put('_id', userId)
                starInfo.put('distance', distance)
                starInfo.put('timestamp', timestamp)
                starInfo.put('price', DEFAULT_PRICE)
                starInfo.put('service_time', ALL_DAY)
                starInfo.put('priv', CallIdentity.甜心.ordinal())
                logger.debug('starInfo is {}', starInfo)
                call_stars().insert($$(starInfo))
            }
        }

        if (Integer.valueOf(result) == CallApply.审批拒绝.ordinal()) {
            def query = $$('_id', id)
            def update = $$('$set':$$('status': CallApply.审批拒绝.ordinal(), 'last_modify_time': timestamp))
            call_stars_apply().update(query, update)
        }
        return IMessageCode.OK
    }

    /**
     * 提现申请列表
     * @param req
     */
    def cash_apply_list(HttpServletRequest req) {
        logger.debug('Received cash_apply_list params is {}', req.getParameterMap())

        def status = req['status']
        def userId = req['user_id']

        def query = Web.fillTimeBetween(req)

        if (StringUtils.isNotBlank(status)) {
            query.and('status').is(status as Integer)
        }
        if (StringUtils.isNotBlank(userId)) {
            query.and('user_id').is(userId as Integer)
        }

        Crud.list(req, cash_logs(), query.get(), CASH_LOG_FIELD, TIMESTAMP_SORT_DESC)
    }

    /**
     * 提现审核结果
     * @param req
     */
    def cash_apply_results(HttpServletRequest req) {
        logger.debug('Received cash_apply_results params is {}', req.getParameterMap())

        def result = req['result']
        def id = req['_id']
        if (StringUtils.isBlank(result) || StringUtils.isBlank(id)) {
            return Web.missParam()
        }

        def query = $$('_id': id, 'status': CallApply.待审批.ordinal())
        def updateQuery = $$('last_modify_time': System.currentTimeMillis())
        if (Integer.valueOf(result) == CallApply.审批通过.ordinal()) {
            updateQuery.append('status', CallApply.审批通过.ordinal())
            cash_logs().update(query, $$('$set': updateQuery))
        }

        if (Integer.valueOf(result) == CallApply.审批拒绝.ordinal()) {
            updateQuery.append('status', CallApply.审批拒绝.ordinal())
            def cashApply = cash_logs().findAndModify(query, $$('$set', updateQuery));
            if (cashApply) {
                def amount = cashApply['cash_amount'] as Long
                def starId = cashApply['user_id'] as Integer
                call_stars().update($$('_id', starId), $$('$inc': ['finance.valid_amount': amount, 'finance.total_amount': amount]))
            }
        }
        return IMessageCode.OK
    }

    /**
     * 提现支付
     * @param req
     */
    def cash_pay(HttpServletRequest req) {
        logger.debug('Received cash_pay params is {}', req.getParameterMap())

        def id = req['_id']
        def query = $$('_id': id, 'status': CallApply.审批通过.ordinal())
        def update_query = $$('last_modify_time': System.currentTimeMillis(), 'status': CallApply.转账成功.ordinal())
        cash_logs().update(query, $$('$set': update_query))
        return IMessageCode.OK
    }

    /**
     * 订单列表
     * @param req
     */
    def order_list(HttpServletRequest req) {
        logger.debug('Received order_list params is {}', req.getParameterMap())

        def orderId = req['_id']
        def status = req['status']

        def query = Web.fillTimeBetween(req)

        if (StringUtils.isNotBlank(orderId)) {
            query.and('_id').is(orderId)
        }
        if (StringUtils.isNotBlank(status)) {
            query.and('status').is(status as Integer)
        }

        Crud.list(req, call_orders(), query.get(), null, TIMESTAMP_SORT_DESC)
    }

    /**
     * 流水列表
     * @param req
     */
    def finance_list(HttpServletRequest req) {
        logger.debug('Received finance_list parmas is {}', req.getParameterMap())

        def query = Web.fillTimeBetween(req)

        Crud.list(req, finance_logs(), query.get(), null, TIMESTAMP_SORT_DESC)
    }

}
