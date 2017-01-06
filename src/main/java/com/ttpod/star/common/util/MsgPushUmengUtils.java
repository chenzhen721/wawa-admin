package com.ttpod.star.common.util;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ttpod.rest.AppProperties;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import push.AndroidNotification;
import push.PushClient;
import push.android.*;
import push.ios.*;

/**
 * 第三方消息推送
 */
public class MsgPushUmengUtils{


    private final static String appkey = "53ab9ff256240b97cf0164a5";
    private final static String appMasterSecret = "kzh2dvj7siu7jsbxi8ux0usgrxdzlsl7";

    private final static String ios_appkey = "544f71eafd98c5a62b002aa3";
    private final static String ios_appMasterSecret = "rj4jyasjy4iuzkgdjetqlld6xmuwinlz";

    private static PushClient client = new PushClient();

    public static void sendAndroidUnicast(String deviceToken, String ticker, String title, String text) throws Exception {
        AndroidUnicast unicast = new AndroidUnicast(appkey,appMasterSecret);
        // Set your device token
        unicast.setDeviceToken(deviceToken);
        unicast.setTicker(ticker);
        unicast.setTitle(title);
        unicast.setText(text);
        unicast.goAppAfterOpen();
        unicast.setDisplayType(AndroidNotification.DisplayType.NOTIFICATION);
        // Set 'production_mode' to 'false' if it's a test device.
        // For how to register a test device, please see the developer doc.
        unicast.setProductionMode();
        // Set customized fields
        unicast.setExtraField("event", "go_app");
        //unicast.setCustomField();
        client.send(unicast);
    }

    public static void sendAndroidListcast(List<String> deviceTokens, String ticker, String title, String text) throws Exception {
        AndroidListcast listcast = new AndroidListcast(appkey,appMasterSecret);
        listcast.setDeviceTokens(StringUtils.join(deviceTokens,","));
        listcast.setTicker(ticker);
        listcast.setTitle(title);
        listcast.setText(text);
        listcast.goAppAfterOpen();
        listcast.setDisplayType(AndroidNotification.DisplayType.NOTIFICATION);
        // Set 'production_mode' to 'false' if it's a test device.
        // For how to register a test device, please see the developer doc.
        listcast.setProductionMode();
        // Set customized fields
        listcast.setExtraField("event", "go_app");
        //unicast.setCustomField();
        client.send(listcast);
    }

    public static void sendIOSUnicast(String deviceToken, String title, String text) throws Exception {
        IOSUnicast unicast = new IOSUnicast(ios_appkey,ios_appMasterSecret);
        //"544f71eafd98c5a62b002aa3", "rj4jyasjy4iuzkgdjetqlld6xmuwinlz"
        //IOSUnicast unicast = new IOSUnicast("544f71eafd98c5a62b002aa3","rj4jyasjy4iuzkgdjetqlld6xmuwinlz");
        unicast.setDeviceToken(deviceToken);
        unicast.setAlert(title);
        unicast.setBadge(0);
        unicast.setSound("default");
        // TODO set 'production_mode' to 'true' if your app is under production mode
        unicast.setProductionMode();
        //unicast.setTestMode();
        unicast.setCustomizedField("event", "msg");
        client.send(unicast);
    }

    public static void sendIOSListcast(List<String> deviceTokens, String title, String text) throws Exception {
        IOSListcast listcast = new IOSListcast(ios_appkey,ios_appMasterSecret);
        //"544f71eafd98c5a62b002aa3", "rj4jyasjy4iuzkgdjetqlld6xmuwinlz"
        //IOSUnicast unicast = new IOSUnicast("544f71eafd98c5a62b002aa3","rj4jyasjy4iuzkgdjetqlld6xmuwinlz");
        listcast.setDeviceTokens(StringUtils.join(deviceTokens,","));
        listcast.setAlert(title);
        listcast.setBadge(0);
        listcast.setSound("default");
        // TODO set 'production_mode' to 'true' if your app is under production mode
        listcast.setProductionMode();
        //unicast.setTestMode();
        listcast.setCustomizedField("event", "msg");
        client.send(listcast);
    }

    public static void sendTestIOSUnicast(String deviceToken, String title, String text) throws Exception {
        IOSUnicast unicast = new IOSUnicast(ios_appkey,ios_appMasterSecret);
        //"544f71eafd98c5a62b002aa3", "rj4jyasjy4iuzkgdjetqlld6xmuwinlz"
        //IOSUnicast unicast = new IOSUnicast("544f71eafd98c5a62b002aa3","rj4jyasjy4iuzkgdjetqlld6xmuwinlz");
        unicast.setDeviceToken(deviceToken);
        unicast.setAlert(title);
        unicast.setBadge(0);
        unicast.setSound("default");
        // TODO set 'production_mode' to 'true' if your app is under production mode
        //unicast.setProductionMode();
        unicast.setTestMode();
        unicast.setCustomizedField("event", "msg");
        client.send(unicast);
    }

    public static void main(String[] args)throws  Exception{
        //MsgPushUmengUtils.sendAndroidUnicast("Aikr-N0DQoLX0lTszuzOy9DTt0a2vu_1rlSdJWk4Iab4", "test111", "test111", "test");
        List<String> tokens = new ArrayList<>();
        tokens.add("AvKd-zx1Uvjwz_UZ9NWp4rI1tYxeiIsN1I4ClsCmbvzs");
        tokens.add("AiNNi5bn-YLO76Z-Bv2b5iZ-0LN9DET_mRm7h2PXw3Js");
        tokens.add("AjgxQK-tRdylf2RJAmWNT2nwGMbritpGmeKQT6Jmo-hG");
        tokens.add("AoBnUh1dj_oPXxfKvjzMIhshJG2FRG4ZfZUSmJ-xuDoG");
        tokens.add("AhvFK8xNOnA52W-1_0bF4DvAcFYbqZkzCeOXYhvGDpTW");
        tokens.add("AvKd-zx1Uvjwz_UZ9NWp4rI1tYxeiIsN1I4ClsCmbvzs");
        tokens.add("AkyIGwhO58tP8O-ZIXuchKRO2xjPBL05ImS_-duuh8TZ");
        MsgPushUmengUtils.sendAndroidListcast(tokens, "测试下批量消息", "测试下批量消息", "看到此条消息麻烦通知下后端研发");
        //MsgPushUmengUtils.sendIOSListcast(tokens, "test111", "test");
        //MsgPushUmengUtils.sendTestIOSUnicast("5770b17a3c905a2285a194604fcf6364284908c4fb60df74c9a637dd9b65c289", "11111", "111111");
        //MsgPushUmengUtils.sendTestIOSUnicast("Aikr-N0DQoLX0lTszuzOy9DTt0a2vu_1rlSdJWk4Iab4", "test111", "test");
        //MsgPushUmengUtils.sendIOSUnicast("Aikr-N0DQoLX0lTszuzOy9DTt0a2vu_1rlSdJWk4Iab4", "test111", "test");
    }
}
