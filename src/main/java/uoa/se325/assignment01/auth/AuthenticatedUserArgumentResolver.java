package uoa.se325.assignment01.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import uoa.se325.assignment01.model.User;
import uoa.se325.assignment01.repository.UserRepository;

// Resolves @AuthenticatedUser parameters by reading the auth cookie's JWT and looking up the
// matching User. Throws a 401 if there's no valid cookie or the user no longer exists.
@Component
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;

    public AuthenticatedUserArgumentResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedUser.class)
                && parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        String username = SecurityUtils.decodeJWTFromCookies(request.getCookies())
                .map(jwt -> jwt.getClaim("username").asString())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
