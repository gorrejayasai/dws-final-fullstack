package com.cognizant.ApiGateway.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.nio.file.AccessDeniedException;

@Component
@RequiredArgsConstructor
public class AdminFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        if(request.path().contains("/admin/")) {
            String role = request.headers().firstHeader("X-User-Role");
            if(!"ADMIN".equals(role)){
                throw new AccessDeniedException("Admin role required");
            }
        }

        return next.handle(request);
    }
}
