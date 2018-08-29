package com.dilippashi.saveapk;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.dilippashi.saveapk.controller.DialogSelectionListener;
import com.dilippashi.saveapk.model.DialogConfigs;
import com.dilippashi.saveapk.model.DialogProperties;
import com.dilippashi.saveapk.receiver.ReceiverActivity;
import com.dilippashi.saveapk.sender.SHAREthemActivity;
import com.dilippashi.saveapk.sender.SHAREthemService;
import com.dilippashi.saveapk.utils.HotspotControl;
import com.dilippashi.saveapk.utils.Utils;
import com.dilippashi.saveapk.view.FilePickerDialog;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import java.io.File;

import static android.os.Environment.isExternalStorageRemovable;

public class SendActivity extends AppCompatActivity {

    FilePickerDialog dialog;
    AdView mAdView;
    String Directory_path;
    private ActionBar actionBar;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        actionBar = getSupportActionBar();
        initToolbar();
        prepareAds();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.Shareit));
        toolbar.setTitleTextColor(Color.WHITE);
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

    private void prepareAds() {
        mAdView = findViewById(R.id.ad_view1);
        AdRequest adRequest = new AdRequest.Builder().build();
        // Start loading the ad in the background.
        mAdView.loadAd(adRequest);
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        AdRequest adRequest2 = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(adRequest2);
    }

    public void showInterstitial() {
        // Show the ad if it's ready
        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
    }

    public void sendFiles(View view) {
        if (isExternalStorageRemovable()) {
            Directory_path = DialogConfigs.DEFAULT_DIR;
        } else {
            Directory_path = Environment.getExternalStorageDirectory().getPath();
        }
        if (Utils.isShareServiceRunning(getApplicationContext())) {
            startActivity(new Intent(getApplicationContext(), SHAREthemActivity.class));
            return;
        }

        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_AND_DIR_SELECT;
        properties.root = new File(Directory_path);
        properties.error_dir = new File(Directory_path);
        properties.extensions = null;

        dialog = new FilePickerDialog(this, properties);
        dialog.setTitle("Select files to share");

        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                if (null == files || files.length == 0) {
                    Toast.makeText(SendActivity.this, "Select at least one file to start Share Mode", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(getApplicationContext(), SHAREthemActivity.class);
                intent.putExtra(SHAREthemService.EXTRA_FILE_PATHS, files);
                intent.putExtra(SHAREthemService.EXTRA_PORT, 52287);
                intent.putExtra(SHAREthemService.EXTRA_SENDER_NAME, "Dilip");
                startActivity(intent);
            }
        });
        dialog.show();
    }

    public void receiveFiles(View view) {
        HotspotControl hotspotControl = HotspotControl.getInstance(getApplicationContext());
        if (null != hotspotControl && hotspotControl.isEnabled()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Sender(Hotspot) mode is active. Please disable it to proceed with Receiver mode");
            builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            builder.show();
            return;
        }
        startActivity(new Intent(getApplicationContext(), ReceiverActivity.class));
    }

    //Add this method to show Dialog when the required permission has been granted to the app.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case FilePickerDialog.EXTERNAL_READ_PERMISSION_GRANT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (dialog != null) {   //Show dialog if the read permission has been granted.
                        dialog.show();
                    }
                } else {
                    //Permission has not been granted. Notify the user.
                    Toast.makeText(this, "Permission is Required for getting list of files", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        showInterstitial();
    }
}
