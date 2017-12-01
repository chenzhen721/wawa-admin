package com.ttpod.star.common.util;

import org.apache.commons.lang.StringUtils;

/**
 * Created by Administrator on 2017/7/11.
 */
public class StrUtils {

    public static String defaultIfBlank(String src, String def) {
        if (StringUtils.isBlank(src)) {
            return def;
        }
        return src;
    }

}
