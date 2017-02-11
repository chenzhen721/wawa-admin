package com.ttpod.star.common.util;


import com.ttpod.star.model.User;
import groovy.transform.CompileStatic;

import java.nio.charset.Charset;

/**
 *
 * 约定的key 值
 */
@CompileStatic
public  abstract class KeyUtils {
    public static final Charset UTF8 =Charset.forName("UTF8");

    public  static byte[] serializer(String string){
        return (string == null ? null : string.getBytes(UTF8));
    }

    public  static String decode(byte[] data){
        return data ==null?null : new String (data,UTF8);
    }

    /**
     * 用作redis的标记值
     */
    public static final String MARK_VAL = "";

    private static final String SPLIT_CHAR = ":";

    public static String gifts(Object type) {
        return "all:aiwanzhibo:gifts:" + type;
    }

    public static String all_gifts() {
        return "all:aiwanzhibo:gifts";
    }

    public static String accessToken(String token){
        return "token:"+token;
    }

    public static class USER {

        public static final String USER = "user:";

        public static String hash(Object uid) {
            return USER + uid ;
        }

        public static String authCode(Object uid) {
            return USER + uid +":auth";
        }

        public static String car(Object uid) {
            return USER + uid+":car";
        }

        public static String token(Object uid) {
            return USER + uid + SPLIT_CHAR+ User.Hash.access_token ;
        }

        public static String following(Object uid) {
            return USER + uid + SPLIT_CHAR+ User.following;
        }

        public static String followers(Object uid) {
            return USER + uid + SPLIT_CHAR +User.followers;
        }
        /**
         * 主播历史所有关注用户
         * @param uid
         * @return
         */
        public static String historyFollowers(Object uid) {
            return USER + uid + SPLIT_CHAR + "history"+ SPLIT_CHAR +User.followers;
        }
        public static String giftList(Object uid) {
            return USER + uid + SPLIT_CHAR+ "gift_list" ;
        }
        public static String giftHash(Object uid) {
            return USER + uid + SPLIT_CHAR + "gift_hash";
        }
        public static String blackClient(String uid) {
            return "uidblack:"+uid;
        }

        public static String vip(Object uid) {
            return USER + uid+":vip";
        }
        public static String vip_limit(Object uid) {
            return USER + uid+":vip_limit";
        }

    }

    public static String onlyToken2id(String token) {
        return "ot2id:"+token;
    }

    /**
     * Medal 徽章
     */
    public static class MEDAL {
        public static final String MEDAL = "medal:" ;

        //徽章累计进度
        public static String medalProgress(Object medalId, Object uid){
            return MEDAL+"progress:" + uid + ":" + medalId;
        }
        //用户徽章
        public static String medals(Object medalId, Object uid){
            return MEDAL + uid+":"+medalId;
        }
        //是否存在徽章
        public static String medalsExist(Object uid){
            return MEDAL + "exists:" + uid;
        }
        //徽章id列表
        public static String medalsList(Object uid){
            return MEDAL + "list:" + uid;
        }
    }


    public static class CHANNEL {
        public static String room(Object roomId) {
            return "ROOMchannel:"+roomId;
        }

        public static String user(Object userId) {
            return "USERchannel:"+userId;
        }
        public static String all() {
            return "ALLchannel";
        }

    }


    public static class LUCK {
        public static String powerList() {
            return "luck:powerlist";
        }
        public static String prizePool() {
            return "luck:prizepool";
        }
    }

    /**
     * 直播间
     */
    public static class ROOM {
        public static final String ROOM = "room:";
        public static String shutup(Object roomId, Object uid) {
            return ROOM + roomId + ":shutup:" + uid;
        }
        public static String shutupSet(Object roomId) {
            return ROOM + roomId + ":shutup:set";
        }
        public static String kickSet(Object roomId) {
            return ROOM + roomId + ":kick:set";
        }
        public static String users(Object roomId) {
            return ROOM + roomId + ":users";
        }

        public static String kick(Object roomId, Object uid) {
            return ROOM + roomId + ":kick:" + uid;
        }


        public static String liveFlag(Object roomId) {
            return ROOM + roomId + ":live";
        }


    }
    public static class LIVE {

        public static final String LIVE = "live:";
        public static String all(Object roomId){
            return LIVE+ roomId + ":*";
        }

        public static String sofaHash(Object roomId){
            return LIVE+ roomId + ":sofa:hash";
        }

        public static String sofaZset(Object roomId){
            return LIVE+ roomId + ":sofa:zset";
        }

        public static String userCostZset(Object roomId){
            return LIVE+ roomId + ":user:costzset";
        }

        public static String blackStar(Object uid){
            return LIVE+"black:"+uid;
        }
    }

    /**
     * Message 消息
     */
    public static class MESSAGE {
        public static final String MESSAGE = "msg:" ;

        public static String hash(Object messageId) {
            return MESSAGE + messageId ;
        }
    }


    public static class Actives
    {
        public static final String ACTIVES = "Actives:" ;
        /**
         * 主播参加活动列表
         */
        public static String starList() {
            return ACTIVES +"star:list";
        }

        /**
         * 活动信息缓存
         * @return
         */
        public static String info(Object activityId) {
            return ACTIVES +"info:"+activityId;
        }
    }


    /**
     * 用户昵称 -> id 转换
     */
    public static final String NAME2ID_NS = "_name2id_:";

}
