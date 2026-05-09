package org.igye.remem3.app.controllers2.validatecards;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.state.StateConstructor;

import java.io.File;

import static org.igye.remem3.app.controllers2.validatecards.ValidateCardsRenderer.PAR_DIR;

@RequiredArgsConstructor
public class ValidateCardsConstructor implements StateConstructor<ValidateCardsState> {
    private final Settings settings;
    private final Cache cache;

    @Override
    public String getName() {
        return "validate_cards";
    }

    @Override
    public ValidateCardsState construct() {
        return ValidateCardsState.builder()
            .dir(new DirSelectorCmpImpl(settings, cache, PAR_DIR, false,
                new File(cache.getStr(PAR_DIR, ""))
            ))
            .build();
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
