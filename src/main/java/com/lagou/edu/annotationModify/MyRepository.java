package com.lagou.edu.annotationModify;


import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyRepository {
    String value() default "";
}
