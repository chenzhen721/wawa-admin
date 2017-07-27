# 日常运营

## 公告通知 - 消息通知
### 1.通知列表


* API {GET|POST} http://test-aiadmin.memeyule.com/push/list.json
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
        "user_ids": [1201065], //消息推送的用户ID
        "push_type": 0, //推送类型
        "text": "哈哈家族，嗨起来！houhou,EEE", //运营消息内容
        "link_url": "",  //运营消息链接
        "img_url": "", //运营消息图片
        "isNotify": true, //是否推送通知栏 （以下字段为true的时候可用）
        "umeng_event": "redirect_msg", //通知栏事件类型: 
                                       //    打开首页("redirect_app"),
                                       //    跳至消息("redirect_msg"),
                                       //    打开房间("redirect_room"),
                                       //    跳至页面("redirect_url");
        "umeng_title": "wo shi title", //通知栏标题
        "umeng_text": "wo shi text",   //通知栏内容
        "umeng_event_room_id": "",      //跳转房间ID
        "umeng_event_url": "",         //跳转页面地址
        "status": "1",                 //推送状态 0-未推送 1-已推送 2-已取消
        "stime": "",                   //推送开始时间
        "etime": "",                   //推送结束时间
        "timestamp": 1499761668656     //创建时间
    }],
    "code": 1,
    "all_page": 12
}
```

### 2.通知添加/修改

* API 添加 {GET|POST} http://test-aiadmin.memeyule.com/push/add.json  
      修改 {GET|POST} http://test-aiadmin.memeyule.com/push/edit.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
\_id|String|false||edit必传
user_ids|String|false||推送用户ID,以逗号分隔
push_type|int|true|默认为0|推送类型 0-指定用户推送 1-全部用户推送
text|String|false||运营消息内容（与img_url至少选其一）
img_url|String|false||运营消息图片（与text至少选其一）
link_url|String|false||运营消息链接
is_notify|Boolean|true|true,false|是否推送通知栏(以下字段标记为必传取决于notify为true)
umeng_event|String|true||打开首页("redirect_app"),跳至消息("redirect_msg"),打开房间("redirect_room"),跳至页面("redirect_url");
umeng_title|String|true||通知栏标题
umeng_text|String|true||通知栏内容
umeng_event_room_id|int|false||跳转房间ID
umeng_event_url|string|false||跳转页面地址
status|int|true|默认为0|推送状态 0-未推送 1-已推送 2-已取消
stime|String|false|yyyy-MM-dd HH: mm:ss|预期推送开始时间(暂不支持，二期实现)
etime|String|false|yyyy-MM-dd HH: mm:ss|预期推送结束时间（暂不支持，二期实现）

* 返回

    成功
```json
{"code": 1}
```
    失败
```json
{"code": 0, "msg":"失败原因"}
```

### 3.修改消息状态

* API 修改 {GET|POST} http://test-aiadmin.memeyule.com/push/change_status.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
\_id|String|true||记录ID
status|int|true||推送状态 1-已推送（立即推送） 2-已取消 不支持修改为0

* 返回

    成功
```json
{"code": 1}
```
    失败
```json
{"code": 0, "msg":"失败原因"}
```

## 英雄令
### 1.活动配置列表
* API {GET|POST} http://test-aiadmin.memeyule.com/herolinks/list.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
status|bool|false||是否激活的活动：否-false；是-true
page|int|false||页码
size|int|false||每页记录数

* 返回
```json
{
    "count": 17,
    "data": [{
        "_id": "1201085_1499852578136",
        "title": "萌新806811", //活动名称
        "content": "20170712", //活动内容
        "click_url": 200, //活动详情地址
        "order": 1, //  有多个活动生效时展示优先级，越小越靠前
        "status": true, //是否开启活动：否-false；是-true
        "stime": 1499852578140, //活动开始时间
        "etime": 1499852578140, //活动结束时间
        "timestamp": 1499852578140 //创建时间
    }],
    "code": 1,
    "all_page": 1
}
```

### 2.英雄令增加/修改
* API 添加 {GET|POST} http://test-aiadmin.memeyule.com/push/add.json  
      修改 {GET|POST} http://test-aiadmin.memeyule.com/push/edit.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
_id|string|修改必传||
title|string|true||英雄令标题
content|string|true||英雄令说明
img_url|string|false||英雄令图标url
click_url|string|true||英雄令H5地址
order|int|false|默认取值1|多个英雄令同时生效的情况下，按照order取最小的
status|bool|true|true/false|是否有效
stime|String|false|yyyy-MM-dd HH: mm:ss|预期推送开始时间
etime|String|false|yyyy-MM-dd HH: mm:ss|预期推送结束时间

说明：当有多个有效英雄令上线时，按照order顺序排列取第一个。发起修改时不修改的字段传null或不传

* 返回
```json
{"code": 1}
```

## 卡牌管理
### 1.卡牌列表
* API {GET|POST} http://test-aiadmin.memeyule.com/card/list.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
status|int|||卡牌是否激活：0-否；1-是
category|int|||卡牌分类：1-攻击卡牌, 2-防守卡牌, 3-金币卡牌, 4-偷盗卡牌
type|int|||卡牌有效性：1-永久,2-一次性
page|int|false||页码
size|int|false||每页记录数

* 返回
```json
{
    "count": 17,
    "data": [{
        "_id": "", //卡牌ID
        "status": 1, //卡牌是否激活：0-否；1-是
        "type": 1, //卡牌有效性：1-永久,2-一次性
        "category": 1, //卡牌分类：1-攻击卡牌, 2-防守卡牌, 3-金币卡牌, 4-偷盗卡牌
        "cate_pic":　"", //卡牌分类图片
        "level": 1, //卡牌等级
        "pic": "", //等级背景图片
        "next_level_id": "", //下一级卡牌ID
        "cds": [], //可能比较长
        "levelup": 2, //升级消耗
        "coin_rate": 0.1, //命中率
        "coin_min": 1,  //概率区间下界
        "coin_max": 100, //概率区间上界
        "cash_rate": 0.1,//命中率
        "cash_min": 1,//概率区间下界
        "cash_max": 1,//概率区间上界
        "exp_rate": 1,//命中率
        "exp_min": 1,//概率区间下界
        "exp_max": 1,//概率区间上界
        "diamond_rate": 1,//命中率
        "diamond_min": 1,//概率区间下界
        "diamond_max": 1,//概率区间上界
        "ack_rate": 1,//命中率
        "ack_min": 1,//概率区间下界
        "ack_max": 1,//概率区间上界
        "def_rate": 1,//命中率
        "def_min": 1,//概率区间下界
        "def_max": 1,//概率区间上界
        "steal_rate": 1,//命中率
        "steal_min": 1,//概率区间下界
        "steal_max": 1,//概率区间上界
        "timestamp": 1499852578140 //创建时间
    }],
    "code": 1,
    "all_page": 1
}
```
说明： 字段中奖励参数意义 coin-金币、cash-现金、exp-经验、diamond-钻石、ack-攻击道具、def-防守道具、steal-偷盗道具

### 2.卡牌增加/修改
* API 添加 {GET|POST} http://test-aiadmin.memeyule.com/card/add_card.json  
      修改 {GET|POST} http://test-aiadmin.memeyule.com/card/edit_card.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
_id|string|edit必传||
status|int|true|0,1|//卡牌是否激活：0-否；1-是
type|int|true|1,2|卡牌有效性：1-永久,2-一次性
category|int|true|1,2,3,4|卡牌分类：1-攻击卡牌, 2-防守卡牌, 3-金币卡牌, 4-偷盗卡牌
cate_pic|string|false||分类图标
level|int|true||卡牌等级
pic|string|false||等级背景图片
next_level_id|string|false||下一级卡牌ID
cds|string|false|默认使用永久卡牌定义的CD|不同翻卡次数对应的卡牌CD，不填使用默认值,多个值之间用","隔开
levelup|int|true||升级消耗的星辰
coin_rate|double|true||命中率取值0-1
coin_min|int|true||概率区间下界
coin_max|int|true||概率区间上界
cash_rate|double|true||命中率取值0-1
cash_min|int|true||概率区间下界
cash_max|int|true||概率区间上界
exp_rate|double|true||命中率取值0-1
exp_min|int|true||概率区间下界
exp_max|int|true||概率区间上界
diamond_rate|double|true||命中率取值0-1
diamond_min|int|true||概率区间下界
diamond_max|int|true||概率区间上界
ack_rate|double|true||命中率取值0-1
ack_min|int|true||概率区间下界
ack_max|int|true||概率区间上界
def_rate|double|true||命中率取值0-1
def_min|int|true||概率区间下界
def_max|int|true||概率区间上界
steal_rate|double|true||命中率取值0-1
steal_min|int|true||概率区间下界
steal_max|int|true||概率区间上界

* 返回
```json
{"code": 1}
```

### 3.卡牌删除
* API 添加 {GET|POST} http://test-aiadmin.memeyule.com/card/del.json  

* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
_id|string|true||

* 返回
```json
{"code": 1}
```


## 提现管理
### 1.提现列表

* API {GET|POST} http://test-aiadmin.memeyule.com/cash/apply_logs.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
user_id|int|false||用户ID
page|int|false||页码
size|int|false||每页记录数

* 返回
```json
{
    "count": 17,
    "data": [{
        "_id": "1201085_1499852578136",
        "nick_name": "萌新806811", //昵称
        "date": "20170712",
        "amount": 200, //申请提现金额
        "income": 200, //到账金额
        "status": 1, //  1未处理，2通过，3拒绝 
        "last_modify": 1499941995599, //更新时间
        "user_id": 1201085, //用户ID
        "account": "ooUNZwajJWx-SCWyvu7rspNlAH0Q", //微信OPENID
        "timestamp": 1499852578140, //申请时间
        "batch_id": 1499941995599 //批量ID
    }],
    "code": 1,
    "all_page": 1
}
```

### 2.批量通过

* API {GET|POST} http://test-aiadmin.memeyule.com/cash/batch_pass.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
_ids|string|true||记录ID，多个以逗号隔开

* 返回
```json
{
    "code": 1,
    "data": "123" //生成微信红包同步文件内容  
}
```

### 3.批量拒绝

* API {GET|POST} http://test-aiadmin.memeyule.com/cash/batch_refuse.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
_ids|string|true||记录ID，多个以逗号隔开

* 返回
```json
{ "code": 1 }
```