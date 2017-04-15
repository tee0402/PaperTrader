package kesira.papertrader;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Set;

public class BuyDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_buy, null);
        final String ticker = getArguments().getString("ticker");
        final float stockPrice = Float.valueOf(((TextView) getActivity().findViewById(R.id.stockPrice)).getText().toString());
        final EditText editText = (EditText) view.findViewById(R.id.quantity);
        final TextView amount = (TextView) view.findViewById(R.id.amountValue);
        final SharedPreferences prefs = getActivity().getSharedPreferences("Save", Context.MODE_PRIVATE);
        final int sharesCanAfford = (int) (prefs.getFloat("cash", -1) / stockPrice);
        final int sharesOwned = prefs.getInt(ticker, 0);

        ((TextView) view.findViewById(R.id.shares)).setText("You can buy " + sharesCanAfford + " shares.");
        amount.setHint(NumberFormat.getCurrencyInstance().format(prefs.getFloat("cash", -1)) + " available");
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editText.getText().toString().equals("") || !(Integer.valueOf(editText.getText().toString()) > 0)) {
                    amount.setText("");
                }
                else {
                    amount.setText(NumberFormat.getCurrencyInstance().format(Integer.valueOf(editText.getText().toString()) * stockPrice));
                }
            }
        });
        builder.setView(view)
                .setPositiveButton(R.string.button_buy, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int quantity = Integer.valueOf(editText.getText().toString());
                        if (quantity < 1) {
                            Toast.makeText(getActivity(), "Please enter a valid number of shares", Toast.LENGTH_LONG).show();
                        }
                        else if (quantity > sharesCanAfford) {
                            Toast.makeText(getActivity(), "You can only afford " + sharesCanAfford + " shares", Toast.LENGTH_LONG).show();
                        }
                        else {
                            SharedPreferences.Editor editor = prefs.edit();
                            if (sharesOwned == 0) {
                                Set<String> positionsSet = new HashSet<>(prefs.getStringSet("positions", new HashSet<String>()));
                                positionsSet.add(ticker);
                                editor.putStringSet("positions", positionsSet);
                                Set<String> watchlistSet = new HashSet<>(prefs.getStringSet("watchlist", new HashSet<String>()));
                                watchlistSet.remove(ticker);
                                editor.putStringSet("watchlist", watchlistSet);
                            }
                            editor.putInt(ticker, sharesOwned + quantity);
                            editor.putFloat("cash", prefs.getFloat("cash", -1) - stockPrice * quantity);
                            editor.apply();
                            Toast.makeText(getActivity(), "Bought " + quantity + " shares successfully", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        BuyDialogFragment.this.getDialog().cancel();
                    }
                });
        AlertDialog dialog = builder.create();
        //noinspection ConstantConditions
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
        return dialog;
    }
}