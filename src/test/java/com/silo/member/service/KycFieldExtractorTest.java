package com.silo.member.service;

import com.silo.member.dto.ExtractedKycFields;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class KycFieldExtractorTest {

    private final KycFieldExtractor extractor = new KycFieldExtractor();

    @Test
    @DisplayName("extracts a National ID number when the text mentions the NIN and contains an 11-digit number")
    void extract_findsNationalId() {
        Optional<ExtractedKycFields> result = extractor.extract(
                "FEDERAL REPUBLIC OF NIGERIA NATIONAL IDENTITY MANAGEMENT COMMISSION NIN: 12345678901");

        assertThat(result).isPresent();
        assertThat(result.get().idType()).isEqualTo("National ID");
        assertThat(result.get().idNumber()).isEqualTo("12345678901");
        assertThat(result.get().confidence()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("extracts a passport number when the text mentions PASSPORT and a letter+8-digit code")
    void extract_findsPassport() {
        Optional<ExtractedKycFields> result = extractor.extract(
                "REPUBLIC OF NIGERIA PASSPORT NO A12345678");

        assertThat(result).isPresent();
        assertThat(result.get().idType()).isEqualTo("Passport");
        assertThat(result.get().idNumber()).isEqualTo("A12345678");
    }

    @Test
    @DisplayName("returns a lower-confidence guess when a document number pattern matches without a keyword")
    void extract_lowerConfidence_whenNoKeywordMatch() {
        Optional<ExtractedKycFields> result = extractor.extract("some scanned text with 12345678901 in it");

        assertThat(result).isPresent();
        assertThat(result.get().confidence()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("returns empty when nothing recognizable is found")
    void extract_returnsEmpty_whenNothingMatches() {
        assertThat(extractor.extract("blurry unreadable text")).isEmpty();
    }

    @Test
    @DisplayName("returns empty for blank input")
    void extract_returnsEmpty_forBlankInput() {
        assertThat(extractor.extract("")).isEmpty();
        assertThat(extractor.extract(null)).isEmpty();
    }
}
