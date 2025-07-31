package org.igye.remem3.controllers.components.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.RepeatStrategyType;
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
    private static final String PAR_RANDOMNESS_FACTOR = "RANDOMNESS_FACTOR";
    private static final String PAR_NUMBER_OF_ROUNDS = "NUMBER_OF_ROUNDS";
    private static final String PROP_ROUNDS = "rounds";
    private static final String PROP_RANDOMNESS = "randomness";

    private final Utils utils;
    private final Cache cache;

    private final String parNumOfRounds;
    private Optional<Integer> valNumOfRounds;

    private final String parRndFactor;
    private BigDecimal valRndFactor;

    public CircleStrategyCmpImpl(Utils utils, Cache cache, String baseParamName, boolean isReadonly) {
        super(baseParamName + "__CIRCLE", isReadonly);
        this.utils = utils;
        this.cache = cache;
        this.parRndFactor = makeParamName(PAR_RANDOMNESS_FACTOR);
        this.parNumOfRounds = makeParamName(PAR_NUMBER_OF_ROUNDS);
    }

    public CircleStrategyCmpImpl(Utils utils, Cache cache, String baseParamName, RequestParams params) {
        this(utils, cache, baseParamName, false);
        valNumOfRounds = utils.try_(() -> parseNumOfRoundsExn(
            readParam(params, parNumOfRounds, str -> str, () -> cache.getStr(parNumOfRounds, ""))
        ));
        valRndFactor = readParam(params, parRndFactor, this::parseRndFactorExn, () -> DEFAULT_RND_FACTOR);
    }

    public CircleStrategyCmpImpl(Utils utils, Cache cache, String baseParamName, File configFile, Properties props) {
        this(utils, cache, baseParamName, true);
        valNumOfRounds = readPropExn(configFile, props, PAR_NUMBER_OF_ROUNDS,
            str -> Optional.of(parseNumOfRoundsExn(str)), Optional::empty
        );
        valRndFactor = readPropExn(configFile, props, PROP_RANDOMNESS, this::parseRndFactorExn,
            () -> DEFAULT_RND_FACTOR
        );
    }

    @Override
    public RepeatStrategyType getStrategyType() {
        return RepeatStrategyType.CIRCLE;
    }

    @Override
    public HtmlElem render() {
        HtmlTag roundsInput = inpText(parNumOfRounds, valNumOfRounds.map(String::valueOf).orElse(""), null);
        if (isReadonly) {
            roundsInput.disabled();
        }
        HtmlTag randomnessInput = inpText(parRndFactor, String.valueOf(valRndFactor), null);
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
    public List<Pair<String, String>> getProperties() {
        return List.of(
            Pair.of(PROP_ROUNDS, valNumOfRounds.map(String::valueOf).orElse("")),
            Pair.of(PROP_RANDOMNESS, String.valueOf(valRndFactor))
        );
    }

    @Override
    public void cacheState() {
        cache.put(parNumOfRounds, valNumOfRounds.map(String::valueOf).orElse(""));
        cache.put(parRndFactor, String.valueOf(valRndFactor));
    }

    private Integer parseNumOfRoundsExn(String intStr) {
        return utils.getInRange(1, Integer.parseInt(intStr), MAX_NUM_OF_ROUNDS);
    }

    private BigDecimal parseRndFactorExn(String str) {
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
