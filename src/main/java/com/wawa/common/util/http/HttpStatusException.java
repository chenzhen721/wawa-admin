package com.wawa.common.util.http;

import java.io.IOException;

/**
 *
 * Http 异常
 *
 */
public class HttpStatusException extends IOException {

    int code;

    public HttpStatusException() {
    }

    public HttpStatusException(int code, String message) {
        super(message);
        this.code =code;
    }

    public int getCode(){
        return code;
    }
}
