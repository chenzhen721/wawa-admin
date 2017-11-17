package com.ttpod.star.web.api.play.model;

/**
 * Created by Administrator on 2017/11/10.
 */
public enum OperateType {
    NULL, 后退, 前进, 左移, 右移, 抓取娃娃, 后退终止, 前进终止, 左移终止, 右移终止;


    public static OperateType getByIndex(int index) {
        OperateType[] operateTypes = OperateType.values();
        if (index < 0 || index >= operateTypes.length) {
            return null;
        }
        return operateTypes[index];
    }

}
