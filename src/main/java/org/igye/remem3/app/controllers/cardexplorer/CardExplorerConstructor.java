package org.igye.remem3.app.controllers.cardexplorer;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.controllers.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.state.StateConstructor;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.impl.RequestParamsImpl;
import org.springframework.core.annotation.Order;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_DIR;
import static org.igye.remem3.app.impl.CardUtilsImpl.CARD_EXTENSION;

@RequiredArgsConstructor
@Order(3)
public class CardExplorerConstructor implements StateConstructor<CardExplorerState> {
    private final Settings settings;
    private final Cache cache;
    private final CardUtils cardUtils;

    @Override
    public String getName() {
        return "card_explorer";
    }

    @Override
    public String getDisplayName() {
        return "Card explorer";
    }

    @Override
    public CardExplorerState construct() {
        return readStateFromParams(RequestParamsImpl.empty());
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @SneakyThrows
    public CardExplorerState readStateFromParams(RequestParams params) {
        DirSelectorCmp dir = new DirSelectorCmpImpl(settings, cache, PAR_DIR, false, params);
        List<Card> cards = Files.list(dir.getSelectedDirectory().toPath())
            .map(Path::toFile)
            .filter(File::isFile)
            .filter(file -> file.getName().endsWith(CARD_EXTENSION))
            .map(cardUtils::loadCard)
            .sorted(Comparator.comparing(Card::getOrder))
            .toList();
        return CardExplorerState.builder()
            .dir(dir)
            .cards(cards)
            .build();
    }

}
