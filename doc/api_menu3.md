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

* API {GET|POST} http://test-aiadmin.memeyule.com/stat/use_item.json
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

