package com.morrisware.flutter.net.utils;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * @author mmw
 * @date 2020/3/11
 **/
public class SizeDeterminer {

    private static Integer maxDisplayLength;

    public static int getTargetHeight(View view) {
        int verticalPadding = view.getPaddingTop() + view.getPaddingBottom();
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        int layoutParamSize = layoutParams != null ? layoutParams.height : 0;
        return getTargetDimen(view, view.getHeight(), layoutParamSize, verticalPadding);
    }

    public static int getTargetWidth(View view) {
        int horizontalPadding = view.getPaddingLeft() + view.getPaddingRight();
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        int layoutParamSize = layoutParams != null ? layoutParams.width : 0;
        return getTargetDimen(view, view.getWidth(), layoutParamSize, horizontalPadding);
    }

    private static int getTargetDimen(View view, int viewSize, int paramSize, int paddingSize) {
        // We consider the View state as valid if the View has non-null layout params and a non-zero
        // layout params width and height. This is imperfect. We're making an assumption that View
        // parents will obey their child's layout parameters, which isn't always the case.
        int adjustedParamSize = paramSize - paddingSize;
        if (adjustedParamSize > 0) {
            return adjustedParamSize;
        }

        // We also consider the View state valid if the View has a non-zero width and height. This
        // means that the View has gone through at least one layout pass. It does not mean the Views
        // width and height are from the current layout pass. For example, if a View is re-used in
        // RecyclerView or ListView, this width/height may be from an old position. In some cases
        // the dimensions of the View at the old position may be different than the dimensions of the
        // View in the new position because the LayoutManager/ViewParent can arbitrarily decide to
        // change them. Nevertheless, in most cases this should be a reasonable choice.
        int adjustedViewSize = viewSize - paddingSize;
        if (adjustedViewSize > 0) {
            return adjustedViewSize;
        }

        // Finally we consider the view valid if the layout parameter size is set to wrap_content.
        // It's difficult for Glide to figure out what to do here. Although Target.SIZE_ORIGINAL is a
        // coherent choice, it's extremely dangerous because original images may be much too large to
        // fit in memory or so large that only a couple can fit in memory, causing OOMs. If users want
        // the original image, they can always use .override(Target.SIZE_ORIGINAL). Since wrap_content
        // may never resolve to a real size unless we load something, we aim for a square whose length
        // is the largest screen size. That way we're loading something and that something has some
        // hope of being downsampled to a size that the device can support. We also log a warning that
        // tries to explain what Glide is doing and why some alternatives are preferable.
        // Since WRAP_CONTENT is sometimes used as a default layout parameter, we always wait for
        // layout to complete before using this fallback parameter (ConstraintLayout among others).
        if (!view.isLayoutRequested() && paramSize == ViewGroup.LayoutParams.WRAP_CONTENT) {
            return getMaxDisplayLength(view.getContext());
        }

        // If the layout parameters are < padding, the view size is < padding, or the layout
        // parameters are set to match_parent or wrap_content and no layout has occurred, we should
        // wait for layout and repeat.
        return 0;
    }

    private static int getMaxDisplayLength(Context context) {
        if (maxDisplayLength == null) {
            WindowManager windowManager =
                    (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager != null) {
                Display display = windowManager.getDefaultDisplay();
                Point displayDimensions = new Point();
                display.getSize(displayDimensions);
                maxDisplayLength = Math.max(displayDimensions.x, displayDimensions.y);
            }
        }
        return maxDisplayLength;
    }


}
