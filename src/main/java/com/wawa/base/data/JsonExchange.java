package com.wawa.base.data;

import groovy.transform.CompileStatic;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * 提供json 格式输出
 */
@CompileStatic
public class JsonExchange implements Map2View {

    private static final SimpleJsonView jsonView = SimpleJsonView.instance();
    @Override
    public ModelAndView exchange(Map data) {
        return new ModelAndView(jsonView,data);
    }
}
