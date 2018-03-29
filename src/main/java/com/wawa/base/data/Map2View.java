package com.wawa.base.data;

import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * 数据交换协议
 *
 */
public interface Map2View {


    /**
     *
     * @param data
     * @return
     */
    ModelAndView exchange(Map data);
}
