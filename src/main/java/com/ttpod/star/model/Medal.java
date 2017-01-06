package com.ttpod.star.model;

import java.util.BitSet;

/**
 * 系统徽章
 */
public enum Medal {

    羽毛徽章(999),签到徽章(998),;


    Medal(Integer id){
        this.id = id;
    }
    private final Integer id;

    public Integer getId() {
        return id;
    }
}
