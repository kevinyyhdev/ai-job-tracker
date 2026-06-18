package com.kevin.jobtracker.common.extraction;

import com.kevin.jobtracker.common.exception.BusinessRuleException;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class TextExtractionService {

    // Tika is thread-safe; one instance is shared across all requests.
    private final Tika tika = new Tika();

    // Parses file bytes using Apache Tika and returns extracted plain text.
    // Throws BusinessRuleException if the file is unreadable or contains no extractable text
    // (e.g. a scanned image PDF with no embedded text layer).
    public String extract(byte[] bytes) {
        try {
            String text = tika.parseToString(new ByteArrayInputStream(bytes));
            if (text == null || text.isBlank()) {
                throw new BusinessRuleException(
                        "Resume contains no extractable text. Scanned image PDFs are not supported.");
            }
            return text.strip();
        } catch (BusinessRuleException e) {
            throw e;
        } catch (TikaException | IOException e) {
            throw new BusinessRuleException("Could not extract text from resume. The file may be corrupt.");
        }
    }
}
