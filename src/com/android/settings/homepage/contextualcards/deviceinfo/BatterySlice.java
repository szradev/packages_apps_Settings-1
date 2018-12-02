/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.homepage.contextualcards.deviceinfo;

import static com.android.settings.slices.CustomSliceRegistry.BATTERY_INFO_SLICE_URI;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.PowerManager;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBuilderUtils;

/**
 * Utility class to build a Battery Slice, and handle all associated actions.
 */
public class BatterySlice implements CustomSliceable {
    private static final String TAG = "BatterySlice";

    private final Context mContext;

    private BatteryInfo mBatteryInfo;
    private boolean mIsBatteryInfoLoading;

    public BatterySlice(Context context) {
        mContext = context;
    }

    @Override
    public Slice getSlice() {
        if (mBatteryInfo == null) {
            mIsBatteryInfoLoading = true;
            loadBatteryInfo();
        }
        final IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings_battery);
        final CharSequence title = mContext.getText(R.string.power_usage_summary_title);
        final SliceAction primarySliceAction = SliceAction.createDeeplink(getPrimaryAction(), icon,
                ListBuilder.ICON_IMAGE, title);
        final Slice slice = new ListBuilder(mContext, BATTERY_INFO_SLICE_URI, ListBuilder.INFINITY)
                .setAccentColor(Utils.getColorAccentDefaultColor(mContext))
                .setHeader(new ListBuilder.HeaderBuilder().setTitle(title))
                .addRow(new ListBuilder.RowBuilder()
                        .setTitle(getBatteryPercentString(), mIsBatteryInfoLoading)
                        .setSubtitle(getSummary(), mIsBatteryInfoLoading)
                        .setPrimaryAction(primarySliceAction))
                .build();
        mBatteryInfo = null;
        mIsBatteryInfoLoading = false;
        return slice;
    }

    @Override
    public Uri getUri() {
        return BATTERY_INFO_SLICE_URI;
    }

    @Override
    public void onNotifyChange(Intent intent) {

    }

    @Override
    public Intent getIntent() {
        final String screenTitle = mContext.getText(R.string.power_usage_summary_title).toString();
        return SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                PowerUsageSummary.class.getName(), "" /* key */, screenTitle,
                MetricsProto.MetricsEvent.SLICE)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName());
    }

    @Override
    public IntentFilter getIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        intentFilter.addAction(Intent.ACTION_BATTERY_LEVEL_CHANGED);
        return intentFilter;
    }

    @VisibleForTesting
    void loadBatteryInfo() {
        BatteryInfo.getBatteryInfo(mContext, info -> {
            mBatteryInfo = info;
            mContext.getContentResolver().notifyChange(getUri(), null);
        }, true);
    }

    @VisibleForTesting
    CharSequence getBatteryPercentString() {
        return mBatteryInfo == null ? null : mBatteryInfo.batteryPercentString;
    }

    @VisibleForTesting
    CharSequence getSummary() {
        if (mBatteryInfo == null) {
            return null;
        }
        return mBatteryInfo.remainingLabel == null ? mBatteryInfo.statusLabel
                : mBatteryInfo.remainingLabel;
    }

    private PendingIntent getPrimaryAction() {
        final Intent intent = getIntent();
        return PendingIntent.getActivity(mContext, 0 /* requestCode */,
                intent, 0 /* flags */);
    }
}