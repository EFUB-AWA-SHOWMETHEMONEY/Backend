package efub.awa.moamoa.global.websocket.plaza.support;

import efub.awa.moamoa.websocket.plaza.support.ChatFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 금지어 목록을 src/main/resources/plaza/banned-words.txt 에서 그대로 읽으므로,
 * 목록을 수정하면 이 테스트의 기대값도 함께 손봐야 한다.
 */
class ChatFilterTest {

    private final ChatFilter filter = new ChatFilter();

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "fuck you   | **** you",
            "FuCk off   | **** off",
            "BITCH      | *****",
            "아 shit     | 아 ****",
            "시발 진짜     | ** 진짜",
            "ㅅㅂ 진짜      | ** 진짜",
            "병신아       | **아",
            "좆         | *",
    })
    @DisplayName("등록한 단어가 그대로 있으면 가린다")
    void masksRegisteredWords(String input, String expected) {
        assertThat(filter.mask(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "fuck and shit | **** and ****",
            "시발 시발       | ** **",
            "shitshit      | ********",
    })
    @DisplayName("한 메시지에 여러 번 나오면 모두 가린다")
    void masksEveryOccurrence(String input, String expected) {
        assertThat(filter.mask(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "신발 샀어",          //ㅅㅂ이 목록에 있어도 완성형 한글은 걸리지 않아야 한다
            "새 신발이 예뻐",
            "불이 꺼져 있어",       //'꺼져'를 목록에서 뺀 결과
            "전원이 꺼져버렸다",
            "shift 키 눌러",     //shit과 한 글자 차이
            "출발 시간 언제야",
            "좋아요",             //'좆'과 모양만 비슷
            "지략이 뛰어나",
            "ㅋㅋㅋ ㅎㅇ",
            "안녕하세요",
    })
    @DisplayName("정상 문장은 건드리지 않는다")
    void keepsCleanText(String input) {
        assertThat(filter.mask(input)).isEqualTo(input);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "시발점이 어디야 | **점이 어디야",
            "지랄맞다     | **맞다",
            "shithead   | ****head",
    })
    @DisplayName("부분 문자열 과차단은 예외 목록을 두지 않기로 한 결과로 수용한다")
    void masksSubstringMatches(String input, String expected) {
        assertThat(filter.mask(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"f u c k", "fuuuck", "f0ck", "ㅅ ㅂ"})
    @DisplayName("우회 패턴은 잡지 않는다 (정규화를 하지 않는 현재 범위)")
    void ignoresEvasions(String input) {
        assertThat(filter.mask(input)).isEqualTo(input);
    }

    @Test
    @DisplayName("소문자 변환으로 길이가 늘어나는 글자가 섞여도 인덱스가 밀리지 않는다")
    void handlesCaseExpandingChars() {
        assertThat(filter.mask("İfuck")).isEqualTo("İ****");
    }
}
