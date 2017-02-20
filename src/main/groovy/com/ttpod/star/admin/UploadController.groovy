package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.ttpod.rest.AppProperties
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.common.doc.IMessageCode
import com.ttpod.rest.common.util.MsgDigestUtil
import com.ttpod.rest.web.Crud
import com.ttpod.star.common.doc.Param
import com.ttpod.star.common.util.ExportUtils
import com.ttpod.star.common.util.KeyUtils
import com.ttpod.star.model.ExportType
import com.ttpod.star.model.OpType
import com.ttpod.star.model.PhotoStatusType
import com.ttpod.star.model.UserType
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.web.bind.ServletRequestUtils
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.commons.CommonsMultipartResolver
import org.springframework.web.util.HtmlUtils

import javax.servlet.http.HttpServletResponse
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.util.MsgDigestUtil.MD5
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * @author: jiao.li@ttpod.com
 * Date: 2015/1/14 16:36
 */
@RestWithSession
class UploadController extends BaseController {

    //
    File pic_folder

    @Value("#{application['pic.folder']}")
    void setPicFolder(String folder) {
        pic_folder = new File(folder)
        pic_folder.mkdirs()
        println "初始化SWF上传目录 : ${folder}"
    }

    @Value("#{application['pic.domain']}")
    String pic_domain = "https://aiimg.sumeme.com/"

    /**
     * 上传swf动画
     * @param request
     * @return
     */
    def swf(HttpServletRequest request,HttpServletResponse response){
        def parse = new CommonsMultipartResolver()
        def req = parse.resolveMultipart(request)
        String iframeCallBack = req["icallback"]
        try{
            String filePath = "swf/"
            for(Map.Entry<String, MultipartFile> entry  : req.getFileMap().entrySet()){
                MultipartFile file = entry.getValue()
                filePath += file.getOriginalFilename()
                def target = new File(pic_folder ,filePath)
                target.getParentFile().mkdirs()
                file.transferTo(target)
                break
            }
            if (StringUtils.isNotBlank(iframeCallBack)){
                def out = response.getWriter()
                out.println("<script>top.${iframeCallBack}({\"code\":1,\"data\":{\"pic_url\":\"${pic_domain}${filePath}\"}});</script>")
                out.close()
                return
            }
            return [code: 1, url:"${pic_domain}${filePath}".toString(),error:0]
        }catch (Exception e){
            if (StringUtils.isNotBlank(iframeCallBack)){
                def out = response.getWriter()
                out.println("<script>top.${iframeCallBack}({\"code\":0});</script>")
                out.close()
                return
            }
        }
        finally {
            parse.cleanupMultipart(req)
        }
    }

}