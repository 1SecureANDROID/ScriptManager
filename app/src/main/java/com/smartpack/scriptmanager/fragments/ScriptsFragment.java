/*
 * Copyright (C) 2020-2021 sunilpaulmathew <sunil.kde@gmail.com>
 *
 * This file is part of Script Manager, an app to create, import, edit
 * and easily execute any properly formatted shell scripts.
 *
 */

package com.smartpack.scriptmanager.fragments;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.DrawableCompat;

import com.smartpack.scriptmanager.BuildConfig;
import com.smartpack.scriptmanager.R;
import com.smartpack.scriptmanager.utils.EditorActivity;
import com.smartpack.scriptmanager.utils.Prefs;
import com.smartpack.scriptmanager.utils.Scripts;
import com.smartpack.scriptmanager.utils.UpdateCheck;
import com.smartpack.scriptmanager.utils.Utils;
import com.smartpack.scriptmanager.utils.ViewUtils;
import com.smartpack.scriptmanager.views.dialog.Dialog;
import com.smartpack.scriptmanager.views.recyclerview.CardView;
import com.smartpack.scriptmanager.views.recyclerview.DescriptionView;
import com.smartpack.scriptmanager.views.recyclerview.RecyclerViewItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by sunilpaulmathew <sunil.kde@gmail.com> on January 12, 2020
 */

public class ScriptsFragment extends RecyclerViewFragment {

    private AsyncTask<Void, Void, List<RecyclerViewItem>> mLoader;

    private boolean mShowCreateNameDialog;

    private boolean mWelcomeDialog = true;

    private Dialog mOptionsDialog;

    private String mCreateName;
    private String mEditScript;
    private String mPath;

    @Override
    protected Drawable getBottomFabDrawable() {
        Drawable drawable = DrawableCompat.wrap(ContextCompat.getDrawable(getActivity(), R.drawable.ic_add));
        DrawableCompat.setTint(drawable, getResources().getColor(R.color.white));
        return drawable;
    }

    @Override
    protected boolean showBottomFab() {
        return true;
    }

    @Override
    protected void init() {
        super.init();

        if (mOptionsDialog != null) {
            mOptionsDialog.show();
        }
        if (mShowCreateNameDialog) {
            showCreateDialog();
        }
    }

    @Override
    public int getSpanCount() {
        int span = Utils.isTablet(getActivity()) ? Utils.getOrientation(getActivity()) ==
                Configuration.ORIENTATION_LANDSCAPE ? 4 : 3 : Utils.getOrientation(getActivity()) ==
                Configuration.ORIENTATION_LANDSCAPE ? 3 : 2;
        if (itemsSize() != 0 && span > itemsSize()) {
            span = itemsSize();
        }
        return span;
    }

    @Override
    protected void addItems(List<RecyclerViewItem> items) {
        if (Utils.checkWriteStoragePermission(getActivity())) {
            reload();
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
    }

    private void reload() {
        if (mLoader == null) {
            getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    clearItems();
                    mLoader = new AsyncTask<Void, Void, List<RecyclerViewItem>>() {

                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                            showProgress();
                        }

                        @Override
                        protected List<RecyclerViewItem> doInBackground(Void... voids) {
                            List<RecyclerViewItem> items = new ArrayList<>();
                            load(items);
                            return items;
                        }

                        @Override
                        protected void onPostExecute(List<RecyclerViewItem> recyclerViewItems) {
                            super.onPostExecute(recyclerViewItems);
                            for (RecyclerViewItem item : recyclerViewItems) {
                                addItem(item);
                            }
                            hideProgress();
                            mLoader = null;
                        }
                    };
                    mLoader.execute();
                }
            }, 250);
        }
    }

    private void load(List<RecyclerViewItem> items) {
        if (!Scripts.ScriptFile().exists()) {
            return;
        }
        for (final String scriptsItems : Scripts.scriptItems()) {
        File scripts = new File(Scripts.ScriptFile() + "/" + scriptsItems);
            if (Scripts.ScriptFile().length() > 0 && Scripts.isScript(scripts.toString())) {
                CardView cardView = new CardView(getActivity());
                cardView.setOnMenuListener(new CardView.OnMenuListener() {
                    @Override
                    public void onMenuReady(CardView cardView, androidx.appcompat.widget.PopupMenu popupMenu) {
                        Menu menu = popupMenu.getMenu();
                        menu.add(Menu.NONE, 0, Menu.NONE, getString(R.string.apply));
                        menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.edit));
                        menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.details));
                        menu.add(Menu.NONE, 3, Menu.NONE, getString(R.string.share));
                        menu.add(Menu.NONE, 4, Menu.NONE, getString(R.string.delete));
                        if (Scripts.isMgiskService()) {
                            MenuItem onBoot = menu.add(Menu.NONE, 5, Menu.NONE, getString(R.string.apply_on_boot)).setCheckable(true);
                            onBoot.setChecked(Scripts.scriptOnBoot(scripts.getName()));
                        }
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    case 0:
                                        new Dialog(getActivity())
                                                .setMessage(getString(R.string.apply_question, scripts.getName().replace(".sh", "")))
                                                .setNegativeButton(getString(R.string.cancel), (dialogInterfacei, ii) -> {
                                                })
                                                .setPositiveButton(getString(R.string.yes), (dialogInterfacei, ii) -> {
                                                    if (!Scripts.isScript(scripts.toString())) {
                                                        Utils.toast(getString(R.string.wrong_script, scripts.getName().replace(".sh", "")), getActivity());
                                                        return;
                                                    }
                                                    new AsyncTask<Void, Void, String>() {
                                                        private ProgressDialog mProgressDialog;
                                                        @Override
                                                        protected void onPreExecute() {
                                                            super.onPreExecute();

                                                            mProgressDialog = new ProgressDialog(getActivity());
                                                            mProgressDialog.setMessage(getString(R.string.applying_script, scripts.getName().replace(".sh", "") + "..."));
                                                            mProgressDialog.setCancelable(false);
                                                            mProgressDialog.show();
                                                        }

                                                        @Override
                                                        protected String doInBackground(Void... voids) {
                                                            return Scripts.applyScript(scripts.toString());
                                                        }

                                                        @Override
                                                        protected void onPostExecute(String s) {
                                                            super.onPostExecute(s);
                                                            try {
                                                                mProgressDialog.dismiss();
                                                            } catch (IllegalArgumentException ignored) {
                                                            }
                                                            if (s != null && !s.isEmpty()) {
                                                                new Dialog(getActivity())
                                                                        .setMessage(s)
                                                                        .setCancelable(false)
                                                                        .setPositiveButton(getString(R.string.cancel), (dialog, id) -> {
                                                                        })
                                                                        .show();
                                                            }
                                                        }
                                                    }.execute();
                                                })
                                                .show();
                                        break;
                                    case 1:
                                        if (Scripts.isMgiskService() && Scripts.scriptOnBoot(scripts.getName())) {
                                            Dialog onbootwarning = new Dialog(getActivity());
                                            onbootwarning.setMessage(getString(R.string.on_boot_warning, scripts.getName()));
                                            onbootwarning.setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> {
                                            });
                                            onbootwarning.setPositiveButton(getString(R.string.edit_anyway), (dialogInterface, i) -> {
                                                showEditDialog(scripts.toString(), scripts.getName());
                                            });
                                            onbootwarning.show();
                                        } else {
                                            showEditDialog(scripts.toString(), scripts.getName());
                                        }
                                        break;
                                    case 2:
                                        new Dialog(getActivity())
                                                .setTitle(scripts.getName().replace(".sh", ""))
                                                .setMessage(Scripts.readScript(scripts.toString()))
                                                .setPositiveButton(getString(R.string.cancel), (dialogInterfacei, ii) -> {
                                                })
                                                .show();
                                        break;
                                    case 3:
                                        Uri uriFile = FileProvider.getUriForFile(getActivity(),
                                                "com.smartpack.scriptmanager.provider", new File(scripts.toString()));
                                        Intent shareScript = new Intent(Intent.ACTION_SEND);
                                        shareScript.setType("application/sh");
                                        shareScript.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.shared_by, scripts.getName()));
                                        shareScript.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message,BuildConfig.VERSION_NAME));
                                        shareScript.putExtra(Intent.EXTRA_STREAM, uriFile);
                                        shareScript.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        startActivity(Intent.createChooser(shareScript, getString(R.string.share_with)));
                                        break;
                                    case 4:
                                        new Dialog(getActivity())
                                                .setMessage(getString(R.string.sure_question, scripts.getName().replace(".sh", "")))
                                                .setNegativeButton(getString(R.string.cancel), (dialogInterfacei, ii) -> {
                                                })
                                                .setPositiveButton(getString(R.string.yes), (dialogInterfacei, ii) -> {
                                                    Scripts.deleteScript(scripts.toString());
                                                    reload();
                                                })
                                                .show();
                                        break;
                                    case 5:
                                        if (Scripts.isMgiskService() && Scripts.scriptOnBoot(scripts.getName())) {
                                            Utils.delete(Scripts.MagiskPostFSFile().toString() + "/" + scripts.getName());
                                            Utils.delete(Scripts.MagiskServiceFile().toString() + "/" + scripts.getName());
                                            Utils.toast(getString(R.string.on_boot_message, scripts.getName()), getActivity());
                                            reload();
                                        } else {
                                            mOptionsDialog = new Dialog(getActivity()).setItems(getResources().getStringArray(
                                                    R.array.onboot_options), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    switch (i) {
                                                        case 0:
                                                            Scripts.setScriptOnPostFS(scripts.toString(), scripts.getName());
                                                            Utils.toast(getString(R.string.post_fs_message, scripts.getName()), getActivity());
                                                            reload();
                                                            break;
                                                        case 1:
                                                            Scripts.setScriptOnServiceD(scripts.toString(), scripts.getName());
                                                            Utils.toast(getString(R.string.late_start_message, scripts.getName()), getActivity());
                                                            reload();
                                                            break;
                                                    }
                                                }
                                            }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                @Override
                                                public void onDismiss(DialogInterface dialogInterface) {
                                                    mOptionsDialog = null;
                                                }
                                            });
                                            mOptionsDialog.show();
                                        }
                                }
                                return false;
                            }
                        });
                    }
                });

                DescriptionView descriptionView = new DescriptionView();
                descriptionView.setDrawable(getResources().getDrawable(R.drawable.ic_shell));
                descriptionView.setSummary(scripts.getName().replace(".sh", ""));

                cardView.addItem(descriptionView);
                items.add(cardView);
            }
        }
        if (items.size() == 0) {
            DescriptionView info = new DescriptionView();
            info.setDrawable(getResources().getDrawable(R.drawable.ic_info));
            info.setTitle(getText(R.string.empty_message));
            info.setOnItemClickListener(new RecyclerViewItem.OnItemClickListener() {
                @Override
                public void onClick(RecyclerViewItem item) {
                    if (!Utils.checkWriteStoragePermission(getActivity())) {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                        Utils.toast(R.string.permission_denied_write_storage, getActivity());
                        return;
                    }

                    showOptions();
                }
            });

            items.add(info);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) return;
        if (requestCode == 0) {
            Scripts.createScript(mEditScript, data.getCharSequenceExtra(EditorActivity.TEXT_INTENT).toString());
            reload();
        } else if (requestCode == 1) {
            Uri uri = data.getData();
            File file = new File(uri.getPath());
            if (Utils.isDocumentsUI(uri)) {
                Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    mPath = Environment.getExternalStorageDirectory().toString() + "/Download/" +
                            cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } else {
                mPath = Utils.getPath(file);
            }
            if (!Utils.getExtension(mPath).equals("sh")) {
                Utils.toast(getString(R.string.wrong_extension, ".sh"), getActivity());
                return;
            }
            if (!Scripts.isScript(mPath)) {
                Utils.toast(getString(R.string.wrong_script, file.getName().replace(".sh", "")), getActivity());
                return;
            }
            if (Utils.existFile(Scripts.scriptExistsCheck(file.getName()))) {
                Utils.toast(getString(R.string.script_exists, file.getName()), getActivity());
                return;
            }
            Dialog selectQuestion = new Dialog(getActivity());
            selectQuestion.setMessage(getString(R.string.select_question, file.getName().replace("primary:", "")
                    .replace("file%3A%2F%2F%2F", "").replace("%2F", "/")));
            selectQuestion.setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> {
            });
            selectQuestion.setPositiveButton(getString(R.string.yes), (dialogInterface, i) -> {
                Scripts.importScript(mPath);
                reload();
            });
            selectQuestion.show();
        } else if (requestCode == 2) {
            Scripts.createScript(mCreateName, data.getCharSequenceExtra(EditorActivity.TEXT_INTENT).toString());
            mCreateName = null;
            reload();
        }
    }

    @Override
    protected void onBottomFabClick() {
        super.onBottomFabClick();

        if (!Utils.checkWriteStoragePermission(getActivity())) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
            Utils.toast(R.string.permission_denied_write_storage, getActivity());
            return;
        }

        showOptions();
    }

    private void showOptions() {
        mOptionsDialog = new Dialog(getActivity()).setItems(getResources().getStringArray(
                R.array.script_options), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0:
                        showCreateDialog();
                        break;
                    case 1:
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");
                        startActivityForResult(intent, 1);
                        break;
                }
            }
        }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                mOptionsDialog = null;
            }
        });
        mOptionsDialog.show();
    }

    private void showEditDialog(String string, String name) {
        mEditScript = string;
        Intent intent = new Intent(getActivity(), EditorActivity.class);
        intent.putExtra(EditorActivity.TITLE_INTENT, name);
        intent.putExtra(EditorActivity.TEXT_INTENT, Scripts.readScript(string));
        startActivityForResult(intent, 0);
    }

    private void showCreateDialog() {
        mShowCreateNameDialog = true;
        ViewUtils.dialogEditText("",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }, new ViewUtils.OnDialogEditTextListener() {
                    @Override
                    public void onClick(String text) {
                        if (text.isEmpty()) {
                            Utils.toast(R.string.name_empty, getActivity());
                            return;
                        }
                        if (!text.endsWith(".sh")) {
                            text += ".sh";
                        }
                        if (text.contains(" ")) {
                            text = text.replace(" ", "_");
                        }
                        if (Utils.existFile(Scripts.scriptExistsCheck(text))) {
                            Utils.toast(getString(R.string.script_exists, text), getActivity());
                            return;
                        }
                        mCreateName = Utils.getInternalDataStorage() + "/" + text;
                        Intent intent = new Intent(getActivity(), EditorActivity.class);
                        intent.putExtra(EditorActivity.TITLE_INTENT, text);
                        intent.putExtra(EditorActivity.TEXT_INTENT, "#!/system/bin/sh\n\n");
                        startActivityForResult(intent, 2);
                    }
                }, getActivity()).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                mShowCreateNameDialog = false;
            }
        }).show();
    }

    /*
     * Taken and used almost as such from https://github.com/morogoku/MTweaks-KernelAdiutorMOD/
     * Ref: https://github.com/morogoku/MTweaks-KernelAdiutorMOD/blob/dd5a4c3242d5e1697d55c4cc6412a9b76c8b8e2e/app/src/main/java/com/moro/mtweaks/fragments/kernel/BoefflaWakelockFragment.java#L133
     */
    private void WelcomeDialog() {
        View checkBoxView = View.inflate(getActivity(), R.layout.rv_checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.checkbox);
        checkBox.setChecked(true);
        checkBox.setText(getString(R.string.always_show));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked)
                -> mWelcomeDialog = isChecked);

        Dialog alert = new Dialog(Objects.requireNonNull(getActivity()));
        alert.setIcon(R.mipmap.ic_launcher);
        alert.setTitle(getString(R.string.app_name));
        alert.setMessage(getText(R.string.welcome_message));
        alert.setCancelable(false);
        alert.setView(checkBoxView);
        alert.setNegativeButton(getString(R.string.cancel), (dialog, id) -> {
        });
        alert.setNeutralButton(getString(R.string.examples), (dialog, id) -> {
            Utils.launchUrl("https://github.com/SmartPack/ScriptManager/tree/master/examples", getActivity());
        });
        alert.setPositiveButton(getString(R.string.got_it), (dialog, id)
                -> Prefs.saveBoolean("welcomeMessage", mWelcomeDialog, getActivity()));

        alert.show();
    }

    @Override
    public void onStart(){
        super.onStart();
        if (Prefs.getBoolean("welcomeMessage", true, getActivity())) {
            WelcomeDialog();
        }
        if (!Utils.checkWriteStoragePermission(getActivity())) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
            return;
        }
        if (UpdateCheck.isPlayStoreInstalled(getActivity())) {
            return;
        }
        if (!Utils.isNetworkAvailable(getActivity())) {
            return;
        }
        if (!UpdateCheck.hasVersionInfo() || (UpdateCheck.lastModified() + 3720000L < System.currentTimeMillis())) {
            UpdateCheck.getVersionInfo();
        }
        if (UpdateCheck.hasVersionInfo() && BuildConfig.VERSION_CODE < UpdateCheck.versionNumber()) {
            UpdateCheck.updateAvailableDialog(getActivity());
        }
    }

}