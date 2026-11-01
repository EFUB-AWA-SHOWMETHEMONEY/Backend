package efub.awa.moamoa.global.websocket.auth;

import java.security.Principal;

public record StompPrincipal(Long memberId) implements Principal {
    @Override
    public String getName() {
        return memberId.toString();
    }
}
