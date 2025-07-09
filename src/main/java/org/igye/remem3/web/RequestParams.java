package org.igye.remem3.web;

import java.util.List;

public interface RequestParams {
    boolean hasParam(String paramName);

    String[] getParams(String paramName);

    String getParam(String paramName, String defaultValue);

    String getParam(String paramName);

    boolean hasKeyValueParam(String key);

    List<String> getKeyValueParams(String key);

    String getKeyValueParam(String key, String defaultValue);

    String getKeyValueParam(String key);

    List<Long> getKeyValueParamsLong(String key);

    Long getKeyValueParamLong(String key, Long defaultValue);

    Long getKeyValueParamLong(String key);
}
