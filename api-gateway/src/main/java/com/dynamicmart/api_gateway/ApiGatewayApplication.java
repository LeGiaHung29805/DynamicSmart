package com.dynamicmart.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiGatewayApplication {

	public static void main(String[] args) {
		if (!hasExplicitServerPort(args)) {
			int selectedPort = GatewayPortSelector.select(8080, 28080);
			System.setProperty("server.port", Integer.toString(selectedPort));
			System.out.printf("DynamicMart API Gateway selected port %d%n", selectedPort);
		}
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	private static boolean hasExplicitServerPort(String[] args) {
		if (System.getProperty("server.port") != null) return true;
		for (String arg : args) {
			if (arg.startsWith("--server.port=")) return true;
		}
		return false;
	}

}
