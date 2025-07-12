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

import static org.igye.remem3.app.impl.RepeatStrategyCircle.DEFAULT_MAX_NUM_OF_CIRCLES;

public class RepeatStrategyCmpImpl extends HtmlBuilder implements RepeatStrategyCmp {

    private final String baseParamName;

    private final String parStrategyType;
    private RepeatStrategyType valStrategyType;

    private final String parCircleRndFactor;
    private double valCircleRndFactor;

    private final String parCircleMaxNumOfCircles;
    private int valCircleMaxNumOfCircles;

    public RepeatStrategyCmpImpl(String baseParamName, RequestParams params) {
        this.baseParamName = baseParamName;
        parStrategyType = makeParamName("TYPE");
        parCircleRndFactor = makeParamName("CIRCLE_RANDOMNESS_FACTOR");
        parCircleMaxNumOfCircles = makeParamName("CIRCLE_MAX_NUMBER_OF_CIRCLES");

        valStrategyType = params.hasParam(parStrategyType)
            ? RepeatStrategyType.valueOf(params.getParam(parStrategyType))
            : RepeatStrategyType.CIRCLE;

        switch (valStrategyType) {
            case CIRCLE -> {
                valCircleRndFactor = params.hasParam(parCircleRndFactor)
                    ? parseCircleRandomnessFactor(params.getParam(parCircleRndFactor))
                    : 0.3;
                valCircleMaxNumOfCircles = params.hasParam(parCircleMaxNumOfCircles)
                    ? parseCircleMaxNumOfCircles(params.getParam(parCircleMaxNumOfCircles))
                    : DEFAULT_MAX_NUM_OF_CIRCLES;
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
            case CIRCLE -> new RepeatStrategyCircle(tasks, Instant.now(), valCircleRndFactor, valCircleMaxNumOfCircles);
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
                inpText(parCircleMaxNumOfCircles, String.valueOf(valCircleMaxNumOfCircles), null)
            )
        ));
    }

    private int parseCircleMaxNumOfCircles(String intStr) {
        try {
            return Math.max(1, Math.min(Integer.parseInt(intStr), DEFAULT_MAX_NUM_OF_CIRCLES));
        } catch (NumberFormatException e) {
            return DEFAULT_MAX_NUM_OF_CIRCLES;
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
