# 用户数据

## 用户管理-用户信息（同线上）

## 用户管理-用户详情
### 基本资料（同线上）

### 翻卡信息

* API {GET|POST} http://test-aiadmin.memeyule.com/user/open_card_list.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
user_id|int|false||用户ID
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束

* 返回

```json
{
    "all_page": 74,
    "code": 1,
    "data": [
        {
            "_id": "1201085_a0004_1501743056651", //流水号
            "user_id": 1201085,   //用户ID
            "type": "open_card",
            "card_id": "a0004",   //卡牌ID
            "award": {
                "steal": 0,    
                "defense": 0,
                "exp": 1173,
                "coin": 464,
                "diamond": 0,
                "cash": 0,
                "attack": 0
            },
            "timestamp": 1501743056651,  //时间
            "nick_name": "萌新806811"   //用户昵称
        }
    ],
    "count": 443
}
```
说明： attack-攻击道具数 defense-防守道具数 steal-偷盗道具数 
       exp-经验值 coin-金币数 diamond-钻石数 cash-现金数 



### 道具使用

* API {GET|POST} http://test-aiadmin.memeyule.com/user/use_item_list.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
user_id|int|false||用户ID
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束

* 返回

```json
{
    "all_page": 74,
    "code": 1,
    "data": [
        {
            "_id": "1201085_1202584_1_1501583337320",
            "user_id": 1201085, //攻击人ID
            "type": "武器",
            "target_uid": 1202584, //被攻击人ID
            "users": [
                1201085,
                1202584
            ],
            "timestamp": 1501583337344,
            "reward": {
                "coin": 1645,
                "diamond": 0,
                "is_win": true
            },
            "nick_name": "萌新806811", //攻击人昵称
            "target_nick_name": "浅紫色的伤" //被攻击人昵称
        }
    ],
    "count": 443
}
```
说明： attack-攻击道具数 defense-防守道具数 steal-偷盗道具数 
       exp-经验值 coin-金币数 diamond-钻石数 cash-现金数 



### 挖矿

* API {GET|POST} http://test-aiadmin.memeyule.com/user/family_event_list.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
user_id|int|false||用户ID
family_id|int|false||家族ID
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束

* 返回

```json
{
    "all_page": 74,
    "code": 1,
    "data": [
        {
            "_id": "1201263_1501757285569",
            "allow_count": 7,
            "users": [   //参与用户ID
                1201085
            ],
            "family_id": 1201263, //家族ID
            "status": 4,
            "timestamp": 1501757285569, //时间
            "lastmodif": 1501757464764,
            "join_time": 1501757403404,
            "award_time": 1501757462664, 
            "family_name": "破骑V5",  //家族名称
            "reward": {   //获得奖励
                "diamond": 0,
                "steal": 1,
                "defense": 0,
                "attack": 0,
                "exp": 0,
                "cash": 1,
                "coin": 1200
            }
        }
    ],
    "count": 443
}
```
说明： attack-攻击道具数 defense-防守道具数 steal-偷盗道具数 
       exp-经验值 coin-金币数 diamond-钻石数 cash-现金数 


### 家族房贡献金币

* API {GET|POST} http://test-aiadmin.memeyule.com/user/family_contribution_list.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
user_id|int|false||用户ID
family_id|int|false||家族ID
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束

* 返回

```json
{
    "all_page": 74,
    "code": 1,
    "data": [
        {
            "_id": "20170803_1202240_1201263",
            "coin": 300, //贡献金币
            "user_id": 1202240,  //用户ID
            "family_id": 1201263, //家族ID
            "date": "20170803",
            "timestamp": 1501750706091, //日期
            "cvalue": 0.004,
            "nick_name": "haha",   //用户昵称
            "family_name": "破骑V5" //家族昵称
        }
    ],
    "count": 443
}
```


### 用户上麦流水

* API {GET|POST} http://test-aiadmin.memeyule.com/user/on_mic_list.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
user_id|int|false||用户ID
family_id|int|false||家族ID
stime|String|false|yyyy-MM-dd HH: mm:ss|开始时间
etime|String|false|yyyy-MM-dd HH: mm:ss|结束时间

* 返回

```json
{
    "all_page": 2,
    "code": 1,
    "data": [
        {
            "_id": "20170804_mic_1201284_1201350_1501827186833",
            "type": "on_mic_user_log",
            "user_id": 1201284,
            "family_id": 1201350,
            "timestamp": 1501776000000, //用于查询
            "start_mic": 1501827186833, //开始时间
            "end_mic": 1501827238354, //结束时间
            "duration": 51521,  //上麦时间（单位：毫秒）
            "nick_name": "K",   //用户昵称
            "family_name": "咯ha" //家族房名称
        }
    ],
    "count": 8
}
```

## 用户标签管理
### 1.标签列表

* API {GET|POST} http://test-aiadmin.memeyule.com/tag/list.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
page|int|false||页码
size|int|false||每页记录数

* 返回
```json
{
    "all_page": 1,
    "code": 1,
    "data": [
        {
            "_id": 12793,
            "cat": "LYB",
            "timestamp": 1504064136428
        }
    ],
    "count": 1
}
```

### 2.标签增加/修改
* API 添加 {GET|POST} http://test-aiadmin.memeyule.com/tag/add.json  
      修改 {GET|POST} http://test-aiadmin.memeyule.com/tag/edit.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
_id|int|||添加时不需要
cat|str|true||标签名称


* 返回
```json
{"code": 1}
```

### 3.标签删除
* API 添加 {GET|POST} http://test-aiadmin.memeyule.com/tag/del.json  

* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
_id|string|true||

* 返回
```json
{"code": 1}
```
