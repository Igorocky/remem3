package org.igye.remem3.app.manager.language;

import lombok.Builder;
import lombok.Data;
import org.igye.remem3.app.db.entities.LangEnt;

import java.util.List;

@Builder
@Data
public class LangState {
    private List<LangEnt> allLangs;
    private Long editLangId;
}
