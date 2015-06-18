package fr.labri.gumtree.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Register {
    String name() default NO_VALUE;
    String description() default "";
    boolean experimental() default false;
    Class<? extends Option.Context> options() default NoOption.class; // FIXME currently unused, will be useful only for help purpose

    String NO_VALUE = "";
    class NoOption implements Option.Context {
        @Override
        public Option[] values() {
            return new Option[]{};
        }
    }
}
