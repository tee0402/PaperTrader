package kesira.papertrader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.math.BigDecimal;

public class TradeDialogFragment extends DialogFragment {
    private final boolean buy;
    private int quantity;
    private AlertDialog dialog;

    TradeDialogFragment(boolean buy) {
        this.buy = buy;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        StockInfoActivity stockInfoActivity = (StockInfoActivity) requireActivity();
        View v = stockInfoActivity.getLayoutInflater().inflate(buy ? R.layout.dialog_buy : R.layout.dialog_sell, null);

        Bundle args = requireArguments();
        String ticker = args.getString("ticker");
        BigDecimal stockPrice = new BigDecimal(args.getString("stockPrice"));

        TextView totalText = (TextView) v.findViewById(R.id.total);
        int sharesOwned = Portfolio.getShares(ticker);
        totalText.setHint(buy ? Portfolio.getCashString() + " available" : sharesOwned + " shares available");
        int sharesCanAfford = Portfolio.divide(Portfolio.getCash(), stockPrice).intValue();
        if (buy) {
            ((TextView) v.findViewById(R.id.shares)).setText("You can afford " + sharesCanAfford + " shares.");
        }

        EditText enterQuantity = (EditText) v.findViewById(R.id.quantity);
        enterQuantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                String quantityString = enterQuantity.getText().toString();
                if (quantityString.matches("^0+")) {
                    enterQuantity.setText(quantityString.replaceFirst("^0+", ""));
                }
                quantity = quantityString.equals("") ? 0 : Integer.parseInt(quantityString);
                if (quantity > 0) {
                    totalText.setText(Portfolio.formatCurrency(new BigDecimal(quantity).multiply(stockPrice)));
                } else {
                    totalText.setText("");
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(quantity > 0 && quantity <= (buy ? sharesCanAfford : sharesOwned));
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(stockInfoActivity);
        builder.setView(v).setPositiveButton(buy ? R.string.button_buy : R.string.button_sell, (dialog, id) -> {
            if (quantity <= 0) {
                Toast.makeText(stockInfoActivity, "Please enter a valid number of shares", Toast.LENGTH_LONG).show();
            } else if (quantity > (buy ? sharesCanAfford : sharesOwned)) {
                Toast.makeText(stockInfoActivity, buy ? "You can only afford " + sharesCanAfford + " shares" : "You only have " + sharesOwned + " shares to sell", Toast.LENGTH_LONG).show();
            } else {
                Portfolio.changePosition(buy, ticker, quantity, stockPrice);
                stockInfoActivity.updatePosition();
                Toast.makeText(stockInfoActivity, (buy ? "Bought " : "Sold ") + quantity + " shares successfully", Toast.LENGTH_LONG).show();
            }
        }).setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());
        dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        return dialog;
    }
}