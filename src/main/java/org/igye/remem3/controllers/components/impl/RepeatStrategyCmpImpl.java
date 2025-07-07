package org.igye.remem3.controllers.components.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.RepeatStrategyType;
import org.igye.remem3.controllers.components.RepeatStrategyCmp;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.web.RequestParams;

import java.util.Arrays;
import java.util.List;

public class RepeatStrategyCmpImpl extends HtmlBuilder implements RepeatStrategyCmp {

    private final String baseParamName;

    private final String parStrategyType;
    private RepeatStrategyType valStrategyType;

    private final String parCircleRndFactor;
    private double valCircleRndFactor;

    public RepeatStrategyCmpImpl(String baseParamName, RequestParams params) {
        this.baseParamName = baseParamName;
        parStrategyType = makeParamName("TYPE");
        parCircleRndFactor = makeParamName("CIRCLE_RANDOMNESS_FACTOR");

        valStrategyType = params.hasParam(parStrategyType)
            ? RepeatStrategyType.valueOf(params.getParam(parStrategyType))
            : RepeatStrategyType.CIRCLE;

        switch (valStrategyType) {
            case CIRCLE -> {
                valCircleRndFactor = params.hasParam(parCircleRndFactor)
                    ? parseCircleRandomnessFactor(params.getParam(parCircleRndFactor))
                    : 0.3;
            }
        }
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

    private HtmlElem rndCircleParams() {
        return table(List.of(
            List.of(
                text("Randomness factor"),
                inpText(parCircleRndFactor, String.valueOf(valCircleRndFactor), null)
            )
        ));
    }

    private double parseCircleRandomnessFactor(String rndFactor) {
        try {
            return Math.max(0, Math.min(Double.parseDouble(rndFactor), 1));
        } catch (NumberFormatException e) {
            return 0.3;
        }
    }

    private String makeParamName(String suffix) {
        return baseParamName + "__" + suffix;
    }
}
