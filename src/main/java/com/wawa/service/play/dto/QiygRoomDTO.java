package com.wawa.service.play.dto;

/**
 * Created by Administrator on 2017/11/10.
 */
public class QiygRoomDTO {

    private String device_id; //设备id
    private String img; //设备图片
    private String name; //设备名称
    private Integer status; //状态
    private String stream_address_1; //正面视频地址
    private String stream_address_2; //侧面视频地址

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStream_address_1() {
        return stream_address_1;
    }

    public void setStream_address_1(String stream_address_1) {
        this.stream_address_1 = stream_address_1;
    }

    public String getStream_address_2() {
        return stream_address_2;
    }

    public void setStream_address_2(String stream_address_2) {
        this.stream_address_2 = stream_address_2;
    }
}
