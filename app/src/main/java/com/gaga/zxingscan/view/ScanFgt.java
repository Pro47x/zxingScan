package com.gaga.zxingscan.view;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.gaga.zxingscan.AmbientLightManager;
import com.gaga.zxingscan.BeepManager;
import com.gaga.zxingscan.CaptureHandler;
import com.gaga.zxingscan.FinishListener;
import com.gaga.zxingscan.InactivityTimer;
import com.gaga.zxingscan.LogUtil;
import com.gaga.zxingscan.R;
import com.gaga.zxingscan.ViewFindView;
import com.gaga.zxingscan.camera.CameraManager;
import com.gaga.zxingscan.camera.IScanManage;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.io.IOException;
import java.util.Collection;

/**
 * @类名：LianDiScanFgt
 * @描述：
 * @创建人：54506
 * @创建时间：2017/1/9 14:19
 * @版本：
 */

public class ScanFgt extends Fragment implements SurfaceHolder.Callback, IScanManage {
    private final String TAG = getClass().getSimpleName();

    protected CameraManager cameraManager;
    protected CaptureHandler handler;
    protected ViewFindView viewfinderView;
    protected TextView statusView;
    protected boolean hasSurface;
    protected Collection<BarcodeFormat> decodeFormats;
    protected String characterSet;
    protected InactivityTimer inactivityTimer;
    protected BeepManager beepManager;
    protected AmbientLightManager ambientLightManager;
    private boolean scaning;

    private Handler mHandler;
    private View mRootView;


    public static ScanFgt newInstance(CharSequence label) {
        Bundle args = new Bundle();
        args.putCharSequence("label", label);
        ScanFgt fragment = new ScanFgt();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = LayoutInflater.from(getActivity()).inflate(R.layout.fgt_scan, new FrameLayout(getActivity()), false);
        return mRootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();
        initData();
    }

    public void initView() {
    }

    public void initData() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                cameraManager.setTorch(true);
            }
        };
        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setWidthScale(9);
        setHeightScale(3);
        CameraManager.setScanBoxIsCenter(false);
        hasSurface = false;
        inactivityTimer = new InactivityTimer(getActivity());
        beepManager = new BeepManager(getActivity(), R.raw.beep);
        ambientLightManager = new AmbientLightManager(getActivity());
    }


    @Override
    public ViewFindView getViewfinderView() {
        return viewfinderView;
    }

    @Override
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    @Override
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        inactivityTimer.onActivity();
        beepManager.playBeepSoundAndVibrate();//播放提醒音乐
        viewfinderView.drawResultBitmap(barcode);
        String barCode = rawResult.getText();
        LogUtil.d("handleDecodeInternally", barCode + "");
        restartPreviewAfterDelay(800);
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /**
     * 在延迟后发起新的扫描
     *
     * @param delayMS
     */
    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
        resetStatusView();
    }

    private void resetStatusView() {
        statusView.setText(R.string.msg_default_status);
        statusView.setVisibility(View.VISIBLE);
        viewfinderView.setVisibility(View.VISIBLE);
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
//                handler = new CaptureActivityHandler(this, decodeFormats, decodeHints, characterSet, cameraManager);
                handler = new CaptureHandler(getActivity(), this, decodeFormats, null, characterSet, cameraManager);
            }
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected requestError initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, new FinishListener(getActivity()));
        builder.setOnCancelListener(new FinishListener(getActivity()));
        builder.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        startScan();
    }

    protected void startScan() {
        if (!scaning) {
            // CameraManager must be initialized here, not in onCreate(). This is necessary because we don't
            // want to open the camera driver and measure the screen size if we're going to show the help on
            // first launch. That led to bugs where the scanning rectangle was the wrong size and partially
            // off screen.
            cameraManager = new CameraManager(getContext());

            viewfinderView = (ViewFindView) mRootView.findViewById(R.id.viewfinder_view);
            viewfinderView.setCameraManager(cameraManager);
            statusView = (TextView) mRootView.findViewById(R.id.status_view);

            handler = null;

            resetStatusView();

            beepManager.updatePrefs();
            ambientLightManager.start(cameraManager);

            inactivityTimer.onResume();

            decodeFormats = null;
            characterSet = null;

            SurfaceView surfaceView = (SurfaceView) mRootView.findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            if (hasSurface) {
                // The activity was paused but not stopped, so the surface still exists. Therefore
                // surfaceCreated() won't be called, so init the camera here.
                initCamera(surfaceHolder);
            } else {
                // Install the callback and wait for surfaceCreated() to init the camera.
                surfaceHolder.addCallback(this);
            }
            scaning = true;
            mHandler.sendEmptyMessageDelayed(0, 500);
        }
    }

    @Override
    public void onPause() {
        stopScan();
        super.onPause();
    }

    protected void stopScan() {
        cameraManager.setTorch(false);
        if (scaning) {
            if (handler != null) {
                handler.quitSynchronously();
                handler = null;
            }
            inactivityTimer.onPause();
            ambientLightManager.stop();
            beepManager.close();
            cameraManager.closeDriver();
            if (!hasSurface) {
                SurfaceView surfaceView = (SurfaceView) mRootView.findViewById(R.id.preview_view);
                SurfaceHolder surfaceHolder = surfaceView.getHolder();
                surfaceHolder.removeCallback(this);
            }
            scaning = false;
        }
    }

    protected void setWidthScale(int scale) {
        CameraManager.setWidthScale(scale);
    }

    protected void setHeightScale(int scale) {
        CameraManager.setHeightScale(scale);
    }

    @Override
    public void onDestroy() {
        if (inactivityTimer != null) {
            inactivityTimer.shutdown();
        }
        super.onDestroy();
    }
}
