package com.google.android.material.appbar;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StyleRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.math.MathUtils;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.animation.AnimationUtils;
import com.google.android.material.internal.DescendantOffsetUtils;
import com.google.android.material.internal.SubtitleCollapsingTextHelper;
import com.google.android.material.internal.ThemeEnforcement;

import org.lsposed.manager.R;

/**
 * @see CollapsingToolbarLayout
 */
public class SubtitleCollapsingToolbarLayout extends FrameLayout {

    private static final int DEFAULT_SCRIM_ANIMATION_DURATION = 600;

    private boolean refreshToolbar = true;
    private int toolbarId;
    @Nullable
    private Toolbar toolbar;
    @Nullable
    private View toolbarDirectChild;
    private View dummyView;

    private int expandedMarginStart;
    private int expandedMarginTop;
    private int expandedMarginEnd;
    private int expandedMarginBottom;

    private final Rect tmpRect = new Rect();
    @NonNull
    final SubtitleCollapsingTextHelper collapsingTextHelper;
    private boolean collapsingTitleEnabled;
    private boolean drawCollapsingTitle;

    @Nullable
    private Drawable contentScrim;
    @Nullable
    Drawable statusBarScrim;
    private int scrimAlpha;
    private boolean scrimsAreShown;
    private ValueAnimator scrimAnimator;
    private long scrimAnimationDuration;
    private int scrimVisibleHeightTrigger = -1;

    private AppBarLayout.OnOffsetChangedListener onOffsetChangedListener;

    int currentOffset;

    @Nullable
    WindowInsetsCompat lastInsets;

    public SubtitleCollapsingToolbarLayout(@NonNull Context context) {
        this(context, null);
    }

    public SubtitleCollapsingToolbarLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SubtitleCollapsingToolbarLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        collapsingTextHelper = new SubtitleCollapsingTextHelper(this);
        collapsingTextHelper.setTextSizeInterpolator(AnimationUtils.DECELERATE_INTERPOLATOR);
        collapsingTextHelper.setRtlTextDirectionHeuristicsEnabled(false);

        TypedArray a = ThemeEnforcement.obtainStyledAttributes(
                context,
                attrs,
                R.styleable.SubtitleCollapsingToolbarLayout,
                defStyleAttr,
                R.style.Widget_Design_SubtitleCollapsingToolbar);

        collapsingTextHelper.setExpandedTextGravity(a.getInt(
                R.styleable.SubtitleCollapsingToolbarLayout_expandedTitleGravity,
                GravityCompat.START | Gravity.BOTTOM));
        collapsingTextHelper.setCollapsedTextGravity(a.getInt(
                R.styleable.SubtitleCollapsingToolbarLayout_collapsedTitleGravity,
                GravityCompat.START | Gravity.CENTER_VERTICAL));

        expandedMarginStart = expandedMarginTop = expandedMarginEnd = expandedMarginBottom =
                a.getDimensionPixelSize(R.styleable.SubtitleCollapsingToolbarLayout_expandedTitleMargin, 0);

        if (a.hasValue(R.styleable.SubtitleCollapsingToolbarLayout_expandedTitleMarginStart)) {
            expandedMarginStart =
                    a.getDimensionPixelSize(R.styleable.SubtitleCollapsingToolbarLayout_expandedTitleMarginStart, 0);
        }
        if (a.hasValue(R.styleable.SubtitleCollapsingToolbarLayout_expandedTitleMarginEnd)) {
            expandedMarginEnd =
                    a.getDimensionPixelSize(R.styleable.SubtitleCollapsingToolbarLayout_expandedTitleMarginEnd, 0);
        }
        if (a.hasValue(R.styleable.SubtitleCollapsingToolbarLayout_expandedTitleMarginTop)) {
            expandedMarginTop =
                    a.getDimensionPixelSize(R.styleable.SubtitleCollapsingToolbarLayout_expandedTitleMarginTop, 0);
        }
        if (a.hasValue(R.styleable.SubtitleCollapsingToolbarLayout_expandedTitleMarginBottom)) {
            expandedMarginBottom =
                    a.getDimensionPixelSize(R.styleable.SubtitleCollapsingToolbarLayout_expandedTitleMarginBottom, 0);
        }

        collapsingTitleEnabled = a.getBoolean(R.styleable.SubtitleCollapsingToolbarLayout_titleEnabled, true);
        setTitle(a.getText(R.styleable.SubtitleCollapsingToolbarLayout_title));
        setSubtitle(a.getText(R.styleable.SubtitleCollapsingToolbarLayout_subtitle));

        // First load the default text appearances
        collapsingTextHelper.setExpandedTitleTextAppearance(
                R.style.TextAppearance_Design_SubtitleCollapsingToolbar_ExpandedTitle);
        collapsingTextHelper.setCollapsedTitleTextAppearance(
                androidx.appcompat.R.style.TextAppearance_AppCompat_Widget_ActionBar_Title);
        collapsingTextHelper.setExpandedSubtitleTextAppearance(
                R.style.TextAppearance_Design_SubtitleCollapsingToolbar_ExpandedSubtitle);
        collapsingTextHelper.setCollapsedSubtitleTextAppearance(
                androidx.appcompat.R.style.TextAppearance_AppCompat_Widget_ActionBar_Subtitle);

        // Now overlay any custom text appearances
        if (a.hasValue(R.styleable.SubtitleCollapsingToolbarLayout_expandedTitleTextAppearance)) {
            collapsingTextHelper.setExpandedTitleTextAppearance(
                    a.getResourceId(R.styleable.SubtitleCollapsingToolbarLayout_expandedTitleTextAppearance, 0));
        }
        if (a.hasValue(R.styleable.SubtitleCollapsingToolbarLayout_collapsedTitleTextAppearance)) {
            collapsingTextHelper.setCollapsedTitleTextAppearance(
                    a.getResourceId(R.styleable.SubtitleCollapsingToolbarLayout_collapsedTitleTextAppearance, 0));
        }
        if (a.hasValue(R.styleable.SubtitleCollapsingToolbarLayout_expandedSubtitleTextAppearance)) {
            collapsingTextHelper.setExpandedSubtitleTextAppearance(
                    a.getResourceId(R.styleable.SubtitleCollapsingToolbarLayout_expandedSubtitleTextAppearance, 0));
        }
        if (a.hasValue(R.styleable.SubtitleCollapsingToolbarLayout_collapsedSubtitleTextAppearance)) {
            collapsingTextHelper.setCollapsedSubtitleTextAppearance(
                    a.getResourceId(R.styleable.SubtitleCollapsingToolbarLayout_collapsedSubtitleTextAppearance, 0));
        }

        scrimVisibleHeightTrigger = a
                .getDimensionPixelSize(R.styleable.SubtitleCollapsingToolbarLayout_scrimVisibleHeightTrigger, -1);

        scrimAnimationDuration = a.getInt(
                R.styleable.SubtitleCollapsingToolbarLayout_scrimAnimationDuration,
                DEFAULT_SCRIM_ANIMATION_DURATION);

        setContentScrim(a.getDrawable(R.styleable.SubtitleCollapsingToolbarLayout_contentScrim));
        setStatusBarScrim(a.getDrawable(R.styleable.SubtitleCollapsingToolbarLayout_statusBarScrim));

        toolbarId = a.getResourceId(R.styleable.SubtitleCollapsingToolbarLayout_toolbarId, -1);

        a.recycle();

        setWillNotDraw(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Add an OnOffsetChangedListener if possible
        final ViewParent parent = getParent();
        if (parent instanceof AppBarLayout) {
            // Copy over from the ABL whether we should fit system windows
            ViewCompat.setFitsSystemWindows(this, ViewCompat.getFitsSystemWindows((View) parent));

            if (onOffsetChangedListener == null) {
                onOffsetChangedListener = new OffsetUpdateListener();
            }
            ((AppBarLayout) parent).addOnOffsetChangedListener(onOffsetChangedListener);

            // We're attached, so lets request an inset dispatch
            ViewCompat.requestApplyInsets(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        // Remove our OnOffsetChangedListener if possible and it exists
        final ViewParent parent = getParent();
        if (onOffsetChangedListener != null && parent instanceof AppBarLayout) {
            ((AppBarLayout) parent).removeOnOffsetChangedListener(onOffsetChangedListener);
        }

        super.onDetachedFromWindow();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);

        // If we don't have a toolbar, the scrim will be not be drawn in drawChild() below.
        // Instead, we draw it here, before our collapsing text.
        ensureToolbar();
        if (toolbar == null && contentScrim != null && scrimAlpha > 0) {
            contentScrim.mutate().setAlpha(scrimAlpha);
            contentScrim.draw(canvas);
        }

        // Let the collapsing text helper draw its text
        if (collapsingTitleEnabled && drawCollapsingTitle) {
            collapsingTextHelper.draw(canvas);
        }

        // Now draw the status bar scrim
        if (statusBarScrim != null && scrimAlpha > 0) {
            final int topInset = lastInsets != null ? lastInsets.getSystemWindowInsetTop() : 0;
            if (topInset > 0) {
                statusBarScrim.setBounds(0, -currentOffset, getWidth(), topInset - currentOffset);
                statusBarScrim.mutate().setAlpha(scrimAlpha);
                statusBarScrim.draw(canvas);
            }
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        // This is a little weird. Our scrim needs to be behind the Toolbar (if it is present),
        // but in front of any other children which are behind it. To do this we intercept the
        // drawChild() call, and draw our scrim just before the Toolbar is drawn
        boolean invalidated = false;
        if (contentScrim != null && scrimAlpha > 0 && isToolbarChild(child)) {
            contentScrim.mutate().setAlpha(scrimAlpha);
            contentScrim.draw(canvas);
            invalidated = true;
        }
        return super.drawChild(canvas, child, drawingTime) || invalidated;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (contentScrim != null) {
            contentScrim.setBounds(0, 0, w, h);
        }
    }

    private void ensureToolbar() {
        if (!refreshToolbar) {
            return;
        }

        // First clear out the current Toolbar
        this.toolbar = null;
        toolbarDirectChild = null;

        if (toolbarId != -1) {
            // If we have an ID set, try and find it and it's direct parent to us
            this.toolbar = findViewById(toolbarId);
            if (this.toolbar != null) {
                toolbarDirectChild = findDirectChild(this.toolbar);
            }
        }

        if (this.toolbar == null) {
            // If we don't have an ID, or couldn't find a Toolbar with the correct ID, try and find
            // one from our direct children
            Toolbar toolbar = null;
            for (int i = 0, count = getChildCount(); i < count; i++) {
                final View child = getChildAt(i);
                if (child instanceof Toolbar) {
                    toolbar = (Toolbar) child;
                    break;
                }
            }
            this.toolbar = toolbar;
        }

        updateDummyView();
        refreshToolbar = false;
    }

    private boolean isToolbarChild(View child) {
        return (toolbarDirectChild == null || toolbarDirectChild == this)
                ? child == toolbar
                : child == toolbarDirectChild;
    }

    /**
     * Returns the direct child of this layout, which itself is the ancestor of the given view.
     */
    @NonNull
    private View findDirectChild(@NonNull final View descendant) {
        View directChild = descendant;
        for (ViewParent p = descendant.getParent(); p != this && p != null; p = p.getParent()) {
            if (p instanceof View) {
                directChild = (View) p;
            }
        }
        return directChild;
    }

    private void updateDummyView() {
        if (!collapsingTitleEnabled && dummyView != null) {
            // If we have a dummy view and we have our title disabled, remove it from its parent
            final ViewParent parent = dummyView.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(dummyView);
            }
        }
        if (collapsingTitleEnabled && toolbar != null) {
            if (dummyView == null) {
                dummyView = new View(getContext());
            }
            if (dummyView.getParent() == null) {
                toolbar.addView(dummyView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        ensureToolbar();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int mode = MeasureSpec.getMode(heightMeasureSpec);
        final int topInset = lastInsets != null ? lastInsets.getSystemWindowInsetTop() : 0;
        if (mode == MeasureSpec.UNSPECIFIED && topInset > 0) {
            // If we have a top inset and we're set to wrap_content height we need to make sure
            // we add the top inset to our height, therefore we re-measure
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight() + topInset, MeasureSpec.EXACTLY);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        // Set our minimum height to enable proper AppBarLayout collapsing
        if (toolbar != null) {
            if (toolbarDirectChild == null || toolbarDirectChild == this) {
                setMinimumHeight(getHeightWithMargins(toolbar));
            } else {
                setMinimumHeight(getHeightWithMargins(toolbarDirectChild));
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (lastInsets != null) {
            // Shift down any views which are not set to fit system windows
            final int insetTop = lastInsets.getSystemWindowInsetTop();
            for (int i = 0, z = getChildCount(); i < z; i++) {
                final View child = getChildAt(i);
                if (!ViewCompat.getFitsSystemWindows(child)) {
                    if (child.getTop() < insetTop) {
                        // If the child isn't set to fit system windows but is drawing within
                        // the inset offset it down
                        ViewCompat.offsetTopAndBottom(child, insetTop);
                    }
                }
            }
        }

        // Update our child view offset helpers so that they track the correct layout coordinates
        for (int i = 0, z = getChildCount(); i < z; i++) {
            getViewOffsetHelper(getChildAt(i)).onViewLayout();
        }

        // Update the collapsed bounds by getting its transformed bounds
        if (collapsingTitleEnabled && dummyView != null) {
            // We only draw the title if the dummy view is being displayed (Toolbar removes
            // views if there is no space)
            drawCollapsingTitle = ViewCompat.isAttachedToWindow(dummyView) && dummyView.getVisibility() == VISIBLE;

            if (drawCollapsingTitle) {
                final boolean isRtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;

                // Update the collapsed bounds
                final int maxOffset =
                        getMaxOffsetForPinChild(toolbarDirectChild != null ? toolbarDirectChild : toolbar);
                DescendantOffsetUtils.getDescendantRect(this, dummyView, tmpRect);
                collapsingTextHelper.setCollapsedBounds(
                        tmpRect.left + (isRtl ? toolbar.getTitleMarginEnd() : toolbar.getTitleMarginStart()),
                        tmpRect.top + maxOffset + toolbar.getTitleMarginTop(),
                        tmpRect.right - (isRtl ? toolbar.getTitleMarginStart() : toolbar.getTitleMarginEnd()),
                        tmpRect.bottom + maxOffset - toolbar.getTitleMarginBottom());

                // Update the expanded bounds
                collapsingTextHelper.setExpandedBounds(
                        isRtl ? expandedMarginEnd : expandedMarginStart,
                        tmpRect.top + expandedMarginTop,
                        right - left - (isRtl ? expandedMarginStart : expandedMarginEnd),
                        bottom - top - expandedMarginBottom);
                // Now recalculate using the new bounds
                collapsingTextHelper.recalculate();
            }
        }

        if (toolbar != null) {
            if (collapsingTitleEnabled && TextUtils.isEmpty(collapsingTextHelper.getTitle())) {
                // If we do not currently have a title, try and grab it from the Toolbar
                setTitle(toolbar.getTitle());
                setSubtitle(toolbar.getSubtitle());
            }
        }

        updateScrimVisibility();

        // Apply any view offsets, this should be done at the very end of layout
        for (int i = 0, z = getChildCount(); i < z; i++) {
            getViewOffsetHelper(getChildAt(i)).applyOffsets();
        }
    }

    private static int getHeightWithMargins(@NonNull final View view) {
        final ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof MarginLayoutParams) {
            final MarginLayoutParams mlp = (MarginLayoutParams) lp;
            return view.getMeasuredHeight() + mlp.topMargin + mlp.bottomMargin;
        }
        return view.getMeasuredHeight();
    }

    static ViewOffsetHelper getViewOffsetHelper(View view) {
        ViewOffsetHelper offsetHelper = (ViewOffsetHelper) view.getTag(com.google.android.material.R.id.view_offset_helper);
        if (offsetHelper == null) {
            offsetHelper = new ViewOffsetHelper(view);
            view.setTag(com.google.android.material.R.id.view_offset_helper, offsetHelper);
        }
        return offsetHelper;
    }

    /**
     * Sets the title to be displayed by this view, if enabled.
     *
     * @attr ref R.styleable#SubtitleCollapsingToolbarLayout_title
     * @see #setTitleEnabled(boolean)
     * @see #getTitle()
     */
    public void setTitle(@Nullable CharSequence title) {
        collapsingTextHelper.setTitle(title);
        updateContentDescriptionFromTitle();
    }

    /**
     * Returns the title currently being displayed by this view. If the title is not enabled, then
     * this will return {@code null}.
     *
     * @attr ref R.styleable#SubtitleCollapsingToolbarLayout_title
     */
    @Nullable
    public CharSequence getTitle() {
        return collapsingTitleEnabled ? collapsingTextHelper.getTitle() : null;
    }

    /**
     * Sets the subtitle to be displayed by this view, if enabled.
     *
     * @attr ref R.styleable#SubtitleCollapsingToolbarLayout_subtitle
     * @see #setTitleEnabled(boolean)
     * @see #getSubtitle()
     */
    public void setSubtitle(@Nullable CharSequence subtitle) {
        collapsingTextHelper.setSubtitle(subtitle);
        updateContentDescriptionFromTitle();
    }

    /**
     * Returns the subtitle currently being displayed by this view. If the title is not enabled, then
     * this will return {@code null}.
     *
     * @attr ref R.styleable#SubtitleCollapsingToolbarLayout_subtitle
     */
    @Nullable
    public CharSequence getSubtitle() {
        return collapsingTitleEnabled ? collapsingTextHelper.getSubtitle() : null;
    }

    /**
     * Sets whether this view should display its own title and subtitle.
     * <p>
     * <p>The title and subtitle displayed by this view will shrink and grow based on the scroll offset.
     *
     * @attr ref R.styleable#SubtitleCollapsingToolbarLayout_titleEnabled
     * @see #setTitle(CharSequence)
     * @see #setSubtitle(CharSequence)
     * @see #isTitleEnabled()
     */
    public void setTitleEnabled(boolean enabled) {
        if (enabled != collapsingTitleEnabled) {
            collapsingTitleEnabled = enabled;
            updateContentDescriptionFromTitle();
            updateDummyView();
            requestLayout();
        }
    }

    /**
     * Returns whether this view is currently displaying its own title and subtitle.
     *
     * @attr ref R.styleable#SubtitleCollapsingToolbarLayout_titleEnabled
     * @see #setTitleEnabled(boolean)
     */
    public boolean isTitleEnabled() {
        return collapsingTitleEnabled;
    }

    /**
     * Set whether the content scrim and/or status bar scrim should be shown or not. Any change in the
     * vertical scroll may overwrite this value. Any visibility change will be animated if this view
     * has already been laid out.
     *
     * @param shown whether the scrims should be shown
     * @see #getStatusBarScrim()
     * @see #getContentScrim()
     */
    public void setScrimsShown(boolean shown) {
        setScrimsShown(shown, ViewCompat.isLaidOut(this) && !isInEditMode());
    }

    /**
     * Set whether the content scrim and/or status bar scrim should be shown or not. Any change in the
     * vertical scroll may overwrite this value.
     *
     * @param shown   whether the scrims should be shown
     * @param animate whether to animate the visibility change
     * @see #getStatusBarScrim()
     * @see #getContentScrim()
     */
    public void setScrimsShown(boolean shown, boolean animate) {
        if (scrimsAreShown != shown) {
            if (animate) {
                animateScrim(shown ? 0xFF : 0x0);
            } else {
                setScrimAlpha(shown ? 0xFF : 0x0);
            }
            scrimsAreShown = shown;
        }
    }

    private void animateScrim(int targetAlpha) {
        ensureToolbar();
        if (scrimAnimator == null) {
            scrimAnimator = new ValueAnimator();
            scrimAnimator.setDuration(scrimAnimationDuration);
            scrimAnimator.setInterpolator(targetAlpha > scrimAlpha
                    ? AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
                    : AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR);
            scrimAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    setScrimAlpha((int) animator.getAnimatedValue());
                }
            });
        } else if (scrimAnimator.isRunning()) {
            scrimAnimator.cancel();
        }

        scrimAnimator.setIntValues(scrimAlpha, targetAlpha);
        scrimAnimator.start();
    }

    void setScrimAlpha(int alpha) {
        if (alpha != scrimAlpha) {
            final Drawable contentScrim = this.contentScrim;
            if (contentScrim != null && toolbar != null) {
                ViewCompat.postInvalidateOnAnimation(toolbar);
            }
            scrimAlpha = alpha;
            ViewCompat.postInvalidateOnAnimation(SubtitleCollapsingToolbarLayout.this);
        }
    }

    int getScrimAlpha() {
        return scrimAlpha;
    }

    /**
     * Set the drawable to use for the content scrim from resources. Providing null will disable the
     * scrim functionality.
     *
     * @param drawable the drawable to display
     * @attr ref R.styleable#SubtitleCollapsingToolbarLayout_contentScrim
     * @see #getContentScrim()
     */
    public void setContentScrim(@Nullable Drawable drawable) {
        if (contentScrim != drawable) {
            if (contentScrim != null) {
                contentScrim.setCallback(null);
            }
            contentScrim = drawable != null ? drawable.mutate() : null;
            if (contentScrim != null) {
                contentScrim.setBounds(0, 0, getWidth(), getHeight());
                contentScrim.setCallback(this);
                contentScrim.setAlpha(scrimAlpha);
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * Set the color to use for the content scrim.
     *
     * @param color the color to display
     * @attr ref R.styleable#SubtitleCollapsingToolbarLayout_contentScrim
     * @see #getContentScrim()
     */
    public void setContentScrimColor(@ColorInt int color) {
        setContentScrim(new ColorDrawable(color));
    }

    /**
     * Set the drawable to use for the content scrim from resources.
     *
     * @param resId drawable resource id
     * @attr ref R.styleable#SubtitleCollapsingToolbarLayout_contentScrim
     * @see #getContentScrim()
     */
    public void setContentScrimResource(@DrawableRes int resId) {
        setContentScrim(ContextCompat.getDrawable(getContext(), resId));
    }

    /**
     * Returns the drawable which is used for the foreground scrim.
     *
     * @attr ref R.styleable#SubtitleCollapsingToolbarLayout_contentScrim
     * @see #setContentScrim(Drawable)
     */
    @Nullable
    public Drawable getContentScrim() {
        return contentScrim;
    }

    /**
     * Set the drawable to use for the status bar scrim from resources. Providing null will disable
     * the scrim functionality.
     * <p>
     * <p>This scrim is only shown when we have been given a top system inset.
     *
     * @param drawable the drawable to display
     * @attr ref R.styleable#SubtitleCollapsingToolbarLayout_statusBarScrim
     * @see #getStatusBarScrim()
     */
    public void setStatusBarScrim(@Nullable Drawable drawable) {
        if (statusBarScrim != drawable) {
            if (statusBarScrim != null) {
                statusBarScrim.setCallback(null);
            }
            statusBarScrim = drawable != null ? drawable.mutate() : null;
            if (statusBarScrim != null) {
                if (statusBarScrim.isStateful()) {
                    statusBarScrim.setState(getDrawableState());
                }
                DrawableCompat.setLayoutDirection(statusBarScrim, ViewCompat.getLayoutDirection(this));
                statusBarScrim.setVisible(getVisibility() == VISIBLE, false);
                statusBarScrim.setCallback(this);
                statusBarScrim.setAlpha(scrimAlpha);
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        final int[] state = getDrawableState();
        boolean changed = false;

        Drawable d = statusBarScrim;
        if (d != null && d.isStateful()) {
            changed |= d.setState(state);
        }
        d = contentScrim;
        if (d != null && d.isStateful()) {
            changed |= d.setState(state);
        }
        if (collapsingTextHelper != null) {
            changed |= collapsingTextHelper.setState(state);
        }

        if (changed) {
            invalidate();
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == contentScrim || who == statusBarScrim;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);

        final boolean visible = visibility == VISIBLE;
        if (statusBarScrim != null && statusBarScrim.isVisible() != visible) {
            statusBarScrim.setVisible(visible, false);
        }
        if (contentScrim != null && contentScrim.isVisible() != visible) {
            contentScrim.setVisible(visible, false);
        }
    }

    /**
     * Set the color to use for the status bar scrim.
     * <p>
     * <p>This scrim is only shown when we have been given a top system inset.
     *
     * @param color the color to display
     * @attr ref R.styleable#SubtitleCollapsingToolbarLayout_statusBarScrim
     * @see #getStatusBarScrim()
     */
    public void setStatusBarScrimColor(@ColorInt int color) {
        setStatusBarScrim(new ColorDrawable(color));
    }

    /**
     * Set the drawable to use for the content scrim from resources.
     *
     * @param resId drawable resource id
     * @attr ref R.styleable#SubtitleCollapsingToolbarLayout_statusBarScrim
     * @see #getStatusBarScrim()
     */
    public void setStatusBarScrimResource(@DrawableRes int resId) {
        setStatusBarScrim(ContextCompat.getDrawable(getContext(), resId));
    }

    /**
     * Returns the drawable which is used for the status bar scrim.
     *
     * @attr ref R.styleable#SubtitleCollapsingToolbarLayout_statusBarScrim
     * @see #setStatusBarScrim(Drawable)
     */
    @Nullable
    public Drawable getStatusBarScrim() {
        return statusBarScrim;
    }

    /**
     * Sets the text color and size for the collapsed title from the specified TextAppearance
     * resource.
     *
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_collapsedTitleTextAppearance
     */
    public void setCollapsedTitleTextAppearance(@StyleRes int resId) {
        collapsingTextHelper.setCollapsedTitleTextAppearance(resId);
    }

    /**
     * Sets the text color of the collapsed title.
     *
     * @param color The new text color in ARGB format
     */
    public void setCollapsedTitleTextColor(@ColorInt int color) {
        setCollapsedTitleTextColor(ColorStateList.valueOf(color));
    }

    /**
     * Sets the text colors of the collapsed title.
     *
     * @param colors ColorStateList containing the new text colors
     */
    public void setCollapsedTitleTextColor(@NonNull ColorStateList colors) {
        collapsingTextHelper.setCollapsedTitleTextColor(colors);
    }

    /**
     * Sets the text color and size for the collapsed subtitle from the specified TextAppearance
     * resource.
     *
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_collapsedSubtitleTextAppearance
     */
    public void setCollapsedSubtitleTextAppearance(@StyleRes int resId) {
        collapsingTextHelper.setCollapsedSubtitleTextAppearance(resId);
    }

    /**
     * Sets the text color of the collapsed subtitle.
     *
     * @param color The new text color in ARGB format
     */
    public void setCollapsedSubtitleTextColor(@ColorInt int color) {
        setCollapsedSubtitleTextColor(ColorStateList.valueOf(color));
    }

    /**
     * Sets the text colors of the collapsed subtitle.
     *
     * @param colors ColorStateList containing the new text colors
     */
    public void setCollapsedSubtitleTextColor(@NonNull ColorStateList colors) {
        collapsingTextHelper.setCollapsedSubtitleTextColor(colors);
    }

    /**
     * Sets the horizontal alignment of the collapsed title and the vertical gravity that will be used
     * when there is extra space in the collapsed bounds beyond what is required for the title itself.
     *
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_collapsedTitleGravity
     */
    public void setCollapsedTitleGravity(int gravity) {
        collapsingTextHelper.setCollapsedTextGravity(gravity);
    }

    /**
     * Returns the horizontal and vertical alignment for title when collapsed.
     *
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_collapsedTitleGravity
     */
    public int getCollapsedTitleGravity() {
        return collapsingTextHelper.getCollapsedTextGravity();
    }

    /**
     * Sets the text color and size for the expanded title from the specified TextAppearance resource.
     *
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_expandedTitleTextAppearance
     */
    public void setExpandedTitleTextAppearance(@StyleRes int resId) {
        collapsingTextHelper.setExpandedTitleTextAppearance(resId);
    }

    /**
     * Sets the text color of the expanded title.
     *
     * @param color The new text color in ARGB format
     */
    public void setExpandedTitleTextColor(@ColorInt int color) {
        setExpandedTitleTextColor(ColorStateList.valueOf(color));
    }

    /**
     * Sets the text colors of the expanded title.
     *
     * @param colors ColorStateList containing the new text colors
     */
    public void setExpandedTitleTextColor(@NonNull ColorStateList colors) {
        collapsingTextHelper.setExpandedTitleTextColor(colors);
    }

    /**
     * Sets the text color and size for the expanded subtitle from the specified TextAppearance resource.
     *
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_expandedSubtitleTextAppearance
     */
    public void setExpandedSubtitleTextAppearance(@StyleRes int resId) {
        collapsingTextHelper.setExpandedSubtitleTextAppearance(resId);
    }

    /**
     * Sets the text color of the expanded subtitle.
     *
     * @param color The new text color in ARGB format
     */
    public void setExpandedSubtitleTextColor(@ColorInt int color) {
        setExpandedSubtitleTextColor(ColorStateList.valueOf(color));
    }

    /**
     * Sets the text colors of the expanded subtitle.
     *
     * @param colors ColorStateList containing the new text colors
     */
    public void setExpandedSubtitleTextColor(@NonNull ColorStateList colors) {
        collapsingTextHelper.setExpandedSubtitleTextColor(colors);
    }

    /**
     * Sets the horizontal alignment of the expanded title and the vertical gravity that will be used
     * when there is extra space in the expanded bounds beyond what is required for the title itself.
     *
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_expandedTitleGravity
     */
    public void setExpandedTitleGravity(int gravity) {
        collapsingTextHelper.setExpandedTextGravity(gravity);
    }

    /**
     * Returns the horizontal and vertical alignment for title when expanded.
     *
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_expandedTitleGravity
     */
    public int getExpandedTitleGravity() {
        return collapsingTextHelper.getExpandedTextGravity();
    }

    /**
     * Set the typeface to use for the collapsed title.
     *
     * @param typeface typeface to use, or {@code null} to use the default.
     */
    public void setCollapsedTitleTypeface(@Nullable Typeface typeface) {
        collapsingTextHelper.setCollapsedTitleTypeface(typeface);
    }

    /**
     * Returns the typeface used for the collapsed title.
     */
    @NonNull
    public Typeface getCollapsedTitleTypeface() {
        return collapsingTextHelper.getCollapsedTitleTypeface();
    }

    /**
     * Set the typeface to use for the expanded title.
     *
     * @param typeface typeface to use, or {@code null} to use the default.
     */
    public void setExpandedTitleTypeface(@Nullable Typeface typeface) {
        collapsingTextHelper.setExpandedTitleTypeface(typeface);
    }

    /**
     * Returns the typeface used for the expanded title.
     */
    @NonNull
    public Typeface getExpandedTitleTypeface() {
        return collapsingTextHelper.getExpandedTitleTypeface();
    }

    /**
     * Set the typeface to use for the collapsed title.
     *
     * @param typeface typeface to use, or {@code null} to use the default.
     */
    public void setCollapsedSubtitleTypeface(@Nullable Typeface typeface) {
        collapsingTextHelper.setCollapsedSubtitleTypeface(typeface);
    }

    /**
     * Returns the typeface used for the collapsed title.
     */
    @NonNull
    public Typeface getCollapsedSubtitleTypeface() {
        return collapsingTextHelper.getCollapsedSubtitleTypeface();
    }

    /**
     * Set the typeface to use for the expanded title.
     *
     * @param typeface typeface to use, or {@code null} to use the default.
     */
    public void setExpandedSubtitleTypeface(@Nullable Typeface typeface) {
        collapsingTextHelper.setExpandedSubtitleTypeface(typeface);
    }

    /**
     * Returns the typeface used for the expanded title.
     */
    @NonNull
    public Typeface getExpandedSubtitleTypeface() {
        return collapsingTextHelper.getExpandedSubtitleTypeface();
    }

    /**
     * Sets the expanded title margins.
     *
     * @param start  the starting title margin in pixels
     * @param top    the top title margin in pixels
     * @param end    the ending title margin in pixels
     * @param bottom the bottom title margin in pixels
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_expandedTitleMargin
     * @see #getExpandedTitleMarginStart()
     * @see #getExpandedTitleMarginTop()
     * @see #getExpandedTitleMarginEnd()
     * @see #getExpandedTitleMarginBottom()
     */
    public void setExpandedTitleMargin(int start, int top, int end, int bottom) {
        expandedMarginStart = start;
        expandedMarginTop = top;
        expandedMarginEnd = end;
        expandedMarginBottom = bottom;
        requestLayout();
    }

    /**
     * @return the starting expanded title margin in pixels
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_expandedTitleMarginStart
     * @see #setExpandedTitleMarginStart(int)
     */
    public int getExpandedTitleMarginStart() {
        return expandedMarginStart;
    }

    /**
     * Sets the starting expanded title margin in pixels.
     *
     * @param margin the starting title margin in pixels
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_expandedTitleMarginStart
     * @see #getExpandedTitleMarginStart()
     */
    public void setExpandedTitleMarginStart(int margin) {
        expandedMarginStart = margin;
        requestLayout();
    }

    /**
     * @return the top expanded title margin in pixels
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_expandedTitleMarginTop
     * @see #setExpandedTitleMarginTop(int)
     */
    public int getExpandedTitleMarginTop() {
        return expandedMarginTop;
    }

    /**
     * Sets the top expanded title margin in pixels.
     *
     * @param margin the top title margin in pixels
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_expandedTitleMarginTop
     * @see #getExpandedTitleMarginTop()
     */
    public void setExpandedTitleMarginTop(int margin) {
        expandedMarginTop = margin;
        requestLayout();
    }

    /**
     * @return the ending expanded title margin in pixels
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_expandedTitleMarginEnd
     * @see #setExpandedTitleMarginEnd(int)
     */
    public int getExpandedTitleMarginEnd() {
        return expandedMarginEnd;
    }

    /**
     * Sets the ending expanded title margin in pixels.
     *
     * @param margin the ending title margin in pixels
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_expandedTitleMarginEnd
     * @see #getExpandedTitleMarginEnd()
     */
    public void setExpandedTitleMarginEnd(int margin) {
        expandedMarginEnd = margin;
        requestLayout();
    }

    /**
     * @return the bottom expanded title margin in pixels
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_expandedTitleMarginBottom
     * @see #setExpandedTitleMarginBottom(int)
     */
    public int getExpandedTitleMarginBottom() {
        return expandedMarginBottom;
    }

    /**
     * Sets the bottom expanded title margin in pixels.
     *
     * @param margin the bottom title margin in pixels
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_expandedTitleMarginBottom
     * @see #getExpandedTitleMarginBottom()
     */
    public void setExpandedTitleMarginBottom(int margin) {
        expandedMarginBottom = margin;
        requestLayout();
    }

    /**
     * Sets whether {@code TextDirectionHeuristics} should be used to determine whether the title text
     * is RTL. Experimental Feature.
     */
    public void setRtlTextDirectionHeuristicsEnabled(boolean rtlTextDirectionHeuristicsEnabled) {
        collapsingTextHelper.setRtlTextDirectionHeuristicsEnabled(rtlTextDirectionHeuristicsEnabled);
    }

    /**
     * Gets whether {@code TextDirectionHeuristics} should be used to determine whether the title text
     * is RTL. Experimental Feature.
     */
    public boolean isRtlTextDirectionHeuristicsEnabled() {
        return collapsingTextHelper.isRtlTextDirectionHeuristicsEnabled();
    }

    /**
     * Set the amount of visible height in pixels used to define when to trigger a scrim visibility
     * change.
     * <p>
     * <p>If the visible height of this view is less than the given value, the scrims will be made
     * visible, otherwise they are hidden.
     *
     * @param height value in pixels used to define when to trigger a scrim visibility change
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_expandedTitleMarginEnd
     */
    public void setScrimVisibleHeightTrigger(@IntRange(from = 0) final int height) {
        if (scrimVisibleHeightTrigger != height) {
            scrimVisibleHeightTrigger = height;
            // Update the scrim visibility
            updateScrimVisibility();
        }
    }

    /**
     * Returns the amount of visible height in pixels used to define when to trigger a scrim
     * visibility change.
     *
     * @see #setScrimVisibleHeightTrigger(int)
     */
    public int getScrimVisibleHeightTrigger() {
        if (scrimVisibleHeightTrigger >= 0) {
            // If we have one explicitly set, return it
            return scrimVisibleHeightTrigger;
        }

        // Otherwise we'll use the default computed value
        final int insetTop = lastInsets != null ? lastInsets.getSystemWindowInsetTop() : 0;

        final int minHeight = ViewCompat.getMinimumHeight(this);
        if (minHeight > 0) {
            // If we have a minHeight set, lets use 2 * minHeight (capped at our height)
            return Math.min((minHeight * 2) + insetTop, getHeight());
        }

        // If we reach here then we don't have a min height set. Instead we'll take a
        // guess at 1/3 of our height being visible
        return getHeight() / 3;
    }

    /**
     * Set the duration used for scrim visibility animations.
     *
     * @param duration the duration to use in milliseconds
     * @attr ref com.google.android.material.R.styleable#SubtitleCollapsingToolbarLayout_scrimAnimationDuration
     */
    public void setScrimAnimationDuration(@IntRange(from = 0) final long duration) {
        scrimAnimationDuration = duration;
    }

    /**
     * Returns the duration in milliseconds used for scrim visibility animations.
     */
    public long getScrimAnimationDuration() {
        return scrimAnimationDuration;
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    public FrameLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected FrameLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public static class LayoutParams extends CollapsingToolbarLayout.LayoutParams {
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height, gravity);
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        @RequiresApi(19)
        public LayoutParams(FrameLayout.LayoutParams source) {
            super(source);
        }
    }

    /**
     * Show or hide the scrims if needed
     */
    final void updateScrimVisibility() {
        if (contentScrim != null || statusBarScrim != null) {
            setScrimsShown(getHeight() + currentOffset < getScrimVisibleHeightTrigger());
        }
    }

    final int getMaxOffsetForPinChild(View child) {
        final ViewOffsetHelper offsetHelper = getViewOffsetHelper(child);
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        return getHeight() - offsetHelper.getLayoutTop() - child.getHeight() - lp.bottomMargin;
    }

    private void updateContentDescriptionFromTitle() {
        // Set this layout's contentDescription to match the title if it's shown by CollapsingTextHelper
        setContentDescription(getTitle());
    }

    private class OffsetUpdateListener implements AppBarLayout.OnOffsetChangedListener {
        OffsetUpdateListener() {
        }

        @Override
        public void onOffsetChanged(AppBarLayout layout, int verticalOffset) {
            currentOffset = verticalOffset;

            final int insetTop = lastInsets != null ? lastInsets.getSystemWindowInsetTop() : 0;

            for (int i = 0, z = getChildCount(); i < z; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                final ViewOffsetHelper offsetHelper = getViewOffsetHelper(child);

                switch (lp.collapseMode) {
                    case LayoutParams.COLLAPSE_MODE_PIN:
                        offsetHelper.setTopAndBottomOffset(
                                MathUtils.clamp(-verticalOffset, 0, getMaxOffsetForPinChild(child)));
                        break;
                    case LayoutParams.COLLAPSE_MODE_PARALLAX:
                        offsetHelper.setTopAndBottomOffset(Math.round(-verticalOffset * lp.parallaxMult));
                        break;
                    default:
                        break;
                }
            }

            // Show or hide the scrims if needed
            updateScrimVisibility();

            if (statusBarScrim != null && insetTop > 0) {
                ViewCompat.postInvalidateOnAnimation(SubtitleCollapsingToolbarLayout.this);
            }

            // Update the collapsing text's fraction
            final int expandRange = getHeight()
                    - ViewCompat.getMinimumHeight(SubtitleCollapsingToolbarLayout.this)
                    - insetTop;
            collapsingTextHelper.setExpansionFraction(Math.abs(verticalOffset) / (float) expandRange);
        }
    }
}
