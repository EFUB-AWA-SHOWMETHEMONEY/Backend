package efub.awa.moamoa.websocket.plaza.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class ChatFilter {

    private static final char MASK = '*';
    private static final String WORDS_PATH = "plaza/banned-words.txt";

    private final List<String> bannedWords;

    public ChatFilter() {
        this.bannedWords = load();
        log.info("채팅 금칙어 {}개 로드", bannedWords.size());
    }

    private List<String> load() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(WORDS_PATH).getInputStream(),
                StandardCharsets.UTF_8
        ))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(ChatFilter::normalizeCase)
                    .distinct()
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("채팅 금칙어 목록 로드 실패: "+ WORDS_PATH, e);
        }
    }

    public String mask (String text) {
        String haystack = normalizeCase(text);
        char[] out = text.toCharArray();
        boolean found = false;

        for (String word : bannedWords) {
            int from = 0;
            int at;
            while ((at = haystack.indexOf(word, from)) >= 0) {
                Arrays.fill(out, at, at+word.length(), MASK);
                found = true;
                from = at + word.length();
            }
        }

        return found ? new String(out) : text;
    }

    private static String normalizeCase (String text) {
        char[] chars = text.toCharArray();
        for (int i=0; i < chars.length; i++) {
            chars[i] = Character.toLowerCase(chars[i]);
        }

        return new String(chars);
    }
}
