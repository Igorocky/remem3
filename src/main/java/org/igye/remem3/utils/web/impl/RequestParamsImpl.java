package org.igye.remem3.utils.web.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.web.RequestParams;

import java.util.HashMap;
import java.util.Map;

public class RequestParamsImpl implements RequestParams {
    private final Map<String, String[]> params;
    private final Map<String, String> submitIdParams;

    public RequestParamsImpl(HttpServletRequest req) {
        params = new HashMap<>();
        submitIdParams = new HashMap<>();
        req.getParameterMap().forEach((param, value) -> {
            params.put(param, value);
            if (param.contains(":")) {
                String[] nameAndId = param.split(":");
                String prevVal = submitIdParams.put(nameAndId[0], nameAndId[1]);
                if (prevVal != null) {
                    throw new Exn(String.format("Cannot reassign value for id param %s", nameAndId[0]));
                }
            }
        });
    }

    @Override
    public boolean hasParam(String paramName) {
        String[] values = params.get(paramName);
        return values != null && values.length > 0;
    }

    @Override
    public String getParam(String paramName) {
        String[] values = params.get(paramName);
        if (values == null || values.length == 0) {
            return null;
        }
        return values[0];
    }

    @Override
    public boolean hasSubmitIdParam(String paramName) {
        return submitIdParams.containsKey(paramName);
    }

    @Override
    public String getSubmitIdParam(String paramName) {
        return submitIdParams.get(paramName);
    }

    @Override
    public Long getSubmitIdParamLong(String paramName) {
        String strVal = getSubmitIdParam(paramName);
        if (strVal == null) {
            return null;
        }
        return Long.parseLong(strVal);
    }
}
