package com.ttpod.star.admin.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.util.MsgDigestUtil
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.BaseController
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang.StringUtils
import com.ttpod.star.admin.UpaiController
import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import java.text.SimpleDateFormat

import static com.ttpod.rest.groovy.CrudClosures.*
/**
 * @author: jiao.li@ttpod.com
 * Date: 13-12-25 上午9:31
 */
@RestWithSession
//@Rest
class ClockController extends BaseController{

    static final Long SIX_HUNDRED_SECONDS = 600L
    static final String NOTIFY_URL = ('http://test.show.dongting.com/upai/notify.json').replace("/","\\/")

    DBCollection table() {adminMongo.getCollection('clocks')}

    @Delegate Crud crud = new Crud(table(),true,
            [_id:Int,pic:Str,pics:{if(it){StringUtils.split(it as String, ',')}else{null}},clocks:{if(it){StringUtils.split(it as String, ',')}else{null}},status:Ne0,timestamp:Timestamp],
            new Crud.QueryCondition(){
                public DBObject sortby(HttpServletRequest req) {new BasicDBObject(timestamp:-1)}
            }
    )

    def token(){
        def json = "{\"bucket\":\"showclock\"," +
                "\"expiration\":${System.unixTime() + SIX_HUNDRED_SECONDS}," +
                "\"save-key\":\"/clock/{mon}{day}/{filemd5}{.suffix}\"," +
               // "\"allow-file-type\":\"jpg,jpeg,gif,png\"," +
               // "\"image-width-range\":\"120,2048\","+
               // "\"image-height-range\":\"120,8192\","+
               // "\"content-length-range\":\"0,3145728\"," + // 0 ~ 3MB
//                    "\"x-gmkerl-type\":\"\","+
                "\"return-url\":\"\","+
                "\"notify-url\":\"${NOTIFY_URL}\"}"
        def policy = Base64.encodeBase64String(json.asBytes())
        return [code:1,data:[download_domain: UpaiController.DOMAIN,
                action:'http://v0.api.upyun.com/showclock/',policy:policy,
                signature:MsgDigestUtil.MD5.digest2HEX("${policy}&${UpaiController.HTTP_FORM_KEY}"
                )
        ]]
    }

}
