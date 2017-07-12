package com.ttpod.star.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * Created by Administrator on 2017/7/6.
 */
public class HttpsClientUtils {

    static final Logger logger = LoggerFactory.getLogger(HttpsClientUtils.class);

    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String KEY_ALGORITHM = ((java.security.Security.getProperty("ssl.KeyManagerFactory.algorithm") == null)? "sunx509" : java.security.Security.getProperty("ssl.KeyManagerFactory.algorithm"));

//    public static final String url = "https://api.mch.weixin.qq.com/mmpaymkttransfers/gethbinfo";
    public static final String url = "https://api.mch.weixin.qq.com/mmpaymkttransfers/sendredpack";


    public static final String mcId = "1432143502";

    public static final String certPath = "/weixin/apiclient_cert.p12";

    public static final String context = "<xml>" +
            "<sign><![CDATA[E1EE61A91C8E90F299DE6AE075D60A2D]]></sign>" +
            "<mch_billno><![CDATA[0010010404201411170000046545]]></mch_billno>" +
            "<mch_id><![CDATA[10000097]]></mch_id>" +
            "<appid><![CDATA[wxe062425f740c30d8]]></appid>" +
            "<bill_type><![CDATA[MCHT]]></bill_type>" +
            "<nonce_str><![CDATA[50780e0cca98c8c8e814883e5caa672e]]></nonce_str>" +
            "</xml>";

    public static SSLContext buildSSLContext(String path, String password) {
        try {
            KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
            InputStream cert = new ClassPathResource(path).getInputStream();
            ks.load(cert, password.toCharArray());

            // Get a KeyManager and initialize it
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_ALGORITHM);
            kmf.init(ks, password.toCharArray());

            // Get a TrustManagerFactory with the DEFAULT KEYSTORE, so we have all
            // the certificates in cacerts trusted
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(KEY_ALGORITHM);
            tmf.init((KeyStore)null);

            // Get the SSLContext to help create SSLSocketFactory
            final SSLContext sslContext = SSLContext.getInstance("TLSv1");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return sslContext;
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void execute() {
        SSLContext sslContext = buildSSLContext(certPath, mcId);
        if (sslContext == null) {
            logger.error("holy shit! init sslContext failed.");
            return;
        }
        SSLSocketFactory sslFactory = sslContext.getSocketFactory();

        HttpsURLConnection urlCon = null;
        try {
            urlCon = (HttpsURLConnection) (new URL(url)).openConnection();

            urlCon.setSSLSocketFactory(sslFactory);

            urlCon.setDoInput(true);
            urlCon.setDoOutput(true);
            urlCon.setRequestMethod("POST");
            urlCon.setRequestProperty("Content-Length",  String.valueOf(context.getBytes().length));
            urlCon.setUseCaches(false);
            //设置为gbk可以解决服务器接收时读取的数据中文乱码问题
            urlCon.getOutputStream().write(context.getBytes("UTF-8"));
            urlCon.getOutputStream().flush();
            urlCon.getOutputStream().close();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*finally {
            if (urlCon != null) {
                urlCon.disconnect();
            }
        }*/
    }

    public static void main(String[] args) throws Exception {

    }
}
