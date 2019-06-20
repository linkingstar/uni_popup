package com.linkstar.popup;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.annotation.JSMethod;

import java.util.ArrayList;
import java.util.List;

public class GlobalPopupModule extends WXSDKEngine.DestroyableModule {

    public String DATA = "data";
    public String TITLE = "title";

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private List<ContentInfo> infoList = new ArrayList<>();
    private Point screenSize = new Point();
    private LinearLayout infoLayout;
    private boolean showInfo = false;
    private View popupView;


    private boolean checkCanPopUp(Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                context.startActivityForResult(intent, 10);
                return false;
            }
        }
        return true;
    }


    @JSMethod(uiThread = true)
    public void showPopup(JSONObject options) {
        if (mWXSDKInstance.getContext() instanceof Activity) {
            final Activity context = (Activity) mWXSDKInstance.getContext();
            if (!checkCanPopUp(context)) {
                return;
            }


            infoList.clear();

            JSONArray array = options.getJSONArray(DATA);
            for (int i = 0; i < array.size(); i++) {
                infoList.add(array.getObject(i, ContentInfo.class));
            }
            hideInfoList();
            String title = options.getString(TITLE);
            if (title == null) {
                title = "钱";
            }
            windowManager = context.getWindowManager();
            windowManager.getDefaultDisplay().getSize(screenSize);


            if (layoutParams == null) {
                WindowManager.LayoutParams params = new WindowManager.LayoutParams();
                params.width = 120;
                params.height = 120;
                params.format = PixelFormat.TRANSPARENT;
                params.gravity = Gravity.CENTER;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                } else {
                    params.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
                }
                params.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                params.x = screenSize.x / 2 - 120;
                params.y = screenSize.y / 2 - 140;
                layoutParams = params;
            }

            if (popupView == null) {
                popupView = View.inflate(context, R.layout.window_view, null);
                View textView = popupView.findViewById(R.id.textView);
                textView.setBackgroundResource(R.color.transparent);
                windowManager.addView(popupView, layoutParams);
                popupView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        windowManager.removeViewImmediate(view);
                    }
                });
                popupView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!showInfo) {
                            showInfoList(context);
                        } else {
                            hideInfoList();
                        }
                    }
                });
                popupView.setOnTouchListener(new View.OnTouchListener() {
                    int lastX = 0;
                    int lastY = 0;
                    int paramX = 0;
                    int paramY = 0;
                    long lastTime = 0;

                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                lastX = (int) motionEvent.getRawX();
                                lastY = (int) motionEvent.getRawY();
                                lastTime = System.currentTimeMillis();
                                paramX = layoutParams.x;
                                paramY = layoutParams.y;
                                break;
                            case MotionEvent.ACTION_MOVE:
                                int dx = (int) motionEvent.getRawX() - lastX;
                                int dy = (int) motionEvent.getRawY() - lastY;
                                layoutParams.x = paramX + dx;
                                layoutParams.y = paramY + dy;
                                windowManager.updateViewLayout(view, layoutParams);
                                break;
                            case MotionEvent.ACTION_UP:
                                if (System.currentTimeMillis() - lastTime < 100) {
                                    view.performClick();
                                }
                                break;
                        }
                        return true;
                    }
                });


            }

            TextView textView = popupView.findViewById(R.id.textView);
            textView.setText(title);
        }
    }

    private WindowManager.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.format = PixelFormat.TRANSPARENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        }
        params.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        return params;
    }

    private float statusHieght(Activity context) {
        // status bar height
        int statusBarHeight = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }

        return statusBarHeight;
    }

    private void showInfoList(final Activity context) {

        if (infoList.size() == 0) {
            return;
        }

        if (infoLayout != null) {
            windowManager.removeView(infoLayout);
        }

        WindowManager.LayoutParams params = getLayoutParams();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;

        infoLayout = new LinearLayout(context);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setBackgroundColor(context.getResources().getColor(R.color.bgTransColor));
        infoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideInfoList();
            }
        });

        LinearLayout innerLayout = new LinearLayout(context);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setBackgroundColor(Color.WHITE);
        innerLayout.setPadding(18, 18, 18, 0);
        LinearLayout.LayoutParams innerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        innerParams.setMargins(18, 18, 18, 18);
        infoLayout.addView(innerLayout, innerParams);

        for (final ContentInfo info : infoList) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            p.bottomMargin = 18;

            View item = View.inflate(context, R.layout.item, null);
            TextView textView = item.findViewById(R.id.itemLabel);
            textView.setText(String.format("%s:%s", info.key, info.value));
            item.findViewById(R.id.copyButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyData(context, info.value);
                }
            });

            innerLayout.addView(item, p);
        }

        windowManager.addView(infoLayout, params);
        showInfo = true;
    }

    private void copyData(Context context, String value) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(value, value);
        clipboard.setPrimaryClip(clip);
    }

    private void hideInfoList() {
        if (infoLayout != null) {
            windowManager.removeView(infoLayout);
            infoLayout = null;
        }
        showInfo = false;
    }

    @JSMethod(uiThread = true)
    public void hidePopup() {
        destroy();
    }

    @Override
    public void destroy() {
        if (popupView != null)
            windowManager.removeView(popupView);
        if (infoLayout != null)
            windowManager.removeView(infoLayout);
        popupView = null;
        infoLayout = null;
    }
}
