/*
 * ZerOS: "Tile size" entry in the long-press menu of a workspace icon.
 *
 * Windows Phone lets a tile be small, wide, medium or large; here the same
 * choice is offered as a dialog with 1x1, 2x1, 2x2 and 4x2.  The actual
 * relayout is done by {@link com.android.launcher3.ZerTileSizer}.
 */
package com.android.launcher3.popup;

import android.app.AlertDialog;
import android.view.View;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.WorkspaceItemInfo;
import com.android.launcher3.ZerTileSizer;

public class ZerTileShortcut extends SystemShortcut<Launcher> {

    private static final int[][] TILE_SIZES = {{1, 1}, {2, 1}, {2, 2}, {4, 2}};

    public ZerTileShortcut() {
        super(R.drawable.zer_ic_tile_size, R.string.tile_size);
    }

    @Override
    public View.OnClickListener getOnClickListener(final Launcher launcher,
            final ItemInfo itemInfo) {
        if (!(itemInfo instanceof WorkspaceItemInfo)
                || itemInfo.container != LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            return null;
        }
        final WorkspaceItemInfo info = (WorkspaceItemInfo) itemInfo;
        return view -> showSizeDialog(launcher, info);
    }

    private void showSizeDialog(final Launcher launcher, final WorkspaceItemInfo info) {
        AbstractFloatingView.closeAllOpenViews(launcher);
        int checked = 0;
        for (int i = 0; i < TILE_SIZES.length; i++) {
            if (TILE_SIZES[i][0] == info.spanX && TILE_SIZES[i][1] == info.spanY) {
                checked = i;
                break;
            }
        }
        new AlertDialog.Builder(launcher)
                .setTitle(R.string.tile_size)
                .setSingleChoiceItems(R.array.tile_size_options, checked,
                        (dialog, which) -> {
                            dialog.dismiss();
                            ZerTileSizer.apply(launcher, info,
                                    TILE_SIZES[which][0], TILE_SIZES[which][1]);
                        })
                .show();
    }
}
