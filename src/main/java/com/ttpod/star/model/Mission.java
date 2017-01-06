package com.ttpod.star.model;

import java.util.BitSet;

/**
 * date: 13-3-3 上午11:06
 *
 * @author: yangyang.cong@ttpod.com
 */
public enum Mission {

    注册("register"),修改昵称("edit_nick"),设置桌面图标("add_icon"),送礼物("send_gift"),
    关注主播("add_following"),抢沙发("grab_sofa")
    ,;


    Mission(String id){
        this.id = id;
    }
    public final String id;

    public String getId() {
        return id;
    }



    private final BitSet users = new BitSet(10000000);

    public boolean hasUser(Integer userId){
        return users.get(userId);
    }

    public void addUser(Integer userId){
        users.set(userId);
    }

    public static enum Status{
        完成未领取奖金,奖金已领取,不再显示;
    }
}
