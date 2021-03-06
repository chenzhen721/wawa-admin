package com.wawa.model;


public enum DiamondActionType {
    充值加钻("user_pay", 0),
    后台加钻("hand_diamond", 0),
    现金兑换("cash_exchange_add_diamond", 0),
    家族奖励("family_award", 0),
    道具奖励("use_item", 0),
    寻宝奖励("family_event", 0),
    翻牌奖励("open_card", 0),
    新手引导("user_guide", 0),
    新用户注册("new_user", 0),
    家族争霸("family_rank", 0),
    家族红包("active_family_redpack", 0),

    钻石翻牌("open_card_decrease", 1),
    开通家族("apply_family_decrease", 1),
    后台减钻("hand_cut_diamond", 1);

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