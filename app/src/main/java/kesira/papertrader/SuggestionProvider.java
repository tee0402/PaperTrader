package kesira.papertrader;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SuggestionProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        MatrixCursor matrixCursor = new MatrixCursor(new String[] {BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2, SearchManager.SUGGEST_COLUMN_INTENT_DATA});
        String query = uri.getLastPathSegment().toUpperCase();
        if (!query.equals("SEARCH_SUGGEST_QUERY")) {
            String result = APIHelper.get("https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keywords=" + query + "&apikey=1275");
            try {
                JSONArray jsonArray = new JSONObject(result).getJSONArray("bestMatches");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String ticker = jsonObject.getString("1. symbol");
                    matrixCursor.addRow(new Object[] {i, ticker, jsonObject.getString("2. name"), ticker});
                }
            } catch (JSONException e) {
                Log.e("Exception", e.getMessage());
            }
        }
        return matrixCursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
