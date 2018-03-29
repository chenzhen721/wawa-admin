package com.wawa.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Created by Administrator on 2017/7/12.
 */
public class WeixinUtils {
    public static final Logger logger = LoggerFactory.getLogger(WeixinUtils.class);
    public static final String SEND_PACK_URL = "https://api.mch.weixin.qq.com/mmpaymkttransfers/sendredpack";
//    public static final String MCH_ID = "1476104702"; //微信支付分配的商户号
    public static final String MCH_ID = "1457175402"; //微信支付分配的商户号
    public static final String APP_ID = "wx45d43a50adf5a470"; //微信分配的公众账号ID（在mp.weixin.qq.com申请的）
    public static final String CERT_PATH = "/weixin/apiclient_cert.p12";
    public static final String API_KEY = "62110ce299081ba6ab3bfbf77ff7be9f";
    public static final String LAIHOU_APP_ID = "wx85c1789a23ef15f9"; //开发者平台注册的APP ID

    public static final Random rd = new Random();

    /**
     * 生成随机数
     * @param length 要生成的长度
     * @return
     */
    public static String createNoncestr(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String res = "";
        for (int i = 0; i < length; i++) {
            res += chars.charAt(rd.nextInt(chars.length() - 1));
        }
        return res;
    }

    /**
     * 创建商户订单号
     * @return
     */
    public static String getMchBillNo() {
        String mchBillNo = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("HHmmssSSS");
        Date now = new Date();
        mchBillNo = MCH_ID + sdf.format(now) + sdf2.format(now) + rd.nextInt(9);
        return mchBillNo;
    }

    public static String createSign(String characterEncoding, Map<String, Object> parameters) {
        StringBuffer sb = new StringBuffer();
        Set es = parameters.entrySet();//所有参与传参的参数按照accsii排序（升序）
        Iterator it = es.iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String k = (String) entry.getKey();
            Object v = entry.getValue();
            if (null != v && !"".equals(v) && !"sign".equals(k) && !"key".equals(k)) {
                sb.append(k + "=" + v + "&");
            }
        }
        sb.append("key=" + API_KEY);
        String sign = MD5Util.MD5Encode(sb.toString(), characterEncoding).toUpperCase();
        logger.info("signStr=" + sb.toString() + ", sign=" + sign);
        return sign;
    }

    /**
     * map转换成微信标准的xml格式
     * @param map 微信入参map
     * @return
     */
    public static String mapToXml(Map<String, Object> map) {
        StringBuilder xml = new StringBuilder("<xml>");

        Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Object> entry = iter.next();
            String key = entry.getKey();
            Object val = entry.getValue();
            xml.append("<" + key + "><![CDATA[" + val + "]]></" + key + ">");
        }
        xml.append("</xml>");
        return xml.toString();
    }

}
