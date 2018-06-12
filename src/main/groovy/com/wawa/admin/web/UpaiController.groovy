package com.wawa.admin.web

import com.mongodb.BasicDBObject
import com.wawa.base.BaseController
import com.wawa.base.anno.Rest
import com.wawa.common.util.MsgDigestUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

/**
 * @author: jiao.li@ttpod.com
 * Date: 13-12-27 上午9:46
 */
@Rest
class UpaiController extends BaseController{

    static final String HTTP_FORM_KEY = "758HbeMpbaTwXymQW4QO1fnKSgI="

    static final String UPAI_NOTIFY_COLLECTION = "upai_notify_log"

    static final  Logger logger = LoggerFactory.getLogger(UpaiController.class)

    static final String DOMAIN = "http://showclock.b0.upaiyun.com";
    static final String STAR_VIDEO_DOMAIN = "http://star-video.b0.upaiyun.com";
    static final Integer USER_DELETE = Integer.valueOf(0)
    static final String NEST_PHOTO_BUCKET = "img-nest"
    static final String STAR_PHOTO_BUCKET = "img-album"

    def notify(HttpServletRequest req){
        def message = req.getParameter('message') as String
        def path = req.getParameter('url') as String
        def time = req.getParameter('time') as String
        def url = DOMAIN + path
        println "map:${req.getParameterMap()}"
        def sign = MsgDigestUtil.MD5.digest2HEX("200&${message}&${path}&${time}&${HTTP_FORM_KEY}")
        if(sign.equals(req.getParameter('sign'))){
            def notifys = adminMongo.getCollection(UPAI_NOTIFY_COLLECTION)
            def obj = new BasicDBObject(_id,path)
            if(notifys.count(obj) == 0){
                obj.put(timestamp,Long.valueOf(time)*1000)
                obj.put("url",url)
//                obj.put("image_width",Integer.valueOf(req.getParameter("image-width")))
//                obj.put("image_height",Integer.valueOf(req.getParameter("image-height")))
//                obj.put("image_type",req.getParameter("image-type"))
//                obj.put("image_frames",req.getParameter("image-frames"))
//                logger.debug("save obj: {}",obj)
                def count = notifys.save(obj).getN()
                return [code:1]
            }
        }
        [code:0]
    }
}