package com.ttpod.star.model;

/**
 * Author: monkey
 * Date: 2017/4/10
 */
public enum RedPacketAcquireType {
    新人奖励("newcomer"),系统发放("system"),好友邀请("friend"),提现拒绝("apply_refuse");
    private  String actionName;

    RedPacketAcquireType(String actionName) {
        this.actionName = actionName;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }
}
