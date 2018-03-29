package com.wawa.base.ext;

/**
 *        扩展 system -> groovy
 */
public final class SystemStaticExtension {

    public static long unixTime(System selfType) {
        return System.currentTimeMillis()/1000;
    }

    public static long currentSeconds(System selfType) {
        return System.currentTimeMillis()/1000;
    }

}
