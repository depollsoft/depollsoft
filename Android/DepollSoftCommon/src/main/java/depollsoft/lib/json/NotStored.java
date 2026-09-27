package depollsoft.lib.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a property's getter as runtime state that {@link JsonSerializer} neither writes nor
 * restores, so a stored copy never replays it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NotStored {
}
