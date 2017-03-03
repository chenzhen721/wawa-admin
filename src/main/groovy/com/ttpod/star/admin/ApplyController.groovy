package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.MongoKey
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.crud.MessageController
import com.ttpod.star.common.util.AuthCode
import com.ttpod.star.common.util.KeyUtils
import com.ttpod.star.model.*
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.ServletRequestUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * date: 13-3-28 下午2:31
 * @author: yangyang.cong@ttpod.com
 */
@Rest
class ApplyController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(ApplyController.class)

    @Resource
    StarController starController

    DBCollection table() { adminMongo.getCollection('applys') }

    DBCollection apply_codes() { adminMongo.getCollection('apply_codes') }

    //TODO 临时身份信息验证
    DBCollection apply_sf_info() { adminMongo.getCollection('apply_sf_info') }

    // 签约申请查询
    def list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        query = Web.fillTimeBetween(query, "lastmodif", req)
        ['status', 'broker', 'xy_user_id'].each { String field ->
            intQuery(query, req, field)
        }
        stringQuery(query, req, 'qq')
        intQuery(query, req, 'live_type')
        booleanQuery(query, req, 'temp')
        String admin = req['via_op']//是否运营开通
        if (admin != null && !'0'.equals(admin)) {
            query.and('via').is('Admin');
        }
        Crud.list(req, table(), query.get(), new BasicDBObject('sfz_pic', 0), MongoKey.SJ_DESC) { List<BasicDBObject> data ->
            def users = users()
            def rooms = rooms()
            for (BasicDBObject obj : data) { // 更新昵称 http://192.168.1.181/redmine/issues/3810
                obj.put("nick_name", users.findOne(obj['xy_user_id'], new BasicDBObject("nick_name", 1))?.get("nick_name"))
                def myroom = rooms.findOne(new BasicDBObject('xy_star_id', obj['xy_user_id'] as Integer))
                obj.put('test', myroom?.get("test"))
                obj.put('tags', myroom?.get("tags"))
                if (myroom?.get("address"))
                    obj.putAll(myroom?.get("address") as Map)
                //主播面试视频
                if (StringUtils.isNotEmpty(obj?.get("video_path") as String)) {
                    obj.put('video_path', UpaiController.STAR_VIDEO_DOMAIN + obj?.get("video_path"))
                }

            }
        }
    }

    @Resource
    MessageController messageController

    /**
     * 主播申请审核接口
     * @param req
     * @return
     */
    def handle(HttpServletRequest req) {
        logger.debug('Received handle params is {}', req.getParameterMap())

        def status = req.getInt('status')
        def brokerId = ServletRequestUtils.getIntParameter(req, 'broker_id', 0)
        Long time = System.currentTimeMillis()

        if (status == ApplyType.通过.ordinal() || status == ApplyType.未通过.ordinal()) {
            def query = $$(_id: req[_id], status: ApplyType.未处理.ordinal())
            def update = $$(status: status, lastmodif: time, lastmodif_user: Web.currentUser(), remark: req['remark'])
            if (brokerId != 0) {
                update.append('broker', brokerId)
            }
            def record = table().findAndModify(query, $$('$set': update))
            logger.debug('record is {},update is {}', record, update)
            if (record) {
                def user_id = record.get('xy_user_id') as Integer
                def invite_code = record.get('invite_code') as String
                if (status == ApplyType.通过.ordinal()) {
                    def sex = record.get('sex') as Integer
                    def live_type = record.get('live_type') as Integer
                    def temp = (record.get('temp') ?: Boolean.FALSE) as Boolean //手机直播临时主播
                    def tag = record.get('tag') as String
                    if (users().update(new BasicDBObject(_id, user_id),
                            new BasicDBObject($set: [priv: UserType.主播.ordinal(), star:
                                    [room_id: user_id, timestamp: time, broker: brokerId, sex: sex]
                            ]), false, false, writeConcern).getN() == 1) {
                        def user = users().findOne(new BasicDBObject(_id, user_id), new BasicDBObject(nick_name: 1, mm_no: 1))
                        String nick_name = user?.get("nick_name")
                        String province = getProvinceBySFZ(record);
                        def mm_no = user?.get('mm_no') ?: user_id
                        if (rooms().update($$(_id, user_id), $$($set: [xy_star_id  : user_id, live: Boolean.FALSE, bean: 0, visiter_count: 0, found_time: time, broker_id: brokerId,
                                                                       room_ids    : mm_no.toString(), nick_name: nick_name, real_sex: sex, "address.province": province, type: RoomType.主播.ordinal()
                                                                       , mic_switch: "true", live_type: live_type, apply_type: live_type, temp: temp]), true, false, writeConcern).getN() == 1) {
                            if (brokerId != 0) {
                                users().update(new BasicDBObject(_id, brokerId),
                                        new BasicDBObject($addToSet: ['broker.stars': user_id], $inc: ['broker.star_total': 1]))
                            }
                            if (StringUtils.isNotEmpty(tag)) {
                                starController.add_star_tag(user_id, tag)
                            }

                            String token = userRedis.opsForValue().get(KeyUtils.USER.token(user_id))
                            if (token) {
                                // 避免出现有token的情况下，另外个key正好ttl到期没有
                                /*userRedis.delete(KeyUtils.USER.token(user_id))
                                userRedis.delete(KeyUtils.accessToken(token))*/
//                                if(userRedis.hasKey(KeyUtils.accessToken(token))){
                                def userInfo = userRedis.opsForHash().entries(KeyUtils.accessToken(token)) as Map<String, String>
                                if (userInfo != null && !userInfo.isEmpty()) {
                                    userInfo.put('room_id', user_id.toString())
                                    userInfo.put("priv", UserType.主播.ordinal().toString())
                                    userRedis.opsForHash().putAll(KeyUtils.accessToken(token), userInfo)
                                }
                            }

                            //设置直播间排行榜新人加成分数
                            //room_rank(user_id)

                            //TODO 临时设置身份信息审核通过
                            record['status'] = ApplyType.通过.ordinal()
                            apply_sf_info().save(record)
                        }

                    }
                }

                if (status == ApplyType.通过.ordinal()) {
                    updateInviteCodeStatus(invite_code, ApplyInviteCodeStatus.使用已通过)

                    messageController.sendSingleMsg(user_id, '主播申请已通过', "您的主播申请已经通过啦，现在开始随时随地尽情的直播吧~", MsgType.系统消息);

                } else if (status == ApplyType.未通过.ordinal()) {
                    updateInviteCodeStatus(invite_code, ApplyInviteCodeStatus.使用未通过)

                    String msg = "尊敬的么么用户，您好！由于" + req['remark'] + "，您的主播申请未通过，谢谢您的理解和支持！"
                    messageController.sendSingleMsg(user_id, '主播申请已拒绝', msg, MsgType.系统消息);
                }
                Crud.opLog(OpType.apply_handle, [user_id: user_id, status: status])
            }

        }
        OK()
    }

    private void room_rank(Integer starId) {
        rankMongo.getCollection('rooms').update($$(_id: starId), $$($set: ["new_points": 18]), true, false)
    }

    private static final Map mapPro = [11: "北京", 12: "天津", 13: "河北", 14: "山西", 15: "内蒙古", 21: "辽宁",
                                       22: "吉林", 23: "黑龙江", 31: "上海", 32: "江苏", 33: "浙江", 34: "安徽", 35: "福建", 36: "江西",
                                       37: "山东", 41: "河南", 42: "湖北", 43: "湖南", 44: "广东", 45: "广西", 46: "海南", 50: "重庆",
                                       51: "四川", 52: "贵州", 53: "云南", 54: "西藏", 61: "陕西", 62: "甘肃", 63: "青海", 64: "宁夏",
                                       65: "新疆", 72: "台湾", 81: "香港", 91: "澳门",]

    /**
     * 主播完善身份信息列表
     * @param req
     * @return
     */
    def sf_renew_list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        query = Web.fillTimeBetween(query, "lastmodif", req)
        ['status', 'broker', 'xy_user_id'].each { String field ->
            intQuery(query, req, field)
        }
        Crud.list(req, apply_sf_info(), query.get(), new BasicDBObject('sfz_pic', 0), MongoKey.SJ_DESC) { List<BasicDBObject> data ->
            def users = users()
            for (BasicDBObject obj : data) {
                obj.put("nick_name", users.findOne(obj['xy_user_id'], new BasicDBObject("nick_name", 1))?.get("nick_name"))
            }
        }
    }

    File pic_folder

    @Value("#{application['pic.folder']}")
    void setPicFolder(String folder) {
        pic_folder = new File(folder)
        pic_folder.mkdirs()
        println "初始化图片上传目录 : ${folder}"
    }

    static String[] apply_props = ['real_name', "sfz", "tel"];
    /**
     * 审批主播完善信息
     * @param req
     * @return
     */
    def handle_sf_renew(HttpServletRequest req) {
        def status = req.getInt('status')
        if (status == ApplyType.通过.ordinal() || status == ApplyType.未通过.ordinal()) {
            Long time = System.currentTimeMillis()
            def record = apply_sf_info().findAndModify(new BasicDBObject(_id: req[_id], status: ApplyType.未处理.ordinal()),
                    new BasicDBObject('$set': [status: status, lastmodif: time, lastmodif_user: Web.currentUser()]))
            if (record) {
                def user_id = record.get('xy_user_id') as Integer
                if (status == ApplyType.通过.ordinal()) {
                    def renewData = $$(xy_user_id: user_id)
                    for (String prop : apply_props) {//
                        renewData.put(prop, record[prop])
                    }
                    table().update($$(xy_user_id: user_id, status: ApplyType.通过.ordinal()), $$($set: renewData))
                    //身份证同步
                    ['0', '1', '2'].each { String type ->
                        String renewPath = "sfz/${user_id}_${type}_renew.jpg"
                        String destPath = "sfz/${user_id}_${type}.jpg"
                        String bakPath = "sfz/${user_id}_${type}_bak.jpg"
                        def target = new File(pic_folder, renewPath)
                        def history = new File(pic_folder, destPath)
                        def bak = new File(pic_folder, bakPath)
                        if (history.exists())
                            FileUtils.copyFile(history, bak) //之前的身份信息备份
                        if (target.exists())
                            FileUtils.copyFile(target, history) //新审核通过的身份证覆盖之前的

                    }

                }
                Crud.opLog(OpType.sf_renew_apply_handle, [user_id: user_id, status: status])
            }
        }
        OK()
    }

    /**
     * 通过身份证获取省份信息
     * @return
     */
    private static String getProvinceBySFZ(DBObject apply) {
        try {
            if (StringUtils.isNotEmpty(apply['sfz'] as String)) {
                Integer num = Integer.valueOf(apply['sfz'].toString().substring(0, 2))
                return mapPro[num]
            }
        } catch (Exception e) {
            return '';
        }
        return '';
    }

    def show(HttpServletRequest req) {
        table().findOne(req[_id])
    }

    /**
     * 生成主播申请邀请码
     * @param req
     */
    def generate_invite_code(HttpServletRequest req) {
        String invite_code = generate();
        def ops = Web.getSession()
        if (ops == null)
            return [code: 0]
        logger.debug("session : {}", ops)
        def invite = $$(_id: invite_code, status: ApplyInviteCodeStatus.未使用.ordinal(), timestamp: System.currentTimeMillis(),
                op_nick_name: ops['nick_name'], op_id: ops['_id'], op_name: ops['name'])
        try {
            apply_codes().insert(invite)
            Crud.opLog(OpType.apply_invite_code_generate, [invite_code: invite_code])
            return [code: 1]
        } catch (Exception e) {
            logger.error("Error: {}", e);
        }
        return [code: 0]
    }

    private static final Integer CODE_LENGTH = 6

    private String generate() {
        String code = AuthCode.randomNumber(CODE_LENGTH)
        while (apply_codes().count($$(_id: code)) == 1) {
            code = AuthCode.randomNumber(CODE_LENGTH);
        }
        return code;
    }
    /**
     * 主播申请邀请码列表
     * @param req
     * @return
     */
    def invite_code_list(HttpServletRequest req) {
        def query = Web.fillTimeBetween(req)
        ['user_id', 'status'].each { String field ->
            intQuery(query, req, field)
        }
        ['op_nick_name', 'op_id', 'op_name'].each { String field ->
            stringQuery(query, req, field)
        }
        Crud.list(req, apply_codes(), query.get(), MongoKey.ALL_FIELD, MongoKey.SJ_DESC) { List<BasicDBObject> data ->
            def users = users()
            for (BasicDBObject obj : data) {
                obj.put("star_nick_name", users.findOne(obj['user_id'], new BasicDBObject("nick_name", 1))?.get("nick_name"))
            }
        }
    }

    private updateInviteCodeStatus(String invite_code, ApplyInviteCodeStatus status) {
        if (StringUtils.isNotEmpty(invite_code)) {
            apply_codes().update($$(_id: invite_code), $$($set: [status: status.ordinal(), lastmodif: System.currentTimeMillis()]))
        }
    }
}
