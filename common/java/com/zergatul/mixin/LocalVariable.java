package com.zergatul.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@SuppressWarnings("unused")
public @interface LocalVariable {

    /**
     * The index of the local variable by type. E.g. if there are 3 {@code String} variables, an ordinal of 2 would
     * target the 3rd one.
     */
    int ordinal();
}