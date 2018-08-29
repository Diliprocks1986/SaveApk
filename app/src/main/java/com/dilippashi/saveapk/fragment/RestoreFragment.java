package com.dilippashi.saveapk.fragment;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.dilippashi.saveapk.BuildConfig;
import com.dilippashi.saveapk.R;
import com.dilippashi.saveapk.adapter.RestoreListAdapter;
import com.dilippashi.saveapk.data.Utils;
import com.dilippashi.saveapk.model.RestoreModel;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RestoreFragment extends Fragment {
    public RestoreListAdapter rAdapter;
    private ListView listView;
    private View view;
    private LinearLayout lyt_not_found;
    private boolean mode_checkall = false;
    private ActionMode act_mode = null;
    private MultiChoiceModeListener multiChoiceModeListener = new MultiChoiceModeListener() {

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            final int checkedCount = listView.getCheckedItemCount();
            mode.setTitle(checkedCount + " selected");
            rAdapter.setSelected(position, checked);
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // Respond to clicks on the actions in the CAB
            switch (item.getItemId()) {
                case R.id.action_check_all:
                    toogleCheckAll();
                    return true;
                case R.id.action_restore:
                    restoreApkFiles(rAdapter.getSelected());
                    return true;
                case R.id.action_delete:
                    deleteApkFiles(rAdapter.getSelected());
                    refresh();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.restore_context_menu, menu);
            mode.setTitle(listView.getCheckedItemCount() + " conversation selected");
            act_mode = mode;
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            rAdapter.resetSelected();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_restore, container, false);
        listView = view.findViewById(R.id.rlistView);
        lyt_not_found = view.findViewById(R.id.lyt_not_found);
        prepareAds();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                dialogApkFileOption(i);
            }
        });
        return view;
    }

    private void prepareAds() {
        AdView mAdView = view.findViewById(R.id.ad_view);
        AdRequest adRequest = new AdRequest.Builder().build();
        // Start loading the ad in the background.
        mAdView.loadAd(adRequest);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void refreshList() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PermissionCheck();
            refresh();
        } else {
            refresh();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void refresh() {
        List<RestoreModel> apkList = Utils.loadBackupAPK(getActivity());
        rAdapter = new RestoreListAdapter(getContext(), apkList);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        // Capture ListView item click
        listView.setMultiChoiceModeListener(multiChoiceModeListener);
        listView.setAdapter(rAdapter);
        if (apkList.size() == 0) {
            lyt_not_found.setVisibility(View.VISIBLE);
        } else {
            lyt_not_found.setVisibility(View.GONE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void PermissionCheck() {
        int hasWriteLocationPermission = Objects.requireNonNull(getActivity()).checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (hasWriteLocationPermission != PackageManager.PERMISSION_GRANTED) {
                int REQUEST_CODE_ASK_PERMISSIONS = 123;
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
            }
        }
    }

    private void toogleCheckAll() {
        mode_checkall = !mode_checkall;
        for (int i = 0; i < rAdapter.getCount(); i++) {
            listView.setItemChecked(i, mode_checkall);
        }
        if (mode_checkall) {
            rAdapter.selectAll();
        } else {
            rAdapter.resetSelected();
        }
    }

    public ActionMode getActionMode() {
        return act_mode;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void dialogApkFileOption(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        final RestoreModel r = rAdapter.getItem(position);
        builder.setTitle("Apk File Option");
        ListView listView = new ListView(getActivity());
        listView.setPadding(25, 25, 25, 25);
        String[] stringArray = new String[]{"Restore", "Share", "Delete file"};
        listView.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, stringArray));
        builder.setView(listView);
        final AppCompatDialog dialog = builder.create();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                dialog.dismiss();
                List<RestoreModel> selected_apk = new ArrayList<>();
                selected_apk.add(r);
                switch (i) {
                    case 0:
                        restoreApkFiles(selected_apk);
                        //restore
                        break;
                    case 1:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Uri apkUri = FileProvider.getUriForFile(Objects.requireNonNull(getActivity()), BuildConfig.APPLICATION_ID + ".provider", r.getFile());
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setData(apkUri);
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            Uri fileUri = Uri.fromFile(r.getFile());
                            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
                            intent.setType("*/*");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        }
                        //share
                        break;
                    case 2:
                        deleteApkFiles(selected_apk);
                        refresh();
                        //Delete file
                        break;
                }
            }
        });

        dialog.show();
    }

    private void restoreApkFiles(List<RestoreModel> apklist) {
        for (RestoreModel restr : apklist) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri apkUri = FileProvider.getUriForFile(Objects.requireNonNull(getActivity()), BuildConfig.APPLICATION_ID + ".provider", restr.getFile());
                Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                intent.setData(apkUri);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } else {
                Uri apkUri = Uri.fromFile(restr.getFile());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }

    private void deleteApkFiles(List<RestoreModel> apklist) {
        for (RestoreModel restr : apklist) {
            if (restr.getFile().exists()) {
                restr.getFile().delete();
            }
        }
    }

}
