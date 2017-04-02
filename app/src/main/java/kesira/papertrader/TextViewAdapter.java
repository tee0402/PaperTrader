package kesira.papertrader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

class TextViewAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private ArrayList<CustomRow> mArrayList;

    private class ViewHolder {
        TextView textView1;
        TextView textView2;
        TextView textView3;
    }

    TextViewAdapter(Context context, ArrayList<CustomRow> arrayList) {
        inflater = LayoutInflater.from(context);
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.listview_row, parent);
            holder = new ViewHolder();
            holder.textView1 = (TextView) convertView.findViewById(R.id.ticker);
            holder.textView2 = (TextView) convertView.findViewById(R.id.quote);
            holder.textView3 = (TextView) convertView.findViewById(R.id.percentChange);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.textView1.setText(mArrayList.get(position).getTicker());
        holder.textView2.setText(mArrayList.get(position).getQuote());
        holder.textView3.setText(mArrayList.get(position).getPercentChange());
        return convertView;
    }
}