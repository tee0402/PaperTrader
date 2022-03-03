package kesira.papertrader;

import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.data.LineData;

import java.math.BigDecimal;
import java.util.ArrayList;

class ChartSetting {
    private final LineData lineData;
    private final ArrayList<String> xAxisValues;
    private final ArrayList<String> markerDates;
    private final BigDecimal change;
    private final BigDecimal percentChange;
    private LimitLine previousCloseLimitLine;

    ChartSetting(LineData lineData, ArrayList<String> xAxisValues, ArrayList<String> markerDates, BigDecimal change, BigDecimal percentChange) {
        this.lineData = lineData;
        this.xAxisValues = xAxisValues;
        this.markerDates = markerDates;
        this.change = change;
        this.percentChange = percentChange;
    }

    LineData getLineData() {
        return lineData;
    }

    ArrayList<String> getXAxisValues() {
        return xAxisValues;
    }

    ArrayList<String> getMarkerDates() {
        return markerDates;
    }

    BigDecimal getChange() {
        return change;
    }

    BigDecimal getPercentChange() {
        return percentChange;
    }

    LimitLine getPreviousCloseLimitLine() {
        return previousCloseLimitLine;
    }
    void setPreviousCloseLimitLine(LimitLine previousCloseLimitLine) {
        this.previousCloseLimitLine = previousCloseLimitLine;
    }
}