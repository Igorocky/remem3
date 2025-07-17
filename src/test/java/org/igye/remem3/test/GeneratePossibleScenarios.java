package org.igye.remem3.test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeneratePossibleScenarios {

    private static final String ANS_X = "0ansX";
    private static final String SHOW_HINT = "1showHint";
    private static final String SHOW_ANS = "2showAns";
    private static final String ANS_V = "3ansV";

    public static void main(String[] args) {
        gen(10, List.of("")).stream()
            .filter(path -> countConsecutiveAnsX(path) <= 1)
            .map(path -> path.stream().collect(Collectors.joining(" ")).trim())
            .distinct()
            .sorted()
            .forEach(System.out::println);
    }

    private static int countConsecutiveAnsX(List<String> path) {
        int cnt = 0;
        int maxCnt = 0;
        for (String elem : path) {
            if (ANS_X.equals(elem)) {
                cnt++;
                maxCnt = Math.max(maxCnt, cnt);
            } else {
                cnt = 0;
            }
        }
        return maxCnt;
    }


    private static List<List<String>> gen(int maxLen, List<String> path) {
        if (path.size() == maxLen || path.contains(ANS_V)) {
            return List.of(path);
        }
        Set<String> prev = new HashSet<>(path);
        String last = path.getLast();
        Set<String> newElems = new HashSet<>();
        newElems.addAll(Set.of(ANS_V, ANS_X, SHOW_HINT, SHOW_ANS));
        if (prev.contains(SHOW_HINT)) {
            newElems.remove(SHOW_HINT);
        }
        if (prev.contains(SHOW_ANS)) {
            newElems.remove(SHOW_HINT);
            newElems.remove(SHOW_ANS);
        }
        return newElems.stream()
            .map(next -> Stream.concat(path.stream(), Stream.of(next)).toList())
            .flatMap(newPath -> gen(maxLen, newPath).stream())
            .toList();
    }
}
