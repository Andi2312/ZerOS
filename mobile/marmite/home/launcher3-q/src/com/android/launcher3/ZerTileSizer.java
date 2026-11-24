/*
 * ZerOS: resize a workspace shortcut into a tile.
 *
 * The grid geometry lives in CellLayout.LayoutParams, while the persistent
 * values live in WorkspaceItemInfo (spanX/spanY/cellX/cellY).  A single change
 * has to touch both, update the occupancy map and write the row back to the
 * favourites database, otherwise the size is lost on the next restart.
 */
package com.android.launcher3;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public final class ZerTileSizer {

    private ZerTileSizer() {
    }

    public static boolean apply(Launcher launcher, WorkspaceItemInfo info, int spanX, int spanY) {
        Workspace workspace = launcher.getWorkspace();
        CellLayout layout = workspace.getScreenWithId(info.screenId);
        if (layout == null) {
            return false;
        }
        if (spanX > layout.getCountX() || spanY > layout.getCountY()) {
            Toast.makeText(launcher, R.string.tile_no_room, Toast.LENGTH_SHORT).show();
            return false;
        }
        View view = findView(layout, info);
        if (view == null || !(view.getLayoutParams() instanceof CellLayout.LayoutParams)) {
            return false;
        }

        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view.getLayoutParams();
        int cellX = lp.cellX;
        int cellY = lp.cellY;
        if (!layout.isRegionFreeForView(cellX, cellY, spanX, spanY, view)) {
            int[] target = new int[2];
            if (!layout.findNearestFreeRegion(cellX, cellY, spanX, spanY, view, target)) {
                Toast.makeText(launcher, R.string.tile_no_room, Toast.LENGTH_SHORT).show();
                return false;
            }
            cellX = target[0];
            cellY = target[1];
        }

        layout.markCellsAsUnoccupiedForView(view);
        lp.cellX = cellX;
        lp.cellY = cellY;
        lp.cellHSpan = spanX;
        lp.cellVSpan = spanY;
        layout.markCellsAsOccupiedForView(view);

        info.cellX = cellX;
        info.cellY = cellY;
        info.spanX = spanX;
        info.spanY = spanY;

        view.requestLayout();
        layout.requestLayout();
        view.invalidate();
        launcher.getModelWriter().updateItemInDatabase(info);
        return true;
    }

    private static View findView(CellLayout layout, ItemInfo info) {
        ViewGroup container = layout.getShortcutsAndWidgets();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child.getTag() == info) {
                return child;
            }
        }
        View child = layout.getChildAt(info.cellX, info.cellY);
        if (child != null && child.getTag() instanceof ItemInfo
                && ((ItemInfo) child.getTag()).id == info.id) {
            return child;
        }
        return null;
    }
}
