package org.igye.remem3.utils.web;

public interface RequestParams {
    boolean hasParam(String paramName);

    String getParam(String paramName);

    boolean hasSubmitIdParam(String paramName);

    String getSubmitIdParam(String paramName);

    Long getSubmitIdParamLong(String paramName);
}
