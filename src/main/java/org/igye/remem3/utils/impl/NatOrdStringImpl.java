package org.igye.remem3.utils.impl;

import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.NatOrdString;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NatOrdStringImpl implements NatOrdString {
    private static final Pattern PATTERN = Pattern.compile("^(\\d*)(.*?)(\\d*)$", Pattern.DOTALL);
    private final String baseString;
    @Getter
    private final String left;
    @Getter
    protected final BigDecimal leftNum;
    @Getter
    private final String middle;
    @Getter
    private final String right;
    @Getter
    protected final BigDecimal rightNum;

    public NatOrdStringImpl(@NonNull String baseString) {
        this.baseString = baseString;
        Matcher matcher = PATTERN.matcher(baseString);
        if (!matcher.matches()) {
            throw new Exn("!matcher.matches() for '%s'".formatted(baseString));
        }
        this.left = matcher.group(1);
        this.middle = matcher.group(2);
        this.right = matcher.group(3);
        this.leftNum = StringUtils.isBlank(left) ? BigDecimal.ZERO : new BigDecimal(left);
        this.rightNum = StringUtils.isBlank(right) ? BigDecimal.ZERO : new BigDecimal(right);
    }

    private static final boolean NULL_IS_LESS = true;

    @Override
    public String getValue() {
        return baseString;
    }

    @Override
    public int compareTo(NatOrdString other) {
        int res = leftNum.compareTo(other.getLeftNum());
        if (res != 0) {
            return res;
        }
        res = StringUtils.compare(this.getMiddle(), other.getMiddle(), NULL_IS_LESS);
        if (res != 0) {
            return res;
        }
        return rightNum.compareTo(other.getRightNum());
    }
}
