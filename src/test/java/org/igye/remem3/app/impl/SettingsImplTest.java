package org.igye.remem3.app.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SettingsImplTest {

    @Test
    void removeExtension() {
        Assertions.assertEquals("abc", SettingsImpl.removeExtension("abc"));
        Assertions.assertEquals("abc", SettingsImpl.removeExtension("abc."));
        Assertions.assertEquals("abc", SettingsImpl.removeExtension("abc.t"));
        Assertions.assertEquals("abc", SettingsImpl.removeExtension("abc.txt"));
        Assertions.assertEquals("", SettingsImpl.removeExtension(".txt"));
        Assertions.assertEquals("abc.def", SettingsImpl.removeExtension("abc.def.txt"));
    }
}