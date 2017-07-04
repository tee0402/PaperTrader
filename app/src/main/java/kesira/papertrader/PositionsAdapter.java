package kesira.papertrader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

class PositionsAdapter extends BaseAdapter {

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final ArrayList<CustomRow> mArrayList;

    private class ViewHolder {
        TextView textView1;
        TextView textView2;
        TextView textView3;
        TextView textView4;
    }

    PositionsAdapter(Context context, ArrayList<CustomRow> arrayList) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mArrayList = arrayList;
    }

    @Override
    public int getCount() {
        return mArrayList.size();
    }

    @Override
    public CustomRow getItem(int position) {
        return mArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.positions_row, null);
            holder = new ViewHolder();
            holder.textView1 = convertView.findViewById(R.id.ticker);
            holder.textView2 = convertView.findViewById(R.id.quote);
            holder.textView3 = convertView.findViewById(R.id.percentChange);
            holder.textView4 = convertView.findViewById(R.id.shares);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.textView1.setText(mArrayList.get(position).getTicker());
        holder.textView2.setText("$" + mArrayList.get(position).getQuote());
        if (Float.valueOf(mArrayList.get(position).getPercentChange()) >= 0) {
            holder.textView3.setText("+" + mArrayList.get(position).getPercentChange() + "%");
            holder.textView3.setTextColor(Color.parseColor("#33CC33"));
        }
        else {
            holder.textView3.setText(String.format("%s%%", mArrayList.get(position).getPercentChange()));
            holder.textView3.setTextColor(Color.RED);
        }
        SharedPreferences prefs = mContext.getSharedPreferences("Save", Context.MODE_PRIVATE);
        holder.textView4.setText(prefs.getInt(mArrayList.get(position).getTicker(), 0) + " shares");
        return convertView;
    }
}