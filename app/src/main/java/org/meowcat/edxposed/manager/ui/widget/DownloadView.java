package org.meowcat.edxposed.manager.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.databinding.DownloadViewBinding;
import org.meowcat.edxposed.manager.ui.activity.BaseActivity;
import org.meowcat.edxposed.manager.util.NavUtil;

public class DownloadView extends LinearLayout {
    public Fragment fragment;
    private String mUrl = null;
    private String mTitle = null;
    private final DownloadViewBinding binding;

    public DownloadView(Context context, final AttributeSet attrs) {
        super(context, attrs);
        setFocusable(false);
        setOrientation(LinearLayout.VERTICAL);

        binding = DownloadViewBinding.inflate(LayoutInflater.from(context), this);

        binding.btnDownload.setOnClickListener(v -> NavUtil.startURL((BaseActivity) context, mUrl));
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
        if (mUrl != null) {
            binding.btnDownload.setVisibility(View.VISIBLE);
            binding.txtInfo.setVisibility(View.GONE);
        } else {
            binding.btnDownload.setVisibility(View.GONE);
            binding.txtInfo.setVisibility(View.VISIBLE);
            binding.txtInfo.setText(R.string.download_view_no_url);
        }
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }
}