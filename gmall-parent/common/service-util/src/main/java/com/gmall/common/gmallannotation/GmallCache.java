package com.gmall.common.gmallannotation;

import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface GmallCache {
    String prefix() default "";
}
