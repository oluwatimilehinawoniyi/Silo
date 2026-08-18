package com.silo.common.ocr;

import java.util.Optional;

public interface TextExtractor {

    /**
     * @return the raw text detected in the image at the given URL, or empty
     * if OCR ran but found nothing / the provider failed - never throws for
     * a "no text found" outcome, only for a genuine call failure.
     */
    Optional<String> extractText(String imageUrl);
}
