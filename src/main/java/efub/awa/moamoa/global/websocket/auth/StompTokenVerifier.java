package efub.awa.moamoa.global.websocket.auth;

public interface StompTokenVerifier {
    Long verify (String token);
}
