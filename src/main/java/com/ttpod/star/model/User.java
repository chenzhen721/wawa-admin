package com.ttpod.star.model;

import groovy.transform.CompileStatic;

/**
 * 用户相关内容
 * date: 12-8-17 上午11:50
 *
 * @author: yangyang.cong@ttpod.com
 */
@CompileStatic
public interface User {


    public static interface Hash{

        String name = "nick_name";


        /**
         * 用户头像 URL
         */
        String headImg = "headImg";


        /**
         * 性别
         */
        String sex = "sex";


        /**
         * 最后活动时间，上下线的时候更新此值
         */
        String lastLogin = "lastLogin";



        /**
         * 新的未读微博数
         */
        String newS = "newS";

        /**
         * 新的粉丝数
         */
        String newF = "newF";

        /**
         * 【不保存】 ，展示时动态 pipline 获取对应size
         * 减少数据同步带来的复杂性
         */
        String statusesCount = "statusesCount";

        /**
         * 【不保存】
         * 减少数据同步带来的复杂性
         */
        String followersCount = "followersCount";

        /**
         * 【不保存】
         * 减少数据同步带来的复杂性
         */
        String followingCount = "followingCount";

        /**
         * 评论数量提示 【不保存】
         * 减少数据同步带来的复杂性
         */
        String commentCount = "commentCount" ;


        /**
         * 消息总量
         */
        String totalCount = "totalCount" ;


        /**
         * 是否关注了对方
         */
        String isFollowing = "isFollowing";

        /**
         * 是否为我的粉丝
         */
        String isFollowers = "isFollowers";


        /**
         * 音乐标签【不保存】
         */
       String musicLabel = "musicLabel" ;

        /**
         * 用户收藏的歌曲列表 【不保存】
         */
        String musicList = "musicList" ;

        /**
         * 用户收藏的歌曲Id列表 【不保存】
         */
        String musicIds = "musicIds" ;


        /**
         * 用户 Id 【不保存】
         */
        String userId = "userId" ;

        /**
         * 品味 【不保存】
         */
        String taste = "taste" ;

        /**
         * 收藏数量 【不保存】
         */
        String favnum = "favnum" ;

        /**
         * 用户系统 主键id
         */
        String tuid = "tuid";

        String access_token = "access_token" ;

    }


    /**
     * 用户关注
     */
    String following = "following";


    /**
     * 用户粉丝
     */
    String followers = "followers";

    /**
     * 时间线。微博墙
     */
    String timeline = "timeline";





}
