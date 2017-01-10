package com.gaga.zxingscan;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

/**
 * Created by 54506 on 2016/5/5.
 * 屏幕相关的工具
 */
public class ScreenUtil {

    private static WindowManager mWindowManager;
    private static Display mDefaultDisplay;
    private static DisplayMetrics mDisplayMetrics;

    private ScreenUtil() {
        /* cannot be instantiated */
        throw new UnsupportedOperationException("cannot be instantiated");
    }


    /**
     * 获取屏幕相关参数
     *
     * @param context 上下文
     * @return DisplayMetrics 屏幕宽高
     */
    public static DisplayMetrics getScreenSize(Context context) {
        getDisplayMetrics(context);
        return mDisplayMetrics;
    }

    /**
     * 获取屏幕的density
     *
     * @param context 上下文
     * @return 屏幕density
     */
    public static float getDeviceDensity(Context context) {
        getDisplayMetrics(context);
        return mDisplayMetrics.density;
    }

    /**
     * 获得屏幕高度
     *
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context) {
        getDisplayMetrics(context);
        return mDisplayMetrics.widthPixels;
    }

    /**
     * 获得屏幕宽度
     *
     * @param context
     * @return
     */
    public static int getScreenHeight(Context context) {
        getDisplayMetrics(context);
        return mDisplayMetrics.heightPixels;
    }

    /**
     * 获得状态栏的高度
     *
     * @param context
     * @return
     */
    public static int getStatusHeight(Context context) {
        int statusHeight = -1;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height")
                    .get(object).toString());
            statusHeight = context.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusHeight;
    }

    /**
     * 获取当前屏幕截图，包含状态栏
     *
     * @param activity
     * @return
     */
    public static Bitmap snapShotWithStatusBar(Activity activity) {
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bmp = view.getDrawingCache();
        int width = getScreenWidth(activity);
        int height = getScreenHeight(activity);
        Bitmap bp = null;
        bp = Bitmap.createBitmap(bmp, 0, 0, width, height);
        view.destroyDrawingCache();
        return bp;

    }

    /**
     * 获取当前屏幕截图，不包含状态栏
     *
     * @param activity
     * @return
     */
    public static Bitmap snapShotWithoutStatusBar(Activity activity) {
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bmp = view.getDrawingCache();
        Rect frame = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        int statusBarHeight = frame.top;

        int width = getScreenWidth(activity);
        int height = getScreenHeight(activity);
        Bitmap bp = null;
        bp = Bitmap.createBitmap(bmp, 0, statusBarHeight, width, height
                - statusBarHeight);
        view.destroyDrawingCache();
        return bp;
    }

    /**
     * 将一个view根据屏幕大小按指定比例缩放
     *
     * @param context     上下文
     * @param view        要缩放的view
     * @param widthScale  宽度的缩放比例
     * @param heightScale 高度缩放比例
     */
    public static void setViewDisplayScale(@NonNull Context context, @NonNull View view, float widthScale, float heightScale) {
        getDisplay(context);
        LayoutParams lp = view.getLayoutParams();
        if (lp == null) {
            throw new UnsupportedOperationException("LayoutParams can not be null");
        }
        lp.width = (int) (mDefaultDisplay.getWidth() * widthScale);
        lp.height = (int) (mDefaultDisplay.getHeight() * heightScale);
        view.setLayoutParams(lp);
    }

    /**
     * 将一个view根据屏幕大小按指定比例缩放
     *
     * @param context 上下文
     * @param view    要缩放的view
     * @param scale   缩放比例
     */
    public static void setViewDisplayScale(Context context, View view, float scale) {
        setViewDisplayScale(context, view, scale, scale);
    }


    /**
     * 获得WindowManager
     *
     * @param context 上下文
     * @return WindowManager
     */
    public static WindowManager getWindowManager(Context context) {
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        }
        return mWindowManager;
    }

    /**
     * 获得Display
     *
     * @param context 上下文
     * @return Display
     */
    public static Display getDisplay(Context context) {
        if (mWindowManager == null) {
            getWindowManager(context);
        }
        if (mDefaultDisplay == null) {
            mDefaultDisplay = mWindowManager.getDefaultDisplay();
        }
        return mDefaultDisplay;
    }

    /**
     * 获得DisplayMetrics
     *
     * @param context 上下文
     * @return DisplayMetrics
     */
    public static DisplayMetrics getDisplayMetrics(Context context) {
        if (mDisplayMetrics == null) {
            mDisplayMetrics = new DisplayMetrics();
            if (mDefaultDisplay == null) {
                getDisplay(context);
            }
            mDefaultDisplay.getMetrics(mDisplayMetrics);
        }
        return mDisplayMetrics;
    }

    public static Bitmap view2bitmap(View view) {
        view.setDrawingCacheEnabled(true);
        view.measure(View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.buildDrawingCache();
        return view.getDrawingCache();
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        return (int) (dpValue * getDisplayMetrics(context).density + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        return (int) (pxValue / getDisplayMetrics(context).density + 0.5f);
    }
}
