package org.igye.remem3.controllers.components.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.controllers.components.DirSelectorCmp;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
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
    private final List<String> selectedDirectoryList;
    private final boolean isReadonly;

    public DirSelectorCmpImpl(Settings settings, Cache cache, RequestParams params, String baseParamName) {
        this.settings = settings;
        this.cache = cache;
        this.baseParamName = baseParamName;
        this.selectedDirectoryList = getSelectedDirectoryList(params);
        isReadonly = false;
    }

    public DirSelectorCmpImpl(Settings settings, Cache cache, String dirStr, String baseParamName) {
        this.settings = settings;
        this.cache = cache;
        this.baseParamName = baseParamName;
        this.selectedDirectoryList = getSelectedDirectoryList(dirStr);
        isReadonly = false;
    }

    public DirSelectorCmpImpl(Settings settings, Cache cache, File dir, String baseParamName) {
        this.settings = settings;
        this.cache = cache;
        this.baseParamName = baseParamName;
        this.selectedDirectoryList = List.of(dir.getAbsolutePath());
        isReadonly = true;
    }

    @Override
    public List<String> getSelectedDirectoryList() {
        return selectedDirectoryList;
    }

    @Override
    public String getSelectedDirectoryStr() {
        return StringUtils.join(getSelectedDirectoryList().stream().filter(dir -> !dir.startsWith(".")).toList(), '/');
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
            String curDirPart = selectedDirectoryList.get(i);
            List<String> options;
            if (i == 0) {
                options = settings.getDirectoriesWithCards();
            } else {
                File[] subDirs = new File(parentPath).listFiles(File::isDirectory);
                List<String> subDirNames = subDirs == null ? List.of() : Arrays.stream(subDirs)
                    .filter(dir -> !dir.getName().startsWith("."))
                    .map(File::getName)
                    .sorted()
                    .toList();
                options = new ArrayList<>();
                options.add(".");
                options.addAll(subDirNames);
            }
            if (i > 0) {
                selectors.add(text("/"));
            }
            selectors.add(rndDirSelector(keyValueParam(baseParamName, i), options, curDirPart));
            parentPath += (i == 0 ? "" : "/") + curDirPart;
        }
        return frag(selectors);
    }

    private List<String> getSelectedDirectoryList(RequestParams params) {
        ArrayList<String> res = new ArrayList<>();
        if (params.hasParam(keyValueParam(baseParamName, 0))) {
            int i = 0;
            while (params.hasParam(keyValueParam(baseParamName, i))) {
                String curDirPart = params.getParam(keyValueParam(baseParamName, i++));
                res.add(curDirPart);
                if (".".equals(curDirPart)) {
                    break;
                }
            }
        } else {
            String cachedDir = cache.getStr(baseParamName, getDefaultDir(settings));
            getSelectedDirectoryList(cachedDir).forEach(res::add);
        }
        return Collections.unmodifiableList(getValidDirs(res, settings));
    }

    private List<String> getSelectedDirectoryList(String dirStr) {
        ArrayList<String> res = new ArrayList<>();
        for (String dirFromSettings : settings.getDirectoriesWithCards()) {
            if (dirStr.startsWith(dirFromSettings)) {
                res.add(dirFromSettings);
                Arrays.stream(dirStr.substring(dirFromSettings.length()).split("/"))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .forEach(res::add);
                break;
            }
        }
        return Collections.unmodifiableList(getValidDirs(res, settings));
    }

    private List<String> getValidDirs(List<String> dirs, Settings settings) {
        ArrayList<String> validDirs = new ArrayList<>();
        String curPath = "";
        for (int i = 0; i < dirs.size(); i++) {
            String curPart = dirs.get(i);
            curPath += (i == 0 ? "" : "/") + curPart;
            File curDir = new File(curPath);
            if (curDir.exists() && curDir.isDirectory()) {
                validDirs.add(curPart);
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
        return select(paramName, true, selected, options.stream().map(opt -> Pair.of(opt, text(opt))).toList());
    }
}
