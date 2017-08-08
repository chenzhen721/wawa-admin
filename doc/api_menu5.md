# 财务数据

## 充值统计（同线上）
### 列表

## 财务月报（待定，未完成）
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

## 现金报表
### 列表

* API {GET|POST} http://test-aiadmin.memeyule.com/stat/cash_log.json
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
            "apply_refuse": "现金兑换",
            "new_user": "新人奖励",
            "use_item": "道具奖励",
            "family_event": "寻宝奖励",
            "open_card": "翻牌奖励"
        },
        "desc": { //减钻石
            "apply_cash": "申请提现",
            "cash_exchange_decrease_cash": "后台减钻"
        }
    },
    "count": 984,
    "data": [
        {
            "_id": "20170719_finance",
            "cash_inc": { //加钻石
                "total": 317,
                "use_item": 8,
                "open_card": 167,
                "family_award": 2,
                "family_event": 2
            },
            "cash_dec": { //减钻石
                "total": 0
            },
			"cash_pay": {
				"total_cash": 10000, 
				"total_expand": 8000
			},
            "cash_begin_surplus": 0, //昨日结余
            "cash_end_surplus": 317, //当日结余
            "cash_today_balance": 317, //合计
            "timestamp": 1500393600000 //时间
        }
    ],
    "code": 1,
    "all_page": 164
}
```

## 真实钻石比例（同线上）


## 钻石变更明细（同线上）

