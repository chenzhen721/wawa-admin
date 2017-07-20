# 运营数据

## 数据月报（同线上）
### 列表

## 运营数据总表（同线上）
### 列表

## 翻牌统计
### 列表

* API {GET|POST} http://test-aiadmin.memeyule.com/stat/open_card_log.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束

* 返回

```json
{
    "count": 68,
    "data": [{
        "_id": "12781_1499761668655",
        "user_count": 10, //参与用户数
        "total_count": 0, //总参与人次
        "type": "open_card", //记录类型
        "defense": 1, // 防守道具总数
        "steal": 2, // 偷盗道具总数
        "exp": 3, // 经验值总数
        "coin": 4, // 金币总数
        "diamond": 5, // 钻石总数
        "cash": 6, // 现金总数
        "attack": 7, // 攻击道具总数
        "timestamp": 1499761668656     //统计日期
    }],
    "code": 1,
    "all_page": 12
}
```

## 挖矿统计
### 列表

* API {GET|POST} http://test-aiadmin.memeyule.com/stat/family_event_log.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束

* 返回

```json
{
    "count": 68,
    "data": [{
        "_id": "12781_1499761668655",
        "user_count": 10, //参与用户数
        "total_count": 0, //总参与人次
        "type": "family_event", //记录类型
        "defense": 1, // 防守道具总数
        "steal": 2, // 偷盗道具总数
        "exp": 3, // 经验值总数
        "coin": 4, // 金币总数
        "diamond": 5, // 钻石总数
        "cash": 6, // 现金总数
        "attack": 7, // 攻击道具总数
        "timestamp": 1499761668656     //统计日期
    }],
    "code": 1,
    "all_page": 12
}
```

## 道具使用统计
### 列表

* API {GET|POST} http://test-aiadmin.memeyule.com/stat/use_item_log.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束

* 返回

```json
{
    "count": 68,
    "data": [{
        "_id": "12781_1499761668655",
        "user_count": 10, //参与用户数
        "total_count": 0, //总参与人次
        "type": "family_event", //记录类型
        "defense": 1, // 防守道具总数
        "steal": 2, // 偷盗道具总数
        "exp": 3, // 经验值总数
        "coin": 4, // 金币总数
        "diamond": 5, // 钻石总数
        "cash": 6, // 现金总数
        "attack": 7, // 攻击道具总数
        "timestamp": 1499761668656     //统计日期
    }],
    "code": 1,
    "all_page": 12
}
```

## 房间上麦统计
### 列表

* API {GET|POST} http://test-aiadmin.memeyule.com/stat/mic_log.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束

* 返回

```json
{
    "count": 68,
    "data": [{
          "_id": "",  //
         "total_count": 13,  //总上麦人次
         "type": "on_mic", //
         "timestamp": 1500220800000, //统计日期
         "room_count": 3,   //房间数
         "user_count": 5, //总参与人数
         "duration": 110374697 //总上麦时长（millisecond）
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
        "desc": { //减钻石
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
