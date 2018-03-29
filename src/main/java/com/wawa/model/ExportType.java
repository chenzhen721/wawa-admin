package com.wawa.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2014/10/13.
 */
public enum ExportType {
    buy_vip("买VIP"), buy_car("买座驾"), grab_sofa("抢沙发"), song("点歌"), broadcast("发广播"),
    send_gift("送礼"), label("爱签记录"), send_fortune("送财神"), send_treasure("送宝藏"),
    football_shoot("点球"), open_egg("砸蛋"), open_card("翻牌");

    private String desc;

    ExportType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public static class ExportItem {
        private String id;
        private String value;

        ExportItem(String id, String value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public String getValue() {
            return value;
        }
    }

    public static List<ExportItem> getListByType(String type) {
        List<ExportItem> list = null;
        switch (type) {
            case "buy_vip":
                list = VIP_LIST;
                break;
            case "buy_car":
                list = CAR_LIST;
                break;
            case "grab_sofa":
                list = SOFA_LIST;
                break;
            case "song":
                list = SONG_LIST;
                break;
            case "broadcast":
                list = BROADCAST_LIST;
                break;
            case "send_gift":
                list = SEND_GIFT_LIST;
                break;
            case "label":
                list = LABEL_LIST;
                break;
            case "send_fortune":
                list = FORTUNE_LIST;
                break;
            case "send_treasure":
                list = TREASURE_LIST;
                break;
            case "football_shoot":
                list = FOOTBALL_LIST;
                break;
            case "open_egg":
                list = EGG_LIST;
                break;
            case "open_card":
                list = CARD_LIST;
                break;
            default:
                list = new ArrayList<>();
                break;
        }
        return list;
    }

    //基础数据-道具销售流水-买VIP，枚举导出报表的key和value
    public static List<ExportItem> VIP_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("timestamp", "交易时间"));
        this.add(new ExportItem("session._id", "用户ID"));
//        this.add(new ExportItem("session.nick_name", "昵称"));
        this.add(new ExportItem("_id", "交易号"));
        this.add(new ExportItem("cost", "花费柠檬"));
        this.add(new ExportItem("live", "购买天数（天）"));
        this.add(new ExportItem("room", "房间来源"));
    }};

    //基础数据-道具销售流水-买座驾，枚举导出报表的key和value
    public static List<ExportItem> CAR_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("timestamp", "交易时间"));
        this.add(new ExportItem("session._id", "用户ID"));
//        this.add(new ExportItem("session.nick_name", "昵称"));
        this.add(new ExportItem("_id", "交易号"));
        this.add(new ExportItem("cost", "花费柠檬"));
    }};

    //基础数据-道具销售流水-抢沙发，枚举导出报表的key和value
    public static List<ExportItem> SOFA_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("timestamp", "交易时间"));
        this.add(new ExportItem("session._id", "用户ID"));
//        this.add(new ExportItem("session.nick_name", "昵称"));
        this.add(new ExportItem("_id", "交易号"));
        this.add(new ExportItem("room", "所在房间"));
        this.add(new ExportItem("cost", "花费柠檬"));
    }};

    //基础数据-道具销售流水-点歌，枚举导出报表的key和value
    public static List<ExportItem> SONG_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("timestamp", "交易时间"));
        this.add(new ExportItem("session._id", "用户ID"));
        this.add(new ExportItem("room", "房间ID"));
        this.add(new ExportItem("session.data.xy_star_id", "主播ID"));
        this.add(new ExportItem("session.data.song_name", "点歌名称"));
        this.add(new ExportItem("cost", "支付柠檬"));
    }};

    //基础数据-道具销售流水-发广播，枚举导出报表的key和value
    public static List<ExportItem> BROADCAST_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("timestamp", "交易时间"));
        this.add(new ExportItem("session._id", "用户ID"));
//        this.add(new ExportItem("session.nick_name", "昵称"));
        this.add(new ExportItem("room", "所在房间"));
        this.add(new ExportItem("cost", "支付柠檬"));
        this.add(new ExportItem("session.content", "广播内容"));
    }};

    //基础数据-道具销售流水-中奖记录，枚举导出报表的key和value
    public static List<ExportItem> LUCK_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("timestamp", "时间"));
        this.add(new ExportItem("session._id", "用户ID"));
//        this.add(new ExportItem("session.nick_name", "昵称"));
        this.add(new ExportItem("room", "所在房间"));
        this.add(new ExportItem("session.data.name", "中奖礼物"));
        this.add(new ExportItem("power", "倍数"));
        this.add(new ExportItem("got", "获得奖励"));
    }};

    //基础数据-道具销售流水-送礼，枚举导出报表的key和value
    public static List<ExportItem> SEND_GIFT_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("timestamp", "时间"));
        this.add(new ExportItem("session._id", "用户ID"));
//        this.add(new ExportItem("session.nick_name", "昵称"));
        this.add(new ExportItem("room", "房间ID"));
        this.add(new ExportItem("session.data.xy_star_id|session.data.xy_user_id", "赠送给"));
        this.add(new ExportItem("session.data.name", "礼物名称"));
        this.add(new ExportItem("session.data.count", "数量"));
        this.add(new ExportItem("cost", "支付柠檬"));
    }};

    //基础数据-道具销售流水-收礼，枚举导出报表的key和value
    public static List<ExportItem> GIFT_REC_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("timestamp", "时间"));
        this.add(new ExportItem("session.data.xy_star_id|session.data.xy_user_id", "用户ID"));
//        this.add(new ExportItem("session.data.xy_nick", "昵称"));
        this.add(new ExportItem("room", "房间ID"));
        this.add(new ExportItem("session._id", "赠送人"));
        this.add(new ExportItem("session.data.name", "礼物名称"));
        this.add(new ExportItem("session.data.count", "数量"));
        this.add(new ExportItem("cost", "支付柠檬"));
    }};

    //基础数据-道具销售流水-抽奖记录，枚举导出报表的key和value
    public static List<ExportItem> LOTTERY_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("timestamp", "时间"));
        this.add(new ExportItem("user_id", "用户ID"));
//        this.add(new ExportItem("nick_name", "昵称"));
        this.add(new ExportItem("star_id", "主播ID"));
        this.add(new ExportItem("cost_coin", "花费柠檬"));
        this.add(new ExportItem("award_coin", "获得柠檬"));
        this.add(new ExportItem("award_name", "中奖礼物"));
        this.add(new ExportItem("active_name", "活动类型"));
        this.add(new ExportItem("", "备注"));//暂无
    }};

    //基础数据-道具销售流水-爱签记录，枚举导出报表的key和value
    public static List<ExportItem> LABEL_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("timestamp", "时间"));
        this.add(new ExportItem("session._id", "用户ID"));
        this.add(new ExportItem("session.data.xy_star_id", "主播ID"));
        this.add(new ExportItem("session.data.label", "签文内容"));
        this.add(new ExportItem("cost", "支付柠檬"));
    }};

    //基础数据-道具销售流水-奇迹礼物，枚举导出报表的key和value
    public static List<ExportItem> SPECIAL_GIFT_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("_id", "时间"));
        this.add(new ExportItem("gift.name", "奇迹礼物"));
        this.add(new ExportItem("star1._id", "获得者ID"));
//        this.add(new ExportItem("star1.nick_name", "获得者昵称"));
        this.add(new ExportItem("star1.count", "获得个数"));
        this.add(new ExportItem("star1.bonus", "奖励维C"));
        this.add(new ExportItem("star1.bonus_time", "领取时间"));
        this.add(new ExportItem("fan1._id", "创造者ID"));
//        this.add(new ExportItem("fan1.nick_name", "创造者昵称"));
        this.add(new ExportItem("fan1.count", "贡献"));

        this.add(new ExportItem("star2._id", "超星获得者ID"));
        this.add(new ExportItem("star2.count", "超星获得个数"));
        this.add(new ExportItem("star2.bonus", "超星奖励维C"));
        this.add(new ExportItem("star2.bonus_time", "领取时间"));
        this.add(new ExportItem("fan2._id", "超星创造者ID"));
        this.add(new ExportItem("fan2.count", "贡献"));

        this.add(new ExportItem("star3._id", "明星获得者ID"));
        this.add(new ExportItem("star3.count", "明星获得个数"));
        this.add(new ExportItem("star3.bonus", "明星奖励维C"));
        this.add(new ExportItem("star3.bonus_time", "领取时间"));
        this.add(new ExportItem("fan3._id", "明星创造者ID"));
        this.add(new ExportItem("fan3.count", "贡献"));

        this.add(new ExportItem("star4._id", "新星获得者ID"));
        this.add(new ExportItem("star4.count", "新星获得个数"));
        this.add(new ExportItem("star4.bonus", "新星奖励维C"));
        this.add(new ExportItem("star4.bonus_time", "领取时间"));
        this.add(new ExportItem("fan4._id", "新星创造者ID"));
        this.add(new ExportItem("fan4.count", "贡献"));
    }};

    //基础数据-注册和收益
    public static List<ExportItem> REG_PAY_LIST(final String client) {
         return new ArrayList<ExportItem>() {{
            this.add(new ExportItem("qd", "渠道ID"));
            this.add(new ExportItem("name", "渠道名称"));
            this.add(new ExportItem("cny", "付费额(元)"));
            this.add(new ExportItem("pay", "付费人数"));
            this.add(new ExportItem("avg_cny", "人均付费(元)"));
            this.add(new ExportItem("month_cny", "月度付费额(元)"));
            this.add(new ExportItem("month_pay", "月度付费人数"));
            this.add(new ExportItem("avg_cny_month_cny", "月度人均付费(元)"));

             if("H5".equals(client))
                 this.add(new ExportItem("weixin_followers", "微信关注数"));

             if ("Android".equals(client) || "iOS".equals(client) || "RIA".equals(client)) {
                 this.add(new ExportItem("active", "激活数"));
                 this.add(new ExportItem("retention", "激活留存率"));
             } else {
                 this.add(new ExportItem("daylogin", "独立访客"));
                 this.add(new ExportItem("active", "新访客"));
             }

             if("RIA".equals(client))
                 this.add(new ExportItem("install", "安装数"));

            this.add(new ExportItem("reg", "注册数"));
            this.add(new ExportItem("reg_rate", "注册率"));
            this.add(new ExportItem("duration", "平均使用时长"));
            this.add(new ExportItem("speech_rate", "发言率"));
            this.add(new ExportItem("1_day", "次日登录数"));
            this.add(new ExportItem("1_day", "次日登录率"));
            this.add(new ExportItem("3_day", "3日登录数"));
            this.add(new ExportItem("3_day", "3日登录率"));
            this.add(new ExportItem("7_day", "7日登录数"));
            this.add(new ExportItem("7_day_rate", "7日登录率"));
            this.add(new ExportItem("30_day", "30日登录数"));
            this.add(new ExportItem("30_day_rate", "30日登录率"));
        }};
    }

    //基础数据-道具销售流水-送财神，枚举导出报表的key和value
    public static List<ExportItem> FORTUNE_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("timestamp", "交易时间"));
        this.add(new ExportItem("session._id", "用户ID"));
//        this.add(new ExportItem("session.nick_name", "昵称"));
        this.add(new ExportItem("room", "所在房间"));
        this.add(new ExportItem("cost", "花费柠檬"));
        this.add(new ExportItem("session.data.count", "财神个数"));
    }};

    //基础数据-道具销售流水-送宝藏，枚举导出报表的key和value
    public static List<ExportItem> TREASURE_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("timestamp", "交易时间"));
        this.add(new ExportItem("session._id", "用户ID"));
//        this.add(new ExportItem("session.nick_name", "昵称"));
        this.add(new ExportItem("room", "所在房间"));
        this.add(new ExportItem("cost", "花费柠檬"));
        this.add(new ExportItem("session.treasure.count", "宝藏个数"));
    }};

    //基础数据-道具销售流水-点球大战，枚举导出报表的key和value
    public static List<ExportItem> FOOTBALL_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("timestamp", "时间"));
        this.add(new ExportItem("session._id", "用户ID"));
//        this.add(new ExportItem("session.nick_name", "昵称"));
        this.add(new ExportItem("room", "所在房间"));
        this.add(new ExportItem("cost", "花费柠檬"));
        this.add(new ExportItem("session.got", "获得奖励"));
    }};

    //基础数据-道具销售流水-砸蛋，枚举导出报表的key和value
    public static List<ExportItem> EGG_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("timestamp", "时间"));
        this.add(new ExportItem("session._id", "用户ID"));
//        this.add(new ExportItem("session.nick_name", "昵称"));
        this.add(new ExportItem("room", "所在房间"));
        this.add(new ExportItem("cost", "花费柠檬"));
        this.add(new ExportItem("session.got", "获得奖励"));
    }};

    //基础数据-道具销售流水-翻牌，枚举导出报表的key和value
    public static List<ExportItem> CARD_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("timestamp", "时间"));
        this.add(new ExportItem("session._id", "用户ID"));
//        this.add(new ExportItem("session.nick_name", "昵称"));
        this.add(new ExportItem("room", "所在房间"));
        this.add(new ExportItem("cost", "花费柠檬"));
        this.add(new ExportItem("session.got_coin", "获得柠檬"));
    }};

    //网站管理-加减柠檬，枚举导出报表的key和value
    public static List<ExportItem> FINANCE_LIST = new ArrayList<ExportItem>() {{
        this.add(new ExportItem("timestamp", "时间"));
        this.add(new ExportItem("cny", "充值金额"));
        this.add(new ExportItem("coin", "充值柠檬"));
        this.add(new ExportItem("user_id", "充值用户"));
        this.add(new ExportItem("to_id", "到账用户"));
        this.add(new ExportItem("via", "支付方式"));
        this.add(new ExportItem("qd", "访问渠道"));
    }};

}
