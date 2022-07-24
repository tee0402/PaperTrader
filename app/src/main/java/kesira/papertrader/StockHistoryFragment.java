package kesira.papertrader;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StockHistoryFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        MainActivity activity = (MainActivity) requireActivity();
        String ticker = requireArguments().getString("ticker");

        activity.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                activity.setActionBarTitle(ticker + " " + getString(R.string.history));
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == android.R.id.home) {
                    activity.onBackPressed();
                }
                return false;
            }
        }, getViewLifecycleOwner());

        List<QueryDocumentSnapshot> history = new ArrayList<>();
        HistoryArrayAdapter adapter = new HistoryArrayAdapter(activity, history);
        NonScrollListView historyListView = view.findViewById(R.id.historyListView);
        historyListView.setAdapter(adapter);
        Portfolio.getInstance().queryHistory(history, adapter, ticker, false, null, null);

        return view;
    }
}
