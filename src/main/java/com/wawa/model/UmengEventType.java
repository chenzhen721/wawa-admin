package com.wawa.model;

/**
 * Created by Administrator on 2017/7/10.
 */
public enum UmengEventType {
    打开首页("redirect_app"),
    跳至消息("redirect_msg"),
    打开房间("redirect_room"),
    跳至页面("redirect_url");

    public String eventType;

    UmengEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}
