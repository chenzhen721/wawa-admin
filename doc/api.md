# 来吼 - ADMIN系统

## 域名

* 正式服： http://aiadmin.memeyule.com
* 测试服： http://test-aiadmin.memeyule.com

## 连接Socket
字段名|类型|是否必须|取值|说明
---|---|---|---|---
access_token|String|true||用户token
platform|Number|false|1:Android(默认),2:iOS,3:PC|平台

eg:
```javascript
connect('http://test.aiim.memeyule.com:6060?accessToken=xxxxx&platform=1')
```

## 数据格式


消息格式：
```json
{
    "action" : "message.receive", //消息action用来区分不同的操作
    "action_extra": "", // 可选字段，客户端根据需要，自己定义内容
    "data" : { // 消息体主体数据
        //...
    }
}
```

用户数据格式（`userInfo`）：
```json
{
    "_id": 1300379,
    "finance": {
        "bean_count_total": 11111,
        "coin_spend_total": 1111
    },
    "nick_name": "我是小新",
    "pic": "http://img.sumeme.com/22/6/1403510731734.jpg",
    "priv": 2
}
```

## 相关接口

* [API](/doc/api.md) (仅后端使用)
* [发送 socket 消息](/doc/send.md)
* [接收 socket 消息](/doc/receive.md)



