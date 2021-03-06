/*
 * Copyright (C) 2020-2021 sunilpaulmathew <sunil.kde@gmail.com>
 *
 * This file is part of Script Manager, an app to create, import, edit
 * and easily execute any properly formatted shell scripts.
 *
 */

package com.smartpack.scriptmanager.utils;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.smartpack.scriptmanager.BuildConfig;
import com.smartpack.scriptmanager.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/*
 * Created by sunilpaulmathew <sunil.kde@gmail.com> on October 05, 2020
 */

public class AboutActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        AppCompatImageView mDeveloper = findViewById(R.id.developer);
        AppCompatTextView mCancelButton = findViewById(R.id.cancel_button);
        AppCompatTextView mAppTitle = findViewById(R.id.app_title);
        AppCompatTextView mChangeLog = findViewById(R.id.changelog);
        mDeveloper.setOnClickListener(v -> {
            Utils.launchUrl("https://github.com/sunilpaulmathew", this);
        });
        mAppTitle.setText(getString(R.string.app_name) + " v" + BuildConfig.VERSION_NAME);
        String change_log = null;
        try {
            change_log = new JSONObject(Objects.requireNonNull(Utils.readAssetFile(
                    this, "update_info.json"))).getString("changelogFull");
        } catch (JSONException ignored) {
        }
        mChangeLog.setText(change_log);
        mCancelButton.setOnClickListener(v -> onBackPressed());
    }

}