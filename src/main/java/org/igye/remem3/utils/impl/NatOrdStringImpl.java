package org.igye.remem3.utils.impl;

import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.utils.NatOrdString;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NatOrdStringImpl implements NatOrdString {
    private static final Pattern PATTERN1 = Pattern.compile("^(\\d+(?:\\.\\d+)*)(.*?)(\\d*)$", Pattern.DOTALL);
    private static final Pattern PATTERN2 = Pattern.compile("^(.*?)(\\d*)$", Pattern.DOTALL);
    private final String baseString;
    @Getter
    private final String[] left;
    @Getter
    protected final BigDecimal[] leftNum;
    @Getter
    private final String middle;
    @Getter
    private final String right;
    @Getter
    protected final BigDecimal rightNum;

    public NatOrdStringImpl(@NonNull String baseString) {
        this.baseString = baseString;
        Matcher matcher = PATTERN1.matcher(baseString);
        if (matcher.matches()) {
            String leftStr = matcher.group(1);
            left = StringUtils.split(leftStr, ".");
            leftNum = new BigDecimal[left.length];
            for (int i = 0; i < left.length; i++) {
                leftNum[i] = new BigDecimal(left[i]);
            }
            middle = matcher.group(2).trim();
            right = matcher.group(3);
            rightNum = StringUtils.isBlank(right) ? BigDecimal.ZERO : new BigDecimal(right);
        } else {
            matcher = PATTERN2.matcher(baseString);
            if (matcher.matches()) {
                left = new String[]{};
                leftNum = new BigDecimal[]{};
                middle = matcher.group(1).trim();
                right = matcher.group(2);
                rightNum = StringUtils.isBlank(right) ? BigDecimal.ZERO : new BigDecimal(right);
            } else {
                left = new String[]{};
                leftNum = new BigDecimal[]{};
                middle = baseString.trim();
                right = "";
                rightNum = BigDecimal.ZERO;
            }
        }
    }

    private static final boolean NULL_IS_LESS = true;

    @Override
    public String getValue() {
        return baseString;
    }

    @Override
    public int compareTo(NatOrdString other) {
        int thisLen = this.leftNum.length;
        int otherLen = other.getLeftNum().length;
        int minLen = Math.min(thisLen, otherLen);
        for (int i = 0; i < minLen; i++) {
            int res = this.leftNum[i].compareTo(other.getLeftNum()[i]);
            if (res != 0) {
                return res;
            }
        }
        if (thisLen != otherLen) {
            return Integer.compare(thisLen, otherLen);
        }
        int res = StringUtils.compare(this.getMiddle(), other.getMiddle(), NULL_IS_LESS);
        if (res != 0) {
            return res;
        }
        return rightNum.compareTo(other.getRightNum());
    }
}
