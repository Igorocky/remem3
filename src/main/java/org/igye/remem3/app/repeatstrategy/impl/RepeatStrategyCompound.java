package org.igye.remem3.app.repeatstrategy.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.repeatstrategy.Task;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.Exn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class RepeatStrategyCompound extends HtmlBuilder implements RepeatStrategy {
    private final List<RepeatStrategy> childStrategies;
    private final List<Integer> sharesInt;
    private final List<Pair<Double, Double>> ranges;
    private final ArrayList<Optional<ArrayList<Task>>> selectedTasks;
    private final Random rnd;
    private final boolean hasDailyUniqueCount;
    private Optional<Integer> lastStrategyIdx = Optional.empty();

    public RepeatStrategyCompound(List<Pair<Integer, RepeatStrategy>> strategies) {
        this.childStrategies = strategies.stream().map(Pair::getRight).toList();
        this.sharesInt = strategies.stream().map(Pair::getLeft).toList();
        sharesInt.forEach(share -> {
            if (share < 0) {
                throw new Exn("Shares must be non-negative, but got %s.".formatted(share));
            }
        });
        double sum = strategies.stream().map(Pair::getLeft).reduce(0, Integer::sum);
        if (sum < 1) {
            throw new Exn("At least one share must be positive");
        }
        List<Double> shares = sharesInt.stream().map(i -> i / sum).toList();
        double prevSum = 0;
        List<Pair<Double, Double>> ranges = new ArrayList<>();
        for (int i = 0; i < shares.size(); i++) {
            Double share = shares.get(i);
            ranges.add(Pair.of(prevSum, prevSum + share));
            prevSum += share;
        }
        this.ranges = Collections.unmodifiableList(ranges);
        this.selectedTasks = childStrategies.stream()
            .map(RepeatStrategy::getNextTasks)
            .map(opt -> opt.map(ArrayList::new))
            .collect(Collectors.toCollection(ArrayList::new));
        this.rnd = new Random();
        this.hasDailyUniqueCount = childStrategies.stream().anyMatch(RepeatStrategy::hasDailyUniqueCount);
    }

    @Override
    public Optional<List<Task>> getNextTasks() {
        int checkedCnt = 0;
        while (true) {
            if (checkedCnt == childStrategies.size()) {
                lastStrategyIdx = Optional.empty();
                if (selectedTasks.stream().allMatch(Optional::isEmpty)) {
                    return Optional.empty();
                }
                return Optional.of(List.of());
            } else {
                checkedCnt++;
                double rndDbl = rnd.nextDouble();
                int strategyIdx = 0;
                for (int i = 0; i < ranges.size(); i++) {
                    Pair<Double, Double> range = ranges.get(i);
                    if (range.getLeft() <= rndDbl && rndDbl < range.getRight()) {
                        strategyIdx = i;
                        break;
                    }
                }
                Optional<ArrayList<Task>> tasksOpt = selectedTasks.get(strategyIdx);
                if (tasksOpt.isEmpty()) {
                    continue;
                }
                List<Task> tasks = tasksOpt.get();
                if (tasks.isEmpty()) {
                    Optional<List<Task>> nextTasks = childStrategies.get(strategyIdx).getNextTasks();
                    if (nextTasks.isEmpty()) {
                        selectedTasks.set(strategyIdx, Optional.empty());
                        continue;
                    }
                    tasks.addAll(nextTasks.get());
                }
                if (!tasks.isEmpty()) {
                    lastStrategyIdx = Optional.of(strategyIdx);
                    return Optional.of(List.of(tasks.removeFirst()));
                }
            }
        }
    }

    @Override
    public HtmlElem renderLessParams(boolean historyUpdated) {
        return null;
    }

    @Override
    public HtmlElem renderMoreParams(boolean historyUpdated) {
        List<List<HtmlElem>> rows = new ArrayList<>();
        for (int i = 0; i < childStrategies.size(); i++) {
            int finalI = i;
            rows.add(List.of(
                text(sharesInt.get(i)),
                childStrategies.get(i).renderMoreParams(
                    lastStrategyIdx.map(idx -> idx == finalI && historyUpdated).orElse(false)
                )
            ));
        }
        return table(rows).attr("class", "table-single-border");
    }

    @Override
    public boolean hasDailyUniqueCount() {
        return hasDailyUniqueCount;
    }

    @Override
    public Optional<Pair<Long, Long>> getDailyUniqueCount() {
        if (!hasDailyUniqueCount) {
            return Optional.empty();
        }
        Pair<Long, Long> res = childStrategies.stream()
            .map(RepeatStrategy::getDailyUniqueCount)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .reduce(Pair.of(0L, 0L), (a, b) -> Pair.of(a.getLeft() + b.getLeft(), a.getRight() + b.getRight()));
        return Optional.of(res);
    }
}
