package com.dynamicmart.api_gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GatewayPortSelectorTest {
    @Test
    void prefersPort8080WhenItIsAvailable() {
        assertThat(GatewayPortSelector.select(8080, 28080, port -> port == 8080)).isEqualTo(8080);
    }

    @Test
    void fallsBackTo28080When8080IsOccupied() {
        assertThat(GatewayPortSelector.select(8080, 28080, port -> port == 28080)).isEqualTo(28080);
    }

    @Test
    void failsClearlyWhenBothPortsAreOccupied() {
        assertThatThrownBy(() -> GatewayPortSelector.select(8080, 28080, port -> false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("8080")
                .hasMessageContaining("28080");
    }
}
