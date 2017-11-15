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

## 娃娃管理
### 1.申请邮寄列表

* API {GET|POST} http://test-aiadmin.memeyule.com/catchu/post_list.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束
user_id|int|false||用户ID
room_id|int|false||用户ID
page|int|false||页码
size|int|false||每页记录数

* 返回
```json
{
    "all_page": 2,
    "code": 1,
    "data": [
        {
            "_id": "1310933_10000017_1508840521",
            "user_id": 1209443, 
            "room_id": 10000017,
            "toy": {    //娃娃详情
                "_id": 12871,
                "name": "wawa1",
                "pic": "http://img-album.b0.upaiyun.com/20059349/0724/bdf3bf1da3405725be763540d6601144.jpg",
                "desc": "娃娃1的说明"
            },
            "timestamp": 1508840526135,  //游戏开始时间
            "pack_id": "1209443_1509593136599",
            "post_type": 1, //申请类型  1 待发货 2 已发货 3 确认收货 4 已拒绝
            "apply_time": 1508840526135, //申请时间
            "address": {   //邮寄地址
                "_id": "1209443_1509593108874",
                "province": "北京",
                "city": "东城区",
                "region": "",
                "address": "  bjhvbbh",
                "name": "fcgvg",
                "tel": "182585885",
                "is_default": true
            }
        }
    ],
    "count": 9
}
```

### 2.批量审批

* API {GET|POST} http://test-aiadmin.memeyule.com/catchu/batch_pass.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
_ids|string|true||记录ID，多个以逗号隔开
type|bool|true||true通过审批， false拒绝

* 返回
```json
{
    "code": 1,
    "data": [
        {
            "_id": "1310933_10000017_1508840526134",
            "toy": {
                "_id": 12871,
                "name": "wawa1",
                "pic": "http://img-album.b0.upaiyun.com/20059349/0724/bdf3bf1da3405725be763540d6601144.jpg",
                "desc": "娃娃1的说明"
            },
            "address": {
                "_id": "1209384_1509535729288",
                "province": "北京",
                "city": "东城区",
                "region": "",
                "address": "啊哈哈哈啊哈哈",
                "name": "哈哈哈",
                "tel": "185163474838",
                "is_default": true
            }
        }
    ]
}
```

## 商城管理
### 1.商城列表

* API {GET|POST} http://test-aiadmin.memeyule.com/shop/list.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
status|bool|false|true;false|是否上架
page|int|false||页码
size|int|false||每页记录数

* 返回
```json
{
    "all_page": 1,
    "code": 1,
    "data": [
        {
            "_id": 12936,
            "item_id": "vip", //商品ID
            "count": 1, //购买数量
            "lastModif": 1506579704416,
            "stime": 1506441600000, //商品生效时间
            "pic": "", //图片
            "cost_diamond": 50, //花费钻石
            "unit": "个月", //单位描述
            "name": "vip", //商品名称
            "limit": 10, //每日限额
            "desc": "", //限额描述
            "tag": "90%OFF", //折扣标签
            "status": true, //是否上架
            "timestamp": 1506579704416 //添加时间
        }
    ],
    "count": 1
}
```

### 2.商城增加/修改
* API 添加 {GET|POST} http://test-aiadmin.memeyule.com/shop/add.json  
      修改 {GET|POST} http://test-aiadmin.memeyule.com/shop/edit.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
_id|int|||添加时不需要
name|str|true||歌曲名称
status|bool|true|true,false|状态 true-上架， false-下架
item_id|str|true||商品ID（非必填参数，支持修改）
pic|str|true||商品icon
count|int|true||购买数量
cost|int|true||花费
unit|int|true||单位描述
limit|int|true||每日购买限额
desc|str|true||描述
tag|str|true||折扣标签
stime|str|true||上架时间
group|str|diamond||聚类，eg: 钻石列表

* 返回
```json
{"code": 1}
```

### 3.可选上架商品

