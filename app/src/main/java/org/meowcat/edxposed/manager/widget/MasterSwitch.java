package org.meowcat.edxposed.manager.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import org.meowcat.edxposed.manager.R;

public class MasterSwitch extends FrameLayout implements View.OnClickListener, Checkable {

    private TextView masterTitle;
    private SwitchCompat switchCompat;

    private String title;

    private OnCheckedChangeListener listener;

    private boolean isChecked;

    public MasterSwitch(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MasterSwitch(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public MasterSwitch(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.master_switch, this, true);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MasterSwitch);
        int colorOn = a.getColor(R.styleable.MasterSwitch_masterSwitchBackgroundOn, 0);
        int colorOff = a.getColor(R.styleable.MasterSwitch_masterSwitchBackgroundOff, 0);
        a.recycle();

        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_selected}, new ColorDrawable(colorOn));
        drawable.addState(new int[]{}, new ColorDrawable(colorOff));
        setBackground(drawable);

        masterTitle = findViewById(android.R.id.title);
        switchCompat = findViewById(R.id.switchWidget);

        setOnClickListener(this);
    }

    public void setTitle(String title) {
        this.title = title;
        masterTitle.setText(title);
    }

    private void updateViews() {
        if (switchCompat != null) {
            setSelected(isChecked);
            switchCompat.setChecked(isChecked);
        }
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void toggle() {
        setChecked(!isChecked);
    }

    @Override
    public void setChecked(boolean checked) {
        final boolean changed = isChecked != checked;
        if (changed) {
            isChecked = checked;
            updateViews();
            if (listener != null) {
                listener.onCheckedChanged(checked);
            }
        }
    }

    public void setOnCheckedChangedListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    public static abstract class OnCheckedChangeListener {
        public abstract void onCheckedChanged(boolean checked);
    }

    @Override
    public void onClick(View v) {
        toggle();
    }
}
