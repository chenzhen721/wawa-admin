package com.wawa.base.spring;

import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 *
 */
public interface SessionInterceptor extends HandlerInterceptor {

    public Map<String,Object> getSession();

}
