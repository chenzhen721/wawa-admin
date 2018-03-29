package com.wawa.groovy

import groovy.transform.CompileStatic

/**
 * Created by Administrator on 2017/12/6.
 */
@CompileStatic
interface CrudExtClosures {
    Closure StrNullable = {String str-> (str == null || str.isEmpty()) ? NULL : str }
    Closure IntNullable = {String str-> (str == null || str.isEmpty()) ? NULL : Integer.parseInt(str) }
    Closure StrEmptyable = {String str-> (str == null || str.isEmpty()) ? Empty : str }



    static class NULL {
        @Override
        int hashCode() {
            return -1
        }

        @Override
        boolean equals(Object obj) {
            return null == obj
        }


        @Override
        public String toString() {
            return null
        }
    }

    static class Empty {
        @Override
        int hashCode() {
            return 0
        }

        @Override
        boolean equals(Object obj) {
            return super.equals(obj)
        }


        @Override
        public String toString() {
            return ""
        }
    }
}