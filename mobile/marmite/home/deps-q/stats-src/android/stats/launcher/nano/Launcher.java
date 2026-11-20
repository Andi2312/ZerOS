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

package android.stats.launcher.nano;

/**
 * Constants of the launcher stats protos
 * (frameworks/base/core/proto/android/stats/launcher/launcher.proto).
 *
 * That proto sets java_multiple_files, so protoc turns its top level enums into
 * the separate interfaces LauncherAction and LauncherState. The stats jar that
 * ships with the platform also exposes them through one {@code Launcher}
 * interface, which is what Launcher3 imports (for example
 * {@code import static android.stats.launcher.nano.Launcher.ALLAPPS}). Extending
 * both interfaces gives exactly those constants, kept in sync with the proto.
 */
public interface Launcher extends LauncherAction, LauncherState {
}
