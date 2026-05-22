package org.igye.remem3.app.components.impl;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.components.DirSelectorCmp;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.impl.NatOrdStringImpl;
import org.igye.remem3.web.RequestParams;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DirSelectorCmpImpl extends HtmlBuilder implements DirSelectorCmp {
    private final Settings settings;
    private final Cache cache;
    private final String baseParamName;
    private final boolean isReadonly;
    private List<String> selectedDirectoryList;

    private DirSelectorCmpImpl(Settings settings, Cache cache, String baseParamName, boolean isReadonly) {
        this.settings = settings;
        this.cache = cache;
        this.baseParamName = baseParamName;
        this.isReadonly = isReadonly;
    }

    public DirSelectorCmpImpl(
        Settings settings,
        Cache cache,
        String baseParamName,
        boolean isReadonly,
        RequestParams params
    ) {
        this(settings, cache, baseParamName, isReadonly);
        this.selectedDirectoryList = getSelectedDirectoryList(params);
    }

    public DirSelectorCmpImpl(
        Settings settings,
        Cache cache,
        String baseParamName,
        boolean isReadonly,
        File dir
    ) {
        this(settings, cache, baseParamName, isReadonly);
        this.selectedDirectoryList = getSelectedDirectoryList(dir);
    }

    @SneakyThrows
    @Override
    public String getSelectedDirectoryStr() {
        return new File(StringUtils.join(selectedDirectoryList, '/')).getCanonicalPath();
    }

    @Override
    public File getSelectedDirectory() {
        return new File(getSelectedDirectoryStr());
    }

    @Override
    public HtmlElem render() {
        if (isReadonly) {
            return text(getSelectedDirectoryStr());
        }
        List<HtmlElem> selectors = new ArrayList<>();
        String parentPath = "";
        for (int i = 0; i < selectedDirectoryList.size(); i++) {
            String curPathPart = selectedDirectoryList.get(i);
            List<String> options;
            if (i == 0) {
                options = settings.getDirectoriesWithCards();
            } else {
                File[] subDirs = new File(parentPath).listFiles(File::isDirectory);
                List<String> subDirNames = subDirs == null
                    ? List.of()
                    : Arrays.stream(subDirs)
                      .filter(dir -> !dir.getName().startsWith("."))
                      .map(File::getName)
                      .map(NatOrdStringImpl::new)
                      .sorted()
                      .map(NatOrdStringImpl::getValue)
                      .toList();
                options = new ArrayList<>();
                options.add(".");
                options.addAll(subDirNames);
            }
            if (i > 0) {
                selectors.add(text("/"));
            }
            selectors.add(rndDirSelector(keyValueParam(baseParamName, i), options, curPathPart));
            parentPath += (i == 0 ? "" : "/") + curPathPart;
        }
        return frag(selectors);
    }

    private List<String> getSelectedDirectoryList(RequestParams params) {
        List<String> res;
        if (params.hasParam(keyValueParam(baseParamName, 0))) {
            res = new ArrayList<>();
            int i = 0;
            while (params.hasParam(keyValueParam(baseParamName, i))) {
                String curPathPart = params.getParam(keyValueParam(baseParamName, i++));
                res.add(curPathPart);
                if (".".equals(curPathPart)) {
                    break;
                }
            }
        } else {
            String cachedDir = cache.getStr(baseParamName, getDefaultDir(settings));
            res = getSelectedDirectoryList(new File(cachedDir));
        }
        return Collections.unmodifiableList(getValidDirs(res, settings));
    }

    @SneakyThrows
    private List<String> getSelectedDirectoryList(File dir) {
        String dirStr = dir.getCanonicalPath();
        ArrayList<String> res = new ArrayList<>();
        for (String dirFromSettings : settings.getDirectoriesWithCards()) {
            if (dirStr.startsWith(dirFromSettings)) {
                res.add(dirFromSettings);
                Arrays.stream(StringUtils.split(dirStr.substring(dirFromSettings.length()), File.separator))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .forEach(res::add);
                break;
            }
        }
        return Collections.unmodifiableList(getValidDirs(res, settings));
    }

    private List<String> getValidDirs(List<String> pathParts, Settings settings) {
        ArrayList<String> validDirs = new ArrayList<>();
        String curPath = "";
        for (int i = 0; i < pathParts.size(); i++) {
            String curPathPart = pathParts.get(i);
            curPath += (i == 0 ? "" : "/") + curPathPart;
            File curDir = new File(curPath);
            if (curDir.exists() && curDir.isDirectory()) {
                validDirs.add(curPathPart);
            } else {
                break;
            }
        }
        if (validDirs.isEmpty()) {
            validDirs.add(getDefaultDir(settings));
        }
        if (!".".equals(validDirs.getLast())) {
            validDirs.add(".");
        }
        return validDirs;
    }

    private String getDefaultDir(Settings settings) {
        return settings.getDirectoriesWithCards().getFirst();
    }

    private HtmlElem rndDirSelector(String paramName, List<String> options, String selected) {
        return select(paramName, selected, options.stream().map(opt -> Pair.of(opt, text(opt))).toList())
            .submitOnChange();
    }
}
