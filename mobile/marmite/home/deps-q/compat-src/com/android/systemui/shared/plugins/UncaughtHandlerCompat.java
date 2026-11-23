/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.systemui.shared.plugins;

import java.lang.reflect.Method;

/**
 * Build-time shim for the hidden libcore API used by {@link PluginManagerImpl}.
 *
 * {@code Thread.getUncaughtExceptionPreHandler()} and
 * {@code Thread.setUncaughtExceptionPreHandler()} exist in Android's libcore, but
 * the platform builds libcore from source, while this Gradle-free build compiles
 * against a plain JDK whose {@code java.lang.Thread} has no such methods. The
 * calls are made reflectively so the class compiles and keeps the same runtime
 * behaviour on the device.
 */
public final class UncaughtHandlerCompat {

    private UncaughtHandlerCompat() {
    }

    public static Thread.UncaughtExceptionHandler getPreHandler() {
        try {
            Method m = Thread.class.getDeclaredMethod("getUncaughtExceptionPreHandler");
            m.setAccessible(true);
            return (Thread.UncaughtExceptionHandler) m.invoke(null);
        } catch (Throwable t) {
            // Hidden API restriction or an older runtime: no previous handler.
            return null;
        }
    }

    public static void setPreHandler(Thread.UncaughtExceptionHandler handler) {
        try {
            Method m = Thread.class.getDeclaredMethod("setUncaughtExceptionPreHandler",
                    Thread.UncaughtExceptionHandler.class);
            m.setAccessible(true);
            m.invoke(null, handler);
        } catch (Throwable t) {
            // Hidden API restriction: the plugin crash handler just is not installed.
        }
    }
}
