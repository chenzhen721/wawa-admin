package com.ttpod.star.admin.ext

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.common.util.MsgDigestUtil
import com.ttpod.star.admin.BaseController
import com.ttpod.star.admin.QdController
import com.ttpod.star.admin.Web
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.util.MsgDigestUtil.MD5

/**
 * 王奋凯-同步数据接口，已废弃
 */
@Rest
class UnionSyncController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(UnionSyncController.class)

    private static final String private_key = "f70ad4d24ef72336295689abbbfcc419"

    @Resource
    QdController qdController

    DBCollection table() { adminMongo.getCollection('stat_channels') }

    def reg_pay_list(HttpServletRequest req) {
        doInQd(req) { String qid ->
            def query = Web.fillTimeBetween(req).and('qd').is(qid).get()
            qdController.reg_pay_list_service(query, req)
        }
    }

    def reg_list(HttpServletRequest req) {
        doInQd(req) { String qid ->
//            qdController.reg_list_service(qid, req)
        }
    }

    def pay_list(HttpServletRequest req) {
        doInQd(req) { String qid ->
//            qdController.pay_list_service(qid, req)
        }
    }

    def pay_rate(HttpServletRequest req) {
        doInQd(req) { String qid ->

            qdController.pay_rate_service(Web.fillTimeBetween(req).and('qd').is(qid).get(), new BasicDBObject('timestamp', -1), req)
        }
    }

    def login_rate(HttpServletRequest req) {
        doInQd(req) { String qid ->
            qdController.login_rate_service(Web.fillTimeBetween(req).and('qd').is(qid).get(), new BasicDBObject('timestamp', -1), req)
        }
    }

    def login_total(HttpServletRequest req) {
        doInQd(req) { String qid ->
            qdController.login_total_service(qid, req)
        }
    }

    private doInQd(HttpServletRequest req, Closure closure) {
        def name = req.getParameter("name") as String
        if (StringUtils.isNotBlank(name)) {
            def user = req.getSession().getAttribute("union_user_${name}".toString())
            if (null == user) {
                def sign = req.getParameter("sign")
                if (StringUtils.isBlank(sign) || StringUtils.isBlank(name)) {
                    return [code: 30406, msg: "参数无效"]
                }
                if (!sign.equals(MD5.digest2HEX("${name}${private_key}".toString()))) {
                    return [code: 30419, msg: "验证失败"]
                }
                user = adminMongo.getCollection("channel_users").findOne(
                        new BasicDBObject(_id: name), new BasicDBObject('password', 0))
                if (null == user) {
                    return [code: 0, msg: '用户名无效']
                }

                if (!user['qd']) {
                    return [code: 0, msg: '权限不足']
                }
                def qd = adminMongo.getCollection("channels").findOne(user['qd'], new BasicDBObject('comment', 0))
                if (!qd) {
                    return [code: 0, msg: '权限不足']
                }
                user.put("qdinfo", qd)
                req.getSession().setAttribute("union_user_${name}".toString(), user)
            }
            if (user != null) {
                return closure.call(user['qd'])
            }
        }
        return [code: 0]
    }

}
