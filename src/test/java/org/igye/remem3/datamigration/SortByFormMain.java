package org.igye.remem3.datamigration;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.BucketDelaysDto;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.impl.CardUtilsImpl;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.NotImplemented;
import org.igye.remem3.utils.impl.UtilsImpl;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

public class SortByFormMain {
    public static void main(String[] args) {
        new SortByFormMain().run();
    }

    public void run() {
        CardUtils cardUtils = new CardUtilsImpl(new UtilsImpl(new ObjectMapper()), makeSettings());
        File sentensesDir = new File("");
        List<Card> allCards = cardUtils.loadAllCards(
            sentensesDir
        );
        List<File> formDirs = Stream.of(1, 2, 3).map(i -> new File(sentensesDir, "form_" + i)).toList();
        for (Card card : allCards) {
            cardUtils.saveCard(
                new File(formDirs.get(getForm((Card.FillGaps) card) - 1), card.getFile().get().getName()),
                card
            );
        }
    }

    private int getForm(Card.FillGaps card) {
        return Integer.parseInt(card.getNotes().substring(0, 1));
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
                throw new Exn("not implemented");
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
