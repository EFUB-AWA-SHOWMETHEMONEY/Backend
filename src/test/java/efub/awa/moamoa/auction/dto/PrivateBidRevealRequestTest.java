package efub.awa.moamoa.auction.dto;

import efub.awa.moamoa.auction.dto.request.PrivateBidRevealRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrivateBidRevealRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private boolean valid(Long amount, String salt) {
        return validator.validate(new PrivateBidRevealRequest(amount, salt)).isEmpty();
    }

    @Test
    @DisplayName("양수 금액과 16~64자 salt는 통과한다")
    void valid() {
        assertThat(valid(1L, "a".repeat(16))).isTrue();
        assertThat(valid(5000L, "a".repeat(64))).isTrue();
    }

    @Test
    @DisplayName("amount가 null·0·음수면 거부한다")
    void amountRejected() {
        assertThat(valid(null, "a".repeat(16))).isFalse();
        assertThat(valid(0L, "a".repeat(16))).isFalse();
        assertThat(valid(-1L, "a".repeat(16))).isFalse();
    }

    @Test
    @DisplayName("salt가 null·빈 문자열·공백이면 거부한다")
    void blankSaltRejected() {
        assertThat(valid(5000L, null)).isFalse();
        assertThat(valid(5000L, "")).isFalse();
        assertThat(valid(5000L, " ".repeat(16))).isFalse();
    }

    @Test
    @DisplayName("salt 길이가 15자·65자면 거부한다")
    void saltLengthRejected() {
        assertThat(valid(5000L, "a".repeat(15))).isFalse();
        assertThat(valid(5000L, "a".repeat(65))).isFalse();
    }
}
