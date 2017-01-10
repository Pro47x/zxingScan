/*
 * Copyright (C) 2008 ZXing authors
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

package com.gaga.zxingscan;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.gaga.zxingscan.camera.IScanManage;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.ResultPointCallback;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * This thread does all the heavy lifting of decoding the images.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class DecodeThread extends Thread {

    public static final String BARCODE_BITMAP = "barcode_bitmap";
    public static final String BARCODE_SCALED_FACTOR = "barcode_scaled_factor";
    private final Map<DecodeHintType, Object> hints;

    private final IScanManage cameraScanManage;
    private final CountDownLatch handlerInitLatch;
    private Handler handler;

    DecodeThread(IScanManage cameraScanManage,
                 Collection<BarcodeFormat> decodeFormats,
                 Map<DecodeHintType, ?> baseHints,
                 String characterSet,
                 ResultPointCallback resultPointCallback) {
        this.cameraScanManage = cameraScanManage;
        handlerInitLatch = new CountDownLatch(1);
        hints = new EnumMap<>(DecodeHintType.class);
        if (baseHints != null) {
            hints.putAll(baseHints);
        }
        // The prefs can't change while the thread is running, so pick them up once here.
        if (decodeFormats == null || decodeFormats.isEmpty()) {
            decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
            decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.AZTEC_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.PDF417_FORMATS);
        }
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

        if (characterSet != null) {
            hints.put(DecodeHintType.CHARACTER_SET, characterSet);
        }

        hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback);
    }

    Handler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new DecodeHandler(cameraScanManage, hints);
        handlerInitLatch.countDown();
        Looper.loop();
    }

}
