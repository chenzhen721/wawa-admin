package com.wawa.admin.web

import com.wawa.base.BaseController
import com.wawa.base.anno.RestWithSession
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.commons.CommonsMultipartResolver

import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest


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
        String iframeCallBack = req.getParameter("icallback")
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