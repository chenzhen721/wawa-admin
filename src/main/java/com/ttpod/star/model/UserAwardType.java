package com.ttpod.star.model;

/**
 *用户奖励类型
 */
public enum UserAwardType {
    UnKnown(null),翻牌("open_card"),挖矿("family_event"),新用户注册("new_user"),新手引导("user_guide");

    private String id;

    UserAwardType(String id){
        this.id = id;
    }

    public String getId(){
        return this.id;
    }
}
