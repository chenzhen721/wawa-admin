package com.ttpod.star.common.util;

import groovy.transform.CompileStatic;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.SSLContext;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.KeyStore;

@CompileStatic
public abstract class HttpsClientUtils {

    static final Logger log = LoggerFactory.getLogger(HttpsClientUtils.class);

    public static final Charset UTF8 =Charset.forName("UTF-8");

    public static final String redPackUrl = "https://api.mch.weixin.qq.com/mmpaymkttransfers/sendredpack";

    public static final String context = "<xml>" +
            "<sign><![CDATA[E1EE61A91C8E90F299DE6AE075D60A2D]]></sign>" +
            "<mch_billno><![CDATA[0010010404201411170000046545]]></mch_billno>" +
            "<mch_id><![CDATA[10000097]]></mch_id>" +
            "<appid><![CDATA[wxe062425f740c30d8]]></appid>" +
            "<bill_type><![CDATA[MCHT]]></bill_type>" +
            "<nonce_str><![CDATA[50780e0cca98c8c8e814883e5caa672e]]></nonce_str>" +
            "</xml>";

    public static final String mchId = "1432143502";

    public static final String certPath = "/weixin/apiclient_cert.p12";

    public static String ssl(String url,String data){
        url = redPackUrl;
        data = context;
        StringBuffer message = new StringBuffer();
        CloseableHttpResponse response = null;
        try {
            KeyStore keyStore  = KeyStore.getInstance("PKCS12");
            InputStream instream = new ClassPathResource(certPath).getInputStream();
            keyStore.load(instream, mchId.toCharArray());
            // Trust own CA and all self-signed certs
            SSLContext sslcontext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, mchId.toCharArray())
                    .build();
            // Allow TLSv1 protocol only
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslcontext,
                    new String[] { "TLSv1" },
                    null,
                    SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
            CloseableHttpClient httpclient = HttpClients.custom()
                    .setSSLSocketFactory(sslsf)
                    .build();
            HttpPost httpost = new HttpPost(url);

            httpost.addHeader("Connection", "keep-alive");
            httpost.addHeader("Accept", "*/*");
            httpost.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            httpost.addHeader("Host", "api.mch.weixin.qq.com");
            httpost.addHeader("X-Requested-With", "XMLHttpRequest");
            httpost.addHeader("Cache-Control", "max-age=0");
            httpost.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0) ");
            httpost.setEntity(new StringEntity(data, "UTF-8"));
            System.out.println("executing request" + httpost.getRequestLine());

            response = httpclient.execute(httpost);
//            try {
                HttpEntity entity = response.getEntity();

                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                if (entity != null) {
                    System.out.println("Response content length: " + entity.getContentLength());
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(entity.getContent(),"UTF-8"));
                    String text;
                    while ((text = bufferedReader.readLine()) != null) {
                        message.append(text);
                    }

                }
                EntityUtils.consume(entity);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        } catch (Exception e1) {
            e1.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return message.toString();
    }


    public static void main(String[] args) throws Exception{



        String response = ssl(redPackUrl, context);

        System.out.println(response);


    }


}
