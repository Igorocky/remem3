package org.igye.remem3.datamigration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cards;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.BucketDelaysDto;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.impl.CardsImpl;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.utils.impl.UtilsImpl;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        Cards cardUtils = new CardsImpl(utils, settings);
        Map<Integer, Card> cards = selectCards(database, langs, dirs, cardUtils);
        System.out.println(dirs);
    }

    private Map<Integer, Card> selectCards(
        Database database,
        Map<Integer, String> langs,
        Map<Integer, File> dirs,
        Cards cardUtils
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
                """).stream()
            .filter(row -> expectedCardTypes.contains((String) row.get("typ")))
            .map(row -> {
                String cardType = (String) row.get("typ");
                Optional<File> file = Optional.of(dirs.get((Integer) row.get("folder_id")));
                Optional<Instant> createdAt = Optional.of(Instant.ofEpochMilli(((Integer) row.get("crt_time")) * 1000));
                Card card = switch (cardType) {
                    case "fill_gaps" -> Card.FillGaps.builder()
                        .file(file)
                        .createdAt(createdAt)
                        .history(List.of())
                        .lang(notNull(langs.get((Integer) row.get("card_fill_lang"))))
                        .descr((String) row.get("card_fill_descr"))
                        .text(cardUtils.parseText(notNull((String) row.get("card_fill_text"))))
                        .notes((String) row.get("card_fill_notes"))
                        .build();
                    case "translate" -> Card.Translate.builder()
                        .file(file)
                        .createdAt(createdAt)
                        .history(List.of())
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

    private <T> T notNull(T obj) {
        if (obj == null) {
            throw new Exn("Expected not null");
        }
        return obj;
    }

    private Map<Integer, List<HistRec>> selectHistForCards(Database database) {
        return null;
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
