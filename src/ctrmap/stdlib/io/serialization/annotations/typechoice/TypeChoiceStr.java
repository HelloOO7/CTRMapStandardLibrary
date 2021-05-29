package ctrmap.stdlib.io.serialization.annotations.typechoice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.lang.model.type.NullType;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(TypeChoicesStr.class)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface TypeChoiceStr {
    public String key();
    public Class value() default NullType.class;
}
