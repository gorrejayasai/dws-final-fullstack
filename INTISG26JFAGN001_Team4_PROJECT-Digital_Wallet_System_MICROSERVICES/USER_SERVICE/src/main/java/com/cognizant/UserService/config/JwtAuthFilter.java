package com.cognizant.UserService.config;

import com.cognizant.UserService.entity.User;
import com.cognizant.UserService.repository.UserRepository;
import com.cognizant.UserService.util.AuthUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final AuthUtil authUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        log.info("Request URI: {}", request.getRequestURI());

        final String requestTokenHeader = request.getHeader("Authorization");

        if (requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = requestTokenHeader.substring(7);
        String username = null;

        try {
            username = authUtil.getUsernameFromToken(token);
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired for request: {}", request.getRequestURI());
            request.setAttribute("jwt_error", "TOKEN_EXPIRED");
            filterChain.doFilter(request, response);
            return;
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException e) {
            log.warn("Invalid JWT for request: {}", request.getRequestURI());
            request.setAttribute("jwt_error", "TOKEN_INVALID");
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                request.setAttribute("jwt_error", "USER_NOT_FOUND");
            }
        }

        filterChain.doFilter(request, response);
    }
}
