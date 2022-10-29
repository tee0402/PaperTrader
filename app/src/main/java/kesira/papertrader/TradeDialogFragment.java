package kesira.papertrader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
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
    private final String ticker;
    private final BigDecimal stockPrice;
    private int quantity;
    private AlertDialog dialog;

    TradeDialogFragment(boolean buy, String ticker, BigDecimal stockPrice) {
        this.buy = buy;
        this.ticker = ticker;
        this.stockPrice = stockPrice;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Portfolio portfolio = Portfolio.getInstance();
        Context context = requireContext();
        StockInfoFragment stockInfoFragment = (StockInfoFragment) requireParentFragment();
        View v = stockInfoFragment.getLayoutInflater().inflate(buy ? R.layout.dialog_buy : R.layout.dialog_sell, null);

        TextView totalText = v.findViewById(R.id.total);
        boolean inPortfolio = portfolio.inPortfolio(ticker);
        int sharesOwned = inPortfolio ? portfolio.getStock(ticker).getShares() : 0;
        totalText.setHint(buy ? portfolio.getCashString() + " available" : portfolio.formatNumber(sharesOwned) + " shares available");
        int sharesCanAfford = portfolio.divide(portfolio.getCash(), stockPrice).intValue();
        if (buy) {
            ((TextView) v.findViewById(R.id.shares)).setText("You can afford " + portfolio.formatNumber(sharesCanAfford) + " shares.");
        }

        EditText enterQuantity = v.findViewById(R.id.quantity);
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
                totalText.setText(quantity > 0 ? portfolio.formatCurrency(new BigDecimal(quantity).multiply(stockPrice)) : "");
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(quantity > 0 && quantity <= (buy ? sharesCanAfford : sharesOwned));
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(v).setPositiveButton(buy ? R.string.button_buy : R.string.button_sell, (dialog, id) -> {
            if (quantity <= 0) {
                Toast.makeText(context, "Please enter a valid number of shares", Toast.LENGTH_LONG).show();
            } else if (quantity > (buy ? sharesCanAfford : sharesOwned)) {
                Toast.makeText(context, buy ? "You can only afford " + portfolio.formatNumber(sharesCanAfford) + " shares" : "You only have " + portfolio.formatNumber(sharesOwned) + " shares to sell", Toast.LENGTH_LONG).show();
            } else {
                if (buy && !inPortfolio) {
                    stockInfoFragment.addToWatchlist();
                }
                portfolio.changePosition(buy, ticker, quantity, stockPrice, stockInfoFragment);
                stockInfoFragment.updatePosition();
                Toast.makeText(context, (buy ? "Bought " : "Sold ") + portfolio.formatNumber(quantity) + " shares successfully", Toast.LENGTH_LONG).show();
            }
        }).setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());
        dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        return dialog;
    }
}