package org.igye.remem3.controllers.components.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.RepeatStrategy;
import org.igye.remem3.app.RepeatStrategyType;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.impl.RepeatStrategyCircle;
import org.igye.remem3.controllers.components.RepeatStrategyCmp;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.web.RequestParams;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.igye.remem3.app.impl.RepeatStrategyCircle.MAX_NUM_OF_ROUNDS;

public class RepeatStrategyCmpImpl extends HtmlBuilder implements RepeatStrategyCmp {

    private final String baseParamName;

    private final String parStrategyType;
    private RepeatStrategyType valStrategyType;

    private final String parCircleRndFactor;
    private double valCircleRndFactor;

    private final String parCircleNumOfRounds;
    private Optional<Integer> valCircleNumOfRounds;

    public RepeatStrategyCmpImpl(String baseParamName, RequestParams params) {
        this.baseParamName = baseParamName;
        parStrategyType = makeParamName("TYPE");
        parCircleRndFactor = makeParamName("CIRCLE_RANDOMNESS_FACTOR");
        parCircleNumOfRounds = makeParamName("CIRCLE_NUMBER_OF_ROUNDS");

        valStrategyType = params.hasParam(parStrategyType)
            ? RepeatStrategyType.valueOf(params.getParam(parStrategyType))
            : RepeatStrategyType.CIRCLE;

        switch (valStrategyType) {
            case CIRCLE -> {
                valCircleRndFactor = params.hasParam(parCircleRndFactor)
                    ? parseCircleRandomnessFactor(params.getParam(parCircleRndFactor))
                    : 0.3;
                valCircleNumOfRounds = params.hasParam(parCircleNumOfRounds)
                    ? parseCircleNumOfRounds(params.getParam(parCircleNumOfRounds))
                    : Optional.empty();
            }
        }
    }

    @Override
    public RepeatStrategyType getStrategyType() {
        return valStrategyType;
    }

    @Override
    public HtmlElem render() {
        return frag(
            table(List.of(List.of(
                text("Repeat strategy"),
                select(
                    parStrategyType,
                    true,
                    valStrategyType.toString(),
                    Arrays.stream(RepeatStrategyType.values())
                        .map(RepeatStrategyType::toString)
                        .map(typ -> Pair.of(typ, text(typ)))
                        .toList()
                )
            ))),
            switch (valStrategyType) {
                case CIRCLE -> rndCircleParams();
            }
        );
    }

    @Override
    public RepeatStrategy makeRepeatStrategy(List<Task> tasks) {
        return switch (valStrategyType) {
            case CIRCLE -> new RepeatStrategyCircle(tasks, Instant.now(), valCircleRndFactor, valCircleNumOfRounds);
        };
    }

    private HtmlElem rndCircleParams() {
        return table(List.of(
            List.of(
                text("Randomness"),
                inpText(parCircleRndFactor, String.valueOf(valCircleRndFactor), null)
            ),
            List.of(
                text("Rounds"),
                inpText(parCircleNumOfRounds, valCircleNumOfRounds.map(String::valueOf).orElse(""), null)
            )
        ));
    }

    private Optional<Integer> parseCircleNumOfRounds(String intStr) {
        try {
            return Optional.of(Math.max(1, Math.min(Integer.parseInt(intStr), MAX_NUM_OF_ROUNDS)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private double parseCircleRandomnessFactor(String doubleStr) {
        try {
            return Math.max(0, Math.min(Double.parseDouble(doubleStr), 1));
        } catch (NumberFormatException e) {
            return 0.3;
        }
    }

    private String makeParamName(String suffix) {
        return baseParamName + "__" + suffix;
    }
}
