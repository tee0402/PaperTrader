package kesira.papertrader;

class SearchRow {

    private final String ticker;
    private final String name;

    SearchRow(String ticker, String name) {
        this.ticker = ticker;
        this.name = name;
    }

    String getTicker() {
        return ticker;
    }

    String getName() {
        return name;
    }
}
