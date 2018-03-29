package com.wawa.groovy

import groovy.transform.CompileStatic

/**
 *
 * 方便的动态扩展特性
 */
@CompileStatic
class WebSupport {


//    @TypeChecked(TypeCheckingMode.SKIP)
//    private static void initHttpServletRequest(){
//        if('true'.equals(AppProperties.get('app.request_getAt')))
//        HttpServletRequest.metaClass['getPmvn roperty'] = {String name->
//            ((HttpServletRequest)delegate).getParameter(name)
//        }
//    }

    static void init(){
        //initHttpServletRequest()
    }

}
