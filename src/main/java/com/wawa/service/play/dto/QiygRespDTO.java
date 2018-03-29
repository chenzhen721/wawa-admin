package com.wawa.service.play.dto;

/**
 * Created by Administrator on 2017/11/10.
 */
public class QiygRespDTO<T> {

    private Boolean done;
    private String msg;
    private T retval;

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getRetval() {
        return retval;
    }

    public void setRetval(T retval) {
        this.retval = retval;
    }
}
