package efub.awa.moamoa.global.config;

import efub.awa.moamoa.global.websocket.exception.StompErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.websocket.allowed-origins}")
    private String[] allowedOrigins;

    private final StompAuthInterceptor stompAuthInterceptor;
    private final StompErrorHandler stompErrorHandler;

    //메시지 브로커 설정
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        //서버 -> 클라이언트 방향 메시지 전달 내장 브로커
        // /topic: 1:N 브로드캐스트, /queue: 1:1 개인 메시지 전송
        registry.enableSimpleBroker("/topic", "/queue");
        //클라이언트 -> 서버 방향 메시지 prefix
        registry.setApplicationDestinationPrefixes("/app"); //
    }

    //STOMP 엔드포인트 등록
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //웹소켓 최초 연결 주소를 /ws로 지정. 클라이언트는 ws://서버주소/ws로 handshake
        registry
                .setErrorHandler(stompErrorHandler)
                .addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins); //CORS 허용 출처. 현재 모든 도메인 허용 (추후 좁혀야함)
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }
}
