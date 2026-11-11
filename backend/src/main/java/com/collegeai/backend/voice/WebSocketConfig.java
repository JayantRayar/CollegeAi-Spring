
package com.collegeai.backend.voice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ExotelVoiceWebSocketHandler voiceWebSocketHandler;

    public WebSocketConfig(ExotelVoiceWebSocketHandler voiceWebSocketHandler) {
        this.voiceWebSocketHandler = voiceWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(voiceWebSocketHandler, "/ws/exotel")
                .setAllowedOriginPatterns("*");
    }

    /**
     * Configures the WebSocket container's incoming text-message buffer.
     * Exotel sends audio as Base64 inside JSON text messages.
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container =
                new ServletServerContainerFactoryBean();

        // Allow JSON messages up to 256 KB.
        container.setMaxTextMessageBufferSize(256 * 1024);

        return container;
    }
}
