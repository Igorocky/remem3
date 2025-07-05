package org.igye.remem3.utils.web.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.web.RequestParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestParamsImpl implements RequestParams {
    private final Map<String, String[]> params;
    private final Map<String, List<String>> keyValueParams;

    public RequestParamsImpl(HttpServletRequest req) {
        params = new HashMap<>();
        keyValueParams = new HashMap<>();
        req.getParameterMap().forEach((param, value) -> {
            params.put(param, value);
            if (param.contains(":")) {
                String[] keyValue = param.split(":");
                keyValueParams.computeIfAbsent(keyValue[0], _ -> new ArrayList<>()).add(keyValue[1]);
            }
        });
    }

    @Override
    public boolean hasParam(String paramName) {
        String[] values = params.get(paramName);
        return values != null && values.length > 0;
    }

    @Override
    public String[] getParams(String paramName) {
        String[] values = params.get(paramName);
        if (values == null || values.length == 0) {
            throw new Exn(String.format("Param '%s' is not present.", paramName));
        }
        return values;
    }

    @Override
    public String getParam(String paramName) {
        return getParams(paramName)[0];
    }

    @Override
    public boolean hasKeyValueParam(String key) {
        List<String> values = keyValueParams.get(key);
        return CollectionUtils.isNotEmpty(values);
    }

    @Override
    public List<String> getKeyValueParams(String key) {
        List<String> values = keyValueParams.get(key);
        if (CollectionUtils.isEmpty(values)) {
            throw new Exn(String.format("Key-value param '%s' is not present.", key));
        }
        return values;
    }

    @Override
    public String getKeyValueParam(String key) {
        return getKeyValueParams(key).getFirst();
    }

    @Override
    public List<Long> getKeyValueParamsLong(String key) {
        return getKeyValueParams(key).stream().map(Long::parseLong).toList();
    }

    @Override
    public Long getKeyValueParamLong(String key) {
        return Long.parseLong(getKeyValueParam(key));
    }
}
