package com.dilippashi.saveapk;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.dilippashi.saveapk.adapter.FragmentAdapter;
import com.dilippashi.saveapk.fragment.BackupFragment;
import com.dilippashi.saveapk.fragment.RestoreFragment;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

public class MainActivity extends AppCompatActivity {
    //public static Toolbar toolbar;
    @SuppressLint("StaticFieldLeak")
    public static BackupFragment frag_backup;
    //for ads
    private InterstitialAd mInterstitialAd;
    private RestoreFragment frag_restore;

    private ViewPager viewPager;
    private ActionBar actionBar;

    private SearchView search;
    private FloatingActionButton fab;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        actionBar = getSupportActionBar();
        viewPager = findViewById(R.id.viewpager);
        fab = findViewById(R.id.fab);
        if (viewPager != null) {
            setupViewPager(viewPager);
        }

        initToolbar();
        prepareAds();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                frag_backup.refresh(true);
            }
        });

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showInterstitial();
                viewPager.setCurrentItem(tab.getPosition());
                // close contextual action mode
                if (frag_backup.getActionMode() != null) {
                    frag_backup.getActionMode().finish();
                }
                if (frag_restore.getActionMode() != null) {
                    frag_restore.getActionMode().finish();
                }

                if (tab.getPosition() == 0) {
                    fab.show();
                } else {
                    frag_restore.refreshList();
                    fab.hide();
                }
                search.onActionViewCollapsed();
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }

        });
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setHomeButtonEnabled(false);
        Window window = this.getWindow();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        FragmentAdapter adapter = new FragmentAdapter(getSupportFragmentManager());

        if (frag_backup == null) {
            frag_backup = new BackupFragment();
        }
        if (frag_restore == null) {
            frag_restore = new RestoreFragment();
        }
        adapter.addFragment(frag_backup, getString(R.string.tab_title_backup));
        adapter.addFragment(frag_restore, getString(R.string.tab_title_restore));
        viewPager.setAdapter(adapter);
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        search = (SearchView) searchItem.getActionView();
        // Detect SearchView icon clicks
        search.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setItemsVisibility(menu, searchItem, false);
                if (viewPager.getCurrentItem() == 0) {
                    search.setQueryHint(getString(R.string.hint_backup_search));
                } else {
                    search.setQueryHint(getString(R.string.hint_restore_search));
                }
                search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String s) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String s) {
                        try {
                            if (viewPager.getCurrentItem() == 0) {
                                frag_backup.bAdapter.getFilter().filter(s);
                            } else {
                                frag_restore.rAdapter.getFilter().filter(s);
                            }
                        } catch (Exception ignored) {

                        }
                        return true;
                    }
                });
            }
        });
        // Detect SearchView close
        search.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                setItemsVisibility(menu, searchItem, true);
                //search.onActionViewCollapsed();
                //search.setIconified(true);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    private void setItemsVisibility(Menu menu, MenuItem exception, boolean visible) {
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            if (item != exception) item.setVisible(visible);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_search: {
                // this do magic
                supportInvalidateOptionsMenu();
                return true;
            }
            case R.id.action_rate: {
                Uri uri = Uri.parse("market://details?id=" + getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
                }
                return true;
            }
            case R.id.Share: {
                showInterstitial();
                Intent intent = new Intent(this, SendActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_about: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("About");
                builder.setMessage(getString(R.string.about_text));
                builder.setNeutralButton("OK", null);
                builder.show();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void prepareAds() {
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        AdRequest adRequest2 = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(adRequest2);
    }

    /**
     * show ads
     */
    public void showInterstitial() {
        // Show the ad if it's ready
        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
    }
}
