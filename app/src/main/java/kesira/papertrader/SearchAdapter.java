package kesira.papertrader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

class SearchAdapter extends BaseAdapter {

    private final LayoutInflater mInflater;
    private final ArrayList<SearchRow> mArrayList;

    private class ViewHolder {
        TextView textView1;
        TextView textView2;
    }

    SearchAdapter(Context context, ArrayList<SearchRow> arrayList) {
        mInflater = LayoutInflater.from(context);
        mArrayList = arrayList;
    }

    @Override
    public int getCount() {
        return mArrayList.size();
    }

    @Override
    public SearchRow getItem(int position) {
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
            convertView = mInflater.inflate(R.layout.search_row, null);
            holder = new ViewHolder();
            holder.textView1 = (TextView) convertView.findViewById(R.id.searchTicker);
            holder.textView2 = (TextView) convertView.findViewById(R.id.searchName);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.textView1.setText(mArrayList.get(position).getTicker());
        holder.textView2.setText(mArrayList.get(position).getName());
        return convertView;
    }
}
