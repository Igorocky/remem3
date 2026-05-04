package org.igye.remem3.app.controllers.components.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.controllers.ParamName;
import org.igye.remem3.app.controllers.PropName;
import org.igye.remem3.app.dto.RepeatStrategyType;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.repeatstrategy.impl.RepeatStrategyCircle;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.web.RequestParams;

import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.igye.remem3.app.repeatstrategy.impl.RepeatStrategyCircle.DEFAULT_RND_FACTOR;
import static org.igye.remem3.app.repeatstrategy.impl.RepeatStrategyCircle.MAX_NUM_OF_ROUNDS;

public class CircleStrategyCmpImpl extends BaseStrategyCmpImpl {
    private static final String PAR_NUMBER_OF_ROUNDS = "NUMBER_OF_ROUNDS";
    private static final String PAR_RANDOMNESS_FACTOR = "RANDOMNESS_FACTOR";
    private static final String PAR_START_TIME = "START_TIME";
    private static final PropName PROP_ROUNDS = new PropName("rounds");
    private static final PropName PROP_RANDOMNESS = new PropName("randomness");
    private static final PropName PROP_START_TIME = new PropName("start_time");

    private final Utils utils;

    private final ParamName parNumOfRounds;
    private Optional<Integer> valNumOfRounds;

    private final ParamName parRndFactor;
    private BigDecimal valRndFactor;

    private final ParamName parStartTime;
    private Optional<Instant> valStartTime;

    public CircleStrategyCmpImpl(Utils utils, Cache cache, String baseParamName, boolean isReadonly) {
        super(cache, baseParamName + "__CIRCLE", isReadonly);
        this.utils = utils;
        this.parRndFactor = makeParamName(PAR_RANDOMNESS_FACTOR);
        this.parNumOfRounds = makeParamName(PAR_NUMBER_OF_ROUNDS);
        this.parStartTime = makeParamName(PAR_START_TIME);
    }

    public CircleStrategyCmpImpl(Utils utils, Cache cache, String baseParamName, RequestParams params) {
        this(utils, cache, baseParamName, false);
        valNumOfRounds = readCachableParam(parNumOfRounds, params, this::parseNumOfRounds, Optional::empty);
        valRndFactor = readCachableParam(parRndFactor, params, this::parseRndFactor, () -> DEFAULT_RND_FACTOR);
        valStartTime = readCachableParam(parStartTime, params, this::parseStartTime, Optional::empty);
    }

    public CircleStrategyCmpImpl(Utils utils, Cache cache, String baseParamName, File configFile, Properties props) {
        this(utils, cache, baseParamName, true);
        valNumOfRounds = readProp(PROP_ROUNDS, configFile, props, this::parseNumOfRounds, Optional::empty);
        valRndFactor = readProp(PROP_RANDOMNESS, configFile, props, this::parseRndFactor, () -> DEFAULT_RND_FACTOR);
        valStartTime = readProp(PROP_START_TIME, configFile, props, this::parseStartTime, Optional::empty);
    }

    @Override
    public RepeatStrategyType getStrategyType() {
        return RepeatStrategyType.CIRCLE;
    }

    @Override
    public HtmlElem render() {
        HtmlTag roundsInput = inpText(parNumOfRounds.name(), valNumOfRounds.map(String::valueOf).orElse(""), null);
        if (isReadonly) {
            roundsInput.disabled();
        }
        HtmlTag randomnessInput = inpText(parRndFactor.name(), String.valueOf(valRndFactor), null);
        if (isReadonly) {
            randomnessInput.disabled();
        }
        HtmlTag startTimeInput = inpText(parStartTime.name(), valStartTime.map(String::valueOf).orElse(""), null);
        if (isReadonly) {
            startTimeInput.disabled();
        }
        return table(List.of(
            List.of(text("Rounds"), roundsInput),
            List.of(text("Randomness"), randomnessInput),
            List.of(text("Start time"), startTimeInput)
        ));
    }

    @Override
    public RepeatStrategy makeRepeatStrategy(List<Task> tasks) {
        return new RepeatStrategyCircle(
            utils, makeTasksForStrategy(tasks),
            valStartTime.orElse(Instant.now()), valRndFactor.doubleValue(), valNumOfRounds
        );
    }

    @Override
    protected List<Pair<PropName, String>> getPropertiesPriv() {
        return List.of(
            Pair.of(PROP_ROUNDS, valNumOfRounds.map(String::valueOf).orElse("")),
            Pair.of(PROP_RANDOMNESS, String.valueOf(valRndFactor)),
            Pair.of(PROP_START_TIME, valStartTime.map(String::valueOf).orElse(""))
        );
    }

    @Override
    protected List<Pair<ParamName, String>> getParamsToCache() {
        return List.of(
            Pair.of(parNumOfRounds, valNumOfRounds.map(String::valueOf).orElse("")),
            Pair.of(parRndFactor, String.valueOf(valRndFactor)),
            Pair.of(parStartTime, valStartTime.map(String::valueOf).orElse(""))
        );
    }

    private Optional<Integer> parseNumOfRounds(String intStr) {
        if (StringUtils.isBlank(intStr)) {
            return Optional.empty();
        }
        return Optional.of(utils.getInRange(1, Integer.parseInt(intStr), MAX_NUM_OF_ROUNDS));
    }

    private Optional<Instant> parseStartTime(String timeStr) {
        if (StringUtils.isBlank(timeStr)) {
            return Optional.empty();
        }
        return Optional.of(Instant.parse(timeStr));
    }

    private BigDecimal parseRndFactor(String str) {
        BigDecimal res = new BigDecimal(str);
        if (res.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (BigDecimal.ONE.compareTo(res) < 0) {
            return BigDecimal.ONE;
        }
        return res;
    }
}
