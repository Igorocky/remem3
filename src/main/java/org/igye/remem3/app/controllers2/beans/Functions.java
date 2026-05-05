package org.igye.remem3.app.controllers2.beans;

import org.apache.commons.lang3.StringUtils;

public class Functions {
    public static String reverse(String str) {
        return StringUtils.reverse(str);
    }

    public static String repeat(String str, int cnt) {
        return StringUtils.repeat(str, cnt);
    }
}
