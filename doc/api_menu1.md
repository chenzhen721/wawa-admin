# 日常运营

## 货架管理
### 1.货架列表

* API {GET|POST} http://test-apiadmin.17laihou.com/catchu/goods_list.json  
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
\_id|int|false||货架ID
room_id|int|false||房间ID
toy_id|int|false||娃娃ID
cate_id|int|false||类目ID
partner|int|false||合作方
is_replace|int|false||是否代抓


* 返回
```json
{
    "count": 10, 
    "data": [
        {
            "_id": 100060, 
            "name": "眯眼熊", //名称
            "type": "true",  //是否备货中
            "online": "online",  //是否上架
            "toy_id": "toy_id",  //娃娃ID
            "head_pic": "head_pic",  //娃娃封面
            "price": "price",  //抓取价格
            "room_id": "room_id", //房间ID
            "cate_id": "cate_id", //类目ID
            "cate_name": "cate_name", //类目名称
            "tag_id": "tag_id", //标签ID
            "tag_pic": "tag_name", //标签图片
            "partner": "partner", //合作方
            "order": "order",  //排序
            "is_replace": true,  //是否代抓
            "rids": [123,123,123],  //代抓房间ID
            "rooms": [{"_id": 123, "name": "名称"}],  //代购房间明细
            "timestamp": 1510925907864
        }], 
    "code": 1, 
    "all_page": 2
}
```

### 2.货架修改

* API 添加 {GET|POST} http://test-apiadmin.17laihou.com/catchu/goods_add.json  
      编辑 {GET|POST} http://test-apiadmin.17laihou.com/catchu/goods_edit.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
\_id|int|false||edit必传
name|String|true||商品名称
type|bool|false|默认为true|备货在页面上能显示
online|bool|true||是否上线  不在页面上显示
is_replace|bool|true||是否代抓
partner|int|true||合作方
room_id|int|true||房间ID
toy_id|int|true||商品ID
cate_id|int|true||商品类型
tag_id|int|true||标签
order|int|true||排序 （小在前）
rids|string|true||代抓房间号 选代抓必填
timestamp|int|true||添加时间

* 返回

    成功
```json
{"code": 1}
```

## 娃娃机房间管理
### 1.房间列表

* API {GET|POST} http://test-apiadmin.17laihou.com/catchu/list.json  
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
\_id|int|false||房间ID
fid|int|false||机器ID
online|bool|false||上线状态
partner|int|false||合作方
device_type|int|false||设备类型

* 返回
```json
{
    "count": 1,
    "data": [
        {
            "_id": 12985,
            "name": "奇异果-zego01",
            "partner": 2, 
            "order": 0,
            "device_type": 2,
            "timestamp": 1515045861127,
            "fid": "WWJ_ZEGO_22b3cddecebe_No.X022", //机器ID
            "winrate": 1,
            "playtime": 40,
            "online": true,
            "desc": "" //描述
        }
    ],
    "code": 1,
    "all_page": 1
}
```

### 2.房间修改

* API 添加 {GET|POST} http://test-apiadmin.17laihou.com/catchu/add.json  
      编辑 {GET|POST} http://test-apiadmin.17laihou.com/catchu/edit.json  
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
\_id|int|false||edit必传
fid|int|true||机器ID
name|String|true||房间名称
desc|String|true||描述
order|int|true||排序 越小越靠前
timestamp|int|true||添加时间
playtime|int|true||游戏时长 5-60
winrate|int|true||命中概率 1-888
partner|int|true||合作方 1-奇异果 2-ZEGO 3-奇异果ZEGO
online|bool|true||是否上线

* 返回

    成功
```json
{"code": 1}
```

## 娃娃商品管理
### 1.商品列表

* API {GET|POST} http://test-apiadmin.17laihou.com/catchu/toy_list.json  
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束

* 返回
```json
{
    "count": 10, 
    "data": [
        {
            "_id": 100060, 
            "name": "眯眼熊", 
            "type": "true", 
            "pic": "https://aiimg.sumeme.com/15/7/1510991024079.png", 
            "head_pic": "https://aiimg.sumeme.com/6/6/1510991028358.png", 
            "desc": "眯眼熊", 
            "points": 123, //可兑换的积分
            "cost": 12, //娃娃成本
            "price": 12, //抓取单价
            "channel": 1, //邮寄通道 0-奇异果, 1-活动人工, 2-即构, 3-自营
            "goods_id": 123,
            "stock": {
                "stock": 123, //库存
                "total": 123, //进货量
                "count": 123, //已发货总数
                "timestamp": 123123123 //末次更新时间
            },
            "timestamp": 1510925907864
        }], 
    "code": 1, 
    "all_page": 2
}
```

### 2.商品修改

* API 添加 {GET|POST} http://test-apiadmin.17laihou.com/catchu/toy_add.json  
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
\_id|int|false||edit必传
name|String|true||商品名称
type|bool|false|默认为true|是否可用
pic|String|true||详情图
head_pic|string|true||缩略图
desc|String|false|false|描述
points|int|true||可兑换积分
cost|int|false||娃娃成本
price|int|false||抓取单价
channel|int|false||邮寄通道 0-奇异果, 1-活动人工, 2-即构, 3-自营
goods_id|int|false||可同步商品ID（邮寄方式为奇异果时必填）

* 返回

    成功
```json
{"code": 1}
```

### 3.商品库存添加

* API {GET|POST} http://test-apiadmin.17laihou.com/catchu/toy_stock_add.json  
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
\_id|int|true||对应商品ID
stock|int|true||库存可以为负，填错的情况

* 返回

    成功
```json
{"code": 1}
```
    失败
```json
{"code": 0, "msg":"失败原因"}
```

## 类目管理
### 1.类目列表

* API {GET|POST} http://test-apiadmin.17laihou.com/category/list.json  
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束

* 返回
```json
{
    "count": 10, 
    "data": [
        {
            "_id": 100060, 
            "name": "眯眼熊",  //类目名称
            "img": "机器ID",  //图片
            "type": 0,  //type 0-房间 1-标签
            "status": 1,  //status 0-上线 1-下线
            "onshow": true,   //onshow true显示在首页 false不显示在首页
            "timestamp": 1510925907864,
            "stime": 123123, //上架时间
            "etime": 123123123 //下架时间
        }], 
    "code": 1, 
    "all_page": 2
}
```

### 2.类目编辑

* API 添加 {GET|POST} http://test-apiadmin.17laihou.com/category/add.json  
      编辑 {GET|POST} http://test-apiadmin.17laihou.com/category/edit.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
\_id|int|false||edit必传
name|String|true||商品名称
img|String|true||标签
type|int|true||type 0-房间 1-标签
status|int|true||status 0-上线 1-下线
onshow|bool|true||true显示在首页 false不显示在首页
timestamp|int|true||时间
stime|String|true||上架时间
etime|String|true||下架时间

* 返回

    成功
```json
{"code": 1}
```

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

### 3.补单

* API {GET|POST} http://test-apiadmin.17laihou.com/catchu/success_record_add.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
\_id|string|false||需要补单的记录ID

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
channel|int|false||邮寄类型：0,奇异果, 1活动, 2ZEGO
is_delete|bool|false|true;false|客户端是否显示此订单 是否删除 true 删除 无字段或false正常
need_postage|bool|false|true;false|需要邮费 true   不需要邮费false
is_pay_postage|bool|false|true;false|未支付 false   已支付 true
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

### 4.订单统计表

* API {GET|POST} http://test-apiadmin.17laihou.com/stat/order_report.json
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
         "_id" : "20171120_order",
         "type" : "order", //
         "timestamp" : 1511107200000, //日期
         "total_pay" : 501,  //总营收
         "total_cost" : 5, //总成本
         "order_count" : 60.0,   //寄出单数
         "goods_count" : 8, //商品个数
         "goods_cost" : 115.0, //商品价值
         "postage" : 5, //快递费用
         "user_count" : 69.0 //邮寄用户数
                 
     }],
    "code": 1,
    "all_page": 12
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
            "cost": 50, //花费金额
            "unit": "个月", //单位描述
            "name": "vip", //商品名称
            "limit": 10, //每日限额
            "desc": "", //描述
            "tag": "90%OFF", //折扣标签
            "status": true, //是否上架
            "timestamp": 1506579704416, //添加时间
            "group": 1506579704416, //商品聚类
            "order": 1506579704416, //排序
            "award": 1506579704416, //额外奖励钻石数，不影响充值钻石数
            "after_award_desc": 1506579704416, //充值后激活奖励描述
            "after_award_diamond": 1506579704416, //充值后激活奖励钻石数
            "after_award_days": 1506579704416, //充值后激活奖励天数
            "award_type": 1506579704416 //奖励类型 0-优惠 1-首冲特权 不填无优惠
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
order|int|true||排序
award|int|true||额外奖励的钻石数
after_award_desc|str|false||充值后激活奖励描述
after_award_diamond|int|false||充值后激活奖励钻石数
after_award_days|int|false||充值后激活奖励天数
award_type|int|false||奖励类型0-优惠 1-首冲特权 不填无优惠

* 返回
```json
{"code": 1}
```

## 申述管理
### 1.申述列表

* API {GET|POST} http://test-apiadmin.17laihou.com/doll/observe_list.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束
\_id|string|false||记录ID
type|int|false||申述类型 0 卡顿延迟 1 抓到娃娃不算
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
            "replay_url": "", //录像地址
            "type": 12,//申述类型 0 卡顿延迟 1 抓到娃娃不算
            "status": 0, //处理状态 0 未处理 1 已处理 2 忽略
            "user": {
                "nick_name": "",
                "pic": ""
            }
        }
    ],
    "count": 1
}
```

### 2.申述处理

* API {GET|POST} http://test-apiadmin.17laihou.com/doll/edit_observe.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
\_id|string|true||记录ID
type|int|true||申述类型 0 卡顿延迟 1 抓到娃娃不算
status|int|true||处理状态 0 未处理 1 已处理 2 忽略
count|int|true||补货数量，填对应的钻石数，或补的娃娃数 status为1时必填
desc|string|true||补货说明，会提示给用户 status为1时必填

* 返回
```json
{
    "code": 1
}
```

## 报修管理
### 1.报修列表

* API {GET|POST} http://test-apiadmin.17laihou.com/doll/repair_list.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
stime|String|false|yyyy-MM-dd HH: mm:ss|创建时间开始
etime|String|false|yyyy-MM-dd HH: mm:ss|创建时间结束
fid|string|false||机器号
problem|int|false||问题类型 0：失灵， 1：延迟， 2： 其它
status|int|false||处理状态 0 未处理 1 处理中 2 完成
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
            "room_id": 1202883, //房间ID
            "fid": "", //机器ID
            "toy_id": 0,   //娃娃ID
            "toy": {
                "_id": 123,
                "name": 123,
                "pic": ""
            },
            "status": 0, //处理状态 0 未处理 1 处理中 2 完成
            "problem": {
                "0": 123,
                "1": 123,
                "2": 123
            },
            "room_name": "", //房间名称
            "timestamp": 123123123123
        }
    ],
    "count": 1
}
```

### 2.申述处理

* API {GET|POST} http://test-apiadmin.17laihou.com/doll/repair_edit.json
* 参数

字段名|类型|是否必须|取值|说明
---|---|---|---|---
\_id|string|true||记录ID
status|int|true||处理状态 0 未处理 1 处理中 2 完成

* 返回
```json
{
    "code": 1
}
```