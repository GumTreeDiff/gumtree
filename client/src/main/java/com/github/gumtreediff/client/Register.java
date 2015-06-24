package com.github.gumtreediff.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Register {
    String name() default no_value;
    String description() default "";
    boolean experimental() default false;
    // FIXME currently unused, will be useful only for help purpose
    Class<? extends Option.Context> options() default NoOption.class;

    String no_value = "";
    class NoOption implements Option.Context {
        @Override
        public Option[] values() {
            return new Option[]{};
        }
    }
}
