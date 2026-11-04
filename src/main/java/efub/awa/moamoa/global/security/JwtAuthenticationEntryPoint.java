package efub.awa.moamoa.global.security;

import efub.awa.moamoa.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

//인증이 필요한 경로에 비로그인으로 접근했을 때의 401 응답 (필터 단계라 GlobalExceptionHandler를 거치지 않는다)
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        //필터가 남긴 에러코드가 없으면 Access Token 자체가 없는 경우
        Object attribute = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        ErrorCode errorCode = attribute instanceof ErrorCode code ? code : ErrorCode.ACCESS_TOKEN_REQUIRED;

        response.setStatus(errorCode.getStatus());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(toJson(errorCode));
    }

    //ApiResponse 실패 응답과 같은 형태 (success, errorCode, message, data)
    private String toJson(ErrorCode errorCode) {
        return "{\"success\":false,\"errorCode\":\"" + errorCode.name()
                + "\",\"message\":\"" + escape(errorCode.getMessage()) + "\",\"data\":null}";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
