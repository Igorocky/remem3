package org.igye.remem3.controllers.components.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.RepeatStrategyType;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.repeatstrategy.impl.RepeatStrategyCircle;
import org.igye.remem3.controllers.ParamName;
import org.igye.remem3.controllers.PropName;
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
    private static final PropName PROP_ROUNDS = new PropName("rounds");
    private static final PropName PROP_RANDOMNESS = new PropName("randomness");

    private final Utils utils;

    private final ParamName parNumOfRounds;
    private Optional<Integer> valNumOfRounds;

    private final ParamName parRndFactor;
    private BigDecimal valRndFactor;

    public CircleStrategyCmpImpl(Utils utils, Cache cache, String baseParamName, boolean isReadonly) {
        super(cache, baseParamName + "__CIRCLE", isReadonly);
        this.utils = utils;
        this.parRndFactor = makeParamName(PAR_RANDOMNESS_FACTOR);
        this.parNumOfRounds = makeParamName(PAR_NUMBER_OF_ROUNDS);
    }

    public CircleStrategyCmpImpl(Utils utils, Cache cache, String baseParamName, RequestParams params) {
        this(utils, cache, baseParamName, false);
        valNumOfRounds = readCachableParam(parNumOfRounds, params, this::parseNumOfRounds, Optional::empty);
        valRndFactor = readCachableParam(parRndFactor, params, this::parseRndFactor, () -> DEFAULT_RND_FACTOR);
    }

    public CircleStrategyCmpImpl(Utils utils, Cache cache, String baseParamName, File configFile, Properties props) {
        this(utils, cache, baseParamName, true);
        valNumOfRounds = readProp(PROP_ROUNDS, configFile, props, this::parseNumOfRounds, Optional::empty);
        valRndFactor = readProp(PROP_RANDOMNESS, configFile, props, this::parseRndFactor, () -> DEFAULT_RND_FACTOR);
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
        return table(List.of(
            List.of(text("Rounds"), roundsInput),
            List.of(text("Randomness"), randomnessInput)
        ));
    }

    @Override
    public RepeatStrategy makeRepeatStrategy(List<Task> tasks) {
        return new RepeatStrategyCircle(
            utils, makeTasksForStrategy(tasks), Instant.now(), valRndFactor.doubleValue(), valNumOfRounds
        );
    }

    @Override
    protected List<Pair<PropName, String>> getPropertiesPriv() {
        return List.of(
            Pair.of(PROP_ROUNDS, valNumOfRounds.map(String::valueOf).orElse("")),
            Pair.of(PROP_RANDOMNESS, String.valueOf(valRndFactor))
        );
    }

    @Override
    protected List<Pair<ParamName, String>> getParamsToCache() {
        return List.of(
            Pair.of(parNumOfRounds, valNumOfRounds.map(String::valueOf).orElse("")),
            Pair.of(parRndFactor, String.valueOf(valRndFactor))
        );
    }

    private Optional<Integer> parseNumOfRounds(String intStr) {
        if (StringUtils.isBlank(intStr)) {
            return Optional.empty();
        }
        return Optional.of(utils.getInRange(1, Integer.parseInt(intStr), MAX_NUM_OF_ROUNDS));
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
