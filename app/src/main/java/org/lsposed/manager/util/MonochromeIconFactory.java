package org.lsposed.manager.util;

/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.graphics.Paint.FILTER_BITMAP_FLAG;
import static android.graphics.drawable.AdaptiveIconDrawable.getExtraInsetFraction;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.nio.ByteBuffer;

/**
 * Utility class to generate monochrome icons version for a given drawable.
 */
@TargetApi(Build.VERSION_CODES.TIRAMISU)
public class MonochromeIconFactory extends Drawable {
    private final Bitmap mFlatBitmap;
    private final Canvas mFlatCanvas;
    private final Paint mCopyPaint;
    private final Bitmap mAlphaBitmap;
    private final Canvas mAlphaCanvas;
    private final byte[] mPixels;
    private final int mBitmapSize;
    private final int mEdgePixelLength;
    private final Paint mDrawPaint;
    private final Rect mSrcRect;

    MonochromeIconFactory(int iconBitmapSize) {
        float extraFactor = getExtraInsetFraction();
        float viewPortScale = 1 / (1 + 2 * extraFactor);
        mBitmapSize = Math.round(iconBitmapSize * 2 * viewPortScale);
        mPixels = new byte[mBitmapSize * mBitmapSize];
        mEdgePixelLength = mBitmapSize * (mBitmapSize - iconBitmapSize) / 2;
        mFlatBitmap = Bitmap.createBitmap(mBitmapSize, mBitmapSize, Config.ARGB_8888);
        mFlatCanvas = new Canvas(mFlatBitmap);
        mAlphaBitmap = Bitmap.createBitmap(mBitmapSize, mBitmapSize, Config.ALPHA_8);
        mAlphaCanvas = new Canvas(mAlphaBitmap);
        mDrawPaint = new Paint(FILTER_BITMAP_FLAG);
        mDrawPaint.setColor(Color.WHITE);
        mSrcRect = new Rect(0, 0, mBitmapSize, mBitmapSize);
        mCopyPaint = new Paint(FILTER_BITMAP_FLAG);
        mCopyPaint.setBlendMode(BlendMode.SRC);
        // Crate a color matrix which converts the icon to grayscale and then uses the average
        // of RGB components as the alpha component.
        ColorMatrix satMatrix = new ColorMatrix();
        satMatrix.setSaturation(0);
        float[] vals = satMatrix.getArray();
        vals[15] = vals[16] = vals[17] = .3333f;
        vals[18] = vals[19] = 0;
        mCopyPaint.setColorFilter(new ColorMatrixColorFilter(vals));
    }

    private void drawDrawable(Drawable drawable) {
        if (drawable != null) {
            drawable.setBounds(0, 0, mBitmapSize, mBitmapSize);
            drawable.draw(mFlatCanvas);
        }
    }

    /**
     * Creates a monochrome version of the provided drawable
     */
    @WorkerThread
    public Drawable wrap(Drawable icon) {
        if (icon instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable aid = (AdaptiveIconDrawable) icon;
            mFlatCanvas.drawColor(Color.BLACK);
            drawDrawable(aid.getBackground());
            drawDrawable(aid.getForeground());
            generateMono();
            return new ClippedMonoDrawable(this);
        } else {
            mFlatCanvas.drawColor(Color.WHITE);
            drawDrawable(icon);
            generateMono();
            return this;
        }
    }

    @WorkerThread
    private void generateMono() {
        mAlphaCanvas.drawBitmap(mFlatBitmap, 0, 0, mCopyPaint);
        // Scale the end points:
        ByteBuffer buffer = ByteBuffer.wrap(mPixels);
        buffer.rewind();
        mAlphaBitmap.copyPixelsToBuffer(buffer);
        int min = 0xFF;
        int max = 0;
        for (byte b : mPixels) {
            min = Math.min(min, b & 0xFF);
            max = Math.max(max, b & 0xFF);
        }
        if (min < max) {
            // rescale pixels to increase contrast
            float range = max - min;
            // In order to check if the colors should be flipped, we just take the average color
            // of top and bottom edge which should correspond to be background color. If the edge
            // colors have more opacity, we flip the colors;
            int sum = 0;
            for (int i = 0; i < mEdgePixelLength; i++) {
                sum += (mPixels[i] & 0xFF);
                sum += (mPixels[mPixels.length - 1 - i] & 0xFF);
            }
            float edgeAverage = sum / (mEdgePixelLength * 2f);
            float edgeMapped = (edgeAverage - min) / range;
            boolean flipColor = edgeMapped > .5f;
            for (int i = 0; i < mPixels.length; i++) {
                int p = mPixels[i] & 0xFF;
                int p2 = Math.round((p - min) * 0xFF / range);
                mPixels[i] = flipColor ? (byte) (255 - p2) : (byte) (p2);
            }
            buffer.rewind();
            mAlphaBitmap.copyPixelsFromBuffer(buffer);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(mAlphaBitmap, mSrcRect, getBounds(), mDrawPaint);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int i) {
        mDrawPaint.setAlpha(i);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mDrawPaint.setColorFilter(colorFilter);
    }
}

class ClippedMonoDrawable extends InsetDrawable {

    @NonNull
    private final AdaptiveIconDrawable mCrop;

    public ClippedMonoDrawable(@Nullable final Drawable base) {
        super(base, -getExtraInsetFraction());
        mCrop = new AdaptiveIconDrawable(new ColorDrawable(Color.BLACK), null);
    }

    @Override
    public void draw(Canvas canvas) {
        mCrop.setBounds(getBounds());
        int saveCount = canvas.save();
        canvas.clipPath(mCrop.getIconMask());
        super.draw(canvas);
        canvas.restoreToCount(saveCount);
    }
}