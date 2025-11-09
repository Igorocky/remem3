package org.igye.remem3.app.controllers.components.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.repeatstrategy.Task;
import org.igye.remem3.app.repeatstrategy.impl.TaskImpl;
import org.igye.remem3.app.controllers.ParamName;
import org.igye.remem3.app.controllers.PropName;
import org.igye.remem3.app.controllers.components.RepeatStrategyCmp;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Func;
import org.igye.remem3.web.RequestParams;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import static java.lang.String.format;

public abstract class BaseStrategyCmpImpl extends HtmlBuilder implements RepeatStrategyCmp {

    private final Cache cache;
    private final String baseParamName;
    protected boolean isReadonly;


    public BaseStrategyCmpImpl(Cache cache, String baseParamName, boolean isReadonly) {
        this.cache = cache;
        this.baseParamName = baseParamName;
        this.isReadonly = isReadonly;
    }

    @Override
    public List<Pair<String, String>> getProperties() {
        return getPropertiesPriv().stream()
            .map(pair -> Pair.of(pair.getLeft().name(), pair.getRight()))
            .toList();
    }

    @Override
    public void cacheState() {
        getParamsToCache().forEach(pair -> cache.put(pair.getLeft().name(), pair.getRight()));
    }

    @Override
    public void setIsReadonly(boolean isReadonly) {
        this.isReadonly = isReadonly;
    }

    protected abstract List<Pair<PropName, String>> getPropertiesPriv();

    protected abstract List<Pair<ParamName, String>> getParamsToCache();

    protected ParamName makeParamName(String suffix) {
        return new ParamName(baseParamName + "__" + suffix);
    }

    protected <T> T readCachableParam(
        ParamName parName,
        RequestParams params,
        Func<String, T> parser,
        Supplier<T> defVal
    ) {
        return readParam(parName, params, cache, parser, defVal);
    }

    protected <T> T readParam(
        ParamName parName,
        RequestParams params,
        Func<String, T> parser,
        Supplier<T> defVal
    ) {
        return readParam(parName, params, null, parser, defVal);
    }

    protected <T> T readProp(
        PropName propName, File file, Properties props, Func<String, T> parser, Supplier<T> defVal
    ) {
        String valStr = props.getProperty(propName.name());
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

    protected List<Task> makeTasksForStrategy(List<org.igye.remem3.app.dto.Task> tasks) {
        return tasks.stream()
            .map(t -> new TaskImpl(t, Instant.MIN, getStrategyType()))
            .map(Task.class::cast)
            .toList();
    }

    private <T> T readParam(
        ParamName parName,
        RequestParams params,
        Cache cache,
        Func<String, T> parser,
        Supplier<T> defVal
    ) {
        try {
            String name = parName.name();
            String valStr = params.hasParam(name)
                ? params.getParam(name)
                : cache == null ? null : cache.getStr(name, null);
            return valStr == null ? defVal.get() : parser.apply(valStr);
        } catch (Exception e) {
            return defVal.get();
        }
    }
}
