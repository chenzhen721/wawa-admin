package com.wawa.admin.web.doll

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.wawa.base.anno.RestWithSession
import com.wawa.base.persistent.KGS
import com.wawa.base.Crud
import com.wawa.base.BaseController
import com.wawa.api.Web
import com.wawa.common.util.KeyUtils
import com.wawa.model.CatchObserveStatus
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.wawa.common.doc.MongoKey.ALL_FIELD
import static com.wawa.common.doc.MongoKey.SJ_DESC
import static com.wawa.common.util.WebUtils.$$

/**
 *
 */
@RestWithSession
class DollController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(DollController.class)

    @Override
    DBCollection table() {
        return catchMongo.getCollection('catch_room')
    }

    DBCollection catch_observe_logs() {
        return logMongo.getCollection('catch_observe_logs')
    }

    DBCollection catch_repair_logs() {
        return logMongo.getCollection('catch_repair_logs')
    }

    DBCollection catch_room() {
        return catchMongo.getCollection('catch_room')
    }

    DBCollection toys() {
        return catchMongo.getCollection('catch_toy')
    }

    DBCollection catch_records() {
        return catchMongo.getCollection('catch_record')
    }

    @Resource
    KGS seqKGS

    /**
     * 申述列表
     * @param req
     */
    def observe_list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).get()
        def _id = ServletRequestUtils.getStringParameter(req, '_id')
        if (StringUtils.isNotBlank(_id)) {
            query.put('user_id', Integer.parseInt(_id))
        }
        def status = ServletRequestUtils.getIntParameter(req, 'status')
        if (status != null) {
            query.put('status', status)
        }
        Crud.list(req, catch_observe_logs(), query, ALL_FIELD, SJ_DESC) {List<BasicDBObject> list->
            for(BasicDBObject obj : list) {
                def record = catch_records().findOne($$(_id: obj['_id']))
                obj['toy'] = record['toy']
            }
        }
    }

    /**
     * 申述审核
     * @param req
     */
    def edit_observe(HttpServletRequest req) {
        def _id = ServletRequestUtils.getStringParameter(req, '_id')
        def status = ServletRequestUtils.getIntParameter(req, 'status')
        if (StringUtils.isBlank(_id) || status == null || status == CatchObserveStatus.未处理.ordinal()) {
            return Web.missParam()
        }
        def set = [status: status] as Map
        if (CatchObserveStatus.已处理.ordinal() == status) {
            def type = ServletRequestUtils.getIntParameter(req, 'type') //申述奖励类型 0 补娃娃 1补币
            def count = ServletRequestUtils.getIntParameter(req, 'count')
            def desc = ServletRequestUtils.getStringParameter(req, 'desc')
            if (type == null || count == null || StringUtils.isBlank(desc)) {
                return Web.missParam()
            }
            set.put('award.type', type)
            set.put('award.count', count)
            set.put('award.desc', desc)
        }

        // 补钻石或补娃娃， 运营手动添加
        if (1 == catch_observe_logs().update($$(_id: _id), $$($set: set), false, false, writeConcern).getN()) {
            return [code: 1]
        }
        return [code: 0]
    }

    /**
     * 报修列表
     */
    def repair_list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).get()
        def fid = ServletRequestUtils.getStringParameter(req, 'fid')
        if (StringUtils.isNotBlank(fid)) {
            query.put('fid', fid)
        }
        def problem = ServletRequestUtils.getStringParameter(req, 'problem')
        if (StringUtils.isNotBlank(problem)) {
            query.put('problem.'+problem, [$exists: true])
        }
        def status = ServletRequestUtils.getIntParameter(req, 'status')
        if (status != null) {
            query.put('status', status)
        }
        Crud.list(req, catch_repair_logs(), query, ALL_FIELD, $$(status: 1, timestamp: -1)) {List<BasicDBObject> list->
            for(BasicDBObject obj: list) {
                def room_id = obj['room_id'] as Integer
                def room = catch_room().findOne($$(_id: room_id), $$(name: 1))
                obj['room_name'] = room['name']
            }
        }
    }

    /**
     * 处理 status: 1 处理中 status: 2 处理完成关闭
     * @param req
     */
    def repair_edit(HttpServletRequest req) {
        def _id = ServletRequestUtils.getStringParameter(req, '_id')
        def status = ServletRequestUtils.getIntParameter(req, 'status')
        if (StringUtils.isBlank(_id) || status == null) {
            return Web.missParam()
        }
        def repair_log = catch_repair_logs().findOne($$(_id: _id))
        if (repair_log == null) {
            return [code: 34001, msg: '无此报修记录']
        }
        if (repair_log.get('status') == repair_log) {
            return [code: 1]
        }
        //不同情况下更新
        if (status == 1) {
            if (1 == catch_repair_logs().update($$(_id: _id, status: 0), $$($set: [status: status]), false, false, writeConcern).getN()) {
                return [code: 1]
            }
        }
        if (status == 2) {
            if (1 == catch_repair_logs().update($$(_id: _id), $$($set: [status: status]), false, false, writeConcern).getN()) {
                mainRedis.delete(KeyUtils.ROOM.room_repair(repair_log['room_id']))
                return [code: 1]
            }
        }
        return [code: 0]
    }



}
