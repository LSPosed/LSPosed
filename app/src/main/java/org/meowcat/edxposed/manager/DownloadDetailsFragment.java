package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import org.meowcat.edxposed.manager.databinding.DownloadDetailsBinding;
import org.meowcat.edxposed.manager.databinding.DownloadMoreinfoBinding;
import org.meowcat.edxposed.manager.repo.Module;
import org.meowcat.edxposed.manager.repo.RepoParser;
import org.meowcat.edxposed.manager.util.NavUtil;
import org.meowcat.edxposed.manager.util.chrome.LinkTransformationMethod;

public class DownloadDetailsFragment extends Fragment {

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        DownloadDetailsActivity mActivity = (DownloadDetailsActivity) getActivity();
        if (mActivity == null) {
            return null;
        }
        final Module module = mActivity.getModule();
        if (module == null) {
            return null;
        }
        DownloadDetailsBinding binding = DownloadDetailsBinding.inflate(inflater, container, false);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            if (insets.getTappableElementInsets().bottom != insets.getSystemWindowInsetBottom()) {
                binding.getRoot().setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        binding.downloadTitle.setText(module.name);
        binding.downloadTitle.setTextIsSelectable(true);

        if (module.author != null && !module.author.isEmpty())
            binding.downloadAuthor.setText(getString(R.string.download_author, module.author));
        else
            binding.downloadAuthor.setText(R.string.download_unknown_author);

        if (module.description != null) {
            if (module.descriptionIsHtml) {
                binding.downloadDescription.setText(RepoParser.parseSimpleHtml(getActivity(), module.description, binding.downloadDescription));
                binding.downloadDescription.setTransformationMethod(new LinkTransformationMethod((BaseActivity) getActivity()));
                binding.downloadDescription.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                binding.downloadDescription.setText(module.description);
            }
            binding.downloadDescription.setTextIsSelectable(true);
        } else {
            binding.downloadDescription.setVisibility(View.GONE);
        }

        for (Pair<String, String> moreInfoEntry : module.moreInfo) {
            DownloadMoreinfoBinding moreinfoBinding = DownloadMoreinfoBinding.inflate(inflater, binding.downloadMoreinfoContainer, false);

            moreinfoBinding.title.setText(moreInfoEntry.first + ":");
            moreinfoBinding.message.setText(moreInfoEntry.second);

            final Uri link = NavUtil.parseURL(moreInfoEntry.second);
            if (link != null) {
                moreinfoBinding.message.setTextColor(moreinfoBinding.message.getLinkTextColors());
                moreinfoBinding.getRoot().setOnClickListener(v -> NavUtil.startURL((BaseActivity) getActivity(), link));
            }

            binding.downloadMoreinfoContainer.addView(moreinfoBinding.getRoot());
        }

        return binding.getRoot();
    }
}