package com.dynamicmart.api_gateway;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.function.IntPredicate;

final class GatewayPortSelector {
    private GatewayPortSelector() { }

    static int select(int primaryPort, int fallbackPort) {
        return select(primaryPort, fallbackPort, GatewayPortSelector::isAvailable);
    }

    static int select(int primaryPort, int fallbackPort, IntPredicate availability) {
        if (availability.test(primaryPort)) return primaryPort;
        if (availability.test(fallbackPort)) return fallbackPort;
        throw new IllegalStateException("Cả cổng " + primaryPort + " và " + fallbackPort + " đều đang bị chiếm.");
    }

    private static boolean isAvailable(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress(port));
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
