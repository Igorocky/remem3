package org.igye.remem3.app.controllers.cardimporter;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.controllers.makenewdir.State;
import org.igye.remem3.app.imprt.CardCollectionDto;
import org.igye.remem3.app.imprt.CardDto;
import org.igye.remem3.app.imprt.ChapterDto;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.web.RequestParams;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.ACT_CLEAR;
import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.ACT_IMPORT;
import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.ACT_LOAD;
import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.PAR_DIR;
import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.PAR_FILE;
import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.PAR_JSON_TEXT;
import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.PAR_ROOT_CHAPTER_NAME;
import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.PAR_SKIP_ROOT_DIR;

@RequiredArgsConstructor
public class CardImporterUpdater implements StateUpdater<CardImporterState> {
    private final CardImporterConstructor constructor;
    private final Cache cache;
    private final Utils utils;
    private final CardUtils cardUtils;

    @Override
    public Object update(CardImporterState st, RequestParams params) {
        st = mergeStateFromParams(st, params);
        if (st.getDir().isMkDirRequest()) {
            CardImporterState finalState = st;
            return State.builder()
                .parentDir(((DirSelectorCmpImpl) st.getDir()).clone().setReadOnly(true))
                .onComplete(newDir -> refreshPreview(
                    finalState.withDir(((DirSelectorCmpImpl) finalState.getDir()).setPath(newDir))
                ))
                .onCancel(refreshPreview(st))
                .build();
        }
        if (params.hasParam(ACT_LOAD)) {
            st = actLoad(st, params);
        } else if (params.hasParam(ACT_CLEAR)) {
            st = clearLoadedCards(st);
        } else if (params.hasParam(ACT_IMPORT)) {
            st = actImport(st);
        }
        return refreshPreview(st);
    }

    private CardImporterState mergeStateFromParams(CardImporterState st, RequestParams params) {
        if (params.hasKeyValueParam(PAR_DIR)) {
            st = st.withDir(constructor.makeDirSelector().setPath(params));
            cache.put(PAR_DIR, st.getDir().getSelectedDirectoryStr());
        }
        st = st.withJsonText(params.getParam(PAR_JSON_TEXT, st.getJsonText()));
        if (st.getCollection().isPresent() && params.hasParam(PAR_ROOT_CHAPTER_NAME)) {
            st = st
                .withRootChapterName(params.getParam(PAR_ROOT_CHAPTER_NAME))
                .withSkipRootDir(params.hasParam(PAR_SKIP_ROOT_DIR));
        }
        return st;
    }

    private CardImporterState actLoad(CardImporterState st, RequestParams params) {
        Optional<String> fileContent = params.getFileContent(PAR_FILE);
        String json = fileContent.orElse(st.getJsonText());
        st = clearLoadedCards(st).withImportResult(Optional.empty());
        if (StringUtils.isBlank(json)) {
            return st.withErrors(List.of("Choose a JSON file or paste a JSON text."));
        }
        CardCollectionDto collection;
        try {
            collection = utils.parseJson(json, CardCollectionDto.class);
        } catch (Exception ex) {
            return st.withErrors(List.of("Cannot parse the JSON: %s".formatted(ex.getMessage())));
        }
        if (collection == null) {
            return st.withErrors(List.of("The JSON doesn't contain a card collection."));
        }
        ChapterDto rootChapter = collection.getCards();
        return st
            .withJsonText("")
            .withCollection(Optional.of(collection))
            .withSource(fileContent.isPresent() ? "the uploaded file" : "the pasted text")
            .withRootChapterName(rootChapter == null ? "" : StringUtils.defaultString(rootChapter.getChapterName()));
    }

    private CardImporterState actImport(CardImporterState st) {
        if (st.getCollection().isEmpty()) {
            return st;
        }
        st = refreshPreview(st);
        if (!st.getErrors().isEmpty()) {
            return st;
        }
        File baseDir = st.getDir().getSelectedDirectory();
        List<String> errors = cardUtils.importCards(baseDir, makeCollectionToImport(st), st.isSkipRootDir());
        if (!errors.isEmpty()) {
            return st.withErrors(errors);
        }
        ImportResult importResult = new ImportResult(
            st.getChapterTree().map(ChapterView::getTotalNumOfCards).orElse(0L),
            st.isSkipRootDir() ? baseDir : new File(baseDir, utils.sanitizeDirName(st.getRootChapterName()))
        );
        return clearLoadedCards(st).withJsonText("").withImportResult(Optional.of(importResult));
    }

    private CardImporterState clearLoadedCards(CardImporterState st) {
        return st
            .withErrors(List.of())
            .withCollection(Optional.empty())
            .withSource("")
            .withRootChapterName("")
            .withSkipRootDir(false)
            .withChapterTree(Optional.empty());
    }

    private CardImporterState refreshPreview(CardImporterState st) {
        if (st.getCollection().isEmpty()) {
            return st;
        }
        File baseDir = st.getDir().getSelectedDirectory();
        CardCollectionDto collection = makeCollectionToImport(st);
        return st
            .withErrors(cardUtils.validateCardsForImport(baseDir, collection, st.isSkipRootDir()))
            .withChapterTree(
                Optional.ofNullable(collection.getCards())
                    .map(rootChapter -> makeChapterView(baseDir, rootChapter, st.isSkipRootDir()))
            );
    }

    private CardCollectionDto makeCollectionToImport(CardImporterState st) {
        CardCollectionDto collection = st.getCollection().get();
        ChapterDto rootChapter = collection.getCards();
        if (rootChapter == null) {
            return collection;
        }
        return CardCollectionDto.builder()
            .generatedBy(collection.getGeneratedBy())
            .cards(
                ChapterDto.builder()
                    .chapterName(st.getRootChapterName())
                    .chapters(rootChapter.getChapters())
                    .cards(rootChapter.getCards())
                    .build()
            )
            .build();
    }

    private ChapterView makeChapterView(File parentDir, ChapterDto chapter, boolean skipDir) {
        String dirName = null;
        String dirError = null;
        File dir = null;
        if (skipDir) {
            dir = parentDir;
        } else {
            try {
                dirName = utils.sanitizeDirName(chapter.getChapterName());
                dir = parentDir == null ? null : new File(parentDir, dirName);
            } catch (Exn ex) {
                dirError = ex.getMessage();
            }
        }
        File chapterDir = dir;
        return ChapterView.builder()
            .chapterName(StringUtils.defaultString(chapter.getChapterName()))
            .dirName(dirName)
            .dirError(dirError)
            .dirSkipped(skipDir)
            .dirExists(!skipDir && dir != null && dir.isDirectory())
            .cardCounts(
                ListUtils.emptyIfNull(chapter.getCards()).stream()
                    .filter(card -> card != null && card.getType() != null)
                    .collect(Collectors.groupingBy(CardDto::getType, TreeMap::new, Collectors.counting()))
            )
            .chapters(
                ListUtils.emptyIfNull(chapter.getChapters()).stream()
                    .filter(Objects::nonNull)
                    .map(subChapter -> makeChapterView(chapterDir, subChapter, false))
                    .toList()
            )
            .build();
    }
}
