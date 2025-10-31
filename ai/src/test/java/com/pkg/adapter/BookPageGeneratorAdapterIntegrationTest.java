package com.pkg.adapter;

import com.pkg.domain.ai.BookPageGenerated;
import com.pkg.domain.book.BookPage;
import com.pkg.domain.bookprogress.BookInProgress;
import com.pkg.domain.bookprogress.BookToProgress;
import com.pkg.domain.character.BookCharacter;
import com.pkg.openai.api.request.TestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@DisplayName("BookPageGeneratorAdapter Integration Test - Real OpenAI API")
class BookPageGeneratorAdapterIntegrationTest {

    @Autowired
    private BookPageGeneratorAdapter bookPageGeneratorAdapter;

    private BookCharacter testCharacter;
    private BookInProgress testBookInProgress;

    @BeforeEach
    void setUp() {
        testCharacter = new BookCharacter(
                1L,
                1L,
                "토끼 토리",
                "흰색 털, 긴 귀, 분홍색 코",
                "호기심 많고 용감한 성격",
                "숲속에 사는 착한 토끼",
                "rabbit-portrait.png"
        );

        testBookInProgress = new BookInProgress(
                "test-book-001",
                1L,
                "숲속 친구들의 모험 이야기",
                testCharacter,
                List.of(
                        new BookPage("토끼 토리는 햇살에 잠을 깨어 집밖으로 나갔습니다.", "", 0)
                ),
                BookInProgress.Status.IN_PROGRESS
        );
    }

    @Test
    @DisplayName("Should generate first page with real OpenAI API")
    void shouldGenerateFirstPageWithRealApi() {
        // Given
        String userInput = "토끼 토리가 아름다운 봄날 아침에 숲속을 산책하기 시작했어요.";
        BookToProgress bookToProgress = new BookToProgress(testBookInProgress, userInput);

        // When
        BookPageGenerated result = bookPageGeneratorAdapter.generatePageFrom(bookToProgress);

        // Then
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.generatedIllustrationUrl(), "Generated illustration URL should not be null");
        assertNotNull(result.context(), "Generated context should not be null");
        assertNotNull(result.questions(), "Generated questions should not be null");

        assertFalse(result.generatedIllustrationUrl().isEmpty(), "Illustration URL should not be empty");
        assertFalse(result.context().isEmpty(), "Context should not be empty");
        assertTrue(result.questions().size() >= 2, "Should have at least 2 questions");

        // Verify the illustration URL is a valid URL format
        assertTrue(result.generatedIllustrationUrl().startsWith("http"),
                "Illustration should be a valid URL");


        System.out.println("Generated Illustration URL: " + result.generatedIllustrationUrl());
        System.out.println("Generated Context: " + result.context());
        System.out.println("Generated Questions: " + result.questions());
    }
}
