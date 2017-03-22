package com.ttpod.star.model;


public enum DiamondActionType {
    送礼("send_gift"),
    商城退款("shop_refunds"),
    商城消费("shop_cost");

    public String actionName;

    private DiamondActionType(String actionName) {
        this.actionName = actionName;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }
}
