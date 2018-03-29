package com.wawa.groovy;

import groovy.transform.CompileStatic

/**
 */
@CompileStatic
interface CrudClosures {

    Closure Str = {it};
    Closure Int = { String str->  (str == null || str.isEmpty()) ? null : Integer.valueOf(str)  };
    Closure Ne0 = {!'0'.equals(it)};
    Closure Eq1 = {'1'.equals(it)};
    Closure Bool = {Boolean.valueOf((String)it)}
    Closure Timestamp = {System.currentTimeMillis()};
    Closure IntNotNull ={ String str-> Integer.valueOf(str) };
}