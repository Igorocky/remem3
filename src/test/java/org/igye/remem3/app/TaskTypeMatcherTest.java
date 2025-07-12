package org.igye.remem3.app;

import org.igye.remem3.app.dto.TaskType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class TaskTypeMatcherTest {
    @Test
    void fillGaps() {
        TaskTypeMatcher matcher = TaskTypeMatcher.fromList(Set.of());
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("ABC")));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("DEF")));

        matcher = TaskTypeMatcher.fromList(Set.of("fill_gaps:ABC"));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("ABC")));
        Assertions.assertFalse(matcher.matches(new TaskType.FillGaps("DEF")));

        matcher = TaskTypeMatcher.fromList(Set.of("fill_gaps:_"));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("ABC")));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("DEF")));

        matcher = TaskTypeMatcher.fromList(Set.of("fill_gaps"));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("ABC")));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("DEF")));

        matcher = TaskTypeMatcher.fromList(Set.of("fill_gaps:ABC", "fill_gaps:GHI"));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("ABC")));
        Assertions.assertFalse(matcher.matches(new TaskType.FillGaps("DEF")));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("GHI")));

        matcher = TaskTypeMatcher.fromList(Set.of(
            new TaskType.FillGaps("ABC").getCode(),
            new TaskType.FillGaps("GHI").getCode()
        ));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("ABC")));
        Assertions.assertFalse(matcher.matches(new TaskType.FillGaps("DEF")));
        Assertions.assertTrue(matcher.matches(new TaskType.FillGaps("GHI")));
    }

    @Test
    void translate() {
        TaskTypeMatcher matcher = TaskTypeMatcher.fromList(Set.of());
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("ABC", "DEF")));
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("DEF", "ABC")));

        matcher = TaskTypeMatcher.fromList(Set.of("translate:ABC->DEF"));
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("ABC", "DEF")));
        Assertions.assertFalse(matcher.matches(new TaskType.Translate("DEF", "ABC")));

        matcher = TaskTypeMatcher.fromList(Set.of("translate:ABC->_"));
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("ABC", "DEF")));
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("ABC", "GHI")));
        Assertions.assertFalse(matcher.matches(new TaskType.Translate("DEF", "ABC")));
        Assertions.assertFalse(matcher.matches(new TaskType.Translate("GHI", "ABC")));

        matcher = TaskTypeMatcher.fromList(Set.of("translate:_->DEF"));
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("ABC", "DEF")));
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("GHI", "DEF")));
        Assertions.assertFalse(matcher.matches(new TaskType.Translate("DEF", "ABC")));
        Assertions.assertFalse(matcher.matches(new TaskType.Translate("GHI", "ABC")));

        matcher = TaskTypeMatcher.fromList(Set.of("translate:_->_"));
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("ABC", "DEF")));
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("GHI", "DEF")));
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("DEF", "ABC")));
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("GHI", "ABC")));

        matcher = TaskTypeMatcher.fromList(Set.of("translate"));
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("ABC", "DEF")));
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("GHI", "DEF")));
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("DEF", "ABC")));
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("GHI", "ABC")));

        matcher = TaskTypeMatcher.fromList(Set.of(
            new TaskType.Translate("ABC", "DEF").getCode(),
            new TaskType.Translate("DEF", "GHI").getCode()
        ));
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("ABC", "DEF")));
        Assertions.assertTrue(matcher.matches(new TaskType.Translate("DEF", "GHI")));
        Assertions.assertFalse(matcher.matches(new TaskType.Translate("ABC", "GHI")));
    }
}