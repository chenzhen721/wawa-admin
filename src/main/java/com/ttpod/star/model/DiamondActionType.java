package com.ttpod.star.model;


public enum DiamondActionType {
    送礼("send_gift", 0),
    订单退款("order_refunds", 0),
    订单消费("order_cost", 1);

    public String actionName;

    public Integer isIncAction;

    private DiamondActionType(String actionName, Integer isIncAction) {
        this.actionName = actionName;
        this.isIncAction = isIncAction;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public Integer getIsIncAction() {
        return isIncAction;
    }

    public void setIsIncAction(Integer isIncAction) {
        this.isIncAction = isIncAction;
    }
}