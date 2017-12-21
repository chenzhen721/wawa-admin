package com.ttpod.star.admin.doll

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.IMessageCode
import com.ttpod.rest.common.util.http.HttpStatusException
import com.ttpod.rest.persistent.KGS
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import com.ttpod.star.admin.Web
import com.ttpod.star.common.util.HttpClientUtils
import com.ttpod.star.common.util.JSONUtil
import com.ttpod.star.model.CatchPostChannel
import com.ttpod.star.model.CatchPostStatus
import com.ttpod.star.model.CatchPostType
import com.ttpod.star.web.api.play.Qiyiguo
import com.ttpod.star.web.api.play.dto.QiygGoodsDTO
import com.ttpod.star.web.api.play.dto.QiygOrderResultDTO
import com.ttpod.star.web.api.play.dto.QiygRespDTO
import groovy.json.JsonSlurper
import org.apache.commons.lang.StringUtils
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import java.nio.charset.Charset

import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 *
 */
@RestWithSession
class CatchuController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(CatchuController.class)

    private static final String HOST = isTest ? "https://catchu.azusasoft.com" : "https://catchu.azusasoft.com"
    private static final String VERSION = isTest ? "v0.2" : "v0.2"
    private static final String APP_ID = isTest ? "786edf5e-56c2-4d41-a415-19aa0e700548" : "786edf5e-56c2-4d41-a415-19aa0e700548"
    public static final JsonSlurper jsonSlurper = new JsonSlurper()
    public static final EntityHandler postHandler = new EntityHandler('catchu post', null)
    public static final EntityHandler putHandler = new EntityHandler('catchu put', null)

    static class EntityHandler extends HttpClientUtils.StringHttpEntityHandler {
        EntityHandler(String name, Charset charset) {
            super(name, charset)
        }

        @Override
        String handleResponse(HttpResponse response) throws IOException {
            int code = response.getStatusLine().getStatusCode()
            if(code != HttpStatus.SC_OK && code != HttpStatus.SC_CREATED){
                throw new HttpStatusException(code,response.toString())
            }
            return handle(response.getEntity())
        }
    }

    @Override
    DBCollection table() {
        return catchMongo.getCollection('catch_room')
    }

    DBCollection toys() {
        return catchMongo.getCollection('catch_toy')
    }

    DBCollection catch_records() {
        return catchMongo.getCollection('catch_record')
    }

    DBCollection catch_success_logs() {
        return logMongo.getCollection('catch_success_logs')
    }

    DBCollection apply_post_logs() {
        return logMongo.getCollection('apply_post_logs')
    }

    @Resource
    KGS seqKGS

    /**
     * 房间列表
     * @param req
     * @return
     */
    def list(HttpServletRequest req) {
        logger.debug('========================>this is a test!!!')
        def query = Web.fillTimeBetween(req)
        intQuery(query, req, "_id")//房间ID
        stringQuery(query, req, "fid")//对应娃娃机ID
        Crud.list(req, table(), query.get(), ALL_FIELD, $$(order: 1, type: -1, timestamp: -1))
    }

    /**
     * 添加房间
     * @param req
     * @return
     */
    def add(HttpServletRequest req) {
        def _id = seqKGS.nextId()
        def fid = ServletRequestUtils.getStringParameter(req, 'fid', '')
        //一个远程房间只能创建一次
        /*if (StringUtils.isNotBlank(fid)) {
            def room = table().findOne($$(fid: fid))
            if (room != null) {
                return [code: 0]
            }
        }*/
        def toy_id = ServletRequestUtils.getIntParameter(req, 'toy_id')
        def toyItem = toys().findOne(toy_id)
        if (toyItem == null) {
            return [code: 0]
        }
        def name = ServletRequestUtils.getStringParameter(req, 'name')
        def type = ServletRequestUtils.getBooleanParameter(req, 'type', true) //是否备货中
        def online = ServletRequestUtils.getBooleanParameter(req, 'online', true) //是否下架
        def pic = ServletRequestUtils.getStringParameter(req, 'pic') //房间图片
        def price = ServletRequestUtils.getIntParameter(req, 'price')
        def desc = ServletRequestUtils.getStringParameter(req, 'desc', '')
        def partner = ServletRequestUtils.getIntParameter(req, 'partner', 1) //合作商户 0 catchu 1 奇异果
        def order = ServletRequestUtils.getIntParameter(req, 'order', 0) //合作商户 0 catchu 1 奇异果
        def winrate = ServletRequestUtils.getIntParameter(req, 'winrate', 25) //25中1
        def playtime = ServletRequestUtils.getIntParameter(req, 'playtime', 40) //40s
        def device_type = ServletRequestUtils.getIntParameter(req, 'device_type', 0) //设备类型 0主板型 1PC型 2即构
        def timestamp = new Date().getTime()

        if (StringUtils.isBlank(name) || type == null || StringUtils.isBlank(pic) || price == null || toy_id == null) {
            return [code: 0]
        }
        def map = [_id: _id, toy_id: toy_id, name: name, type: type, partner: partner, online: online, pic: pic, price: price, desc: desc, order: order, device_type: device_type, timestamp: timestamp]
        if (fid != null) {
            map.put('fid', fid)
        }
        if (StringUtils.isNotBlank(fid)) {
            if (winrate < 1|| winrate > 888) {
                return [code: 30406]
            }
            QiygRespDTO respDTO = Qiyiguo.winning_rate(fid, winrate)
            if (respDTO == null || !respDTO.getDone()) {
                logger.error('change winning rate fail.' + fid + ' to: ' + winrate)
                return [code: 30404]
            }
            map.put('winrate', winrate)
            if (playtime < 5|| playtime > 60) {
                return [code: 30407]
            }
            respDTO = Qiyiguo.playtime(fid, playtime)
            if (respDTO == null || !respDTO.getDone()) {
                logger.error('change playtime fail.' + fid + ' to: ' + playtime)
                return [code: 30405]
            }
            map.put('playtime', playtime)
        }
        if(table().save(new BasicDBObject(map)).getN() == 1){
            Crud.opLog(table().getName() + "_add", map)
        }
        return IMessageCode.OK
    }

    /**
     * 编辑房间
     * @param req
     * @return
     */
    def edit(HttpServletRequest req) {
        def _id = ServletRequestUtils.getIntParameter(req, '_id')
        if (_id == null) {
            return [code: 0]
        }
        def room = table().findOne(_id)
        if (room == null) {
            return [code: 30402]
        }
        def map = [:]
        def name = ServletRequestUtils.getStringParameter(req, 'name')
        if (StringUtils.isNotBlank(name)) {
            map.put('name', name)
        }
        def fid = ServletRequestUtils.getStringParameter(req, 'fid')
        if (StringUtils.isNotBlank(fid)) {
            map.put('fid', fid)
        }
        def type = ServletRequestUtils.getBooleanParameter(req, 'type') //是否备货中
        if (type != null) {
            map.put('type', type)
        }
        def device_type = ServletRequestUtils.getIntParameter(req, 'device_type') //是否备货中
        if (device_type != null) {
            map.put('device_type', device_type)
        }
        def online = ServletRequestUtils.getBooleanParameter(req, 'online') //是否上架
        if (online != null) {
            map.put('online', online)
        }
        def pic = ServletRequestUtils.getStringParameter(req, 'pic') //房间图片
        if (StringUtils.isNotBlank(pic)) {
            map.put('pic', pic)
        }
        def price = ServletRequestUtils.getIntParameter(req, 'price')
        if (price != null) {
            map.put('price', price)
        }
        def desc = ServletRequestUtils.getStringParameter(req, 'desc')
        if (StringUtils.isNotBlank(desc)) {
            map.put('desc', desc)
        }
        def partner = ServletRequestUtils.getIntParameter(req, 'partner')
        if (partner != null) {
            map.put('partner', partner)
        }
        def toyId = ServletRequestUtils.getIntParameter(req, 'toy_id')
        def order = ServletRequestUtils.getIntParameter(req, 'order')
        if (order != null) {
            map.put('order', order)
        }
        if (toyId != null && toyId != (room['toy_id'] as Integer)) {
            map.put('toy_id', toyId)
            def toyItem = toys().findOne(toyId)
            if (toyItem == null) {
                return [code: 30401]
            }
        }
        def rec = table().findOne($$(_id: _id))
        if (rec['online'] == Boolean.TRUE && (online == null || online == Boolean.TRUE)) {
            def winrate = ServletRequestUtils.getIntParameter(req, 'winrate', 25) //25中1
            def playtime = ServletRequestUtils.getIntParameter(req, 'playtime', 40) //40s

            if (rec == null) {
                return [code: 30400]
            }

            logger.info('rec result: ' + rec.toString())
            logger.info('winrate result: ' + (rec['fid'] != null && winrate != null && winrate != rec['winrate']))
            logger.info('fid result: ' + (rec['fid'] != null))
            logger.info('winrate: ' + winrate)
            logger.info('rec result: ' + (winrate != rec['winrate']))
            if (rec['fid'] != null && winrate != rec['winrate']) {
                def device_id = rec['fid'] as String
                if (winrate < 1 || winrate > 888) {
                    return [code: 30406]
                }
                QiygRespDTO respDTO = Qiyiguo.winning_rate(device_id, winrate)
                logger.info('respdto: ' + respDTO)
                if (respDTO == null || !respDTO.getDone()) {
                    logger.error('change winning rate fail.' + device_id + ' to: ' + winrate)
                    return [code: 30404]
                }
                map.put('winrate', winrate)
            }
            if (rec['fid'] != null && playtime != rec['playtime']) {
                def device_id = rec['fid'] as String
                if (playtime < 5 || playtime > 60) {
                    return [code: 30407]
                }
                def respDTO = Qiyiguo.playtime(device_id, playtime)
                if (respDTO == null || !respDTO.getDone()) {
                    logger.error('change playtime fail.' + device_id + ' to: ' + playtime)
                    return [code: 30405]
                }
                map.put('playtime', playtime)
            }
        }
        if(table().update($$(_id: _id), $$($set: map)).getN() == 1) {
            Crud.opLog(table().getName() + "_edit", map)
        }
        return IMessageCode.OK
    }

    /**
     * 商品列表
     * @param req
     * @return
     */
    def toy_list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        Crud.list(req, toys(), query.get(), ALL_FIELD, SJ_DESC)
    }

    /**
     * 商品添加
     * @param req
     */
    def toy_add(HttpServletRequest req) {
        def _id = seqKGS.nextId()
        def name = ServletRequestUtils.getStringParameter(req, 'name')
        def type = ServletRequestUtils.getBooleanParameter(req, 'type', true) //是否可用
        def pic = ServletRequestUtils.getStringParameter(req, 'pic') //图片
        def head_pic = ServletRequestUtils.getStringParameter(req, 'head_pic') //缩略图
        def desc = ServletRequestUtils.getStringParameter(req, 'desc', '') //描述
        def tid = ServletRequestUtils.getStringParameter(req, 'tid') //
        def total_stock = ServletRequestUtils.getIntParameter(req, 'stock', 0) //总库存， stock 发货库存
        def timestamp = new Date().getTime()
        if (toys().count($$(_id: _id)) > 0) {
            return [code: 0]
        }
        def stock = [stock: total_stock, count: 0, total: total_stock, timestamp: System.currentTimeMillis()]
        def map = [_id: _id, name: name, type: type, tid: tid, stock: stock, pic: pic, head_pic: head_pic, desc: desc, timestamp: timestamp]
        if(toys().save(new BasicDBObject(map)).getN() == 1){
            Crud.opLog(toys().getName() + "_add", map)
            return [code: 1]
        }
        return [code: 0]
    }

    /**
     * 商品编辑
     * @param req
     */
    def toy_edit(HttpServletRequest req) {
        def _id = ServletRequestUtils.getIntParameter(req, '_id')
        if (_id == null) {
            return [code: 0]
        }
        def toy = toys().findOne(_id)
        if (toy == null) {
            return toy
        }
        def map = [:]
        def name = ServletRequestUtils.getStringParameter(req, 'name')
        if (StringUtils.isNotBlank(name)) {
            map.put('name', name)
        }
        def type = ServletRequestUtils.getBooleanParameter(req, 'type') //是否开放
        if (type != null) {
            map.put('type', type as String)
        }
        def pic = ServletRequestUtils.getStringParameter(req, 'pic') //房间图片
        def head_pic = ServletRequestUtils.getStringParameter(req, 'head_pic') //缩略图
        if (StringUtils.isNotBlank(pic)) {
            map.put('pic', pic)
        }
        if (StringUtils.isNotBlank(head_pic)) {
            map.put('head_pic', head_pic)
        }
        def desc = ServletRequestUtils.getStringParameter(req, 'desc')
        if (StringUtils.isNotBlank(desc)) {
            map.put('desc', desc)
        }
        def tid = ServletRequestUtils.getStringParameter(req, 'tid')
        if (StringUtils.isNotBlank(tid)) {
            map.put('tid', tid)
        }
        if(toys().update($$(_id: _id), new BasicDBObject($set: map)).getN() == 1){
            Crud.opLog(toys().getName() + "_edit", map)
            return [code: 1]
        }
        return [code: 0]
    }

    /**
     * 娃娃补库存
     * [total_stock: total_stock, stock: total_stock, count: 0, total: total_stock]
     */
    def toy_stock_add(HttpServletRequest req) {
        def _id = ServletRequestUtils.getIntParameter(req, '_id')
        def total_stock = ServletRequestUtils.getIntParameter(req, 'stock')
        if (_id == null || total_stock == null) {
            return Web.missParam()
        }
        def inc = [stock: [stock: total_stock, total: total_stock]]
        if (toys().update($$(_id: _id), $$($inc: $$("stock.stock", total_stock).append("stock.total", total_stock), $set: ['stock.timestamp': System.currentTimeMillis()])).getN() == 1) {
            Crud.opLog(toys().getName() + "_stock_add", inc)
            return [code: 1]
        }
        return [code: 0]
    }

    /**
     * API 获取房间详情
     * status表示房间状态， null表示房间未打开， "None"或者"Prepare"表示设备启动中， "Ready","Play","
     * Error",分别表示 准备就绪（⽆⼈）、游戏中、出错
     * @param roomId
     * @return
     */
    /*static Map room_detail(String roomId) {
        if (roomId == null) {
            return null
        }
        def url = "${HOST}/open_api/${VERSION}/rooms/${roomId}?app_id=${APP_ID}&room_id=${roomId}".toString()
        try {
            String value = null
            if (url.startsWith('http://')) {
                value = HttpClientUtils.get(url, null)
            } else if (url.startsWith('https://')) {
                value = HttpsClientUtils.get(url, null)
            }
            if (StringUtils.isNotBlank(value)) {
                def result = jsonSlurper.parseText(value)
                return result['room'] as Map
            }
        } catch (Exception e) {
            logger.error("Get ${url} error.", e)
        }
        return null
    }*/

    /**
     * 商品列表
     * @param req
     * @return
     */
    /*def toys_list(HttpServletRequest req) {
        def page = Web.getPage(req)
        def size = Web.getPageSize(req)
        def url = "${HOST}/open_api/${VERSION}/toys?app_id=${APP_ID}&limit=${size}&page=${page}".toString()
        String value = null
        if (url.startsWith('http://')) {
            value = HttpClientUtils.get(url, null)
        } else if (url.startsWith('https://')) {
            value = HttpsClientUtils.get(url, null)
        }
        if (StringUtils.isBlank(value)) {
            return [code: 0]
        }
        def result = jsonSlurper.parseText(value)
        return [code: 1, data: result]
    }*/

    /**
     * 商品详情
     * @param req
     * @return
     */
    /*Map toy(Integer toyId) {
        def url = "${HOST}/open_api/${VERSION}/toys/${toyId}?app_id=${APP_ID}&toy_id=${toyId}".toString()
        String value = null
        if (url.startsWith('http://')) {
            value = HttpClientUtils.get(url, null)
        } else if (url.startsWith('https://')) {
            value = HttpsClientUtils.get(url, null)
        }
        if (StringUtils.isBlank(value)) {
            return null
        }
        return jsonSlurper.parseText(value) as Map
    }*/

    /**
     * 修改房间信息
     * @param req
     *//*
    def edit(HttpServletRequest req) {
        def roomId = Web.roomId(req)
        def url = "${HOST}/open_api/${VERSION}/rooms/${roomId}".toString()
        def params = [app_id: APP_ID, room_id: roomId] as Map
        if (req['price'] != null) {
            params.put('price', req['price'])
        }
        if (req['autostart'] != null) {
            params.put('autostart', req['autostart'])
        }
        if (req['stock'] != null) {
            params.put('stock', req['stock'])
        }
        if (req['warning_stock'] != null) {
            params.put('warning_stock', req['warning_stock'])
        }
        String value = HttpClientUtils.put(url, params, null)
        if (StringUtils.isBlank(value)) {
            return [code: Code.ERROR]
        }
        return [code: 1, data: jsonSlurper.parseText(value)]
    }

    *//**
     * 创建商品
     * @param req
     */
    /*def add_toy(Map props) {
        def url = "${HOST}/open_api/${VERSION}/toys".toString()
        def params = [app_id: APP_ID] as Map
        if (props['name'] != null) {
            params.put('name', props['name'])
        }
        if (props['url'] != null) {
            params.put('url', props['url'])
        }
        if (props['price'] != null) {
            params.put('price', props['price'])
        }
        if (props['desc'] != null) {
            params.put('desc', props['desc'])
        }
        String value = null
        if (url.startsWith('http://')) {
            value = HttpClientUtils.post(url, params, null, postHandler)
        } else if (url.startsWith('https://')) {
            value = HttpsClientUtils.post(url, params, null, postHandler)
        }
        if (StringUtils.isBlank(value)) {
            return null
        }
        return jsonSlurper.parseText(value) as Map
    }*/

    /**
     * 修改商品
     * @param req
     */
    /*def edit_toy(Integer toyId, Map props) {
        def url = "${HOST}/open_api/${VERSION}/toys/${toyId}".toString()
        def params = [app_id: APP_ID] as Map
        if (props['name'] != null) {
            params.put('name', props['name'])
        }
        if (props['url'] != null) {
            params.put('url', props['url'])
        }
        if (props['price'] != null) {
            params.put('price', props['price'])
        }
        if (props['desc'] != null) {
            params.put('desc', props['desc'])
        }
        String value = null
        if (url.startsWith('http://')) {
            value = HttpClientUtils.put(url, params, null, putHandler)
        } else if (url.startsWith('https://')) {
            value = HttpsClientUtils.put(url, params, null, putHandler)
        }
        if (StringUtils.isBlank(value)) {
            return null
        }
        return jsonSlurper.parseText(value) as Map
    }*/

    /**
     * 绑定商品
     * @param req
     * @return
     */
    /*def bind_toy(String roomId, Integer toyId) {
        def url = "${HOST}/open_api/${VERSION}/rooms/${roomId}/bind/${toyId}".toString()
        def params = [app_id: APP_ID, room_id: roomId as String, toy_id: toyId as String] as Map
        String value = null
        if (url.startsWith('http://')) {
            value = HttpClientUtils.put(url, params, null, putHandler)
        } else if (url.startsWith('https://')) {
            value = HttpsClientUtils.put(url, params, null, putHandler)
        }
        if (StringUtils.isBlank(value)) {
            return null
        }
        return jsonSlurper.parseText(value) as Map
    }*/

    /**
     * 关闭服务器
     * @param req
     * @return
     *//*
    def stop(HttpServletRequest req) {
        def roomId = Web.roomId(req)
        def url = "${HOST}/open_api/${VERSION}/rooms/${roomId}/stop?app_id=${APP_ID}&room_id=${roomId}".toString()
        String value = HttpClientUtils.get(url, null)
        if (StringUtils.isBlank(value)) {
            return [code: Code.ERROR]
        }
        return [code: 1, data: jsonSlurper.parseText(value)]
    }

    *//**
     * 重置库存
     * @param req
     *//*
    def reset_stock(HttpServletRequest req) {
        def roomId = Web.roomId(req)
        def url = "${HOST}/open_api/${VERSION}/rooms/${roomId}/reset_stock".toString()
        def params = [app_id: APP_ID, room_id: roomId] as Map
        String value = HttpClientUtils.put(url, params, null)
        if (StringUtils.isBlank(value)) {
            return [code: Code.ERROR]
        }
        return [code: 1, data: jsonSlurper.parseText(value)]
    }*/

    /**
     * 根据 房间、用户查询
     * 游戏记录流水
     */
    def record_list(HttpServletRequest req) {
        def user_id = ServletRequestUtils.getIntParameter(req, 'user_id')
        def room_id = ServletRequestUtils.getIntParameter(req, 'room_id')
        def status = ServletRequestUtils.getBooleanParameter(req, 'status') //是否抓中
        def type = ServletRequestUtils.getIntParameter(req, 'type') //是否结束
        def _id = ServletRequestUtils.getStringParameter(req, '_id')
        def query = Web.fillTimeBetween(req).get()

        if (user_id != null) {
            query.put('user_id', user_id)
        }
        if (room_id != null) {
            query.put('room_id', room_id)
        }
        if (status != null) {
            query.put('status', status)
        }
        if (type) {
            query.put('type', type)
        }
        if (StringUtils.isNotBlank(_id)) {
            query.put('_id', _id)
        }

        Crud.list(req, catch_records(), query, $$(coin_record: 0, play_record: 0), SJ_DESC) { List<BasicDBObject> list->
            if (type == 0) {
                for (BasicDBObject obj: list) {
                    obj['user'] = users().findOne(obj['user_id'] as Integer, $$(nick_name: 1, pic: 1))
                }
            }
        }
    }

    /**
     * 成功记录
     */
    def success_list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).get()
        def _id = ServletRequestUtils.getStringParameter(req, '_id')
        if (StringUtils.isNotBlank(_id)) {
            query.put('_id', _id)
        }
        def user_id = ServletRequestUtils.getIntParameter(req, 'user_id')
        if (user_id != null) {
            query.put('user_id', user_id)
        }
        def room_id = ServletRequestUtils.getIntParameter(req, 'room_id')
        if (room_id != null) {
            query.put('room_id', room_id)
        }
        def post_type = ServletRequestUtils.getIntParameter(req, 'post_type')
        if (post_type != null) {
            query.put('post_type', post_type)
        }
        //客户端是否显示此记录，是否删除 true 删除 无字段或false正常
        def is_delete = ServletRequestUtils.getBooleanParameter(req, 'is_delete')
        if (is_delete == null || !is_delete) {
            query.put('is_delete', [$ne: true])
        } else {
            query.put('is_delete', is_delete)
        }
        Crud.list(req, catch_success_logs(), query, ALL_FIELD, SJ_DESC)
    }

    def success_record_add(HttpServletRequest req) {
        def _id = ServletRequestUtils.getStringParameter(req, '_id')
        def records = catch_records().findOne($$(_id: _id))
        if (records == null) {
            return Web.missParam()
        }
        def success_log = $$(_id: '' + records['_id'] + '_supplement',
                room_id: records['room_id'],
                user_id: records['user_id'],
                toy: records['toy'],
                post_type: CatchPostType.未处理.ordinal(),
                coin: records['coin'],
                timestamp: records['timestamp'],
                replay_url: records['replay_url'],
                goods_id: records['goods_id'],
                relative_record: _id //对应的补单记录
        )
        catch_success_logs().save(success_log)
        Crud.opLog(catch_success_logs().getName() + '_success_record_add', success_log)
        return [code: 1]
    }

    /**
     * 异常回退
     */
    def success_record_refuse(HttpServletRequest req) {
        def _id = ServletRequestUtils.getStringParameter(req, '_id')
        if (StringUtils.isBlank(_id)) {
            return Web.missParam()
        }
        def desc = ServletRequestUtils.getStringParameter(req, 'desc', '')//简述
        //如果逻辑删除这条记录，需要把对应的快递申请回退
        def post_log = apply_post_logs().findOne($$('toys.record_id': _id, is_delete: [$ne: true]))
        if (post_log != null) {
            apply_post_logs().update($$(_id: post_log['_id']), $$($set: [is_delete: true, status: 2, desc: desc]))
            def toys = post_log['toys'] as List
            if (toys != null && toys.size() > 0) {
                toys.each { BasicDBObject toy ->
                    def r_id = toy['record_id'] as String
                    //正常抓取的记录还原
                    if (r_id != _id) {
                        catch_success_logs().update($$(_id: r_id), $$($set: [post_type: 0], $unset: [pack_id: 1, apply_time: 1]))
                    }
                }
            }
        }
        if (1 == catch_success_logs().update($$(_id: _id, is_delete: [$ne: true]), $$($set: [is_delete: true]), false, false, writeConcern).getN()) {
            Crud.opLog(catch_success_logs().getName() + '_success_record_refuse', [is_delete: true])
            return [code: 1]
        }
        return [code: 0]
    }

    /**
     * 发货清单
     * @param req
     * @return
     */
    def post_list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req).get()
        def _id = ServletRequestUtils.getStringParameter(req, '_id')
        if (StringUtils.isNotBlank(_id)) {
            query.put('_id', _id)
        }
        def user_id = ServletRequestUtils.getIntParameter(req, 'user_id')
        if (user_id != null) {
            query.put('user_id', user_id)
        }
        def room_id = ServletRequestUtils.getIntParameter(req, 'room_id')
        if (room_id != null) {
            query.put('room_id', room_id)
        }
        //发货通过这个状态来判断 审核状态：0, 未审核 1, 通过 2,未通过
        def status = ServletRequestUtils.getIntParameter(req, 'status')
        if (status != null) {
            query.put('status', status)
        }
        //客户端是否显示此订单，是否删除 true 删除 无字段或false正常
        def is_delete = ServletRequestUtils.getBooleanParameter(req, 'is_delete')
        if (is_delete == null || !is_delete) {
            query.put('is_delete', [$ne: true])
        } else {
            query.put('is_delete', is_delete)
        }
        //正常情况下的，邮寄状态 0,未处理, 1待发货, 2已发货, 3已同步订单
        def post_type = ServletRequestUtils.getIntParameter(req, 'post_type')
        if (post_type != null) {
            query.put('post_type', post_type)
        }
        def order_id = ServletRequestUtils.getStringParameter(req, 'order_id')
        if (StringUtils.isNotBlank(order_id)) {
            query.put('order_id', order_id)
        }
        def record_id = ServletRequestUtils.getStringParameter(req, 'record_id')
        if (StringUtils.isNotBlank(record_id)) {
            query.put('toys.record_id', record_id)
        }
        def channel = ServletRequestUtils.getIntParameter(req, 'channel')
        if (channel != null) {
            query.put('channel', channel)
        }
        Crud.list(req, apply_post_logs(), query, ALL_FIELD, SJ_DESC)
    }

    /**
     * 批量拒绝订单
     * @param req
     * @return
     */
    def batch_refuse(HttpServletRequest req) {
        def ids = ServletRequestUtils.getStringParameter(req, 'ids')
        if (StringUtils.isBlank(ids)) {
            return Web.missParam()
        }
        def postIdList = ids.split(',')?.collect{it as String}
        if (postIdList == null || postIdList.size() <= 0) {
            return Web.missParam()
        }
        def is_delete = ServletRequestUtils.getBooleanParameter(req, 'is_delete', false)
        def desc = ServletRequestUtils.getStringParameter(req, 'desc', '')//简述
        //更新成功
        def query = $$(_id: [$in: postIdList], post_type: CatchPostType.待发货.ordinal(), status: CatchPostStatus.未审核.ordinal())
        def set = [status: CatchPostStatus.审核失败.ordinal(), is_delete: is_delete, desc: desc]
        if (1 <= apply_post_logs().update(query, $$($set: set), false, true, writeConcern).getN()) {
            //def list = catch_records().find($$(pack_id: [$in: packIdList]), $$(toy: 1, address: 1)).sort($$(pack_id: 1, timestamp: -1)).toArray()
            Crud.opLog(catch_records().getName() + '_batch_post', set)
            return [code: 1]
        }
        //有不符合条件的记录
        return [code: 0]
    }

    /**
     * 修改快递信息
     * @param req
     * @return
     */
    def edit_post_info(HttpServletRequest req) {
        //影响快递单号查询
        def _id = ServletRequestUtils.getStringParameter(req, '_id')
        if (StringUtils.isBlank(_id)) {
            return Web.missParam()
        }
        def shipping_no = ServletRequestUtils.getStringParameter(req, 'shipping_no')
        def shipping_com = ServletRequestUtils.getStringParameter(req, 'shipping_com')
        def shipping_name = ServletRequestUtils.getStringParameter(req, 'shipping_name')
        def query = new BasicDBObject(_id: _id)
        def set = new BasicDBObject()
        if (StringUtils.isNotBlank(shipping_no)) {
            set.put('shipping_no', shipping_no)
        }
        if (StringUtils.isNotBlank(shipping_com)) {
            set.put('shipping_com', shipping_com)
        }
        if (StringUtils.isNotBlank(shipping_name)) {
            set.put('shipping_name', shipping_name)
        }
        if (1 == apply_post_logs().update(query, $$($set: set), false, false, writeConcern).getN()) {
            Crud.opLog(apply_post_logs().getName() + '_edit_post_info', set)
            return [code: 1]
        }
        return [code: 0]
    }

    /**
     * 包裹拆单
     */
    def post_unbox(HttpServletRequest req) {
        def _id = ServletRequestUtils.getStringParameter(req, '_id')
        if (StringUtils.isBlank(_id)) {
            return Web.missParam()
        }
        def desc = ServletRequestUtils.getStringParameter(req, 'desc', '')//简述
        //如果逻辑删除这条记录，需要把对应的快递申请回退
        def post_log = apply_post_logs().findOne($$(_id: _id, post_type: [$ne: CatchPostType.已同步订单.ordinal()], is_delete: [$ne: true], is_pay_postage: [$ne: true]))
        if (post_log != null) {
            apply_post_logs().update($$(_id: post_log['_id']), $$($set: [is_delete: true, status: 2, desc: desc]))
            def toys = post_log['toys'] as List
            if (toys != null && toys.size() > 0) {
                toys.each { BasicDBObject toy ->
                    def r_id = toy['record_id'] as String
                    //记录还原
                    catch_success_logs().update($$(_id: r_id), $$($set: [post_type: 0], $unset: [pack_id: 1, apply_time: 1]))
                }
            }
            if (1 == apply_post_logs().update($$(_id: _id, is_delete: [$ne: true]), $$($set: [is_delete: true]), false, false, writeConcern).getN()) {
                Crud.opLog(apply_post_logs().getName() + '_post_unbox', [is_delete: true])
                return [code: 1]
            }
        }
        return [code: 0]
    }

    /**
     * 已发货
     * @param req
     * @return
     */
    def batch_post(HttpServletRequest req) {
        def ids = ServletRequestUtils.getStringParameter(req, 'ids')
        if (StringUtils.isBlank(ids)) {
            return Web.missParam()
        }
        def postIdList = ids.split(',')?.collect{it as String}
        if (postIdList == null || postIdList.size() <= 0) {
            return Web.missParam()
        }

        //更新成功
        def query = $$(_id: [$in: postIdList], post_type: CatchPostType.待发货.ordinal(), is_delete: [$ne: true], status: CatchPostStatus.未审核.ordinal())
        def set = [post_type: CatchPostType.已发货.ordinal(), status: CatchPostStatus.审核通过.ordinal(), is_pay_postage: [$ne: false]]
        if (1 <= apply_post_logs().update(query, $$($set: set), false, true, writeConcern).getN()) {
            Crud.opLog(catch_records().getName() + '_batch_post', set)
            return [code: 1]
        }
        //有不符合条件的记录
        return [code: 0]
    }

    /**
     * 推送订单
     * @param req
     */
    def push_order(HttpServletRequest req) {
        def start = Web.getStime(req)
        def end = Web.getEtime(req)
        def timestamp = [:]
        if (start != null) {
            timestamp.put('$gte', start)
        }
        if (end != null) {
            timestamp.put('$lt', end)
        }
        def query = $$(channel: CatchPostChannel.奇异果.ordinal(), push_time: [$exists: false], status: CatchPostStatus.审核通过.ordinal(),
                is_delete: [$ne: true], post_type: CatchPostType.已发货.ordinal(), is_pay_postage: [$ne: false])
        if (timestamp.size() > 0) {
            query.put('timestamp', timestamp)
        }
        def list = []
        def missing = []
        def error = []
        apply_post_logs().find(query).limit(50).toArray().each {BasicDBObject obj ->
            def set = new BasicDBObject()
            def inc = [n: 1]
            def queryforupdate = $$(_id: obj['_id'], push_time: [$exists: false])
            if (obj['address'] != null && obj['toys'] != null) {
                def time = System.currentTimeMillis()
                //先更新后
                if (1 == apply_post_logs().update(queryforupdate, $$($set: [push_time: time]), false, false, writeConcern).getN()) {
                    //更新成功，下单
                    def userId = obj['user_id'] as Integer
                    def user = users().findOne($$(_id: userId), $$(nick_name: 1))
                    def address = obj['address']
                    def addressstr = "${address['province'] ?: ''}${address['city'] ?: ''}${address['region'] ?: ''}${address['address']}".toString()
                    def tel = address['tel'] as String
                    def name = address['name'] as String

                    def toys = obj['toys']
                    def missing_goods = [] as List
                    def goods = MapWithDefault.<Integer, Integer> newInstance(new HashMap<Integer, Integer>()) {
                        return 0
                    }
                    toys.each { BasicDBObject toy ->
                        if (toy['goods_id'] == null) {
                            logger.error('========>goods_id witch relate to toy not found, in apply_post_log by id:' + toy['goods_id'])
                            missing_goods.add(toy)
                        }
                        def goods_id = toy['goods_id'] as Integer
                        goods.put(goods_id, goods.get(goods_id) + 1)
                    }
                    if (missing_goods.size() > 0) {
                        set.put('missing_goods', missing_goods)
                    }
                    if (goods.size() > 0) {
                        List<QiygGoodsDTO> goodsList = new ArrayList<>()
                        goods.each { Integer key, Integer num ->
                            if (key != null && num > 0) {
                                goodsList.add(new QiygGoodsDTO(key, num))
                            }
                        }
                        QiygOrderResultDTO order = Qiyiguo.createOrder(userId, (user?.get('nick_name') as String ?: ''), JSONUtil.beanToJson(goodsList), addressstr, tel, name)
                        //更新订单信息至apply_post_logs
                        if (order != null) {
                            def order_id = (obj['_id'] as String) + '_' + order.getOrder_id()
                            set.put('order_id', order.getOrder_id())
                            //更新订单号
                            if (1 != apply_post_logs().update($$(_id: obj['_id'], push_time: time), $$($set: set, $inc: inc), false, false, writeConcern).getN()) {
                                missing.add(order_id)
                            } else {
                                list.add(obj['_id'])
                            }
                        } else {
                            //下单失败回退，如果回退失败记录单号
                            apply_post_logs().update($$(_id: obj['_id'], push_time: time), $$($unset: [push_time: 1], $inc: inc), false, false, writeConcern).getN()
                            error.add(obj['_id'])
                        }
                    }
                } else {
                    //更新失败，记录单号
                    error.add(obj['_id'])
                }
            }
        }
        return [code: 1, data: [succ: list, error: error, missing_order: missing]]
    }

}
