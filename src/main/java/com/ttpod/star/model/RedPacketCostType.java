package com.ttpod.star.model;

/**
 * Author: monkey
 * Date: 2017/4/10
 */
public enum RedPacketCostType {
    兑换阳光("exchange");
    private  String actionName;

    RedPacketCostType(String actionName) {
        this.actionName = actionName;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }
}
