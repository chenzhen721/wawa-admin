package com.ttpod.star.web.api.play.dto;

/**
 * 需要下单的商品接口
 * Created by Administrator on 2017/11/10.
 */
public class QiygGoodsDTO {
    private Integer goods_id;
    private Integer num;

    public QiygGoodsDTO() {
    }

    public QiygGoodsDTO(Integer goods_id, Integer num) {
        this.goods_id = goods_id;
        this.num = num;
    }

    public Integer getGoods_id() {
        return goods_id;
    }

    public void setGoods_id(Integer goods_id) {
        this.goods_id = goods_id;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }
}
