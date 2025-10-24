package dev.luxury.modules.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)

public @interface ModuleAnnotation {
    String name();
    String desc() default "";
    Category category();
    int key() default -1;
}