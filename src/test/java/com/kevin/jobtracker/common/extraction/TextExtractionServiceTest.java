package com.kevin.jobtracker.common.extraction;

import com.kevin.jobtracker.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextExtractionServiceTest {

    private final TextExtractionService textExtractionService = new TextExtractionService();

    @Test
    void extractReturnsTextFromPlainContent() {
        byte[] bytes = "Software engineer with 5 years of Java experience".getBytes(StandardCharsets.UTF_8);

        String result = textExtractionService.extract(bytes);

        assertThat(result).contains("Software engineer");
        assertThat(result).contains("Java experience");
    }

    @Test
    void extractThrowsForEmptyBytes() {
        assertThatThrownBy(() -> textExtractionService.extract(new byte[0]))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("corrupt");
    }

    @Test
    void extractThrowsForWhitespaceOnlyContent() {
        byte[] bytes = "   \n   \t  ".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> textExtractionService.extract(bytes))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no extractable text");
    }

    @Test
    void extractStripsLeadingAndTrailingWhitespace() {
        byte[] bytes = "  Java developer  ".getBytes(StandardCharsets.UTF_8);

        String result = textExtractionService.extract(bytes);

        assertThat(result).isEqualTo("Java developer");
    }
}
