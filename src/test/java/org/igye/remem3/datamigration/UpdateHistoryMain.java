package org.igye.remem3.datamigration;

import lombok.SneakyThrows;
import org.igye.remem3.app.RepeatStrategyType;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.utils.impl.UtilsImpl;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class UpdateHistoryMain {
    private static final Pattern OLD_HIST_PATTERN = Pattern.compile(
        "(\\n)(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z)( +)(\\S+)( +)(\\d+(\\.\\d+)?)( +([^\\n]*))?"
    );
    private final Utils utils;

    public UpdateHistoryMain(Utils utils) {
        this.utils = utils;
    }

    public static void main(String[] args) {
        new UpdateHistoryMain(new UtilsImpl(new ObjectMapper())).run();
    }

    @SneakyThrows
    private void run() {
        getAllCardFiles(new File("...")).forEach(this::updateHistory);
    }

    private String updateHistory(String cardText) {
        StringBuilder sb = new StringBuilder();
        int lastIdx = 0;
        Matcher matcher = OLD_HIST_PATTERN.matcher(cardText);
        int histStartIdx = cardText.indexOf("###hist");
        while (matcher.find()) {
            if (matcher.start() <= histStartIdx) {
                continue;
            }
            sb.append(cardText, lastIdx, matcher.start());
            sb.append(matcher.group(1)).append(matcher.group(2));
            sb.append(" ").append(RepeatStrategyType.CIRCLE);
            sb.append(matcher.group(3)).append(matcher.group(4));
            sb.append(matcher.group(5)).append(matcher.group(6));
            String lastGrp = matcher.group(8);
            if (lastGrp != null) {
                sb.append(lastGrp);
            }
            lastIdx = matcher.end();
        }
        sb.append(cardText, lastIdx, cardText.length());
        return sb.toString();
    }

    private void updateHistory(File cardFile) {
        String cardText = utils.readStringFromFile(cardFile);
        utils.writeStringToFile(updateHistory(cardText), cardFile);
    }

    @SneakyThrows
    private List<File> getAllCardFiles(File dir) {
        try (Stream<Path> stream = Files.walk(dir.toPath())) {
            return stream
                .map(Path::toFile)
                .filter(File::isFile)
                .filter(file -> file.getName().endsWith(".card"))
                .toList();
        }
    }

}
