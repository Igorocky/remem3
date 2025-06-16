package org.igye.remem3.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.utils.sqlite.Database;
import org.igye.remem3.web.StatefulWebController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DbAccessController extends HtmlBuilder implements StatefulWebController<Void, Void> {
    private final Database database;

    public DbAccessController(Database database) {
        this.database = database;
    }

    @Override
    public String getPath() {
        return "db_access";
    }

    @Override
    public Void loadState(HttpServletRequest req) {
        return null;
    }

    @Override
    public Optional<Void> decodeAction(HttpServletRequest req, Void state) {
        return Optional.empty();
    }

    @Override
    public Void updateState(Void state, Void action) {
        return null;
    }

    @Override
    public void saveState(Void state) {

    }

    @Override
    public String renderState(Void state) {
        database.transaction(tx -> {
            List<Map<String, Object>> res = tx.executeQuery("select 10");
            return null;
        });
        return simplePageWithTitle(
            "DB Access",
            frag()
        ).toString();
    }
}
