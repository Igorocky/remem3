package org.igye.remem3.app.controllers.cardimporter;

import jakarta.servlet.http.HttpServletRequest;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.cardexplorer.CardExplorerConstructor;
import org.igye.remem3.app.controllers.makenewdir.State;
import org.igye.remem3.app.imprt.CardType;
import org.igye.remem3.app.impl.CardUtilsImpl;
import org.igye.remem3.app.impl.SettingsImpl;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.impl.RequestParamsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.ACT_APPLY;
import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.ACT_CLEAR;
import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.ACT_IMPORT;
import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.ACT_LOAD;
import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.PAR_DIR;
import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.PAR_JSON_TEXT;
import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.PAR_ROOT_CHAPTER_NAME;
import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.PAR_SKIP_ROOT_DIR;

class CardImporterUpdaterTest {
    private static final String JSON = """
        {
          "generatedBy": "generator1",
          "cards": {
            "chapterName": "root: chapter",
            "chapters": [
              {
                "chapterName": "child1",
                "cards": [
                  {"type": "QUESTION_ANSWER", "question": "question1", "answer": "answer1"},
                  {"type": "QUESTION_ANSWER", "question": "question2", "answer": "answer2"}
                ]
              },
              {"chapterName": "child2"}
            ],
            "cards": [
              {
                "type": "TRANSLATE",
                "fromLanguage": "LANG1",
                "textToTranslate": "text1",
                "toLanguage": "LANG2",
                "translatedText": "text2"
              }
            ]
          }
        }
        """;

    @TempDir
    File baseDir;

    private CardUtilsImpl cardUtils;
    private CardImporterConstructor constructor;
    private CardImporterUpdater updater;
    private CardImporterRenderer renderer;

    @BeforeEach
    void setUp() {
        Settings settings = SettingsImpl.builder()
            .languages(List.of("LANG1", "LANG2", "QLANG", "ALANG"))
            .questionLanguage("QLANG")
            .answerLanguage("ALANG")
            .directoriesWithCards(List.of(baseDir.getAbsolutePath()))
            .build();
        Cache cache = Mockito.mock(Cache.class);
        Mockito.when(cache.getStr(Mockito.any(), Mockito.any())).thenAnswer(inv -> inv.getArgument(1));
        UtilsImpl utils = new UtilsImpl(new ObjectMapper());
        cardUtils = new CardUtilsImpl(utils, settings);
        constructor = new CardImporterConstructor(settings, cache);
        updater = new CardImporterUpdater(constructor, cache, utils, cardUtils);
        renderer = new CardImporterRenderer(new CardExplorerConstructor(settings, cache, cardUtils));
    }

    @Test
    void load_shows_chapter_tree_with_card_counts() {
        CardImporterState st = update(constructor.construct(), Map.of(ACT_LOAD, "Load", PAR_JSON_TEXT, JSON));

        Assertions.assertEquals(List.of(), st.getErrors());
        Assertions.assertEquals("", st.getJsonText());
        Assertions.assertEquals("root: chapter", st.getRootChapterName());
        ChapterView root = st.getChapterTree().get();
        Assertions.assertEquals("root: chapter", root.getChapterName());
        Assertions.assertEquals("root chapter", root.getDirName());
        Assertions.assertFalse(root.isDirExists());
        Assertions.assertEquals(Map.of(CardType.TRANSLATE, 1L), root.getCardCounts());
        Assertions.assertEquals(3, root.getTotalNumOfCards());
        Assertions.assertEquals(2, root.getChapters().size());
        Assertions.assertEquals(Map.of(CardType.QUESTION_ANSWER, 2L), root.getChapters().get(0).getCardCounts());
        Assertions.assertEquals(Map.of(), root.getChapters().get(1).getCardCounts());
        String html = renderer.render(st).toString();
        Assertions.assertTrue(html.contains("QUESTION_ANSWER: 2"));
        Assertions.assertTrue(html.contains("Import 3 cards"));
        Assertions.assertEquals(0, baseDir.list().length);
    }

    @Test
    void load_shows_error_for_invalid_json() {
        CardImporterState st = update(constructor.construct(), Map.of(ACT_LOAD, "Load", PAR_JSON_TEXT, "{abc"));

        Assertions.assertTrue(st.getCollection().isEmpty());
        Assertions.assertEquals("{abc", st.getJsonText());
        Assertions.assertEquals(1, st.getErrors().size());
        Assertions.assertTrue(st.getErrors().get(0).startsWith("Cannot parse the JSON:"));
        renderer.render(st);
    }

    @Test
    void load_shows_validation_errors() {
        CardImporterState st = update(
            constructor.construct(),
            Map.of(ACT_LOAD, "Load", PAR_JSON_TEXT, JSON.replace("LANG2", "LANG3"))
        );

        Assertions.assertTrue(st.getCollection().isPresent());
        Assertions.assertEquals(
            List.of("Chapter /root: chapter, card #1: Language2 'LANG3' is not registered."),
            st.getErrors()
        );

        st = update(st, Map.of(ACT_IMPORT, "Import", PAR_ROOT_CHAPTER_NAME, "root"));

        Assertions.assertTrue(st.getImportResult().isEmpty());
        Assertions.assertEquals(
            List.of("Chapter /root, card #1: Language2 'LANG3' is not registered."),
            st.getErrors()
        );
        Assertions.assertEquals(0, baseDir.list().length);
    }

    @Test
    void import_creates_cards_in_renamed_root_dir_and_resets_the_page() {
        new File(baseDir, "new root").mkdir();
        CardImporterState st = update(constructor.construct(), Map.of(ACT_LOAD, "Load", PAR_JSON_TEXT, JSON));

        st = update(st, Map.of(ACT_APPLY, "Apply", PAR_ROOT_CHAPTER_NAME, "new root"));

        Assertions.assertEquals("new root", st.getChapterTree().get().getChapterName());
        Assertions.assertTrue(st.getChapterTree().get().isDirExists());
        Assertions.assertFalse(st.getChapterTree().get().getChapters().get(0).isDirExists());

        st = update(st, Map.of(ACT_IMPORT, "Import", PAR_ROOT_CHAPTER_NAME, "new root"));

        Assertions.assertEquals(List.of(), st.getErrors());
        Assertions.assertTrue(st.getCollection().isEmpty());
        Assertions.assertTrue(st.getChapterTree().isEmpty());
        Assertions.assertEquals(3, st.getImportResult().get().getNumOfCreatedCards());
        File rootDir = new File(baseDir, "new root");
        Assertions.assertEquals(rootDir, st.getImportResult().get().getDir());
        Assertions.assertEquals(1, cardUtils.loadCardsNonRec(rootDir).size());
        Assertions.assertEquals(2, cardUtils.loadCardsNonRec(new File(rootDir, "child1")).size());
        Assertions.assertTrue(new File(rootDir, "child2").isDirectory());
        Assertions.assertTrue(renderer.render(st).toString().contains("3 cards created in"));
    }

    @Test
    void import_puts_child_chapters_to_base_dir_when_root_dir_is_skipped() throws Exception {
        CardImporterState st = update(constructor.construct(), Map.of(ACT_LOAD, "Load", PAR_JSON_TEXT, JSON));

        st = update(st, Map.of(PAR_ROOT_CHAPTER_NAME, "root: chapter", PAR_SKIP_ROOT_DIR, PAR_SKIP_ROOT_DIR));

        Assertions.assertTrue(st.isSkipRootDir());
        Assertions.assertTrue(st.getChapterTree().get().isDirSkipped());
        renderer.render(st);

        st = update(st, Map.of(
            ACT_IMPORT, "Import", PAR_ROOT_CHAPTER_NAME, "root: chapter", PAR_SKIP_ROOT_DIR, PAR_SKIP_ROOT_DIR
        ));

        Assertions.assertEquals(3, st.getImportResult().get().getNumOfCreatedCards());
        Assertions.assertEquals(baseDir.getCanonicalFile(), st.getImportResult().get().getDir());
        Assertions.assertEquals(1, cardUtils.loadCardsNonRec(baseDir).size());
        Assertions.assertEquals(2, cardUtils.loadCardsNonRec(new File(baseDir, "child1")).size());
        Assertions.assertTrue(new File(baseDir, "child2").isDirectory());
        Assertions.assertFalse(new File(baseDir, "root chapter").exists());
    }

    @Test
    void clear_removes_loaded_cards() {
        CardImporterState st = update(constructor.construct(), Map.of(ACT_LOAD, "Load", PAR_JSON_TEXT, JSON));

        st = update(st, Map.of(ACT_CLEAR, "Clear", PAR_ROOT_CHAPTER_NAME, "root: chapter"));

        Assertions.assertTrue(st.getCollection().isEmpty());
        Assertions.assertTrue(st.getChapterTree().isEmpty());
        Assertions.assertEquals(0, baseDir.list().length);
    }

    @Test
    void selecting_plus_in_dir_selector_opens_make_new_dir_page() throws Exception {
        CardImporterState st = update(constructor.construct(), Map.of(ACT_LOAD, "Load", PAR_JSON_TEXT, JSON));

        Object newState = updater.update(st, params(Map.of(
            PAR_DIR + ":0", baseDir.getAbsolutePath(), PAR_DIR + ":1", "+", PAR_ROOT_CHAPTER_NAME, "root"
        )));

        State makeNewDirState = Assertions.assertInstanceOf(State.class, newState);
        File newDir = new File(baseDir, "newDir");
        newDir.mkdir();
        CardImporterState afterMkDir = (CardImporterState) makeNewDirState.getOnComplete().apply(newDir);
        Assertions.assertEquals(newDir.getCanonicalPath(), afterMkDir.getDir().getSelectedDirectoryStr());
        Assertions.assertEquals("root", afterMkDir.getChapterTree().get().getChapterName());
    }

    private CardImporterState update(CardImporterState st, Map<String, String> params) {
        return (CardImporterState) updater.update(st, params(params));
    }

    private RequestParams params(Map<String, String> params) {
        Map<String, String[]> paramMap = new HashMap<>();
        params.forEach((name, value) -> paramMap.put(name, new String[]{value}));
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getParameterMap()).thenReturn(paramMap);
        return new RequestParamsImpl(req);
    }
}
