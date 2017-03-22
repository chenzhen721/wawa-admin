package com.ttpod.star.model;


public enum DiamondActionType {
    送礼("send_gift"),
    订单退款("order_refunds"),
    订单消费("order_cost");

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
