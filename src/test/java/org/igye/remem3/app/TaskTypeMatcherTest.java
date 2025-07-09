package org.igye.remem3.app;

import org.igye.remem3.app.dto.TaskType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class TaskTypeMatcherTest {
    @Test
    void fromList() {
        TaskTypeMatcher matcher = TaskTypeMatcher.fromList(List.of());
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("ABC")));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("DEF")));

        matcher = TaskTypeMatcher.fromList(List.of("fill_gaps:ABC"));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("ABC")));
        Assertions.assertFalse(matcher.matches(new TaskType.FillGaps("DEF")));

        matcher = TaskTypeMatcher.fromList(List.of("fill_gaps:_"));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("ABC")));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("DEF")));

        matcher = TaskTypeMatcher.fromList(List.of("fill_gaps"));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("ABC")));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("DEF")));

        matcher = TaskTypeMatcher.fromList(List.of("fill_gaps:ABC", "fill_gaps:GHI"));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("ABC")));
        Assertions.assertFalse(matcher.matches(new TaskType.FillGaps("DEF")));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("GHI")));

        matcher = TaskTypeMatcher.fromList(List.of(
            new TaskType.FillGaps("ABC").getCode(),
            new TaskType.FillGaps("GHI").getCode()
        ));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("ABC")));
        Assertions.assertFalse(matcher.matches(new TaskType.FillGaps("DEF")));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("GHI")));
    }
}