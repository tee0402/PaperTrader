package kesira.papertrader;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

public class MainActivity extends AppCompatActivity {
    private ActionBar actionBar;
    private FragmentManager supportFragmentManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        APIHelper.initializeISODateFormat();

        actionBar = getSupportActionBar();
        assert actionBar != null;
        setActionBarUpIndicatorAsMenu();
        actionBar.setDisplayHomeAsUpEnabled(true);

        supportFragmentManager = getSupportFragmentManager();
        supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragmentContainerView, MainFragment.class, null)
                .commit();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    v.clearFocus();
                    hideSoftInput(v);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    void hideSoftInput(View v) {
        ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    void setActionBarUpIndicatorAsMenu() {
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
    }
    void setActionBarUpIndicatorAsBack() {
        actionBar.setHomeAsUpIndicator(0);
    }
    void setActionBarTitle(String title) {
        actionBar.setTitle(title);
    }
    @Override
    public void onBackPressed() {
        if (supportFragmentManager.getBackStackEntryCount() == 1) {
            setActionBarUpIndicatorAsMenu();
        }
        super.onBackPressed();
    }
}