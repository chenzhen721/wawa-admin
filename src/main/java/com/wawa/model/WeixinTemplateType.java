package com.wawa.model;

/**
 * Created by monkey on 2016/10/18.
 */
public enum WeixinTemplateType {
    // 文字信息
    TEXT("text"),
    // 图文信息
    NEWS("news");

    WeixinTemplateType(String msgType){
        this.msgType = msgType;
    }

    private String msgType;

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

}
