package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.QueryBuilder
import com.ttpod.rest.AppProperties
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.IMessageCode
import com.ttpod.rest.common.util.MsgDigestUtil
import com.ttpod.rest.web.Crud
import com.ttpod.star.admin.crud.MessageController
import com.ttpod.star.model.ApplyType
import com.ttpod.star.model.MsgType
import com.ttpod.star.model.OpType
import com.ttpod.star.model.PhotoStatusType
import net.coobird.thumbnailator.Thumbnails
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.util.regex.Pattern
import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * date: 13-3-28 下午2:31
 * @author: yangyang.cong@ttpod.com
 */

//@Rest
@RestWithSession
class UnionPicController extends BaseController {

    @Resource
    public StringRedisTemplate liveRedis;
    static final Logger logger = LoggerFactory.getLogger(UnionPicController.class)

    DBCollection table() { users() }

    def uphoto_list(HttpServletRequest req) {
        QueryBuilder query = QueryBuilder.start();

        String uid = req[_id]
        if (StringUtils.isNotBlank(uid))
            query.and("user_id").is(Integer.parseInt(uid))

        String status = req['status']
        if (StringUtils.isNotBlank(status))
            query.and("status").is(Integer.parseInt(status))

        Crud.list(req, adminMongo.getCollection("union_photos"), query.get(), ALL_FIELD, SJ_DESC)
    }

    static final Long SIX_HUNDRED_SECONDS = 600L

    static final String notify_url = (AppProperties.get('api.domain') + 'upai/notify').replace("/", "\\/")

    static final Pattern URL_PATT = Pattern.compile("/\\d+/\\d{4}/\\w{32}.[a-z]{3,4}")

    static final String HTTP_FORM_KEY = "bl37fSAQyZ0ZMcF/cZMGjwWNuQU="

    def token(HttpServletRequest req) {
        def uid = req.getInt(_id);
        def json = "{\"bucket\":\"showphoto\"," +
                "\"expiration\":${System.unixTime() + SIX_HUNDRED_SECONDS}," +
                "\"save-key\":\"/${uid}/{mon}{day}/{filemd5}{.suffix}\"," +
                "\"allow-file-type\":\"jpg,jpeg,gif,png\"," +
                "\"image-width-range\":\"120,2048\"," +
                "\"image-height-range\":\"120,8192\"," +
                "\"content-length-range\":\"0,3145728\"," + // 0 ~ 3MB
//                    "\"x-gmkerl-type\":\"\","+
                "\"return-url\":\"\"," +
                "\"notify-url\":\"${notify_url}\"}"
        def policy = Base64.encodeBase64String(json.asBytes())
        return [code: 1, data: [
                action   : 'http://v0.api.upyun.com/showphoto/', policy: policy,
                signature: MsgDigestUtil.MD5.digest2HEX("${policy}&${HTTP_FORM_KEY}")
        ]]
    }

    @Value("#{application['pic.domain']}")
    String pic_domain = "https://aiimg.sumeme.com/"

    def add_union(HttpServletRequest req) {
        def path = req['path']
        def pic_url = req['pic_url']
        def uid = req.getInt(_id);
        //if(URL_PATT.matcher(path.clean()).matches() && path.startsWith("/"+uid+"/")){
        if (adminMongo.getCollection("union_photos").count($$('user_id', uid)) <= 5) {
            if (adminMongo.getCollection("union_photos").save(new BasicDBObject(
                    _id: path,
                    user_id: uid as Integer,
                    pic_url: pic_url,
                    timestamp: System.currentTimeMillis(),
                    status: PhotoStatusType.未处理.ordinal()
            )).getN() > 0)
                return IMessageCode.OK

        }
    }

    /**
     * status 1 : 未处理,2 : 通过,3 : 未通过;
     * @param req
     * @return
     */
    def edit_union(HttpServletRequest req) {
        def path = req['path']
        def status = req['status'];
        adminMongo.getCollection("union_photos").update($$(_id, path), $$($set, $$('status', Integer.valueOf(status))))
        return IMessageCode.OK
    }

    //图片处理
    File pic_folder

    @Value("#{application['pic.folder']}")
    void setPicFolder(String folder) {
        pic_folder = new File(folder)
        pic_folder.mkdirs()
        println "初始化图片上传目录 : ${folder}"
    }

    def del_union_photo(HttpServletRequest req) {
        def path = req['path'] as String
        Integer uid = req.getInt(_id)
//            users().update($$([_id : Web.currentUserId() as Integer]),
//                    $$($pull, $$('union_pic.stars', $$(["path": path,"status": 0])))
//                ,false,false,writeConcern)
        def obj = adminMongo.getCollection("union_photos").findAndRemove($$(_id, path).append("user_id", uid))
        if (obj != null) {
//            obj.put("del_time",System.currentTimeMillis())
//            obj.put("s",0)
//            mainMongo.getCollection("ban_photos").save(obj)
//            return IMessageCode.OK
            File file = new File(pic_folder, path)
            file.delete();
            return [code: 1]
        }
        return [code: 0]
    }

    //封面图审核
    static final BasicDBObject COVER_DESC = new BasicDBObject('audit_pic_status', -1);

    def cover_list(HttpServletRequest req) {
        QueryBuilder query = QueryBuilder.start();
        String room_id = req[_id]
        String leader_id = req['leader_id']
        if (StringUtils.isNotBlank(room_id))
            query.and("_id").is(Integer.parseInt(room_id))
        if (StringUtils.isNotBlank(leader_id))
            query.and("xy_star_id").is(Integer.parseInt(leader_id))
        Crud.list(req, rooms(), query.get(), ALL_FIELD, COVER_DESC)
    }


    @Resource
    MessageController messageController

    static final String PIC_AUDIT_AGREE_TITLE = "您提交的封面审核通过"
    static final String PIC_AUDIT_AGREE_CONTENT = "您的封面已经通过审核，刷新家族房列表后会展示你新的封面哟"
    static final String PIC_AUDIT_REFUSE_TITLE = "您提交的封面审核不通过"
    static final String PIC_AUDIT_REFUSE_CONTENT = "您的封面没有通过审核，请检查封面清晰度，和封面相关规则，若有疑问请联系官方客服人员"
    //封面图审核
    def pic_audit(HttpServletRequest req) {
        def familys = familyMongo.getCollection('familys')
        def status = req.getInt('status')
        def room_id = req[_id] as Integer
        if (status == ApplyType.通过.ordinal() || status == ApplyType.未通过.ordinal()) {
            Long time = System.currentTimeMillis()
            def record = rooms().findAndModify(new BasicDBObject(_id: room_id),
                    new BasicDBObject('$set': [audit_app_pic_status: status, lastmodif: time]))
            def familyRecord = familys.findAndModify(new BasicDBObject(_id: room_id),
                new BasicDBObject($set: [audit_app_pic_status: status, lastmodif: time]))
            if (record && familyRecord) {
                if (status == ApplyType.通过.ordinal()) {
                    def pic_modify = ((record?.get('pic_modify') ?: 0) as Integer) ^ 1;
                    rooms().update($$(_id, room_id), $$($set, $$('app_pic_url': record['audit_app_pic_url'], 'pic_modify': pic_modify)))
                    familys.update($$(_id, room_id), $$($set, $$('app_pic_url': record['audit_app_pic_url'], 'pic_modify': pic_modify)))
                }

                /*def title, content
                if (status == ApplyType.通过.ordinal()) {
                    title = PIC_AUDIT_AGREE_TITLE
                    content = PIC_AUDIT_AGREE_CONTENT
                } else {
                    title = PIC_AUDIT_REFUSE_TITLE
                    content = PIC_AUDIT_REFUSE_CONTENT
                }
                messageController.sendSingleMsg(room_id, title, content, MsgType.系统消息)*/
                Crud.opLog(OpType.pic_audit, [room_id: room_id, status: status])
            }
        }

        OK()
    }

    def generateUnionPic(Integer userId, String url) {
        def map = new HashMap()
        if (StringUtils.isNotEmpty(url)) {
            //W:231 ,H:142
            map.put("union_pic.bd_231X142", cutImage(url, 231, 142))
            //W:172, H:107
            map.put("union_pic.bd_172X107", cutImage(url, 172, 107))
            //快播图片
            map.put("union_pic.kb_200x200", cutImage(url, 200, 200))
            map.put("union_pic.kb_304x200", cutImage(url, 304, 200))
            map.put("union_pic.kb_616x252", cutImage(url, 616, 252))

            users().update(new BasicDBObject('_id', userId), new BasicDBObject($set, map), false, false, writeConcern)
        }
    }

    private String cutImage(String allow_url, int rw, int rh) {
        String fpath = "";
        try {
            def url = new URL(allow_url)
            BufferedImage img = ImageIO.read(url)
            fpath = url.getPath().replace('.jpg', '').substring(1) + "_${rw}${rh}.jpg"
            File file = new File(pic_folder, fpath)
            file.getParentFile().mkdirs()
            Thumbnails.of(img).size(rw, rh).keepAspectRatio(false).outputQuality(0.95f).toFile(file);
        } catch (Exception e) {
            return fpath
        }
        return fpath
    }
}
