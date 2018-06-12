package com.wawa.admin.web.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.wawa.base.anno.RestWithSession
import com.wawa.common.doc.IMessageCode
import com.wawa.common.util.http.HttpClientUtil
import com.wawa.common.util.http.HttpEntityHandler
import com.wawa.base.persistent.KGS
import com.wawa.base.Crud
import com.wawa.base.BaseController
import com.wawa.common.util.MD5Util
import org.apache.commons.lang.StringUtils
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.FileEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.commons.CommonsMultipartResolver

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.regex.Pattern

import static com.wawa.groovy.CrudClosures.Bool
import static com.wawa.groovy.CrudClosures.Str
import static com.wawa.groovy.CrudClosures.Timestamp

/**
 * 音乐管理相关接口
 */
@RestWithSession
class AudioController extends BaseController {

    static final Logger logger = LoggerFactory.getLogger(AudioController.class)
    static final String pic_domain = "http://laihou-audio.b0.upaiyun.com/"
    static final String ftp_domain = "http://v0.api.upyun.com"
    static final String bucket = "laihou-audio"
    static final String AUTHORIZATION = 'bGFpaG91OmxhaWhvdTEyMw=='

    File pic_folder

    @Value("#{application['pic.folder']}")
    void setPicFolder(String folder){
        pic_folder = new File(folder)
        pic_folder.mkdirs()
        println "初始化图片上传目录 : ${folder}"
    }

    public static final HttpClient httpClient = new DefaultHttpClient()

    DBCollection table() {adminMongo.getCollection('audios')}

    //直接上传至又拍云
    def upload(HttpServletRequest request, HttpServletResponse resp){
        def parse = new CommonsMultipartResolver()
        def req = parse.resolveMultipart(request)
        File target = null
        try{
            Boolean result = Boolean.FALSE
            String id = System.currentTimeMillis()
            String ymd = new Date().format('yyyyMMdd')
            String filePath = null;
            for(Map.Entry<String, MultipartFile> entry  : req.getFileMap().entrySet()){
                MultipartFile file = entry.getValue()
                String fullName = file.getOriginalFilename()
                String fileName = StringUtils.substringBefore(fullName, ".")
                String uploadName = MD5Util.MD5Encode(id + fileName, null)
                String suffix = StringUtils.substringAfter(fullName,".")
                logger.debug("file name : {}, {},{}", file.getName(), suffix, file.getContentType())
                filePath = "audio/${ymd}/${uploadName}.${suffix}"
                target = new File(pic_folder ,filePath)
                target.getParentFile().mkdirs()
                file.transferTo(target)
                //上传至又拍云
                HttpPut httpPut = new HttpPut("${ftp_domain}/${bucket}/${filePath}")
                FileEntity fileEntity = new FileEntity(target, file.getContentType())
                httpPut.setEntity(fileEntity)
                Map<String,String> headers = new HashMap<>()
                headers.put('Authorization', 'Basic ' + AUTHORIZATION)
                headers.put('Date', new Date().toString())
                //headers.put('Content-Length', String.valueOf(file.getSize()))
                result = HttpClientUtil.http(httpClient, httpPut, headers, new HttpEntityHandler<Boolean>() {

                    @Override
                    Boolean handleResponse(HttpResponse response) throws IOException {
                        int code = response.getStatusLine().getStatusCode();
                        if(code == HttpStatus.SC_OK){
                            return Boolean.TRUE
                        }
                        return Boolean.FALSE
                    }

                    @Override
                    Boolean handle(HttpEntity entity) throws IOException {
                        return null
                    }

                    @Override
                    String getName() {
                        return null
                    }
                })
                break
            }

            if (!result) {
                return [code: 0]
            }

            String iframeCallBack = req.getParameter("icallback")
            if (StringUtils.isNotBlank(iframeCallBack)){
                def out = resp.getWriter()
                out.println("<script>top.${iframeCallBack}({\"code\":1,\"data\":{\"pic_url\":\"${pic_domain}${filePath}\"}});</script>")
                out.close()
                return
            }

            [code: 1,url:"${pic_domain}${filePath}".toString(),error:0]
        } finally {
            parse.cleanupMultipart(req)
            if (target != null) {
                target.delete()
            }
        }
    }

    @Resource
    KGS seqKGS
    @Delegate Crud crud = new Crud(table(),true,
            [_id    :{seqKGS.nextId()}, name:Str, status: Bool, url: Str, singer: Str, album: Str, format: Str,
             duration: {String str->  (str == null || str.isEmpty()) ? null : Long.valueOf(str) },timestamp:Timestamp],

            new Crud.QueryCondition(){
                public DBObject query(HttpServletRequest req) {
                    def query = super.query(req)
                    if (req.getParameter('status')) {
                        query.put('query', req.getParameter('status'))
                    }
                    if (req.getParameter('name')){
                        query.put('name', Pattern.compile("/"+req.getParameter('name')+"/"))
                    }
                    return query
                }
            }
    )

    def del(HttpServletRequest req) {
        DBObject remove = table().findAndRemove(new BasicDBObject(_id, parseId(req)))
        if (remove != null) {
            if (remove['url']) {
                String url = (remove['url'] as String).replaceAll(/^https?:\/\/[^\/]*\//, '')
                HttpDelete httpDelete = new HttpDelete("${ftp_domain}/${bucket}/${url}")
                Map<String,String> headers = new HashMap<>()
                headers.put('Authorization', 'Basic ' + AUTHORIZATION)
                Boolean result = HttpClientUtil.http(httpClient, httpDelete, headers, new HttpEntityHandler<Boolean>() {

                    @Override
                    Boolean handleResponse(HttpResponse response) throws IOException {
                        int code = response.getStatusLine().getStatusCode();
                        if(code == HttpStatus.SC_OK){
                            return Boolean.TRUE
                        }
                        return Boolean.FALSE
                    }

                    @Override
                    Boolean handle(HttpEntity entity) throws IOException {
                        return null
                    }

                    @Override
                    String getName() {
                        return null
                    }
                })
                if (!result) {
                    logger.error("upai audio delete failed. Url: {}", "${ftp_domain}/${bucket}/${url}".toString())
                }
            }
            Crud.opLog(table().getName() + "_del", remove)
        }
        return IMessageCode.OK
    }


}
