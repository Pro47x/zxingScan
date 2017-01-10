/*
 * Copyright (C) 2008 ZXing authors
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

package com.gaga.zxingscan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.gaga.zxingscan.camera.CameraManager;
import com.google.zxing.ResultPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewFindView extends View {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final int[] SCANNER_SPEED = {
            5, 6, 7, 8, 9,
            10, 10, 10, 10, 11, 12, 13, 15
    };//扫描条的速度
    //    private static final long ANIMATION_DELAY = 80L;
    private static final long ANIMATION_DELAY = 15;//刷新时间  单位ms
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;
    private final Paint paint;
    private final int maskColor;
    private final int resultColor;
    private final int laserColor;
    private final int resultPointColor;
    private CameraManager cameraManager;
    private Bitmap resultBitmap;
    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;
    private float scanTop = 0;
    private int boxOffset;
    private int boxOutOffset;
    private int scanLineOffset;
    private int scanBox;

    // This constructor is used when the class is built from an XML resource.
    public ViewFindView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
        resultColor = resources.getColor(R.color.result_view);
        laserColor = resources.getColor(R.color.viewfinder_laser);
        resultPointColor = resources.getColor(R.color.possible_result_points);
        scanBox = resources.getColor(R.color.scan_box);
        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = null;
        boxOffset = ScreenUtil.dip2px(getContext(), 7.5f);
        boxOutOffset = ScreenUtil.dip2px(getContext(), 25f);
        scanLineOffset = ScreenUtil.dip2px(getContext(), 3f);//扫描线的（宽度）
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        Rect frame = cameraManager.getFramingRect();
        Rect previewFrame = cameraManager.getFramingRectInPreview();

        if (frame == null || previewFrame == null) {
            return;
        }
        int width = canvas.getWidth();  //画布宽度
        int height = canvas.getHeight();//画布高度

        int left = frame.left;          //扫描位置：左边界
        int top = frame.top;            //扫描位置：上边界
        int right = frame.right;        //扫描位置：右边界
        int bottom = frame.bottom;      //扫描位置：下边界

        //四个扫描边框
        Paint p = new Paint();
        p.setColor(scanBox);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.XOR));//给画笔设置绘画模式
        //以下步骤完成了一个稍大于扫描框的外边框
        canvas.drawRect(left - boxOffset, top - boxOffset, right + boxOffset, bottom + boxOffset, p);//画一个比扫描框稍微大一点的矩形
        canvas.drawRect(left, top, right, bottom, p);//画一个和扫描框一样大的矩形
        //以下步骤将外边框切割成了四个护角框
        p.setAlpha(0X55);//给画笔设置透明度
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));//给画笔设置绘画模式
        canvas.drawRect(left + boxOutOffset, 0, right - boxOutOffset, height, p);//宽度为扫描框，高度为画布高度的矩形，用于切割垂直方向的护角
        canvas.drawRect(0, top + boxOutOffset, width, bottom - boxOutOffset, p);//宽度为画布宽度，高度为扫描框，用于切割横向的护角
        //以下步骤绘制一个颜色稍微深一点的外边框
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
        p.setColor(Color.WHITE);
        p.setAlpha(0XCC);
        canvas.drawRect(left - 3, top, left, bottom, p);    //左边框
        canvas.drawRect(left, top - 3, right, top, p);      //上边框
        canvas.drawRect(right, top, right + 3, bottom, p);  //右边框
        canvas.drawRect(left, bottom, right, bottom + 3, p);//下边框


        // Draw the exterior (i.e. outside the framing rect) darkened
        //以下是画扫描框之外的阴影部分
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, top, paint);           //画扫描框上面的阴影部分
        canvas.drawRect(0, top, left, bottom, paint);       //画扫描框左边的阴影部分
        canvas.drawRect(right, top, width, bottom, paint);  //画扫描框右边的阴影部分
        canvas.drawRect(0, bottom, width, height, paint);   //画扫描框下边的阴影部分

        if (resultBitmap != null) {
            //如果扫描结果的bitmap不为null，就把扫描结果画在扫描框
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {
            // Draw a red "laser scanner" line through the middle to show decoding is active
            int h = bottom - top;
            int v = (int) (scanTop / h * (SCANNER_SPEED.length - 1));
            paint.setColor(scanBox);
//            paint.setAlpha(SCANNER_ALPHA[scanPosition % SCANNER_ALPHA.length]);
            scanTop += SCANNER_SPEED[v];
            if (scanTop > frame.height() - 20) {
                scanTop = 0;
            }
            float scanLineTop = top + scanTop + scanLineOffset;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                canvas.drawRoundRect(left + scanLineOffset, scanLineTop, right - scanLineOffset, scanLineTop + scanLineOffset, 30, scanLineOffset, paint);
            } else {
                canvas.drawRect(left + scanLineOffset, scanLineTop, right - scanLineOffset, scanLineTop + scanLineOffset, paint);
            }

            float scaleX = frame.width() / (float) previewFrame.width();
            float scaleY = frame.height() / (float) previewFrame.height();

            List<ResultPoint> currentPossible = possibleResultPoints;
            List<ResultPoint> currentLast = lastPossibleResultPoints;
            if (currentPossible.isEmpty()) {
                lastPossibleResultPoints = null;
            } else {
                possibleResultPoints = new ArrayList<>(5);
                lastPossibleResultPoints = currentPossible;
                paint.setAlpha(CURRENT_POINT_OPACITY);
                paint.setColor(resultPointColor);
                synchronized (currentPossible) {
                    for (ResultPoint point : currentPossible) {
                        canvas.drawCircle(left + (int) (point.getX() * scaleX),
                                top + (int) (point.getY() * scaleY),
                                POINT_SIZE, paint);
                    }
                }
            }
            if (currentLast != null) {
                paint.setAlpha(CURRENT_POINT_OPACITY / 2);
                paint.setColor(resultPointColor);
                synchronized (currentLast) {
                    float radius = POINT_SIZE / 2.0f;
                    for (ResultPoint point : currentLast) {
                        canvas.drawCircle(left + (int) (point.getX() * scaleX),
                                top + (int) (point.getY() * scaleY),
                                radius, paint);
                    }
                }
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(ANIMATION_DELAY,
                    left - POINT_SIZE,
                    top - POINT_SIZE,
                    right + POINT_SIZE,
                    bottom + POINT_SIZE);
        }
    }

    /**
     * 清除缓存的扫码成功的显示用bitmap
     * 重新进入扫描页面
     */
    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        scanTop = 0;
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

}
