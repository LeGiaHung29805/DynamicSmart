package com.dynamicmart.engagement_service.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dynamicmart.engagement_service.exception.EngagementException;
import org.junit.jupiter.api.Test;

class InternalApiGuardTest {
    @Test
    void acceptsMatchingKeyOnly() {
        InternalApiGuard guard = new InternalApiGuard("secret");

        assertThatCode(() -> guard.requireValid("secret")).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.requireValid("wrong")).isInstanceOf(EngagementException.class);
    }
}
