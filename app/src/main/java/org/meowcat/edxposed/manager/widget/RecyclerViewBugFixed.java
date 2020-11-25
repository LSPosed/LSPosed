package org.meowcat.edxposed.manager.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.EdgeEffect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewBugFixed extends RecyclerView {


    public RecyclerViewBugFixed(@NonNull Context context) {
        super(context);
        setEdgeEffectFactory(getClipToPadding() ? new EdgeEffectFactory() : new AlwaysClipToPaddingEdgeEffectFactory());
    }

    public RecyclerViewBugFixed(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setEdgeEffectFactory(getClipToPadding() ? new EdgeEffectFactory() : new AlwaysClipToPaddingEdgeEffectFactory());
    }

    public RecyclerViewBugFixed(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setEdgeEffectFactory(getClipToPadding() ? new EdgeEffectFactory() : new AlwaysClipToPaddingEdgeEffectFactory());
    }

    public static class AlwaysClipToPaddingEdgeEffectFactory extends RecyclerView.EdgeEffectFactory {

        @NonNull
        @Override
        protected EdgeEffect createEdgeEffect(@NonNull RecyclerView view, int direction) {
            return new EdgeEffect(view.getContext()) {
                private boolean ensureSize = false;

                private void ensureSize() {
                    if (ensureSize) return;
                    ensureSize = true;
                    switch (direction) {
                        case DIRECTION_LEFT:
                        case DIRECTION_RIGHT:
                            setSize(view.getMeasuredHeight() - view.getPaddingTop() - view.getPaddingBottom(),
                                    view.getMeasuredWidth() - view.getPaddingLeft() - view.getPaddingRight());
                            break;
                        case DIRECTION_TOP:
                        case DIRECTION_BOTTOM:
                            setSize(view.getMeasuredWidth() - view.getPaddingLeft() - view.getPaddingRight(),
                                    view.getMeasuredHeight() - view.getPaddingTop() - view.getPaddingBottom());
                            break;
                    }
                }

                @Override
                public boolean draw(Canvas c) {
                    ensureSize();

                    int restore = c.save();
                    switch (direction) {
                        case DIRECTION_LEFT:
                            c.translate(view.getPaddingBottom(), 0f);
                            break;
                        case DIRECTION_TOP:
                            c.translate(view.getPaddingLeft(), view.getPaddingTop());
                            break;
                        case DIRECTION_RIGHT:
                            c.translate(-view.getPaddingTop(), 0f);
                            break;
                        case DIRECTION_BOTTOM:
                            c.translate(view.getPaddingRight(), view.getPaddingBottom());
                            break;
                    }
                    boolean res = super.draw(c);
                    c.restoreToCount(restore);
                    return res;
                }
            };
        }
    }
}
