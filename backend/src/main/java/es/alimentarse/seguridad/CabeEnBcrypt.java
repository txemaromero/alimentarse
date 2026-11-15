package es.alimentarse.seguridad;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * La contraseña cabe en los 72 bytes de bcrypt (D-090), y no solo en 72 caracteres. Se pone junto al
 * {@code @Size(max = 72)} del campo y solo falla con 72 caracteres o menos: con más, ya lo dice el {@code @Size}, y así
 * no salen dos mensajes.
 */
@Documented
@Constraint(validatedBy = CabeEnBcrypt.Validador.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface CabeEnBcrypt {

    String message() default
            "La contraseña es demasiado larga: la ñ, las tildes y los emojis cuentan como más de un carácter";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<CabeEnBcrypt, String> {

        @Override
        public boolean isValid(String password, ConstraintValidatorContext contexto) {
            return password == null
                    || password.length() > CifradoPassword.MAXIMO_BYTES
                    || CifradoPassword.cabe(password);
        }
    }
}
