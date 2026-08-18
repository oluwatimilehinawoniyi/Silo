package com.silo.member.service;

import com.silo.member.dto.ExtractedKycFields;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns raw OCR text into a best-guess idType/idNumber for Nigeria's three
 * common KYC documents. Regex-based heuristics, not an official format
 * spec - tuned to catch the common case and flag lower confidence rather
 * than guess wrong. Never authoritative: this only pre-fills a form the
 * member confirms and an officer separately reviews.
 */
@Component
class KycFieldExtractor {

    private static final Pattern NIN_PATTERN = Pattern.compile("\\b\\d{11}\\b");
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("\\b[A-Z]\\d{8}\\b");
    private static final Pattern DRIVERS_LICENSE_PATTERN = Pattern.compile("\\b[A-Z]{3}\\d{6,9}[A-Z]{0,2}\\b");

    Optional<ExtractedKycFields> extract(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }

        String upperText = rawText.toUpperCase();
        boolean mentionsPassport = upperText.contains("PASSPORT");
        boolean mentionsNin = upperText.contains("NATIONAL IDENTITY") || upperText.contains("NIN");
        boolean mentionsDriversLicense = upperText.contains("DRIVER");

        if (mentionsPassport) {
            Matcher matcher = PASSPORT_PATTERN.matcher(rawText);
            if (matcher.find()) {
                return Optional.of(new ExtractedKycFields("Passport", matcher.group(), 0.8));
            }
        }

        if (mentionsNin) {
            Matcher matcher = NIN_PATTERN.matcher(rawText);
            if (matcher.find()) {
                return Optional.of(new ExtractedKycFields("National ID", matcher.group(), 0.8));
            }
        }

        if (mentionsDriversLicense) {
            Matcher matcher = DRIVERS_LICENSE_PATTERN.matcher(rawText);
            if (matcher.find()) {
                return Optional.of(new ExtractedKycFields("Driver's License", matcher.group(), 0.8));
            }
        }

        Matcher ninMatcher = NIN_PATTERN.matcher(rawText);
        if (ninMatcher.find()) {
            return Optional.of(new ExtractedKycFields("National ID", ninMatcher.group(), 0.5));
        }

        Matcher passportMatcher = PASSPORT_PATTERN.matcher(rawText);
        if (passportMatcher.find()) {
            return Optional.of(new ExtractedKycFields("Passport", passportMatcher.group(), 0.5));
        }

        return Optional.empty();
    }
}
