package org.igye.remem3.web.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.web.RequestParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RequestParamsImpl implements RequestParams {
    private final Map<String, String[]> params;
    private final Map<String, List<String>> keyValueParams;

    public RequestParamsImpl(HttpServletRequest req) {
        params = new HashMap<>();
        keyValueParams = new HashMap<>();
        req.getParameterMap().forEach((param, value) -> {
            params.put(param, value);
            int colonIdx = param.indexOf(':');
            if (colonIdx > 0) {
                keyValueParams.computeIfAbsent(
                    param.substring(0, colonIdx),
                    _ -> new ArrayList<>()
                ).add(param.substring(colonIdx + 1));
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
        if (values == null) {
            return new String[]{};
        }
        return values;
    }

    @Override
    public String getParam(String paramName, String defaultValue) {
        if (!hasParam(paramName)) {
            return defaultValue;
        }
        String[] params = getParams(paramName);
        if (params.length == 0) {
            return defaultValue;
        }
        return params[0];
    }

    @Override
    public String getParam(String paramName) {
        String res = getParam(paramName, null);
        if (res == null) {
            throw new Exn(String.format("Param '%s' is not present.", paramName));
        }
        return res;
    }

    @Override
    public Optional<String> getParamOpt(String paramName) {
        return Optional.ofNullable(getParam(paramName, null));
    }

    @Override
    public boolean hasKeyValueParam(String key) {
        List<String> values = keyValueParams.get(key);
        return CollectionUtils.isNotEmpty(values);
    }

    @Override
    public List<String> getKeyValueParams(String key) {
        List<String> values = keyValueParams.get(key);
        if (values == null) {
            return List.of();
        }
        return values;
    }

    @Override
    public String getKeyValueParam(String key, String defaultValue) {
        if (!hasKeyValueParam(key)) {
            return defaultValue;
        }
        List<String> values = getKeyValueParams(key);
        if (CollectionUtils.isEmpty(values)) {
            return defaultValue;
        }
        return values.getFirst();
    }

    @Override
    public String getKeyValueParam(String key) {
        String value = getKeyValueParam(key, null);
        if (value == null) {
            throw new Exn(String.format("Key-value param '%s' is not present.", key));
        }
        return value;
    }

    @Override
    public List<Long> getKeyValueParamsLong(String key) {
        return getKeyValueParams(key).stream().map(Long::parseLong).toList();
    }

    @Override
    public Long getKeyValueParamLong(String key, Long defaultValue) {
        String value = getKeyValueParam(key, null);
        if (value == null) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    @Override
    public Long getKeyValueParamLong(String key) {
        return Long.parseLong(getKeyValueParam(key));
    }
}
