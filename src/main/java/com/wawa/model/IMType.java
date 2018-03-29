package com.wawa.model;


public enum IMType {
    系统消息("msg.system","msg_system")
    ;

    private String action;

    private String event;

    IMType(String action, String event) {
        this.action = action;
        this.event = event;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }
}
