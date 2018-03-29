package com.wawa.base.anno;

import com.wawa.base.spring.Interceptors;
import org.codehaus.groovy.transform.GroovyASTTransformationClass;
import org.springframework.stereotype.Controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 */
@java.lang.annotation.Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
@GroovyASTTransformationClass("com.wawa.base.anno.RestStaticCompileProcessor")
public @interface RestWithSession {
    Class[] value = {Controller.class, Interceptors.class};
}
