package org.igye.remem3.app.dto;

import org.igye.remem3.utils.Exn;

public enum CardType {
    FILL_GAPS("fill_gaps", "Fill Gaps"),
    TRANSLATE("translate", "Translate");

    private final String code;
    private final String displayName;

    CardType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CardType fromCode(String code) {
        for (CardType t : values()) {
            if (t.getCode().equals(code)) {
                return t;
            }
        }
        throw new Exn(String.format("Unknown code of card type %s", code));
    }
}
