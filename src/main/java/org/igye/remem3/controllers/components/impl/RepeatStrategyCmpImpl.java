package org.igye.remem3.controllers.components.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.RepeatStrategyType;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.controllers.ParamName;
import org.igye.remem3.controllers.PropName;
import org.igye.remem3.controllers.components.RepeatStrategyCmp;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.web.RequestParams;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class RepeatStrategyCmpImpl extends BaseStrategyCmpImpl {

    private static final String PAR_TYPE = "TYPE";
    private static final PropName PROP_REPEAT_STRATEGY = new PropName("repeat_strategy");

    private final ParamName parStrategyType;
    private RepeatStrategyType valStrategyType;

    private RepeatStrategyCmp childCmp;

    public RepeatStrategyCmpImpl(Cache cache, String baseParamName, boolean isReadonly) {
        super(cache, baseParamName, isReadonly);
        this.parStrategyType = makeParamName(PAR_TYPE);
    }

    public RepeatStrategyCmpImpl(
        Settings settings, Utils utils, Cache cache, String baseParamName, RequestParams params
    ) {
        this(cache, baseParamName, false);

        valStrategyType = readCachableParam(parStrategyType, params, RepeatStrategyType::valueOf,
            () -> RepeatStrategyType.CIRCLE
        );

        childCmp = switch (valStrategyType) {
            case CIRCLE -> new CircleStrategyCmpImpl(utils, cache, baseParamName, params);
            case BUCKETS -> new BucketsStrategyCmpImpl(settings, utils, cache, baseParamName, params);
            case QUEUE -> new QueueStrategyCmpImpl(utils, cache, baseParamName, params);
        };
    }

    public RepeatStrategyCmpImpl(
        Settings settings, Utils utils, Cache cache, String baseParamName, File configFile, Properties props
    ) {
        this(cache, baseParamName, true);

        valStrategyType = readProp(PROP_REPEAT_STRATEGY, configFile, props, RepeatStrategyType::valueOf, null);

        childCmp = switch (valStrategyType) {
            case CIRCLE -> new CircleStrategyCmpImpl(utils, cache, baseParamName, configFile, props);
            case BUCKETS -> new BucketsStrategyCmpImpl(settings, utils, cache, baseParamName, configFile, props);
            case QUEUE -> new QueueStrategyCmpImpl(utils, cache, baseParamName, configFile, props);
        };
    }

    @Override
    public RepeatStrategyType getStrategyType() {
        return valStrategyType;
    }

    @Override
    public HtmlElem render() {
        HtmlTag strategySelector = select(
            parStrategyType.name(),
            true,
            valStrategyType.toString(),
            Arrays.stream(RepeatStrategyType.values())
                .map(RepeatStrategyType::toString)
                .map(typ -> Pair.of(typ, text(typ)))
                .toList()
        );
        if (isReadonly) {
            strategySelector.disabled();
        }
        return frag(
            table(List.of(List.of(
                text("Repeat strategy"),
                strategySelector
            ))),
            childCmp.render()
        );
    }

    @Override
    public RepeatStrategy makeRepeatStrategy(List<Task> tasks) {
        return childCmp.makeRepeatStrategy(tasks);
    }

    @Override
    public List<Pair<String, String>> getProperties() {
        ArrayList<Pair<String, String>> props = new ArrayList<>();
        props.add(Pair.of(PROP_REPEAT_STRATEGY.name(), valStrategyType.toString()));
        props.addAll(childCmp.getProperties());
        return props;
    }

    @Override
    public void cacheState() {
        super.cacheState();
        childCmp.cacheState();
    }

    @Override
    protected List<Pair<PropName, String>> getPropertiesPriv() {
        throw new Exn("This method should not be called.");
    }

    @Override
    protected List<Pair<ParamName, String>> getParamsToCache() {
        return List.of(Pair.of(parStrategyType, valStrategyType.toString()));
    }
}
