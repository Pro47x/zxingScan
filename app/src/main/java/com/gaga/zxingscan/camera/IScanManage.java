package com.gaga.zxingscan.camera;

import android.graphics.Bitmap;
import android.os.Handler;

import com.gaga.zxingscan.ViewFindView;
import com.google.zxing.Result;

/**
 * @类名：ILianDiScanManage
 * @描述：
 * @创建人：54506
 * @创建时间：2017/1/4 15:57
 * @版本：
 */

public interface IScanManage {

    void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor);

    void drawViewfinder();

    ViewFindView getViewfinderView();

    CameraManager getCameraManager();

    Handler getHandler();
}
