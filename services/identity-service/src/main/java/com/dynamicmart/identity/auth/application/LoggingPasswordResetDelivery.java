package com.dynamicmart.identity.auth.application;

import com.dynamicmart.identity.auth.domain.UserAccount;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Adapter tạm cho môi trường phát triển; thay bằng email adapter khi có nhà cung cấp email. */
@Component
public class LoggingPasswordResetDelivery implements PasswordResetDelivery {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingPasswordResetDelivery.class);
    private final String frontendBaseUrl;

    public LoggingPasswordResetDelivery(@Value("${app.password-reset.frontend-base-url:http://localhost:3000}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public void deliver(UserAccount user, String rawToken) {
        String link = frontendBaseUrl + "/reset-password?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        LOGGER.info("Password reset link for {}: {}", user.getEmail(), link);
    }
}
