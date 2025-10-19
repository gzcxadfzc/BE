package com.pkg.domain.ai;

public interface CharacterGenerator {

    String extractAppearanceKeywords(String url);
    String extractAppearanceKeywords(byte[] imageBytes);
}
