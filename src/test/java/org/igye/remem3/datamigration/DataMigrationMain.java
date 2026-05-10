package org.igye.remem3.datamigration;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.BucketDelaysDto;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.app.impl.CardUtilsImpl;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.NotImplemented;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.utils.impl.UtilsImpl;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.igye.remem3.app.impl.CardUtilsImpl.CARD_FILL_GAPS_FILE_EXTENSION;
import static org.igye.remem3.app.impl.CardUtilsImpl.CARD_TRANSLATE_FILE_EXTENSION;

public class DataMigrationMain {
    public static void main(String[] args) {
        new DataMigrationMain().run();
    }

    private void run() {
        String dbUrl = "jdbc:sqlite:/home/igor/tmp/remem__2025_06_14__18_55_51.sqlite";
        Database database = new DatabaseImpl(makeDataSource(dbUrl));
        Map<Integer, String> langs = selectLangs(database);
        Map<Integer, File> dirs = selectDirs(database);
        Settings settings = makeSettings();
        ObjectMapper objectMapper = new ObjectMapper();
        Utils utils = new UtilsImpl(objectMapper);
        CardUtils cardUtils = new CardUtilsImpl(utils, settings);
        Map<Integer, Card> cardsWithoutHistory = selectCards(database, langs, dirs, cardUtils);
        List<Card> cards = loadHistForCards(database, cardsWithoutHistory);
        saveCards(new File("/home/igor/tmp/remem_migrated"), cards, cardUtils);
    }

    private void saveCards(File baseDir, List<Card> cards, CardUtils cardUtils) {
        for (Card card : cards) {
            File dir = new File(baseDir, card.getFile().get().getPath());
            cardUtils.saveCard(new File(dir, makeFileName(card)), card);
        }
    }

    private String makeFileName(Card card) {
        String baseName = UUID.randomUUID().toString().replace("-", "_");
        String extension = switch (card) {
            case Card.FillGaps _ -> CARD_FILL_GAPS_FILE_EXTENSION;
            case Card.Translate _ -> CARD_TRANSLATE_FILE_EXTENSION;
        };
        return baseName + extension;
    }

    private Map<Integer, Card> selectCards(
        Database database,
        Map<Integer, String> langs,
        Map<Integer, File> dirs,
        CardUtils cardUtils
    ) {
        Set<String> expectedCardTypes = Set.of("fill_gaps", "translate");
        return database.select("""
                select card.id, card.crt_time, card.folder_id, ct.code typ,
                       card_fill.lang_id card_fill_lang, card_fill.descr card_fill_descr, card_fill.text card_fill_text, card_fill.notes card_fill_notes,
                       card_tran.lang1_id card_tran_lang1, card_tran.read_only1 card_tran_readonly1, card_tran.text1 card_tran_text1,
                       card_tran.lang2_id card_tran_lang2, card_tran.read_only2 card_tran_readonly2, card_tran.text2 card_tran_text2,
                       card_tran.notes card_tran_notes
                from CARD card
                left join CARD_TYPE ct on card.card_type_id = ct.id
                left join CARD_FILL card_fill on card.id = card_fill.id
                left join CARD_TRAN card_tran on card.id = card_tran.id
                """
            ).stream()
            .filter(row -> expectedCardTypes.contains((String) row.get("typ")))
            .map(row -> {
                String cardType = (String) row.get("typ");
                Optional<File> file = Optional.of(dirs.get((Integer) row.get("folder_id")));
                Optional<Instant> createdAt = Optional.of(toInstant((Integer) row.get("crt_time")));
                Card card = switch (cardType) {
                    case "fill_gaps" -> Card.FillGaps.builder()
                        .file(file)
                        .createdAt(createdAt)
                        .history(new ArrayList<>())
                        .lang(notNull(langs.get((Integer) row.get("card_fill_lang"))))
                        .descr((String) row.get("card_fill_descr"))
                        .text(cardUtils.parseText(notNull((String) row.get("card_fill_text"))))
                        .notes((String) row.get("card_fill_notes"))
                        .build();
                    case "translate" -> Card.Translate.builder()
                        .file(file)
                        .createdAt(createdAt)
                        .history(new ArrayList<>())
                        .lang1(notNull(langs.get((Integer) row.get("card_tran_lang1"))))
                        .text1((String) row.get("card_tran_text1"))
                        .exactMatch1(((Integer) row.get("card_tran_readonly1")) == 0)
                        .lang2(notNull(langs.get((Integer) row.get("card_tran_lang2"))))
                        .text2((String) row.get("card_tran_text2"))
                        .exactMatch2(((Integer) row.get("card_tran_readonly2")) == 0)
                        .notes((String) row.get("card_tran_notes"))
                        .build();
                    default -> throw new Exn(String.format("Unexpected card type %s.", cardType));
                };
                return Pair.of((Integer) row.get("id"), card);
            })
            .collect(Collectors.toMap(
                Pair::getLeft,
                Pair::getRight
            ));
    }

    private Instant toInstant(Integer seconds) {
        return Instant.ofEpochMilli(seconds.longValue() * 1000);
    }

    private List<Card> loadHistForCards(
        Database database,
        Map<Integer, Card> cards
    ) {
        Map<Integer, List<HistRec>> hist = database.select("""
                select task.card_id, hist.time, tt.code, hist.mark, hist.note
                from TASK_HIST hist
                left join TASK task on hist.task_id = task.id
                left join TASK_TYPE tt on task.task_type_id = tt.id            
                """
            ).stream()
            .map(row -> {
                int cardId = (Integer) row.get("card_id");
                HistRec histRec = HistRec.builder()
                    .time(toInstant((Integer) row.get("time")))
                    .taskType(makeTaskType(cardId, (String) row.get("code"), cards))
                    .mark(new BigDecimal((Double) row.get("mark")))
                    .notes((String) row.get("note"))
                    .build();
                return Pair.of(cardId, histRec);
            })
            .collect(Collectors.groupingBy(Pair::getLeft, Collectors.mapping(Pair::getRight, Collectors.toList())));
        cards.entrySet().forEach(e -> {
            List<HistRec> history = e.getValue().getHistory();
            List<HistRec> cardHist = hist.get(e.getKey());
            if (history.isEmpty() && cardHist != null) {
                history.addAll(cardHist.stream().sorted(Comparator.comparing(HistRec::getTime)).toList());
            }
        });
        return cards.values().stream().toList();
    }

    private String makeTaskType(int cardId, String taskCode, Map<Integer, Card> cards) {
        return switch (taskCode) {
            case "fill_gaps" -> new TaskType.FillGaps(((Card.FillGaps) cards.get(cardId)).getLang()).getCode();
            case "lang1->lang2" -> {
                Card.Translate card = (Card.Translate) cards.get(cardId);
                yield new TaskType.Translate(card.getLang1(), card.getLang2()).getCode();
            }
            case "lang2->lang1" -> {
                Card.Translate card = (Card.Translate) cards.get(cardId);
                yield new TaskType.Translate(card.getLang2(), card.getLang1()).getCode();
            }
            default -> throw new Exn(String.format("Unexpected task type code %s.", taskCode));
        };
    }

    private <T> T notNull(T obj) {
        if (obj == null) {
            throw new Exn("Expected not null");
        }
        return obj;
    }

    private Map<Integer, String> selectLangs(Database database) {
        return database.select("select id, name from LANGUAGE").stream()
            .collect(Collectors.toMap(
                r -> (Integer) r.get("id"),
                r -> (String) r.get("name")
            ));
    }

    private Map<Integer, File> selectDirs(Database database) {
        Map<Integer, Pair<Integer, String>> dirs = database.select("select id, parent_id, name from FOLDER").stream()
            .collect(Collectors.toMap(
                r -> (Integer) r.get("id"),
                r -> Pair.of((Integer) r.get("parent_id"), (String) r.get("name"))
            ));
        return dirs.keySet().stream()
            .collect(Collectors.toMap(
                Function.identity(),
                id -> makeDir(id, dirs)
            ));
    }

    private File makeDir(Integer id, Map<Integer, Pair<Integer, String>> dirs) {
        Pair<Integer, String> parentAndName = dirs.get(id);
        Integer parentId = parentAndName.getLeft();
        String dirName = StringUtils.strip(parentAndName.getRight(), ".");
        if (parentId == null) {
            return new File(dirName);
        }
        return new File(makeDir(parentId, dirs), dirName);
    }

    private BasicDataSource makeDataSource(String url) {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUsername("");
        ds.setPassword("");
        ds.setUrl(url);
        ds.setMaxTotal(5);
        ds.setMaxIdle(5);
        ds.setInitialSize(5);
        ds.setValidationQuery("select 1");
        ds.setDefaultAutoCommit(true);
        ds.setAutoCommitOnReturn(true);
        return ds;
    }

    private Settings makeSettings() {
        return new Settings() {
            @Override
            public String getCacheFile() {
                throw new Exn("not implemented");
            }

            @Override
            public String getBeansFile() {
                throw new NotImplemented();
            }

            @Override
            public List<String> getLanguages() {
                return List.of("ENG", "POL", "UKR", "RUS");
            }

            @Override
            public List<String> getDirectoriesWithCards() {
                throw new Exn("not implemented");
            }

            @Override
            public String getCardEditor() {
                throw new Exn("not implemented");
            }

            @Override
            public List<String> getPropsToPassToBeans() {
                throw new NotImplemented();
            }

            @Override
            public List<BucketDelaysDto> getBucketDelays() {
                throw new Exn("not implemented");
            }

            @Override
            public List<Pair<String, File>> getExercises() {
                throw new Exn("not implemented");
            }
        };
    }
}
