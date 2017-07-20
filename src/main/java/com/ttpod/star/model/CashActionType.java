package com.ttpod.star.model;


public enum CashActionType {
    现金兑换("apply_refuse", 0),//
    道具奖励("use_item", 0),//
    寻宝奖励("family_event", 0),//
    翻牌奖励("open_card", 0),//
    新人奖励("new_user", 0),//

    申请提现("apply_cash", 1),//
    后台减钻("cash_exchange_decrease_cash", 1);//

    public String actionName;

    public Integer isIncAction;

    private CashActionType(String actionName, Integer isIncAction) {
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