package efub.awa.moamoa.websocket.auth;

public interface StompTokenVerifier {
    Long verify (String token);
}
