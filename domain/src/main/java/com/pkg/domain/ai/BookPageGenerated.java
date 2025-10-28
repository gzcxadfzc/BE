package com.pkg.domain.ai;

import java.util.List;

public record BookPageGenerated(
        String generatedIllustrationUrl,
        String context,
        List<String> questions
) {
}
