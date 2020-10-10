package xlk.sample.mediacodecdemo.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Method;

import androidx.annotation.NonNull;

import static android.view.View.NO_ID;

/**
 * @author Created by xlk on 2020/9/27.
 * @desc
 */
public class ScreenUtils {
    private static final String TAG = "ScreenUtils-->";

    public static void all(Activity activity) {
        Log.d(TAG, "############################");
        isVertical(activity);
        getScreenWidth(activity);
        getScreenHeight(activity);
        getStatusBarHeight(activity);
        getTitleBarHeight(activity);
        isNavigationBarShown(activity);
        getVirtualBarHeight(activity);
        getNavigationBarHeight(activity);
        checkNavigationBarShow(activity);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 获得屏幕宽度
     */
    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        Log.i(TAG, "屏幕宽度：" + outMetrics.widthPixels);
        return outMetrics.widthPixels;
    }

    /**
     * 获得屏幕高度
     */
    public static int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        Log.i(TAG, "屏幕高度：" + outMetrics.heightPixels);
        return outMetrics.heightPixels;
    }

    /**
     * 获得状态栏的高度
     */
    public static int getStatusBarHeight(Context context) {
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
        Log.i(TAG, "状态栏高度：" + statusHeight);
        return statusHeight;
    }

    /**
     * 标题栏高度
     */
    public static int getTitleBarHeight(Activity activity) {
        int top = activity.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
        Log.i(TAG, "标题栏高度：" + top);
        return top;
    }

    /**
     * 获取虚拟功能键高度，只有屏幕垂直时才有效
     */
    public static int getVirtualBarHeight(Context context) {
        int vh = 0;
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        try {
            @SuppressWarnings("rawtypes")
            Class c = Class.forName("android.view.Display");
            @SuppressWarnings("unchecked")
            Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
            method.invoke(display, dm);
            vh = dm.heightPixels - display.getHeight();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "垂直虚拟功能键高度：" + vh);
        return vh;
    }

    /**
     * 获取虚拟键高度(无论是否隐藏)
     *
     */
    public static int getNavigationBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        Log.i(TAG, "虚拟功能键高度：" + result);
        return result;
    }

    /**
     * 判断当前是否是竖屏方向
     *
     * @return =true 竖屏，=false 横屏
     */
    public static boolean isVertical(Context context) {
        boolean b = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        Log.i(TAG, "设备屏幕方向：" + (b ? "竖屏" : "横屏"));
        return b;
    }

    /**
     * 虚拟按键是否打开
     */
    public static boolean isNavigationBarShown(Activity activity) {
        boolean isShow = false;
        //虚拟键的view,为空或者不可见时是隐藏状态
        View view = activity.findViewById(android.R.id.navigationBarBackground);
        if (view != null) {
            int visible = view.getVisibility();
            isShow = visible != View.GONE && visible != View.INVISIBLE;
        }
        Log.i(TAG, "虚拟按键是否打开：" + isShow);
        return isShow;
    }

    /**
     * 判断虚拟导航栏是否显示
     *
     * @param context 上下文对象
     * @return true(显示虚拟导航栏)，false(不显示或不支持虚拟导航栏)
     */
    public static boolean checkNavigationBarShow(@NonNull Context context) {
        boolean hasNavigationBar = false;
        Resources rs = context.getResources();
        int id = rs.getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0) {
            hasNavigationBar = rs.getBoolean(id);
        }
        try {
            Class systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method m = systemPropertiesClass.getMethod("get", String.class);
            String navBarOverride = (String) m.invoke(systemPropertiesClass, "qemu.hw.mainkeys");
            //判断是否隐藏了底部虚拟导航
            int navigationBarIsMin = 0;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                navigationBarIsMin = Settings.System.getInt(context.getContentResolver(),
                        "navigationbar_is_min", 0);
            } else {
                navigationBarIsMin = Settings.Global.getInt(context.getContentResolver(),
                        "navigationbar_is_min", 0);
            }
            if ("1".equals(navBarOverride) || 1 == navigationBarIsMin) {
                hasNavigationBar = false;
            } else if ("0".equals(navBarOverride)) {
                hasNavigationBar = true;
            }
        } catch (Exception e) {
        }
        Log.i(TAG, "checkNavigationBarShow 判断虚拟导航栏是否显示:" + hasNavigationBar);
        return hasNavigationBar;
    }

}
