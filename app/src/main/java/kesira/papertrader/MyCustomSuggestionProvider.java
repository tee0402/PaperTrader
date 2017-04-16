package kesira.papertrader;

import android.app.SearchManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MyCustomSuggestionProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] strings, @Nullable String s, @Nullable String[] strings1, @Nullable String s1) {
        String query = uri.getLastPathSegment();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2, SearchManager.SUGGEST_COLUMN_INTENT_DATA});
        String result;
        try {
            URL url = new URL("http://dev.markitondemand.com/MODApis/Api/v2/Lookup/json?input=" + query);
            HttpURLConnection urlConnection;
            do {
                urlConnection = (HttpURLConnection) url.openConnection();
            }
            while (urlConnection.getResponseCode() >= 400);
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                result = "";
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    result += line + "\n";
                }
                bufferedReader.close();
            }
            finally {
                urlConnection.disconnect();
            }
        }
        catch (Exception e) {
            Log.e("Exception", e.toString());
            return null;
        }
        Log.i("INFO", result);
        try {
            ArrayList<String> arrayList = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(result);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (arrayList.contains(jsonObject.getString("Symbol"))) {
                    continue;
                }
                arrayList.add(jsonObject.getString("Symbol"));
                matrixCursor.addRow(new Object[] {i, jsonObject.getString("Symbol"), jsonObject.getString("Name"), jsonObject.getString("Symbol")});
            }

        }
        catch (Exception e) {
            Log.e("Exception", e.getMessage());
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
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }
}
