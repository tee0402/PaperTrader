package kesira.papertrader;

import android.view.MenuItem;

class SearchMenuItemHelper {
    private static final SearchMenuItemHelper searchMenuItemHelper = new SearchMenuItemHelper();
    private MenuItem searchMenuItem;

    private SearchMenuItemHelper() {}

    static SearchMenuItemHelper getInstance() {
        return searchMenuItemHelper;
    }

    void initialize(MenuItem searchMenuItem) {
        this.searchMenuItem = searchMenuItem;
    }

    void collapseSearch() {
        searchMenuItem.collapseActionView();
    }
}
