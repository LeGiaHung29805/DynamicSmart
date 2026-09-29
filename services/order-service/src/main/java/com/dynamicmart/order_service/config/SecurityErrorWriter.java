package com.dynamicmart.order_service.config;

import com.dynamicmart.order_service.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityErrorWriter {
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SecurityErrorWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                "Bạn cần đăng nhập để thực hiện thao tác này.");
    }

    public void forbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN, "ACCESS_DENIED",
                "Bạn không có quyền thực hiện thao tác này.");
    }

    private void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                new ApiErrorResponse(code, message, List.of(), Instant.now(clock)));
    }
}
