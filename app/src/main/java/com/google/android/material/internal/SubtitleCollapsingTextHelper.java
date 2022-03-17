package com.google.android.material.internal;

import android.animation.TimeInterpolator;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;
import androidx.core.text.TextDirectionHeuristicsCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.animation.AnimationUtils;
import com.google.android.material.resources.CancelableFontCallback;
import com.google.android.material.resources.TextAppearance;

/**
 * Helper class for {@link com.google.android.material.appbar.SubtitleCollapsingToolbarLayout}.
 *
 * @see CollapsingTextHelper
 */
public final class SubtitleCollapsingTextHelper {

    // Pre-JB-MR2 doesn't support HW accelerated canvas scaled title so we will workaround it
    // by using our own texture
    private static final boolean USE_SCALING_TEXTURE = Build.VERSION.SDK_INT < 18;

    private static final boolean DEBUG_DRAW = false;
    @NonNull
    private static final Paint DEBUG_DRAW_PAINT;

    static {
        DEBUG_DRAW_PAINT = DEBUG_DRAW ? new Paint() : null;
        if (DEBUG_DRAW_PAINT != null) {
            DEBUG_DRAW_PAINT.setAntiAlias(true);
            DEBUG_DRAW_PAINT.setColor(Color.MAGENTA);
        }
    }

    private final View view;

    private boolean drawTitle;
    private float expandedFraction;

    @NonNull
    private final Rect expandedBounds;
    @NonNull
    private final Rect collapsedBounds;
    @NonNull
    private final RectF currentBounds;
    private int expandedTextGravity = Gravity.CENTER_VERTICAL;
    private int collapsedTextGravity = Gravity.CENTER_VERTICAL;
    private float expandedTitleTextSize, expandedSubtitleTextSize = 15;
    private float collapsedTitleTextSize, collapsedSubtitleTextSize = 15;
    private ColorStateList expandedTitleTextColor, expandedSubtitleTextColor;
    private ColorStateList collapsedTitleTextColor, collapsedSubtitleTextColor;

    private float expandedTitleDrawY, expandedSubtitleDrawY;
    private float collapsedTitleDrawY, collapsedSubtitleDrawY;
    private float expandedTitleDrawX, expandedSubtitleDrawX;
    private float collapsedTitleDrawX, collapsedSubtitleDrawX;
    private float currentTitleDrawX, currentSubtitleDrawX;
    private float currentTitleDrawY, currentSubtitleDrawY;
    private Typeface collapsedTitleTypeface, collapsedSubtitleTypeface;
    private Typeface expandedTitleTypeface, expandedSubtitleTypeface;
    private Typeface currentTitleTypeface, currentSubtitleTypeface;
    private CancelableFontCallback expandedTitleFontCallback, expandedSubtitleFontCallback;
    private CancelableFontCallback collapsedTitleFontCallback, collapsedSubtitleFontCallback;

    @Nullable
    private CharSequence title, subtitle;
    @Nullable
    private CharSequence titleToDraw, subtitleToDraw;
    private boolean isRtl;
    private boolean isRtlTextDirectionHeuristicsEnabled = true;

    private boolean useTexture;
    @Nullable
    private Bitmap expandedTitleTexture, expandedSubtitleTexture;
    private Paint titleTexturePaint, subtitleTexturePaint;
    private float titleTextureAscent, subtitleTextureAscent;
    private float titleTextureDescent, subtitleTextureDescent;

    private float titleScale, subtitleScale;
    private float currentTitleTextSize, currentSubtitleTextSize;

    private int[] state;

    private boolean boundsChanged;

    @NonNull
    private final TextPaint titleTextPaint, subtitleTextPaint;
    @NonNull
    private final TextPaint titleTmpPaint, subtitleTmpPaint;

    private TimeInterpolator positionInterpolator;
    private TimeInterpolator textSizeInterpolator;

    private float collapsedTitleShadowRadius, collapsedSubtitleShadowRadius;
    private float collapsedTitleShadowDx, collapsedSubtitleShadowDx;
    private float collapsedTitleShadowDy, collapsedSubtitleShadowDy;
    private ColorStateList collapsedTitleShadowColor, collapsedSubtitleShadowColor;

    private float expandedTitleShadowRadius, expandedSubtitleShadowRadius;
    private float expandedTitleShadowDx, expandedSubtitleShadowDx;
    private float expandedTitleShadowDy, expandedSubtitleShadowDy;
    private ColorStateList expandedTitleShadowColor, expandedSubtitleShadowColor;

    public SubtitleCollapsingTextHelper(View view) {
        this.view = view;

        titleTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        titleTmpPaint = new TextPaint(titleTextPaint);
        subtitleTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        subtitleTmpPaint = new TextPaint(subtitleTextPaint);

        collapsedBounds = new Rect();
        expandedBounds = new Rect();
        currentBounds = new RectF();
    }


    public void setTextSizeInterpolator(TimeInterpolator interpolator) {
        textSizeInterpolator = interpolator;
        recalculate();
    }

    public void setPositionInterpolator(TimeInterpolator interpolator) {
        positionInterpolator = interpolator;
        recalculate();
    }

    public void setExpandedTitleTextSize(float textSize) {
        if (expandedTitleTextSize != textSize) {
            expandedTitleTextSize = textSize;
            recalculate();
        }
    }

    public void setCollapsedTitleTextSize(float textSize) {
        if (collapsedTitleTextSize != textSize) {
            collapsedTitleTextSize = textSize;
            recalculate();
        }
    }

    public void setExpandedSubtitleTextSize(float textSize) {
        if (expandedSubtitleTextSize != textSize) {
            expandedSubtitleTextSize = textSize;
            recalculate();
        }
    }

    public void setCollapsedSubtitleTextSize(float textSize) {
        if (collapsedSubtitleTextSize != textSize) {
            collapsedSubtitleTextSize = textSize;
            recalculate();
        }
    }

    public void setCollapsedTitleTextColor(ColorStateList textColor) {
        if (collapsedTitleTextColor != textColor) {
            collapsedTitleTextColor = textColor;
            recalculate();
        }
    }

    public void setExpandedTitleTextColor(ColorStateList textColor) {
        if (expandedTitleTextColor != textColor) {
            expandedTitleTextColor = textColor;
            recalculate();
        }
    }

    public void setCollapsedSubtitleTextColor(ColorStateList textColor) {
        if (collapsedSubtitleTextColor != textColor) {
            collapsedSubtitleTextColor = textColor;
            recalculate();
        }
    }

    public void setExpandedSubtitleTextColor(ColorStateList textColor) {
        if (expandedSubtitleTextColor != textColor) {
            expandedSubtitleTextColor = textColor;
            recalculate();
        }
    }

    public void setExpandedBounds(int left, int top, int right, int bottom) {
        if (!rectEquals(expandedBounds, left, top, right, bottom)) {
            expandedBounds.set(left, top, right, bottom);
            boundsChanged = true;
            onBoundsChanged();
        }
    }

    public void setExpandedBounds(@NonNull Rect bounds) {
        setExpandedBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }

    public void setCollapsedBounds(int left, int top, int right, int bottom) {
        if (!rectEquals(collapsedBounds, left, top, right, bottom)) {
            collapsedBounds.set(left, top, right, bottom);
            boundsChanged = true;
            onBoundsChanged();
        }
    }

    public void setCollapsedBounds(@NonNull Rect bounds) {
        setCollapsedBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }

    public void getCollapsedTitleTextActualBounds(@NonNull RectF bounds) {
        boolean isRtl = calculateIsRtl(title);

        bounds.left = !isRtl ? collapsedBounds.left : collapsedBounds.right - calculateCollapsedTitleTextWidth();
        bounds.top = collapsedBounds.top;
        bounds.right = !isRtl ? bounds.left + calculateCollapsedTitleTextWidth() : collapsedBounds.right;
        bounds.bottom = collapsedBounds.top + getCollapsedTitleTextHeight();
    }

    public float calculateCollapsedTitleTextWidth() {
        if (title == null) {
            return 0;
        }
        getTitleTextPaintCollapsed(titleTmpPaint);
        return titleTmpPaint.measureText(title, 0, title.length());
    }

    public void getCollapsedSubtitleTextActualBounds(@NonNull RectF bounds) {
        boolean isRtl = calculateIsRtl(subtitle);

        bounds.left = !isRtl ? collapsedBounds.left : collapsedBounds.right - calculateCollapsedSubtitleTextWidth();
        bounds.top = collapsedBounds.top;
        bounds.right = !isRtl ? bounds.left + calculateCollapsedSubtitleTextWidth() : collapsedBounds.right;
        bounds.bottom = collapsedBounds.top + getCollapsedSubtitleTextHeight();
    }

    public float calculateCollapsedSubtitleTextWidth() {
        if (subtitle == null) {
            return 0;
        }
        getSubtitleTextPaintCollapsed(subtitleTmpPaint);
        return subtitleTmpPaint.measureText(subtitle, 0, subtitle.length());
    }

    public float getExpandedTitleTextHeight() {
        getTitleTextPaintExpanded(titleTmpPaint);
        // Return expanded height measured from the baseline.
        return -titleTmpPaint.ascent();
    }

    public float getCollapsedTitleTextHeight() {
        getTitleTextPaintCollapsed(titleTmpPaint);
        // Return collapsed height measured from the baseline.
        return -titleTmpPaint.ascent();
    }

    public float getExpandedSubtitleTextHeight() {
        getSubtitleTextPaintExpanded(subtitleTmpPaint);
        // Return expanded height measured from the baseline.
        return -subtitleTmpPaint.ascent();
    }

    public float getCollapsedSubtitleTextHeight() {
        getSubtitleTextPaintCollapsed(subtitleTmpPaint);
        // Return collapsed height measured from the baseline.
        return -subtitleTmpPaint.ascent();
    }

    private void getTitleTextPaintExpanded(@NonNull TextPaint textPaint) {
        textPaint.setTextSize(expandedTitleTextSize);
        textPaint.setTypeface(expandedTitleTypeface);
    }

    private void getTitleTextPaintCollapsed(@NonNull TextPaint textPaint) {
        textPaint.setTextSize(collapsedTitleTextSize);
        textPaint.setTypeface(collapsedTitleTypeface);
    }

    private void getSubtitleTextPaintExpanded(@NonNull TextPaint textPaint) {
        textPaint.setTextSize(expandedSubtitleTextSize);
        textPaint.setTypeface(expandedSubtitleTypeface);
    }

    private void getSubtitleTextPaintCollapsed(@NonNull TextPaint textPaint) {
        textPaint.setTextSize(collapsedSubtitleTextSize);
        textPaint.setTypeface(collapsedSubtitleTypeface);
    }

    void onBoundsChanged() {
        drawTitle = collapsedBounds.width() > 0
                && collapsedBounds.height() > 0
                && expandedBounds.width() > 0
                && expandedBounds.height() > 0;
    }

    public void setExpandedTextGravity(int gravity) {
        if (expandedTextGravity != gravity) {
            expandedTextGravity = gravity;
            recalculate();
        }
    }

    public int getExpandedTextGravity() {
        return expandedTextGravity;
    }

    public void setCollapsedTextGravity(int gravity) {
        if (collapsedTextGravity != gravity) {
            collapsedTextGravity = gravity;
            recalculate();
        }
    }

    public int getCollapsedTextGravity() {
        return collapsedTextGravity;
    }

    public void setCollapsedTitleTextAppearance(int resId) {
        TextAppearance textAppearance = new TextAppearance(view.getContext(), resId);

        if (textAppearance.getTextColor() != null) {
            collapsedTitleTextColor = textAppearance.getTextColor();
        }
        if (textAppearance.getTextSize() != 0) {
            collapsedTitleTextSize = textAppearance.getTextSize();
        }
        if (textAppearance.shadowColor != null) {
            collapsedTitleShadowColor = textAppearance.shadowColor;
        }
        collapsedTitleShadowDx = textAppearance.shadowDx;
        collapsedTitleShadowDy = textAppearance.shadowDy;
        collapsedTitleShadowRadius = textAppearance.shadowRadius;

        // Cancel pending async fetch, if any, and replace with a new one.
        if (collapsedTitleFontCallback != null) {
            collapsedTitleFontCallback.cancel();
        }
        collapsedTitleFontCallback = new CancelableFontCallback(new CancelableFontCallback.ApplyFont() {
            @Override
            public void apply(Typeface font) {
                setCollapsedTitleTypeface(font);
            }
        }, textAppearance.getFallbackFont());
        textAppearance.getFontAsync(view.getContext(), collapsedTitleFontCallback);

        recalculate();
    }

    public void setExpandedTitleTextAppearance(int resId) {
        TextAppearance textAppearance = new TextAppearance(view.getContext(), resId);
        if (textAppearance.getTextColor() != null) {
            expandedTitleTextColor = textAppearance.getTextColor();
        }
        if (textAppearance.getTextSize() != 0) {
            expandedTitleTextSize = textAppearance.getTextSize();
        }
        if (textAppearance.shadowColor != null) {
            expandedTitleShadowColor = textAppearance.shadowColor;
        }
        expandedTitleShadowDx = textAppearance.shadowDx;
        expandedTitleShadowDy = textAppearance.shadowDy;
        expandedTitleShadowRadius = textAppearance.shadowRadius;

        // Cancel pending async fetch, if any, and replace with a new one.
        if (expandedTitleFontCallback != null) {
            expandedTitleFontCallback.cancel();
        }
        expandedTitleFontCallback = new CancelableFontCallback(new CancelableFontCallback.ApplyFont() {
            @Override
            public void apply(Typeface font) {
                setExpandedTitleTypeface(font);
            }
        }, textAppearance.getFallbackFont());
        textAppearance.getFontAsync(view.getContext(), expandedTitleFontCallback);

        recalculate();
    }

    public void setCollapsedSubtitleTextAppearance(int resId) {
        TextAppearance textAppearance = new TextAppearance(view.getContext(), resId);

        if (textAppearance.getTextColor() != null) {
            collapsedSubtitleTextColor = textAppearance.getTextColor();
        }
        if (textAppearance.getTextSize() != 0) {
            collapsedSubtitleTextSize = textAppearance.getTextSize();
        }
        if (textAppearance.shadowColor != null) {
            collapsedSubtitleShadowColor = textAppearance.shadowColor;
        }
        collapsedSubtitleShadowDx = textAppearance.shadowDx;
        collapsedSubtitleShadowDy = textAppearance.shadowDy;
        collapsedSubtitleShadowRadius = textAppearance.shadowRadius;

        // Cancel pending async fetch, if any, and replace with a new one.
        if (collapsedSubtitleFontCallback != null) {
            collapsedSubtitleFontCallback.cancel();
        }
        collapsedSubtitleFontCallback = new CancelableFontCallback(new CancelableFontCallback.ApplyFont() {
            @Override
            public void apply(Typeface font) {
                setCollapsedSubtitleTypeface(font);
            }
        }, textAppearance.getFallbackFont());
        textAppearance.getFontAsync(view.getContext(), collapsedSubtitleFontCallback);

        recalculate();
    }

    public void setExpandedSubtitleTextAppearance(int resId) {
        TextAppearance textAppearance = new TextAppearance(view.getContext(), resId);
        if (textAppearance.getTextColor() != null) {
            expandedSubtitleTextColor = textAppearance.getTextColor();
        }
        if (textAppearance.getTextSize() != 0) {
            expandedSubtitleTextSize = textAppearance.getTextSize();
        }
        if (textAppearance.shadowColor != null) {
            expandedSubtitleShadowColor = textAppearance.shadowColor;
        }
        expandedSubtitleShadowDx = textAppearance.shadowDx;
        expandedSubtitleShadowDy = textAppearance.shadowDy;
        expandedSubtitleShadowRadius = textAppearance.shadowRadius;

        // Cancel pending async fetch, if any, and replace with a new one.
        if (expandedSubtitleFontCallback != null) {
            expandedSubtitleFontCallback.cancel();
        }
        expandedSubtitleFontCallback = new CancelableFontCallback(new CancelableFontCallback.ApplyFont() {
            @Override
            public void apply(Typeface font) {
                if (font != null) setExpandedSubtitleTypeface(font);
            }
        }, null);
        textAppearance.getFontAsync(view.getContext(), expandedSubtitleFontCallback);

        recalculate();
    }

    public void setCollapsedTitleTypeface(Typeface typeface) {
        if (setCollapsedTitleTypefaceInternal(typeface)) {
            recalculate();
        }
    }

    public void setExpandedTitleTypeface(Typeface typeface) {
        if (setExpandedTitleTypefaceInternal(typeface)) {
            recalculate();
        }
    }

    public void setCollapsedSubtitleTypeface(Typeface typeface) {
        if (setCollapsedSubtitleTypefaceInternal(typeface)) {
            recalculate();
        }
    }

    public void setExpandedSubtitleTypeface(Typeface typeface) {
        if (setExpandedSubtitleTypefaceInternal(typeface)) {
            recalculate();
        }
    }

    public void setTitleTypefaces(Typeface typeface) {
        boolean collapsedFontChanged = setCollapsedTitleTypefaceInternal(typeface);
        boolean expandedFontChanged = setExpandedTitleTypefaceInternal(typeface);
        if (collapsedFontChanged || expandedFontChanged) {
            recalculate();
        }
    }

    public void setSubtitleTypefaces(Typeface typeface) {
        boolean collapsedFontChanged = setCollapsedSubtitleTypefaceInternal(typeface);
        boolean expandedFontChanged = setExpandedSubtitleTypefaceInternal(typeface);
        if (collapsedFontChanged || expandedFontChanged) {
            recalculate();
        }
    }

    @SuppressWarnings("ReferenceEquality") // Matches the Typeface comparison in TextView
    private boolean setCollapsedTitleTypefaceInternal(Typeface typeface) {
        // Explicit Typeface setting cancels pending async fetch, if any, to avoid old font overriding
        // already updated one when async op comes back after a while.
        if (collapsedTitleFontCallback != null) {
            collapsedTitleFontCallback.cancel();
        }
        if (collapsedTitleTypeface != typeface) {
            collapsedTitleTypeface = typeface;
            return true;
        }
        return false;
    }

    @SuppressWarnings("ReferenceEquality") // Matches the Typeface comparison in TextView
    private boolean setExpandedTitleTypefaceInternal(Typeface typeface) {
        // Explicit Typeface setting cancels pending async fetch, if any, to avoid old font overriding
        // already updated one when async op comes back after a while.
        if (expandedTitleFontCallback != null) {
            expandedTitleFontCallback.cancel();
        }
        if (expandedTitleTypeface != typeface) {
            expandedTitleTypeface = typeface;
            return true;
        }
        return false;
    }

    @SuppressWarnings("ReferenceEquality") // Matches the Typeface comparison in TextView
    private boolean setCollapsedSubtitleTypefaceInternal(Typeface typeface) {
        // Explicit Typeface setting cancels pending async fetch, if any, to avoid old font overriding
        // already updated one when async op comes back after a while.
        if (collapsedSubtitleFontCallback != null) {
            collapsedSubtitleFontCallback.cancel();
        }
        if (collapsedSubtitleTypeface != typeface) {
            collapsedSubtitleTypeface = typeface;
            return true;
        }
        return false;
    }

    @SuppressWarnings("ReferenceEquality") // Matches the Typeface comparison in TextView
    private boolean setExpandedSubtitleTypefaceInternal(Typeface typeface) {
        // Explicit Typeface setting cancels pending async fetch, if any, to avoid old font overriding
        // already updated one when async op comes back after a while.
        if (expandedSubtitleFontCallback != null) {
            expandedSubtitleFontCallback.cancel();
        }
        if (expandedSubtitleTypeface != typeface) {
            expandedSubtitleTypeface = typeface;
            return true;
        }
        return false;
    }

    public Typeface getCollapsedTitleTypeface() {
        return collapsedTitleTypeface != null ? collapsedTitleTypeface : Typeface.DEFAULT;
    }

    public Typeface getExpandedTitleTypeface() {
        return expandedTitleTypeface != null ? expandedTitleTypeface : Typeface.DEFAULT;
    }

    public Typeface getCollapsedSubtitleTypeface() {
        return collapsedSubtitleTypeface != null ? collapsedSubtitleTypeface : Typeface.DEFAULT;
    }

    public Typeface getExpandedSubtitleTypeface() {
        return expandedSubtitleTypeface != null ? expandedSubtitleTypeface : Typeface.DEFAULT;
    }

    /**
     * Set the value indicating the current scroll value. This decides how much of the background will
     * be displayed, as well as the title metrics/positioning.
     *
     * <p>A value of {@code 0.0} indicates that the layout is fully expanded. A value of {@code 1.0}
     * indicates that the layout is fully collapsed.
     */
    public void setExpansionFraction(float fraction) {
        fraction = MathUtils.clamp(fraction, 0f, 1f);

        if (fraction != expandedFraction) {
            expandedFraction = fraction;
            calculateCurrentOffsets();
        }
    }

    public final boolean setState(final int[] state) {
        this.state = state;

        if (isStateful()) {
            recalculate();
            return true;
        }

        return false;
    }

    public final boolean isStateful() {
        return (collapsedTitleTextColor != null && collapsedTitleTextColor.isStateful())
                || (expandedTitleTextColor != null && expandedTitleTextColor.isStateful());
    }

    public float getExpansionFraction() {
        return expandedFraction;
    }

    public float getCollapsedTitleTextSize() {
        return collapsedTitleTextSize;
    }

    public float getExpandedTitleTextSize() {
        return expandedTitleTextSize;
    }

    public float getCollapsedSubtitleTextSize() {
        return collapsedSubtitleTextSize;
    }

    public float getExpandedSubtitleTextSize() {
        return expandedSubtitleTextSize;
    }

    public void setRtlTextDirectionHeuristicsEnabled(boolean rtlTextDirectionHeuristicsEnabled) {
        isRtlTextDirectionHeuristicsEnabled = rtlTextDirectionHeuristicsEnabled;
    }

    public boolean isRtlTextDirectionHeuristicsEnabled() {
        return isRtlTextDirectionHeuristicsEnabled;
    }

    private void calculateCurrentOffsets() {
        calculateOffsets(expandedFraction);
    }

    private void calculateOffsets(final float fraction) {
        interpolateBounds(fraction);
        currentTitleDrawX = lerp(expandedTitleDrawX, collapsedTitleDrawX, fraction, positionInterpolator);
        currentTitleDrawY = lerp(expandedTitleDrawY, collapsedTitleDrawY, fraction, positionInterpolator);
        currentSubtitleDrawX = lerp(expandedSubtitleDrawX, collapsedSubtitleDrawX, fraction, positionInterpolator);
        currentSubtitleDrawY = lerp(expandedSubtitleDrawY, collapsedSubtitleDrawY, fraction, positionInterpolator);

        setInterpolatedTitleTextSize(lerp(expandedTitleTextSize, collapsedTitleTextSize, fraction, textSizeInterpolator));
        setInterpolatedSubtitleTextSize(lerp(expandedSubtitleTextSize, collapsedSubtitleTextSize, fraction, textSizeInterpolator));

        if (collapsedTitleTextColor != expandedTitleTextColor) {
            // If the collapsed and expanded title colors are different, blend them based on the
            // fraction
            titleTextPaint.setColor(blendColors(getCurrentExpandedTitleTextColor(), getCurrentCollapsedTitleTextColor(), fraction));
        } else {
            titleTextPaint.setColor(getCurrentCollapsedTitleTextColor());
        }

        titleTextPaint.setShadowLayer(
                lerp(expandedTitleShadowRadius, collapsedTitleShadowRadius, fraction, null),
                lerp(expandedTitleShadowDx, collapsedTitleShadowDx, fraction, null),
                lerp(expandedTitleShadowDy, collapsedTitleShadowDy, fraction, null),
                blendColors(getCurrentColor(expandedTitleShadowColor), getCurrentColor(collapsedTitleShadowColor), fraction));

        if (collapsedSubtitleTextColor != expandedSubtitleTextColor) {
            // If the collapsed and expanded title colors are different, blend them based on the
            // fraction
            subtitleTextPaint.setColor(blendColors(getCurrentExpandedSubtitleTextColor(), getCurrentCollapsedSubtitleTextColor(), fraction));
        } else {
            subtitleTextPaint.setColor(getCurrentCollapsedSubtitleTextColor());
        }

        subtitleTextPaint.setShadowLayer(
                lerp(expandedSubtitleShadowRadius, collapsedSubtitleShadowRadius, fraction, null),
                lerp(expandedSubtitleShadowDx, collapsedSubtitleShadowDx, fraction, null),
                lerp(expandedSubtitleShadowDy, collapsedSubtitleShadowDy, fraction, null),
                blendColors(getCurrentColor(expandedSubtitleShadowColor), getCurrentColor(collapsedSubtitleShadowColor), fraction));

        ViewCompat.postInvalidateOnAnimation(view);
    }

    @ColorInt
    private int getCurrentExpandedTitleTextColor() {
        return getCurrentColor(expandedTitleTextColor);
    }

    @ColorInt
    private int getCurrentExpandedSubtitleTextColor() {
        return getCurrentColor(expandedSubtitleTextColor);
    }

    @ColorInt
    public int getCurrentCollapsedTitleTextColor() {
        return getCurrentColor(collapsedTitleTextColor);
    }

    @ColorInt
    public int getCurrentCollapsedSubtitleTextColor() {
        return getCurrentColor(collapsedSubtitleTextColor);
    }

    @ColorInt
    private int getCurrentColor(@Nullable ColorStateList colorStateList) {
        if (colorStateList == null) {
            return 0;
        }
        if (state != null) {
            return colorStateList.getColorForState(state, 0);
        }
        return colorStateList.getDefaultColor();
    }

    private void calculateBaseOffsets() {
        final float currentTitleSize = this.currentTitleTextSize;
        final float currentSubtitleSize = this.currentSubtitleTextSize;
        final boolean isTitleOnly = TextUtils.isEmpty(subtitle);

        // We then calculate the collapsed title size, using the same logic
        calculateUsingTitleTextSize(collapsedTitleTextSize);
        calculateUsingSubtitleTextSize(collapsedSubtitleTextSize);
        float titleWidth = titleToDraw != null ? titleTextPaint.measureText(titleToDraw, 0, titleToDraw.length()) : 0;
        float subtitleWidth = subtitleToDraw != null ? subtitleTextPaint.measureText(subtitleToDraw, 0, subtitleToDraw.length()) : 0;
        final int collapsedAbsGravity =
                GravityCompat.getAbsoluteGravity(
                        collapsedTextGravity,
                        isRtl ? ViewCompat.LAYOUT_DIRECTION_RTL : ViewCompat.LAYOUT_DIRECTION_LTR);

        // reusable dimension
        float titleHeight = titleTextPaint.descent() - titleTextPaint.ascent();
        float titleOffset = titleHeight / 2 - titleTextPaint.descent();
        float subtitleHeight = subtitleTextPaint.descent() - subtitleTextPaint.ascent();
        float subtitleOffset = subtitleHeight / 2 - subtitleTextPaint.descent();

        if (isTitleOnly) {
            switch (collapsedAbsGravity & Gravity.VERTICAL_GRAVITY_MASK) {
                case Gravity.BOTTOM:
                    collapsedTitleDrawY = collapsedBounds.bottom;
                    break;
                case Gravity.TOP:
                    collapsedTitleDrawY = collapsedBounds.top - titleTextPaint.ascent();
                    break;
                case Gravity.CENTER_VERTICAL:
                default:
                    float textHeight = titleTextPaint.descent() - titleTextPaint.ascent();
                    float textOffset = (textHeight / 2) - titleTextPaint.descent();
                    collapsedTitleDrawY = collapsedBounds.centerY() + textOffset;
                    break;
            }
        } else {
            final float offset = (collapsedBounds.height() - (titleHeight + subtitleHeight)) / 3;
            collapsedTitleDrawY = collapsedBounds.top + offset - titleTextPaint.ascent();
            collapsedSubtitleDrawY = collapsedBounds.top + offset * 2 + titleHeight - subtitleTextPaint.ascent();
        }
        switch (collapsedAbsGravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
            case Gravity.CENTER_HORIZONTAL:
                collapsedTitleDrawX = collapsedBounds.centerX() - (titleWidth / 2);
                collapsedSubtitleDrawX = collapsedBounds.centerX() - (subtitleWidth / 2);
                break;
            case Gravity.RIGHT:
                collapsedTitleDrawX = collapsedBounds.right - titleWidth;
                collapsedSubtitleDrawX = collapsedBounds.right - subtitleWidth;
                break;
            case Gravity.LEFT:
            default:
                collapsedTitleDrawX = collapsedBounds.left;
                collapsedSubtitleDrawX = collapsedBounds.left;
                break;
        }

        calculateUsingTitleTextSize(expandedTitleTextSize);
        calculateUsingSubtitleTextSize(expandedSubtitleTextSize);
        titleWidth = titleToDraw != null ? titleTextPaint.measureText(titleToDraw, 0, titleToDraw.length()) : 0;
        subtitleWidth = subtitleToDraw != null ? subtitleTextPaint.measureText(subtitleToDraw, 0, subtitleToDraw.length()) : 0;

        // dimension modification
        titleHeight = titleTextPaint.descent() - titleTextPaint.ascent();
        titleOffset = titleHeight / 2 - titleTextPaint.descent();
        subtitleHeight = subtitleTextPaint.descent() - subtitleTextPaint.ascent();
        subtitleOffset = subtitleHeight / 2 - subtitleTextPaint.descent();

        final int expandedAbsGravity = GravityCompat.getAbsoluteGravity(
                expandedTextGravity,
                isRtl ? ViewCompat.LAYOUT_DIRECTION_RTL : ViewCompat.LAYOUT_DIRECTION_LTR
        );
        if (isTitleOnly) {
            switch (expandedAbsGravity & Gravity.VERTICAL_GRAVITY_MASK) {
                case Gravity.BOTTOM:
                    expandedTitleDrawY = expandedBounds.bottom;
                    break;
                case Gravity.TOP:
                    expandedTitleDrawY = expandedBounds.top - titleTextPaint.ascent();
                    break;
                case Gravity.CENTER_VERTICAL:
                default:
                    float textHeight = titleTextPaint.descent() - titleTextPaint.ascent();
                    float textOffset = (textHeight / 2) - titleTextPaint.descent();
                    expandedTitleDrawY = expandedBounds.centerY() + textOffset;
                    break;
            }
        } else {
            switch (expandedAbsGravity & Gravity.VERTICAL_GRAVITY_MASK) {
                case Gravity.BOTTOM:
                    expandedTitleDrawY = expandedBounds.bottom - subtitleHeight - titleOffset;
                    expandedSubtitleDrawY = expandedBounds.bottom;
                    break;
                case Gravity.TOP:
                    expandedTitleDrawY = expandedBounds.top - titleTextPaint.ascent();
                    expandedSubtitleDrawY = expandedTitleDrawY + subtitleHeight + titleOffset;
                    break;
                case Gravity.CENTER_VERTICAL:
                default:
                    expandedTitleDrawY = expandedBounds.centerY() + titleOffset;
                    expandedSubtitleDrawY = expandedTitleDrawY + subtitleHeight + titleOffset;
                    break;
            }
        }
        switch (expandedAbsGravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
            case Gravity.CENTER_HORIZONTAL:
                expandedTitleDrawX = expandedBounds.centerX() - (titleWidth / 2);
                expandedSubtitleDrawX = expandedBounds.centerX() - (subtitleWidth / 2);
                break;
            case Gravity.RIGHT:
                expandedTitleDrawX = expandedBounds.right - titleWidth;
                expandedSubtitleDrawX = expandedBounds.right - subtitleWidth;
                break;
            case Gravity.LEFT:
            default:
                expandedTitleDrawX = expandedBounds.left;
                expandedSubtitleDrawX = expandedBounds.left;
                break;
        }

        // The bounds have changed so we need to clear the texture
        clearTexture();
        // Now reset the title size back to the original
        setInterpolatedTitleTextSize(currentTitleSize);
        setInterpolatedSubtitleTextSize(currentSubtitleSize);
    }

    private void interpolateBounds(float fraction) {
        currentBounds.left = lerp(expandedBounds.left, collapsedBounds.left, fraction, positionInterpolator);
        currentBounds.top = lerp(expandedTitleDrawY, collapsedTitleDrawY, fraction, positionInterpolator);
        currentBounds.right = lerp(expandedBounds.right, collapsedBounds.right, fraction, positionInterpolator);
        currentBounds.bottom = lerp(expandedBounds.bottom, collapsedBounds.bottom, fraction, positionInterpolator);
    }

    public void draw(@NonNull Canvas canvas) {
        final int saveCount = canvas.save();

        if (drawTitle && titleToDraw != null) {
            float titleX = currentTitleDrawX;
            float titleY = currentTitleDrawY;
            float subtitleX = currentSubtitleDrawX;
            float subtitleY = currentSubtitleDrawY;

            final boolean drawTitleTexture = useTexture && expandedTitleTexture != null;
            final boolean drawSubtitleTexture = useTexture && expandedSubtitleTexture != null;

            final float titleAscent;
            final float titleDescent;
            if (drawTitleTexture) {
                titleAscent = titleTextureAscent * titleScale;
                titleDescent = titleTextureDescent * titleScale;
            } else {
                titleAscent = titleTextPaint.ascent() * titleScale;
                titleDescent = titleTextPaint.descent() * titleScale;
            }

            if (DEBUG_DRAW) {
                // Just a debug tool, which drawn a magenta rect in the text bounds
                canvas.drawRect(currentBounds.left, titleY + titleAscent, currentBounds.right, titleY + titleDescent, DEBUG_DRAW_PAINT);
            }

            if (drawTitleTexture) {
                titleY += titleAscent;
            }

            // additional canvas save for subtitle
            if (subtitleToDraw != null) {
                final int subtitleSaveCount = canvas.save();

                if (subtitleScale != 1f) {
                    canvas.scale(subtitleScale, subtitleScale, subtitleX, subtitleY);
                }

                if (drawSubtitleTexture) {
                    // If we should use a texture, draw it instead of title
                    canvas.drawBitmap(expandedSubtitleTexture, subtitleX, subtitleY, subtitleTexturePaint);
                } else {
                    canvas.drawText(subtitleToDraw, 0, subtitleToDraw.length(), subtitleX, subtitleY, subtitleTextPaint);
                }
                canvas.restoreToCount(subtitleSaveCount);
            }

            if (titleScale != 1f) {
                canvas.scale(titleScale, titleScale, titleX, titleY);
            }

            if (drawTitleTexture) {
                // If we should use a texture, draw it instead of text
                canvas.drawBitmap(expandedTitleTexture, titleX, titleY, titleTexturePaint);
            } else {
                canvas.drawText(titleToDraw, 0, titleToDraw.length(), titleX, titleY, titleTextPaint);
            }
        }

        canvas.restoreToCount(saveCount);
    }

    private boolean calculateIsRtl(@NonNull CharSequence text) {
        final boolean defaultIsRtl = isDefaultIsRtl();
        return isRtlTextDirectionHeuristicsEnabled
                ? isTextDirectionHeuristicsIsRtl(text, defaultIsRtl)
                : defaultIsRtl;
    }

    private boolean isDefaultIsRtl() {
        return ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    private boolean isTextDirectionHeuristicsIsRtl(@NonNull CharSequence text, boolean defaultIsRtl) {
        return (defaultIsRtl
                ? TextDirectionHeuristicsCompat.FIRSTSTRONG_RTL
                : TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR)
                .isRtl(text, 0, text.length());
    }

    private void setInterpolatedTitleTextSize(float textSize) {
        calculateUsingTitleTextSize(textSize);

        // Use our texture if the scale isn't 1.0
        useTexture = USE_SCALING_TEXTURE && titleScale != 1f;

        if (useTexture) {
            // Make sure we have an expanded texture if needed
            ensureExpandedTitleTexture();
        }

        ViewCompat.postInvalidateOnAnimation(view);
    }

    private void setInterpolatedSubtitleTextSize(float textSize) {
        calculateUsingSubtitleTextSize(textSize);

        // Use our texture if the scale isn't 1.0
        useTexture = USE_SCALING_TEXTURE && subtitleScale != 1f;

        if (useTexture) {
            // Make sure we have an expanded texture if needed
            ensureExpandedSubtitleTexture();
        }

        ViewCompat.postInvalidateOnAnimation(view);
    }

    @SuppressWarnings("ReferenceEquality") // Matches the Typeface comparison in TextView
    private void calculateUsingTitleTextSize(final float size) {
        if (title == null) {
            return;
        }

        final float collapsedWidth = collapsedBounds.width();
        final float expandedWidth = expandedBounds.width();

        final float availableWidth;
        final float newTextSize;
        boolean updateDrawText = false;

        if (isClose(size, collapsedTitleTextSize)) {
            newTextSize = collapsedTitleTextSize;
            titleScale = 1f;
            if (currentTitleTypeface != collapsedTitleTypeface) {
                currentTitleTypeface = collapsedTitleTypeface;
                updateDrawText = true;
            }
            availableWidth = collapsedWidth;
        } else {
            newTextSize = expandedTitleTextSize;
            if (currentTitleTypeface != expandedTitleTypeface) {
                currentTitleTypeface = expandedTitleTypeface;
                updateDrawText = true;
            }
            if (isClose(size, expandedTitleTextSize)) {
                // If we're close to the expanded title size, snap to it and use a scale of 1
                titleScale = 1f;
            } else {
                // Else, we'll scale down from the expanded title size
                titleScale = size / expandedTitleTextSize;
            }

            final float textSizeRatio = collapsedTitleTextSize / expandedTitleTextSize;
            // This is the size of the expanded bounds when it is scaled to match the
            // collapsed title size
            final float scaledDownWidth = expandedWidth * textSizeRatio;

            if (scaledDownWidth > collapsedWidth) {
                // If the scaled down size is larger than the actual collapsed width, we need to
                // cap the available width so that when the expanded title scales down, it matches
                // the collapsed width
                availableWidth = Math.min(collapsedWidth / textSizeRatio, expandedWidth);
            } else {
                // Otherwise we'll just use the expanded width
                availableWidth = expandedWidth;
            }
        }

        if (availableWidth > 0) {
            updateDrawText = (currentTitleTextSize != newTextSize) || boundsChanged || updateDrawText;
            currentTitleTextSize = newTextSize;
            boundsChanged = false;
        }

        if (titleToDraw == null || updateDrawText) {
            titleTextPaint.setTextSize(currentTitleTextSize);
            titleTextPaint.setTypeface(currentTitleTypeface);
            // Use linear title scaling if we're scaling the canvas
            titleTextPaint.setLinearText(titleScale != 1f);

            // If we don't currently have title to draw, or the title size has changed, ellipsize...
            final CharSequence text =
                    TextUtils
                            .ellipsize(this.title, titleTextPaint, availableWidth, TextUtils.TruncateAt.END);
            if (!TextUtils.equals(text, titleToDraw)) {
                titleToDraw = text;
                isRtl = calculateIsRtl(titleToDraw);
            }
        }
    }

    @SuppressWarnings("ReferenceEquality") // Matches the Typeface comparison in TextView
    private void calculateUsingSubtitleTextSize(final float size) {
        if (subtitle == null) {
            return;
        }

        final float collapsedWidth = collapsedBounds.width();
        final float expandedWidth = expandedBounds.width();

        final float availableWidth;
        final float newTextSize;
        boolean updateDrawText = false;

        if (isClose(size, collapsedSubtitleTextSize)) {
            newTextSize = collapsedSubtitleTextSize;
            subtitleScale = 1f;
            if (currentSubtitleTypeface != collapsedSubtitleTypeface) {
                currentSubtitleTypeface = collapsedSubtitleTypeface;
                updateDrawText = true;
            }
            availableWidth = collapsedWidth;
        } else {
            newTextSize = expandedSubtitleTextSize;
            if (currentSubtitleTypeface != expandedSubtitleTypeface) {
                currentSubtitleTypeface = expandedSubtitleTypeface;
                updateDrawText = true;
            }
            if (isClose(size, expandedSubtitleTextSize)) {
                // If we're close to the expanded title size, snap to it and use a scale of 1
                subtitleScale = 1f;
            } else {
                // Else, we'll scale down from the expanded title size
                subtitleScale = size / expandedSubtitleTextSize;
            }

            final float textSizeRatio = collapsedSubtitleTextSize / expandedSubtitleTextSize;
            // This is the size of the expanded bounds when it is scaled to match the
            // collapsed title size
            final float scaledDownWidth = expandedWidth * textSizeRatio;

            if (scaledDownWidth > collapsedWidth) {
                // If the scaled down size is larger than the actual collapsed width, we need to
                // cap the available width so that when the expanded title scales down, it matches
                // the collapsed width
                availableWidth = Math.min(collapsedWidth / textSizeRatio, expandedWidth);
            } else {
                // Otherwise we'll just use the expanded width
                availableWidth = expandedWidth;
            }
        }

        if (availableWidth > 0) {
            updateDrawText = (currentSubtitleTextSize != newTextSize) || boundsChanged || updateDrawText;
            currentSubtitleTextSize = newTextSize;
            boundsChanged = false;
        }

        if (subtitleToDraw == null || updateDrawText) {
            subtitleTextPaint.setTextSize(currentSubtitleTextSize);
            subtitleTextPaint.setTypeface(currentSubtitleTypeface);
            // Use linear title scaling if we're scaling the canvas
            subtitleTextPaint.setLinearText(subtitleScale != 1f);

            // If we don't currently have title to draw, or the title size has changed, ellipsize...
            final CharSequence text =
                    TextUtils.ellipsize(this.subtitle, subtitleTextPaint, availableWidth, TextUtils.TruncateAt.END);
            if (!TextUtils.equals(text, subtitleToDraw)) {
                subtitleToDraw = text;
                isRtl = calculateIsRtl(subtitleToDraw);
            }
        }
    }

    private void ensureExpandedTitleTexture() {
        if (expandedTitleTexture != null || expandedBounds.isEmpty() || TextUtils.isEmpty(titleToDraw)) {
            return;
        }

        calculateOffsets(0f);
        titleTextureAscent = titleTextPaint.ascent();
        titleTextureDescent = titleTextPaint.descent();

        final int w = Math.round(titleTextPaint.measureText(titleToDraw, 0, titleToDraw.length()));
        final int h = Math.round(titleTextureDescent - titleTextureAscent);

        if (w <= 0 || h <= 0) {
            return; // If the width or height are 0, return
        }

        expandedTitleTexture = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(expandedTitleTexture);
        c.drawText(titleToDraw, 0, titleToDraw.length(), 0, h - titleTextPaint.descent(), titleTextPaint);

        if (titleTexturePaint == null) {
            // Make sure we have a paint
            titleTexturePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        }
    }

    private void ensureExpandedSubtitleTexture() {
        if (expandedSubtitleTexture != null || expandedBounds.isEmpty() || TextUtils.isEmpty(subtitleToDraw)) {
            return;
        }

        calculateOffsets(0f);
        subtitleTextureAscent = subtitleTextPaint.ascent();
        subtitleTextureDescent = subtitleTextPaint.descent();

        final int w = Math.round(subtitleTextPaint.measureText(subtitleToDraw, 0, subtitleToDraw.length()));
        final int h = Math.round(subtitleTextureDescent - subtitleTextureAscent);

        if (w <= 0 || h <= 0) {
            return; // If the width or height are 0, return
        }

        expandedSubtitleTexture = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(expandedSubtitleTexture);
        c.drawText(subtitleToDraw, 0, subtitleToDraw.length(), 0, h - subtitleTextPaint.descent(), subtitleTextPaint);

        if (subtitleTexturePaint == null) {
            // Make sure we have a paint
            subtitleTexturePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        }
    }

    public void recalculate() {
        if (view.getHeight() > 0 && view.getWidth() > 0) {
            // If we've already been laid out, calculate everything now otherwise we'll wait
            // until a layout
            calculateBaseOffsets();
            calculateCurrentOffsets();
        }
    }

    /**
     * Set the title to display
     *
     * @param title
     */
    public void setTitle(@Nullable CharSequence title) {
        if (title == null || !title.equals(this.title)) {
            this.title = title;
            titleToDraw = null;
            clearTexture();
            recalculate();
        }
    }

    @Nullable
    public CharSequence getTitle() {
        return title;
    }

    /**
     * Set the subtitle to display
     *
     * @param subtitle
     */
    public void setSubtitle(@Nullable CharSequence subtitle) {
        if (subtitle == null || !subtitle.equals(this.subtitle)) {
            this.subtitle = subtitle;
            subtitleToDraw = null;
            clearTexture();
            recalculate();
        }
    }

    @Nullable
    public CharSequence getSubtitle() {
        return subtitle;
    }

    private void clearTexture() {
        if (expandedTitleTexture != null) {
            expandedTitleTexture.recycle();
            expandedTitleTexture = null;
        }
        if (expandedSubtitleTexture != null) {
            expandedSubtitleTexture.recycle();
            expandedSubtitleTexture = null;
        }
    }

    /**
     * Returns true if {@code value} is 'close' to it's closest decimal value. Close is currently
     * defined as it's difference being < 0.001.
     */
    private static boolean isClose(float value, float targetValue) {
        return Math.abs(value - targetValue) < 0.001f;
    }

    public ColorStateList getExpandedTitleTextColor() {
        return expandedTitleTextColor;
    }

    public ColorStateList getExpandedSubtitleTextColor() {
        return expandedSubtitleTextColor;
    }

    public ColorStateList getCollapsedTitleTextColor() {
        return collapsedTitleTextColor;
    }

    public ColorStateList getCollapsedSubtitleTextColor() {
        return collapsedSubtitleTextColor;
    }

    /**
     * Blend {@code color1} and {@code color2} using the given ratio.
     *
     * @param ratio of which to blend. 0.0 will return {@code color1}, 0.5 will give an even blend,
     *              1.0 will return {@code color2}.
     */
    private static int blendColors(int color1, int color2, float ratio) {
        final float inverseRatio = 1f - ratio;
        float a = (Color.alpha(color1) * inverseRatio) + (Color.alpha(color2) * ratio);
        float r = (Color.red(color1) * inverseRatio) + (Color.red(color2) * ratio);
        float g = (Color.green(color1) * inverseRatio) + (Color.green(color2) * ratio);
        float b = (Color.blue(color1) * inverseRatio) + (Color.blue(color2) * ratio);
        return Color.argb((int) a, (int) r, (int) g, (int) b);
    }

    private static float lerp(
            float startValue, float endValue, float fraction, @Nullable TimeInterpolator interpolator) {
        if (interpolator != null) {
            fraction = interpolator.getInterpolation(fraction);
        }
        return AnimationUtils.lerp(startValue, endValue, fraction);
    }

    private static boolean rectEquals(@NonNull Rect r, int left, int top, int right, int bottom) {
        return !(r.left != left || r.top != top || r.right != right || r.bottom != bottom);
    }
}
