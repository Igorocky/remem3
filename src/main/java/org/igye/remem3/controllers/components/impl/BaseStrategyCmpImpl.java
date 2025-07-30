package org.igye.remem3.controllers.components.impl;

import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.controllers.components.RepeatStrategyCmp;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Func;
import org.igye.remem3.web.RequestParams;

import java.io.File;
import java.util.Properties;
import java.util.function.Supplier;

import static java.lang.String.format;

public abstract class BaseStrategyCmpImpl extends HtmlBuilder implements RepeatStrategyCmp {

    private final String baseParamName;
    protected final boolean isReadonly;


    public BaseStrategyCmpImpl(String baseParamName, boolean isReadonly) {
        this.baseParamName = baseParamName;
        this.isReadonly = isReadonly;
    }

    protected String makeParamName(String suffix) {
        return baseParamName + "__" + suffix;
    }

    protected <T> T readParam(RequestParams params, String parName, Func<String, T> parser, Supplier<T> defVal) {
        try {
            return params.hasParam(parName) ? parser.apply(params.getParam(parName)) : defVal.get();
        } catch (Exception e) {
            return defVal.get();
        }
    }

    protected <T> T readPropExn(
        File file, Properties props, String propName, Func<String, T> parser, Supplier<T> defVal
    ) {
        String valStr = props.getProperty(propName);
        if (StringUtils.isBlank(valStr)) {
            if (defVal == null) {
                throw new Exn(format("Missing required property %s in %s.", propName, file.getAbsolutePath()));
            }
            return defVal.get();
        }
        try {
            return parser.apply(valStr);
        } catch (Exception e) {
            throw new Exn(format(
                "Cannot parse %s=%s in %s, got an error %s.",
                propName,
                valStr,
                file.getAbsolutePath(),
                e.getMessage()
            ), e);
        }
    }
}
