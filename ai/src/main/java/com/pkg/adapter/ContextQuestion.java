package com.pkg.adapter;

import java.util.List;

public record ContextQuestion(
        String context,
        List<String> questions
) {
}
