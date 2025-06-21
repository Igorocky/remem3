package org.igye.remem3.app.manager.language;

import lombok.Builder;
import lombok.Data;
import lombok.With;
import org.igye.remem3.app.db.entities.LangEnt;

import java.util.List;
import java.util.UUID;

@Builder
@Data
@With
public class LangState {
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private List<LangEnt> allLangs;
    private Long editLangId;
}
