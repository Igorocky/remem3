package org.igye.remem3.app.imprt;

import org.igye.remem3.utils.impl.UtilsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

class CardCollectionDtoTest {
    private final UtilsImpl utils = new UtilsImpl(new ObjectMapper());

    @Test
    void parseJson_deserializes_whole_hierarchy() {
        String json = """
            {
              "generatedBy": "test-generator",
              "cards": {
                "chapterName": "root",
                "chapters": [
                  {
                    "chapterName": "child-1",
                    "chapters": [
                      {
                        "chapterName": "grandchild",
                        "chapters": [],
                        "cards": [
                          {"type": "QUESTION_ANSWER", "question": "2+2?", "answer": "4"}
                        ]
                      }
                    ],
                    "cards": [
                      {
                        "type": "TRANSLATE",
                        "fromLanguage": "L1",
                        "textToTranslate": "text1",
                        "toLanguage": "L2",
                        "translatedText": "text2"
                      }
                    ]
                  },
                  {"chapterName": "child-2"}
                ],
                "cards": [
                  {"type": "QUESTION_ANSWER", "question": "Capital of France?", "answer": "Paris", "notes": "notes1"},
                  {
                    "type": "TRANSLATE",
                    "fromLanguage": "L3",
                    "textToTranslate": "text3",
                    "toLanguage": "L4",
                    "translatedText": "text4",
                    "notes": "notes4"
                  }
                ]
              }
            }
            """;

        CardCollectionDto collection = utils.parseJson(json, CardCollectionDto.class);

        Assertions.assertEquals("test-generator", collection.getGeneratedBy());
        ChapterDto root = collection.getCards();
        Assertions.assertEquals("root", root.getChapterName());
        Assertions.assertEquals(2, root.getChapters().size());
        Assertions.assertEquals(2, root.getCards().size());
        assertQuestionAnswer(root.getCards().get(0), "Capital of France?", "Paris");
        Assertions.assertEquals("notes1", root.getCards().get(0).getNotes());
        assertTranslation(root.getCards().get(1), "L3", "text3", "L4", "text4");
        Assertions.assertEquals("notes4", root.getCards().get(1).getNotes());

        ChapterDto child1 = root.getChapters().get(0);
        Assertions.assertEquals("child-1", child1.getChapterName());
        Assertions.assertEquals(1, child1.getCards().size());
        assertTranslation(child1.getCards().get(0), "L1", "text1", "L2", "text2");
        Assertions.assertNull(child1.getCards().get(0).getNotes());

        Assertions.assertEquals(1, child1.getChapters().size());
        ChapterDto grandchild = child1.getChapters().get(0);
        Assertions.assertEquals("grandchild", grandchild.getChapterName());
        Assertions.assertTrue(grandchild.getChapters().isEmpty());
        Assertions.assertEquals(1, grandchild.getCards().size());
        assertQuestionAnswer(grandchild.getCards().get(0), "2+2?", "4");

        ChapterDto child2 = root.getChapters().get(1);
        Assertions.assertEquals("child-2", child2.getChapterName());
        Assertions.assertNull(child2.getChapters());
        Assertions.assertNull(child2.getCards());
    }

    @Test
    void parseJson_restores_object_serialized_by_objToJson() {
        CardCollectionDto original = CardCollectionDto.builder()
            .generatedBy("gen")
            .cards(ChapterDto.builder()
                .chapterName("root")
                .chapters(List.of())
                .cards(List.of(
                    QuestionAnswerCardDto.builder().question("q").answer("a").notes("n").build(),
                    TranslationCardDto.builder()
                        .fromLanguage("L1").textToTranslate("text1")
                        .toLanguage("L2").translatedText("text2")
                        .build()
                ))
                .build())
            .build();

        String json = utils.objToJson(original);
        CardCollectionDto parsed = utils.parseJson(json, CardCollectionDto.class);

        Assertions.assertEquals(json, utils.objToJson(parsed));
        Assertions.assertEquals("gen", parsed.getGeneratedBy());
        Assertions.assertEquals(2, parsed.getCards().getCards().size());
        assertQuestionAnswer(parsed.getCards().getCards().get(0), "q", "a");
        Assertions.assertEquals("n", parsed.getCards().getCards().get(0).getNotes());
        assertTranslation(parsed.getCards().getCards().get(1), "L1", "text1", "L2", "text2");
    }

    @Test
    void parseJson_fails_for_unknown_card_type() {
        String json = """
            {"generatedBy": "g", "cards": {"chapterName": "root", "cards": [{"type": "UNKNOWN", "question": "q"}]}}
            """;

        Assertions.assertThrows(Exception.class, () -> utils.parseJson(json, CardCollectionDto.class));
    }

    private void assertQuestionAnswer(CardDto card, String question, String answer) {
        QuestionAnswerCardDto qa = Assertions.assertInstanceOf(QuestionAnswerCardDto.class, card);
        Assertions.assertEquals(CardType.QUESTION_ANSWER, qa.getType());
        Assertions.assertEquals(question, qa.getQuestion());
        Assertions.assertEquals(answer, qa.getAnswer());
    }

    private void assertTranslation(
        CardDto card, String fromLanguage, String textToTranslate, String toLanguage, String translatedText
    ) {
        TranslationCardDto tr = Assertions.assertInstanceOf(TranslationCardDto.class, card);
        Assertions.assertEquals(CardType.TRANSLATE, tr.getType());
        Assertions.assertEquals(fromLanguage, tr.getFromLanguage());
        Assertions.assertEquals(textToTranslate, tr.getTextToTranslate());
        Assertions.assertEquals(toLanguage, tr.getToLanguage());
        Assertions.assertEquals(translatedText, tr.getTranslatedText());
    }
}
