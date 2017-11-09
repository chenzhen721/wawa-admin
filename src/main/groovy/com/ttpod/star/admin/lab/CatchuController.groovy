package com.ttpod.star.admin.lab

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
import com.ttpod.star.common.util.HttpsClientUtils
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
        return labMongo.getCollection('catch_room')
    }

    DBCollection toys() {
        return labMongo.getCollection('catch_toy')
    }

    DBCollection catch_records() {
        return labMongo.getCollection('catch_record')
    }

    @Resource
    KGS seqKGS

    /**
     * 房间列表
     * @param req
     * @return
     */
    def list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        intQuery(query, req, "_id")//房间ID
        stringQuery(query, req, "fid")//对应娃娃机ID
        Crud.list(req, table(), query.get(), ALL_FIELD, $$(type: -1, timestamp: -1))
    }

    /**
     * 添加房间
     * @param req
     * @return
     */
    def add(HttpServletRequest req) {
        def _id = ServletRequestUtils.getIntParameter(req, '_id')
        def fid = ServletRequestUtils.getStringParameter(req, 'fid')
        //一个远程房间只能创建一次
        def room = table().findOne($$(fid: fid))
        if (room != null) {
            return [code: 0]
        }
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
        def partner = ServletRequestUtils.getIntParameter(req, 'partner', 0) //合作商户 0 catchu 1 奇异果
        def timestamp = new Date().getTime()
        if (StringUtils.isBlank(name) || fid == null || type == null || StringUtils.isBlank(pic) || price == null || toy_id == null) {
            return [code: 0]
        }
        if (partner == 0) {
            if (toy(toy_id) == null) {
                return [code: 0]
            }
        }
        def result = room_detail(fid)
        if (!result) {
            return [code: 0]
        }
        def map = [_id: _id, fid: fid, toy_id: toy_id, name: name, type: type, partner: partner, online: online, pic: pic, price: price, desc: desc, timestamp: timestamp]
        if (table().count($$(fid: fid)) > 0) {
            return [code: 0]
        }
        if (partner == 0) {
            if (bind_toy(fid, toyItem['tid'] as Integer) == null) {
                return [code: 0]
            }
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
            return [code: 0]
        }
        def map = [:]
        def name = ServletRequestUtils.getStringParameter(req, 'name')
        if (StringUtils.isNotBlank(name)) {
            map.put('name', name)
        }
        def type = ServletRequestUtils.getBooleanParameter(req, 'type') //是否备货中
        if (type != null) {
            map.put('type', type)
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
        if (toyId != null && toyId != (room['toy_id'] as Integer)) {
            map.put('toy_id', toyId)
            def toyItem = toys().findOne(toyId)
            if (toyItem == null) {
                return [code: 0]
            }
            if (partner == 0) {
                if (bind_toy(room['fid'] as String, toyItem['tid'] as Integer) == null) {
                    return [code: 0]
                }
            }
        }
        if(table().update($$(_id: _id), new BasicDBObject($set: map)).getN() == 1){
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
        def desc = ServletRequestUtils.getStringParameter(req, 'desc', '') //描述
        def timestamp = new Date().getTime()
        def props = [name: name, url: pic, price: '1', desc: desc]
        def toy = add_toy(props)
        def map = [_id: _id, tid: toy['toy']['id'], name: name, type: type, pic: pic, desc: desc, timestamp: timestamp]
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
        def props = [price:'1']
        def name = ServletRequestUtils.getStringParameter(req, 'name')
        if (StringUtils.isNotBlank(name)) {
            map.put('name', name)
            props.put('name', name)
        }
        def type = ServletRequestUtils.getBooleanParameter(req, 'type') //是否开放
        if (type != null) {
            map.put('type', type as String)
        }
        def pic = ServletRequestUtils.getStringParameter(req, 'pic') //房间图片
        if (StringUtils.isNotBlank(pic)) {
            map.put('pic', pic)
            props.put('url', pic)
        }
        def desc = ServletRequestUtils.getStringParameter(req, 'desc')
        if (StringUtils.isNotBlank(desc)) {
            map.put('desc', desc)
            props.put('desc', desc)
        }
        if (edit_toy(toy['tid'] as Integer, props) == null) {
            return [code: 0]
        }
        if(toys().update($$(_id: _id), new BasicDBObject($set: map)).getN() == 1){
            Crud.opLog(toys().getName() + "_edit", map)
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
    static Map room_detail(String roomId) {
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
    }

    /**
     * 商品列表
     * @param req
     * @return
     */
    def toys_list(HttpServletRequest req) {
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
    }

    /**
     * 商品详情
     * @param req
     * @return
     */
    Map toy(Integer toyId) {
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
    }

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
    def add_toy(Map props) {
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
    }

    /**
     * 修改商品
     * @param req
     */
    def edit_toy(Integer toyId, Map props) {
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
    }

    /**
     * 绑定商品
     * @param req
     * @return
     */
    def bind_toy(String roomId, Integer toyId) {
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
    }

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
        def status = ServletRequestUtils.getBooleanParameter(req, 'status')
        def type = ServletRequestUtils.getIntParameter(req, 'type')
        def post_type = ServletRequestUtils.getIntParameter(req, 'post_type')
        def query = new BasicDBObject()

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
        if (post_type) {
            query.put('post_type', post_type)
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
     * 发货清单
     * @param req
     * @return
     */
    def post_list(HttpServletRequest req) {
        def query = $$('pack_id', [$exists: true])
        query.putAll(Web.fillTimeBetween(req).get())
        def user_id = ServletRequestUtils.getIntParameter(req, 'user_id')
        if (user_id != null) {
            query.put('user_id', user_id)
        }
        def room_id = ServletRequestUtils.getIntParameter(req, 'room_id')
        if (room_id != null) {
            query.put('room_id', room_id)
        }
        def sort = $$(apply_time: 1, post_type: 1, 'timestamp': -1)
        def field = $$(_id: 1, user_id: 1, room_id: 1, toy: 1, timestamp: 1, pack_id: 1, post_type: 1, address: 1, apply_time: 1)
        Crud.list(req, catch_records(), query, field, sort)
    }

    /**
     * 批量通过发货
     * @param req
     * @return
     */
    def batch_post(HttpServletRequest req) {
        def ids = ServletRequestUtils.getStringParameter(req, 'ids')
        def type = ServletRequestUtils.getBooleanParameter(req, 'type')
        if (StringUtils.isBlank(ids) || type == null) {
            return Web.missParam()
        }
        def packIdList = ids.split('\\|')?.collect{it as String}
        if (packIdList == null || packIdList.size() <= 0) {
            return Web.missParam()
        }
        //更新成功
        if (1 <= catch_records().update($$(pack_id: [$in: packIdList], post_type: 1), $$($set: [post_type: type ? 2 : 4]), false, true, writeConcern).getN()) {
            def list = catch_records().find($$(pack_id: [$in: packIdList]), $$(toy: 1, address: 1)).sort($$(pack_id: 1, timestamp: -1)).toArray()
            Crud.opLog(catch_records().getName() + '_batch_post', [post_type: type ? 2 : 4])
            return [code: 1, data: list]
        }
        //有不符合条件的记录
        return [code: 0]
    }

    /**
     * 抓娃娃统计（简易版）
     * @param req
     */
    def record_stat(HttpServletRequest req) {
        //todo 时间分页
        def stime = Web.getStime(req)
        def etime = Web.getEtime(req)
    }





}
