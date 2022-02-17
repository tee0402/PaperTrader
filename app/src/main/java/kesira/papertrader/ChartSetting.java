package kesira.papertrader;

import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.data.LineData;

import java.math.BigDecimal;
import java.util.ArrayList;

class ChartSetting {
    private final LineData lineData;
    private final ArrayList<String> xAxisValues;
    private final BigDecimal change;
    private final BigDecimal percentChange;
    private LimitLine limitLine;

    ChartSetting(LineData lineData, ArrayList<String> xAxisValues, BigDecimal change, BigDecimal percentChange) {
        this.lineData = lineData;
        this.xAxisValues = xAxisValues;
        this.change = change;
        this.percentChange = percentChange;
    }

    LineData getLineData() {
        return lineData;
    }

    ArrayList<String> getXAxisValues() {
        return xAxisValues;
    }

    BigDecimal getChange() {
        return change;
    }

    BigDecimal getPercentChange() {
        return percentChange;
    }

    LimitLine getLimitLine() {
        return limitLine;
    }
    void setLimitLine(LimitLine limitLine) {
        this.limitLine = limitLine;
    }
}