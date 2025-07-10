package com.liuclc.mcp_activemq;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpActivemqApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpActivemqApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider activemqMessageTools(ActivemqMessageService activemqMessageService) {
		return MethodToolCallbackProvider.builder()
					   .toolObjects(activemqMessageService)
					   .build();
	}
}
