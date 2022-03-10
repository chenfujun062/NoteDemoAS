/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.rockchip.notedemo.eink;

import android.util.Log;
import com.rockchip.notedemo.util.PropertyUtil;

/**
 * 刷新管理器
 */
public class EinkManager {

    private static final String TAG = "EinkManager";
    private static final boolean DEBUG = true;
    private static boolean EINK = true;
    private static int num = 1;
    private static EinkManager einkManager;

    public class EinkMode {

        private EinkMode() {
        }

        public static final String EPD_NULL = "-1";
        public static final String EPD_AUTO = "0";
        public static final String EPD_OVERLAY = "1";
        public static final String EPD_FULL_GC16 = "2";
        public static final String EPD_FULL_GL16 = "3";
        public static final String EPD_FULL_GLR16 = "4";
        public static final String EPD_FULL_GLD16 = "5";
        public static final String EPD_FULL_GCC16 = "6";
        public static final String EPD_PART_GC16 = "7";
        public static final String EPD_PART_GL16 = "8";
        public static final String EPD_PART_GLR16 = "9";
        public static final String EPD_PART_GLD16 = "10";
        public static final String EPD_PART_GCC16 = "11";
        public static final String EPD_A2 = "12";
        public static final String EPD_A2_DITHER = "13";
        public static final String EPD_DU = "14";
        public static final String EPD_DU4 = "15";
        public static final String EPD_A2_ENTER = "16";
        public static final String EPD_RESET = "17";
        public static final String EPD_AUTO_DU = "22";
        public static final String EPD_AUTO_DU4 = "23";
    }


    private static void LOG(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }

    private EinkManager() {

    }

    public static EinkManager getInstance() {
        if (einkManager == null) {
            synchronized (EinkManager.class) {
                if (einkManager == null) {
                    einkManager = new EinkManager();
                }
            }
        }
        return einkManager;
    }

    public void sendOneFullFrame() {
        LOG("sendOneFullFrame");
        num = ++num;
        if (num > Integer.MAX_VALUE - 100) {
            num = 1;
        }
        String numStr = num + "";
        PropertyUtil.setProperty("sys.eink.one_full_mode_timeline", numStr);
    }

    public void setMode(String einkMode) {
        LOG("EinkManager.setMode");
        PropertyUtil.setProperty("sys.eink.mode", einkMode);
    }

    public String getMode() {
        LOG("EinkManager.getMode");
        return PropertyUtil.getProperty("sys.eink.mode", EinkMode.EPD_PART_GC16);
    }

}
