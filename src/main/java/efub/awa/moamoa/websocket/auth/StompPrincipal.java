package efub.awa.moamoa.websocket.auth;

import java.security.Principal;

public record StompPrincipal(Long memberId) implements Principal {
    @Override
    public String getName() {
        return memberId.toString();
    }
}
