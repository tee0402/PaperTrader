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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyCustomSuggestionProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        MatrixCursor matrixCursor = new MatrixCursor(new String[] {BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2, SearchManager.SUGGEST_COLUMN_INTENT_DATA});
        String query = uri.getLastPathSegment();
        System.out.println("uri:" + uri);
        System.out.println("last:" + query);
        if (!query.equals("search_suggest_query")) {
            try {
                URL url = new URL("https://api.polygon.io/v3/reference/tickers?search=" + query + "&active=true&sort=ticker&order=asc&limit=10&apiKey=lTkAIOnwJ9vpjDvqYAF0RWt9yMkhD0up");
                HttpURLConnection urlConnection;
                do {
                    urlConnection = (HttpURLConnection) url.openConnection();
                } while (urlConnection.getResponseCode() >= 400);
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        result.append(line).append("\n");
                    }
                    bufferedReader.close();
                    try {
                        JSONArray jsonArray = new JSONObject(result.toString()).getJSONArray("results");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String ticker = jsonObject.getString("ticker");
                            matrixCursor.addRow(new Object[]{i, ticker, jsonObject.getString("name"), ticker});
                        }
                    } catch (JSONException e) {
                        Log.e("Exception", e.getMessage());
                    }
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                Log.e("Exception", e.toString());
                return null;
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
