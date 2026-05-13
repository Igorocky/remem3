package org.igye.remem3.utils.impl;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.utils.NatOrdPath;
import org.igye.remem3.utils.NatOrdString;

import java.io.File;
import java.util.Arrays;

public class NatOrdPathImpl implements NatOrdPath {
    private final File file;
    private final NatOrdString[] parts;

    @SneakyThrows
    public NatOrdPathImpl(File file) {
        this.file = file;
        String[] partsStr = StringUtils.split(file.toPath().normalize().toString(), File.separatorChar);
        NatOrdString[] filteredParts = new NatOrdString[partsStr.length];
        int cnt = 0;
        for (int i = 0; i < partsStr.length; i++) {
            String part = partsStr[i];
            if (StringUtils.isNotBlank(part)) {
                filteredParts[cnt++] = new NatOrdStringImpl(part.trim());
            }
        }
        parts = Arrays.copyOf(filteredParts, cnt);
    }

    @Override
    public NatOrdString[] getParts() {
        return parts;
    }

    @Override
    public File getValue() {
        return file;
    }

    @Override
    public int compareTo(NatOrdPath other) {
        int thisLen = this.parts.length;
        int otherLen = other.getParts().length;
        int minLen = Math.min(thisLen, otherLen);
        for (int i = 0; i < minLen; i++) {
            int res = this.parts[i].compareTo(other.getParts()[i]);
            if (res != 0) {
                return res;
            }
        }
        return Integer.compare(thisLen, otherLen);
    }
}
