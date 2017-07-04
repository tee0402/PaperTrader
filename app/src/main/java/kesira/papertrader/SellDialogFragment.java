package kesira.papertrader;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Set;

public class SellDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_sell, null);
        final String ticker = getArguments().getString("ticker");
        final float stockPrice = getArguments().getFloat("stockPrice");
        final EditText editText = view.findViewById(R.id.quantity);
        final TextView amount = view.findViewById(R.id.amountValue);
        final SharedPreferences prefs = getActivity().getSharedPreferences("Save", Context.MODE_PRIVATE);
        final int sharesOwned = prefs.getInt(ticker, 0);

        amount.setHint(sharesOwned + " shares available");

        builder.setView(view)
                .setPositiveButton(R.string.button_sell, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int quantity = Integer.valueOf(editText.getText().toString());
                        if (quantity > sharesOwned) {
                            Toast.makeText(getActivity(), "You only have " + sharesOwned + " shares to sell", Toast.LENGTH_LONG).show();
                        }
                        else {
                            SharedPreferences.Editor editor = prefs.edit();
                            if (sharesOwned - quantity == 0) {
                                Set<String> positionsSet = new HashSet<>(prefs.getStringSet("positions", new HashSet<String>()));
                                positionsSet.remove(ticker);
                                editor.putStringSet("positions", positionsSet);
                                Set<String> watchlistSet = new HashSet<>(prefs.getStringSet("watchlist", new HashSet<String>()));
                                watchlistSet.add(ticker);
                                editor.putStringSet("watchlist", watchlistSet);
                                editor.remove(ticker + "_cost");
                                getActivity().findViewById(R.id.position).setVisibility(View.GONE);
                            }
                            else {
                                ((TextView) getActivity().findViewById(R.id.sharesOwned)).setText(String.valueOf(sharesOwned - quantity));
                                ((TextView) getActivity().findViewById(R.id.positionValue)).setText(NumberFormat.getCurrencyInstance().format((sharesOwned - quantity) * stockPrice));
                                ((TextView) getActivity().findViewById(R.id.percentageOfPortfolio)).setText(new DecimalFormat("0.00").format((sharesOwned - quantity) * stockPrice / prefs.getFloat("portfolioValue", 0) * 100) + "%");
                                float costBasis = prefs.getFloat(ticker + "_cost", 0);
                                if (stockPrice - costBasis >= 0) {
                                    ((TextView) getActivity().findViewById(R.id.positionPerformance)).setText("+" + NumberFormat.getCurrencyInstance().format((sharesOwned - quantity) * (stockPrice - costBasis)) + " (+" + new DecimalFormat("0.00").format((stockPrice - costBasis) / costBasis * 100) + "%)");
                                    ((TextView) getActivity().findViewById(R.id.positionPerformance)).setTextColor(Color.parseColor("#33CC33"));
                                }
                                else {
                                    ((TextView) getActivity().findViewById(R.id.positionPerformance)).setText(NumberFormat.getCurrencyInstance().format((sharesOwned - quantity) * (stockPrice - costBasis)) + " (" + new DecimalFormat("0.00").format((stockPrice - costBasis) / costBasis * 100) + "%)");
                                    ((TextView) getActivity().findViewById(R.id.positionPerformance)).setTextColor(Color.RED);
                                }
                            }
                            editor.putInt(ticker, sharesOwned - quantity);
                            editor.putFloat("cash", prefs.getFloat("cash", -1) + stockPrice * quantity);
                            editor.apply();
                            Toast.makeText(getActivity(), "Sold " + quantity + " shares successfully", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        SellDialogFragment.this.getDialog().cancel();
                    }
                });
        final AlertDialog dialog = builder.create();
        //noinspection ConstantConditions
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
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
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
                else {
                    amount.setText(NumberFormat.getCurrencyInstance().format(Integer.valueOf(editText.getText().toString()) * stockPrice));
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        });

        return dialog;
    }
}