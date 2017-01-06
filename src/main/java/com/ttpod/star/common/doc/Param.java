package com.ttpod.star.common.doc;

/**
 *
 * Param key.
 *
 * date: 13-2-25 上午10:26
 *
 * @author: yangyang.cong@ttpod.com
 */
public interface Param {


    String user_name = "user_name";
    String password = "password";

    String access_token ="access_token";



    String first = "id1";

    String second = "id2";


    /**
     * X-Forwarded-For
     */
    String XFF = "X-FORWARDED-FOR";
    String HXFF = "http_x_forwarded_for";

    /**
     * 访问token 有效期 30天
     */
    long TOKEN_SECONDS = 24*3600L;
}
