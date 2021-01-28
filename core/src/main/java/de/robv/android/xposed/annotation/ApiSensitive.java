package de.robv.android.xposed.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that indicates a element is sensitive to Android API level.
 * <p>
 * Annotated elements' compatibility should be checked when adapting to new Android versions.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
public @interface ApiSensitive {
    Level value() default Level.HIGH;
}
