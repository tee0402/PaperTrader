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
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {
    private FragmentManager supportFragmentManager;
    private ActionBar actionBar;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        supportFragmentManager = getSupportFragmentManager();
        setNavigationDrawer();
        supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragmentContainerView, MainFragment.class, null, "main")
                .commit();
    }

    private void setNavigationDrawer() {
        actionBar = getSupportActionBar();
        assert actionBar != null;
        setActionBarUpIndicatorAsMenu();
        actionBar.setDisplayHomeAsUpEnabled(true);
        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(item -> {
            MainFragment mainFragment = (MainFragment) supportFragmentManager.findFragmentByTag("main");
            assert mainFragment != null;
            supportFragmentManager.beginTransaction()
                    .hide(mainFragment)
                    .add(R.id.fragmentContainerView, HistoryFragment.class, null)
                    .setReorderingAllowed(true)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
            drawerLayout.close();
            return false;
        });
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

    private void hideSoftInput(View v) {
        ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    void toggleDrawer() {
        if (drawerLayout.isOpen()) {
            drawerLayout.close();
        } else {
            drawerLayout.open();
        }
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
        super.onBackPressed();
        setActionBarUpIndicatorAsMenu();
    }
}