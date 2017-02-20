package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.common.util.MsgDigestUtil
import com.ttpod.star.common.util.AuthCode
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.commons.CommonsMultipartResolver

import javax.annotation.Resource
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 图片上传
 */
@Rest
class Controller extends BaseController{

    static final Logger logger = LoggerFactory.getLogger(Controller.class)

    @Resource
    SysController sysController

    @Value("#{application['pic.domain']}")
    String pic_domain = "https://aiimg.sumeme.com/"

    File pic_folder

    @Value("#{application['pic.folder']}")
    void setPicFolder(String folder){
        pic_folder = new File(folder)
        pic_folder.mkdirs()
        println "初始化图片上传目录 : ${folder}"
    }

    def upload(HttpServletRequest request,HttpServletResponse response){
        def parse = new CommonsMultipartResolver()
        def req = parse.resolveMultipart(request)

        try{
            Long id = System.currentTimeMillis()
            String filePath = null;
            for(Map.Entry<String, MultipartFile> entry  : req.getFileMap().entrySet()){
                MultipartFile file = entry.getValue()
                logger.debug("file name : {}, {},{}", file.getName(), StringUtils.substringAfter(file.getOriginalFilename(),"."), file.getContentType())

                filePath = "${id&63}/${id&7}/${id}.${StringUtils.substringAfter(file.getOriginalFilename(),".")}"
                def target = new File(pic_folder ,filePath)
                target.getParentFile().mkdirs()
                file.transferTo(target)
                break
            }

            String iframeCallBack = req["icallback"]
            if (StringUtils.isNotBlank(iframeCallBack)){
                def out = response.getWriter()
                out.println("<script>top.${iframeCallBack}({\"code\":1,\"data\":{\"pic_url\":\"${pic_domain}${filePath}\"}});</script>")
                out.close()
                return
            }

            [code: 1,url:"${pic_domain}${filePath}".toString(),error:0]
        }finally {
            parse.cleanupMultipart(req)
        }
    }


    def login(HttpServletRequest request){
        /*request.getCookies().each {Cookie c ->
            logger.debug("request cookies : {}", c.getName()+":"+c.getValue())
        }
        logger.debug("request parameters : {}", request.getParameterMap())*/
        String input = request[auth_code]
        String ver = request['ver'] ?: '0'
        if (codeVerifError(request,input)){
            return [code: 30419,msg:'验证码错误']
        }

        String name = request["name"]
        String password = MsgDigestUtil.SHA.digest2HEX(request["password"].toString())
        def user = adminMongo.getCollection("admins").findOne(new BasicDBObject(name:name,password:password))
        if (null == user){
            return [code: 0,msg:'密码错误']
        }

        Map menus = ver.equals('0') ? user.get("menus") as Map : user.get("menus1") as Map
        if (menus == null || menus.isEmpty()){
            return [code: 0,msg:'权限不足']
        }

        Map<String,String> sMap = new HashMap()
        sMap.put(_id,user.get(_id) as String)
        sMap.put("nick_name", user.get("nick_name") as String)
        sMap.put("name", user.get("name") as String)
        sMap.put("ip", Web.getClientIp(request))

        request.getSession().setAttribute("user",sMap)
        request.getSession().setAttribute("menus",new HashMap(menus))

        Map modules = user.get("modules") as Map
        if (modules != null){
            request.getSession().setAttribute("modules",new HashMap(modules))
        }

        def data = sMap.clone() as Map
        data.put("menus",menus)
        data.put("modules",modules)
        // data.put("modules",menus)
        return [code: 1,data: data]
    }


    def logout(HttpServletRequest request){
        request.getSession().invalidate()
        return [code: 1]
    }


    def modif_pwd(HttpServletRequest req){
        Map user = req.getSession().getAttribute("user") as Map
        if (null == user){
            return [code: 0]
        }
        String pwd = MsgDigestUtil.SHA.digest2HEX(req['password'].toString())
        adminMongo.getCollection('admins')
                .update(new BasicDBObject(_id,user.get(_id) as Integer),new BasicDBObject('$set',[password:pwd]))
        [code: 1]
    }


    /*  def authcode(HttpServletRequest request,HttpServletResponse response){
          String code =  AuthCode.random(4 +  ((int)System.currentTimeMillis()&1) )
          request.getSession().setAttribute(auth_code,code)
          response.addHeader('Content-Type',"image/png")
          Captcha.draw(code,160,48,response.getOutputStream())
      }*/

    def authcode(HttpServletRequest request,HttpServletResponse response){
        String code =  AuthCode.random(4  + ( (int)System.currentTimeMillis()&1))
        // mainRedis.opsForValue().set(KeyUtils.USER.authCode(Web.currentUserId()),code,60L,TimeUnit.SECONDS)

        request.getSession().setAttribute(auth_code,code)
        response.addHeader('Content-Type',"image/png")
        AuthCode.draw(code,160,48,response.getOutputStream())
    }

    def session(HttpServletRequest req){
        def user = req.getSession().getAttribute("user")
        if (null == user){
            return [code: 0]
        }
        def data = new HashMap(user as Map)
        data.put("menus",req.getSession().getAttribute("menus"))
        data.put("modules",req.getSession().getAttribute("modules"))
        [code: 1,data: data]
    }


}
