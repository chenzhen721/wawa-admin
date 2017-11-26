# 日常运营

## 公告通知 - 消息通知
### 1.通知列表


* API {GET|POST} http://test-apiadmin.17laihou.com/push/list.json
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

* API 添加 {GET|POST} http://test-apiadmin.17laihou.com/push/add.json  
      修改 {GET|POST} http://test-apiadmin.17laihou.com/push/edit.json
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

* API 修改 {GET|POST} http://test-apiadmin.17laihou.com/push/change_status.json
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

## 成功记录管理
### 1.成功记录列表

* API {GET|POST} http://test-apiadmin.17laihou.com/catchu/success_list.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束
\_id|string|false||记录ID
user_id|int|false||用户ID
room_id|int|false||房间ID
post_type|int|false||正常情况下的邮寄状态：0,未处理, 1待发货
is_delete|bool|false|true;false|是否删除 true 删除 无字段或false正常
page|int|false||页码
size|int|false||每页记录数

* 返回
```json
{
    "all_page": 138,
    "code": 1,
    "data": [
        {
            "_id": "test-play_1204425_1000110_1511525413864",
            "room_id": 1000110,
            "user_id": 1204425,
            "toy": {
                "_id": 12880,
                "name": "穿衣Line",  //娃娃名称
                "pic": "http://img.sumeme.com/55/7/1509072854647.jpg", //娃娃图片
                "desc": "穿衣Line"
            },
            "post_type": 0, //0未申请, 1已申请
            "coin": 2,  //投币数
            "timestamp": 1511525413864, //申请时间
            "replay_url": "${gif.domain}20171124/1000110/test-play_1204425_1000110_1511525413864.gif", //录像
            "goods_id": 1711022117 //对方的商品id
        }
    ],
    "count": 1
}
```

### 2.异常审核

* API {GET|POST} http://test-apiadmin.17laihou.com/catchu/success_record_refuse.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
\_id|string|false||记录ID
desc|string|false||说明

* 返回
```json
{
    "code": 1
}
```

## 订单管理
### 1.订单列表

* API {GET|POST} http://test-apiadmin.17laihou.com/catchu/post_list.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束
\_id|string|false||记录ID
user_id|int|false||用户ID
room_id|int|false||房间ID
status|int|false||发货通过这个状态来判断 审核状态：0, 未审核 1, 通过 2,未通过
post_type|int|false||正常情况下的邮寄状态：0,未处理, 1待发货, 2已发货, 3已同步订单
is_delete|bool|false|true;false|客户端是否显示此订单 是否删除 true 删除 无字段或false正常
page|int|false||页码
size|int|false||每页记录数

* 返回
```json
{
    "all_page": 11,
    "code": 1,
    "data": [
        {
            "_id": "1711232013251202883",
            "user_id": 1202883,
            "record_ids": [
                "play_1202883_1000131_1511261574104"
            ],
            "toys": [
                {
                    "_id": 100021,
                    "name": "假熊猫",
                    "pic": "https://aiimg.sumeme.com/42/2/1510923819114.9寸假熊猫-详情.png",
                    "head_pic": "https://aiimg.sumeme.com/16/0/1510989126928.9寸假熊猫-封面.png",
                    "desc": "假熊猫",
                    "goods_id": 1711207738,
                    "room_id": 1000131,
                    "record_id": "play_1202883_1000131_1511261574104"
                }
            ],
            "timestamp": 1511439205038,
            "post_type": 1,
            "address": {
                "_id": "1202883_1511185941121",
                "province": "北京",
                "city": "崇文区",
                "region": "陵川县",
                "address": "哈哈",
                "name": "周泽新",
                "tel": "18516347584",
                "is_default": true
            },
            "order_id": "7460",
            "post_info": {
                "shipping_no": "", //快递单号
                "shipping_com": "", //快递公司号
                "shipping_name": "", //快递公司名称
                "shipping_memo": "", //说明
                "shipping_time": "", //订单时间
                "order_id": ""
            }
        }
    ],
    "count": 1
}
```

### 2.批量拒绝

* API {GET|POST} http://test-apiadmin.17laihou.com/catchu/batch_refuse.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
ids|string|false||记录ID，多条记录以逗号分隔
desc|string|false||描述信息

* 返回
```json
{
    "code": 1
}
```

### 2.批量通过

* API {GET|POST} http://test-apiadmin.17laihou.com/catchu/batch_post.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
ids|string|false||记录ID，多条记录以逗号分隔

* 返回
```json
{
    "code": 1
}
```

### 3.同步订单

* API {GET|POST} http://test-apiadmin.17laihou.com/catchu/push_order.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束

* 返回
```json
{
    "code": 1,
    "data": {
        "succ": [
            "1711242229171202904"
        ],
        "error": [],
        "missing_order": []
    }
}
```

## 商城管理
### 1.商城列表

* API {GET|POST} http://test-apiadmin.17laihou.com/shop/list.json
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
* API 添加 {GET|POST} http://test-apiadmin.17laihou.com/shop/add.json  
      修改 {GET|POST} http://test-apiadmin.17laihou.com/shop/edit.json
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
group|str|true||聚类，eg: 钻石列表 diamond

* 返回
```json
{"code": 1}
```