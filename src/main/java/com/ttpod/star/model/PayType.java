package com.ttpod.star.model;

import java.util.*;

/**
 * date: 13-3-30 下午2:14
 *
 * @author: yangyang.cong@ttpod.com
 */
@Deprecated
public enum PayType {

    Ipay,YB,ali_m,ali_pc;


    public static String[] PC ={"YB","vpay","ali_pc","unipay","kb","Ali", "tenpay"};

    public static String[] MOBILE ={"Ipay","ledou","ayouxi","itunes","km-","ali_m","tenpay_m","unionpay",
            "ali_wap","vpay_SMS","3g", "op-", "hulu", "paypal","YB_wap", "vnet_SMS","yl-","kx-","ql-"};

    static class  PayItem{

        public String id ="";
        public String desc = "";
        PayItem(String id, String desc){
            this.id = id;
            this.desc = desc;
        }
    }
    //易宝:手机充值卡 游戏卡
    public static List<PayItem> PC_LIST = new ArrayList<PayItem>(){{
        this.add(0,new PayItem("ali_pc","支付宝"));
        this.add(1, new PayItem("YB","易宝"));
        this.add(2, new PayItem("vpay","V币电话钱包"));
        this.add(3, new PayItem("paypal","paypal(贝宝)"));
        this.add(4, new PayItem("vpay_SMS","盈华短信支付"));
        this.add(3,new PayItem("tenpay","财付通"));

    }};

    public static List<PayItem> MOBILE_LIST = new ArrayList<PayItem>(){{
        this.add(0,new PayItem("ali_m","支付宝移动"));
        this.add(1,new PayItem("unionpay","中国银联"));
        this.add(2,new PayItem("YB_wap","易宝"));
        this.add(3,new PayItem("tenpay_m","财付通移动"));
        this.add(4,new PayItem("Ipay","爱贝"));
        this.add(5,new PayItem("itunes","苹果IAP"));
        this.add(6,new PayItem("ali_wap","支付宝wap"));
        this.add(7,new PayItem("unipay","联通沃商店"));
        this.add(8,new PayItem("vnet_SMS","盈华短信支付"));
        this.add(9,new PayItem("kx-","开迅"));
        this.add(9,new PayItem("ql-","奇天乐地"));
    }};

    @Deprecated//TODO 联运代码已过期，待后续确认
    public static List<PayItem> QD_LIST = new ArrayList<PayItem>(){{
        this.add(0, new PayItem("ledou","乐逗"));
        this.add(0, new PayItem("ayouxi","阿游戏"));
        this.add(0, new PayItem("km-","快播移动"));
        this.add(0, new PayItem("kb","快播"));
        this.add(0, new PayItem("3g","3g"));
        this.add(0, new PayItem("op-","欧朋"));
        this.add(0, new PayItem("hulu","水葫芦"));
        this.add(0, new PayItem("yl-","缘来网"));
    }};

}
