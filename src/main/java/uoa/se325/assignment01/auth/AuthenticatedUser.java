package uoa.se325.assignment01.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Put on a controller method parameter of type User to inject the currently authenticated user.
// Resolved by AuthenticatedUserArgumentResolver; returns 401 if not logged in.
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticatedUser {
}
