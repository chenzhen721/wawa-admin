# 运营数据

## 数据月报（同线上）
### 列表

## 运营数据总表
### 列表

* API {GET|POST} http://test-apiadmin.17laihou.com/stat/total_report.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束

* 返回

```json
{
    "count": 11, 
    "data": [{
        "_id": "20171129_allreport", 
        "pay_cny": 1254,   //充值金额
        "pay_user": 75,    //充值人数 
                           //充值ARPU 充值金额/充值人数
        "pay_coin": 13465, //充钻数 没要不展示
        "logins": 3267,    //日活
        "doll_count": 5052,  //抓取次数
        "bingo_count": 214,  //抓中次数
        "user_count": 1748,  //抓取人数
        "diamond_count": 101225,  //钻石消耗
        "regs": 2441,      //新增总人数
        "reg_pay_cny": 0,  //新增充值金额
        "reg_pay_user": 0, //新增充值人数
                           //新增ARPU  新增充值/新增充值人数
        "reg_count": 3616,  //新增用户抓取次数
        "reg_user_count": 1578, //新增用户抓取人数
        "1_pay": 0,         //充值用户次日流程
        "type": "allreport", 
        "stay": {
            "1_day": 0,     //次日留存
            "3_day": 0,     //3日留存
            "7_day": 0,     //7日留存
            "30_day": 0     //30日留存
        }, 
        "timestamp": 1511884800000  //统计时间
    }],
    "code": 1,
    "all_page": 12
}
```

## 登录日统计 （同线上）

## 钻石日报表
### 列表

* API {GET|POST} http://test-aiadmin.memeyule.com/stat/diamond_log.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束

* 返回

```json
{
    "title": { //表头信息
        "inc": {  // 加钻石
            "user_pay": "充值加钻",
            "hand_coin": "后台加钻",
            "cash_exchange_add_diamond": "现金兑换",
            "family_award": "家族奖励",
            "use_item": "道具奖励",
            "family_event": "寻宝奖励",
            "open_card": "翻牌奖励"
        },
        "dec": { //减钻石
            "open_card": "钻石翻牌",
            "apply_family": "开通家族",
            "hand_cut_diamond": "后台减钻"
        }
    },
    "count": 984,
    "data": [
        {
            "_id": "20170719_finance",
            "inc": { //加钻石
                "total": 317,
                "use_item": 8,
                "open_card": 167,
                "family_award": 2,
                "family_event": 2
            },
            "dec": { //减钻石
                "total": 0
            },
            "begin_surplus": 0, //昨日结余
            "end_surplus": 317, //当日结余
            "today_balance": 317, //合计
            "timestamp": 1500393600000 //时间
        }
    ],
    "code": 1,
    "all_page": 164
}
```
