package org.igye.remem3.app.components.impl;

import lombok.Getter;
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
    @Getter
    private boolean isReadonly;
    private boolean allowMkDir;
    private List<String> selectedDirectoryList;
    private boolean isMkDirRequest;

    public DirSelectorCmpImpl(Settings settings, Cache cache, String baseParamName) {
        this.settings = settings;
        this.cache = cache;
        this.baseParamName = baseParamName;
        this.isReadonly = false;
        this.allowMkDir = false;
        this.selectedDirectoryList = List.of();
    }

    public DirSelectorCmpImpl setReadOnly(boolean isReadonly) {
        this.isReadonly = isReadonly;
        return this;
    }

    public DirSelectorCmpImpl setAllowMkDir(boolean allowMkDir) {
        this.allowMkDir = allowMkDir;
        return this;
    }

    public DirSelectorCmpImpl setPath(RequestParams params) {
        Pair<List<String>, Boolean> validDirs = getSelectedDirectoryList(params);
        this.selectedDirectoryList = validDirs.getLeft();
        this.isMkDirRequest = validDirs.getRight();
        return this;
    }

    public DirSelectorCmpImpl setPath(File dir) {
        Pair<List<String>, Boolean> validDirs = getSelectedDirectoryList(dir);
        this.selectedDirectoryList = validDirs.getLeft();
        this.isMkDirRequest = validDirs.getRight();
        return this;
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
                if (allowMkDir) {
                    options.add("+");
                }
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

    @Override
    public boolean isMkDirRequest() {
        return isMkDirRequest;
    }

    @Override
    public DirSelectorCmpImpl clone() {
        DirSelectorCmpImpl res = new DirSelectorCmpImpl(settings, cache, baseParamName)
            .setReadOnly(this.isReadonly)
            .setAllowMkDir(this.allowMkDir);
        res.selectedDirectoryList = this.selectedDirectoryList;
        return res;
    }

    private Pair<List<String>, Boolean> getSelectedDirectoryList(RequestParams params) {
        List<String> res;
        if (params.hasParam(keyValueParam(baseParamName, 0))) {
            res = new ArrayList<>();
            int i = 0;
            while (params.hasParam(keyValueParam(baseParamName, i))) {
                String curPathPart = params.getParam(keyValueParam(baseParamName, i++));
                res.add(curPathPart);
                if (".".equals(curPathPart) || "+".equals(curPathPart)) {
                    break;
                }
            }
        } else {
            String cachedDir = cache.getStr(baseParamName, getDefaultDir());
            res = getSelectedDirectoryList(new File(cachedDir)).getLeft();
        }
        Pair<List<String>, Boolean> validDirs = getValidDirs(res);
        return Pair.of(Collections.unmodifiableList(validDirs.getLeft()), validDirs.getRight());
    }

    @SneakyThrows
    private Pair<List<String>, Boolean> getSelectedDirectoryList(File dir) {
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
        Pair<List<String>, Boolean> validDirs = getValidDirs(res);
        return Pair.of(Collections.unmodifiableList(validDirs.getLeft()), validDirs.getRight());
    }

    private Pair<List<String>, Boolean> getValidDirs(List<String> pathParts) {
        ArrayList<String> validDirs = new ArrayList<>();
        String curPath = "";
        boolean mkNewDirRequested = false;
        for (int i = 0; i < pathParts.size(); i++) {
            String curPathPart = pathParts.get(i);
            if (curPathPart.equals("+")) {
                mkNewDirRequested = true;
                break;
            }
            curPath += (i == 0 ? "" : "/") + curPathPart;
            File curDir = new File(curPath);
            if (curDir.exists() && curDir.isDirectory()) {
                validDirs.add(curPathPart);
            } else {
                break;
            }
        }
        if (validDirs.isEmpty()) {
            validDirs.add(getDefaultDir());
        }
        if (!".".equals(validDirs.getLast())) {
            validDirs.add(".");
        }
        return Pair.of(validDirs, mkNewDirRequested);
    }

    private String getDefaultDir() {
        return settings.getDirectoriesWithCards().getFirst();
    }

    private HtmlElem rndDirSelector(String paramName, List<String> options, String selected) {
        return select(paramName, selected, options.stream().map(opt -> Pair.of(opt, text(opt))).toList())
            .submitOnChange();
    }
}
