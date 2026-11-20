package efub.awa.moamoa.auction.dto;

import efub.awa.moamoa.auction.dto.request.PrivateBidCommitRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrivateBidCommitRequestTest {

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

    private boolean valid(String commitHash) {
        return validator.validate(new PrivateBidCommitRequest(commitHash)).isEmpty();
    }

    @Test
    @DisplayName("소문자 hex 64자는 통과한다 (FE 가이드의 테스트 벡터)")
    void lowercaseHex64Valid() {
        assertThat(valid("15b7b4defdac1437d78a0e12d7be416478c8859538a7f975dbd2ee241642b961")).isTrue();
    }

    @Test
    @DisplayName("대문자가 섞이면 거부한다")
    void uppercaseRejected() {
        assertThat(valid("15B7B4DEFDAC1437D78A0E12D7BE416478C8859538A7F975DBD2EE241642B961")).isFalse();
        assertThat(valid("15b7b4defdac1437d78a0e12d7be416478c8859538a7f975dbd2ee241642b96A")).isFalse();
    }

    @Test
    @DisplayName("null·빈 문자열·공백은 거부한다")
    void blankRejected() {
        assertThat(valid(null)).isFalse();
        assertThat(valid("")).isFalse();
        assertThat(valid("   ")).isFalse();
    }

    @Test
    @DisplayName("길이가 63자·65자면 거부한다")
    void wrongLengthRejected() {
        assertThat(valid("a".repeat(63))).isFalse();
        assertThat(valid("a".repeat(65))).isFalse();
    }

    @Test
    @DisplayName("hex가 아닌 문자나 앞뒤 공백·개행이 있으면 거부한다")
    void nonHexRejected() {
        assertThat(valid("g".repeat(64))).isFalse();
        assertThat(valid(" " + "a".repeat(64))).isFalse();
        assertThat(valid("a".repeat(64) + "\n")).isFalse();
    }
}
