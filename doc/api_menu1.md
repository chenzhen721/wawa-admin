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