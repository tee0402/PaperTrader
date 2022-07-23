package kesira.papertrader;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.List;

class HistoryArrayAdapter extends ArrayAdapter<QueryDocumentSnapshot> {
    private final Portfolio portfolio = Portfolio.getInstance();
    private final LayoutInflater layoutInflater;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance();

    HistoryArrayAdapter(Context context, List<QueryDocumentSnapshot> history) {
        super(context, R.layout.history_row, history);
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView[] textViews;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.history_row, null);
            textViews = new TextView[5];
            textViews[0] = convertView.findViewById(R.id.date);
            textViews[1] = convertView.findViewById(R.id.ticker);
            textViews[2] = convertView.findViewById(R.id.buy);
            textViews[3] = convertView.findViewById(R.id.shares);
            textViews[4] = convertView.findViewById(R.id.total);
            convertView.setTag(textViews);
        } else {
            textViews = (TextView[]) convertView.getTag();
        }
        QueryDocumentSnapshot queryDocumentSnapshot = getItem(position);
        Timestamp timestamp = queryDocumentSnapshot.getTimestamp("date");
        assert timestamp != null;
        textViews[0].setText(dateFormat.format(timestamp.toDate()));
        textViews[1].setText(queryDocumentSnapshot.getString("ticker"));
        boolean buy = Boolean.TRUE.equals(queryDocumentSnapshot.getBoolean("buy"));
        textViews[2].setText(buy ? "Buy" : "Sell");
        textViews[2].setTextColor(buy ? Color.parseColor("#33CC33") : Color.RED);
        String sharesString = queryDocumentSnapshot.getString("shares");
        BigDecimal shares = new BigDecimal(sharesString);
        BigDecimal price = new BigDecimal(queryDocumentSnapshot.getString("price"));
        textViews[3].setText(sharesString + " shares @ " + portfolio.formatCurrency(price));
        textViews[4].setText(portfolio.formatCurrency(shares.multiply(price)));
        return convertView;
    }
}