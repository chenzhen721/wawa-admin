package com.wawa.admin.web.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.wawa.base.anno.RestWithSession
import com.wawa.base.Crud
import com.wawa.base.BaseController
import com.wawa.model.OpType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

import static com.wawa.groovy.CrudClosures.Str

@RestWithSession
class WeixinTemplateController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(WeixinTemplateController.class)

    @Delegate
    Crud crud = new Crud(unionMongo.getCollection('weixin_template'),
            [_id  : Str, msg_type: Str, title: Str, url: Str, pic_url: Str, description: Str, content: Str,
             begin: { String str -> (str == null || str.isEmpty()) ? null : Long.valueOf(str) },
             end  : { String str -> (str == null || str.isEmpty()) ? null : Long.valueOf(str) }],
            new Crud.QueryCondition() {
                public DBObject query(HttpServletRequest req) {
                    return new BasicDBObject('msg_type': [$ne: null])
                }

                public DBObject sortby(HttpServletRequest req) { new BasicDBObject(timestamp: -1) }
            }
    )

    def add(HttpServletRequest req) {
        logger.debug("Recv event_callback params : {}", req.getParameterMap())
        def msgType = req.getParameter('msg_type') // 客服消息类型 news 图文信息,text 文字信息
        def title = req.getParameter('title')
        def url = req.getParameter('url')
        def picUrl = req.getParameter('pic_url')
        def description = req.getParameter('description')
        def content = req.getParameter('content')
        def begin = req.getParameter('begin') as Long
        def end = req.getParameter('end') as Long
        def fire = req.getParameter('fire') as String //执行时间 HH:mm:ss
        String[] times = fire.split(":")
        String hour = times[0]
        String minute = times[1]
        String seconds = times[2]
        Long now = System.currentTimeMillis()
        def id = new Date().format('yyyy-MM-dd') + '_' + msgType + '_' + now
        String current = new Date().format('HH:mm:ss')
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(begin));
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
        calendar.set(Calendar.MINUTE, Integer.parseInt(minute));
        calendar.set(Calendar.SECOND, Integer.parseInt(seconds));
        // 计算下一次执行时间,当前时间大于执行时间,就将日期加1
        if (Integer.parseInt(current.replace(':', '')) > Integer.parseInt(fire.replace(':', ''))) {
            calendar.add(Calendar.DATE, 1);
        }
        def prop = [
                _id        : id,
                msg_type   : msgType,
                title      : title,
                url        : url,
                pic_url    : picUrl,
                description: description,
                content    : content,
                begin      : begin,
                end        : end,
                next_fire  : calendar.getTimeInMillis(),// 下一次执行时间
                last_fire  : 0L,// 最近一次执行时间
                fire       : fire,
                timestamp  : now
        ]
        unionMongo.getCollection('weixin_template').save(new BasicDBObject((Map) prop))
        Crud.opLog(OpType.add_weixin_template,prop)
        return [code: 1]
    }

}
