package org.igye.remem3.app.controllers.cardimporter;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.state.StateConstructor;
import org.igye.remem3.web.impl.RequestParamsImpl;
import org.springframework.core.annotation.Order;

import static org.igye.remem3.app.controllers.cardimporter.CardImporterRenderer.PAR_DIR;

@RequiredArgsConstructor
@Order(7)
public class CardImporterConstructor implements StateConstructor<CardImporterState> {
    private final Settings settings;
    private final Cache cache;

    @Override
    public String getName() {
        return "card_importer";
    }

    @Override
    public String getDisplayName() {
        return "Card importer";
    }

    @Override
    public CardImporterState construct() {
        return CardImporterState.builder()
            .dir(makeDirSelector().setPath(RequestParamsImpl.empty()))
            .build();
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    public DirSelectorCmpImpl makeDirSelector() {
        return new DirSelectorCmpImpl(settings, cache, PAR_DIR).setAllowMkDir(true);
    }
}
