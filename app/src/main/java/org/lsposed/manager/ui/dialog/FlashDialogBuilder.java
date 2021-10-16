package org.lsposed.manager.ui.dialog;

import static org.lsposed.manager.App.TAG;

import android.content.Context;
import android.graphics.Typeface;
import android.os.ParcelFileDescriptor;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.textview.MaterialTextView;

import org.lsposed.manager.App;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.DialogWarningBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import rikka.widget.borderview.BorderNestedScrollView;

public class FlashDialogBuilder extends BlurBehindDialogBuilder {
    private final String zipPath;
    private final TextView textView;
    private final BorderNestedScrollView rootView;

    public FlashDialogBuilder(@NonNull Context context) {
        super(context);
        var pref = App.getPreferences();
        var notes = pref.getString("release_notes", "");
        this.zipPath = pref.getString("zip_file", null);
        setTitle(R.string.update_lsposed);

        textView = new MaterialTextView(context);
        var text = notes + "\n\n\n" + context.getString(R.string.update_lsposed_msg) + "\n\n";
        textView.setText(text);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        LayoutInflater inflater = LayoutInflater.from(context);
        DialogWarningBinding binding = DialogWarningBinding.inflate(inflater, null, false);
        binding.container.addView(textView);
        rootView = binding.getRoot();
        setView(rootView);

        setNegativeButton(android.R.string.cancel, null);
        setPositiveButton(R.string.install, null);
        setCancelable(false);
    }

    @Override
    public AlertDialog show() {
        var dialog = super.show();
        var button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        rootView.setBorderVisibilityChangedListener((t, ot, b, ob) -> button.setEnabled(!b));
        button.setOnClickListener((v) -> {
            rootView.setBorderVisibilityChangedListener(null);
            setFlashView(v, dialog);
        });
        return dialog;
    }

    private void setFlashView(View view, AlertDialog dialog) {
        var positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        var negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        positiveButton.setEnabled(false);
        positiveButton.setText(android.R.string.ok);
        positiveButton.setOnClickListener((v) -> dialog.dismiss());
        negativeButton.setVisibility(View.GONE);

        textView.setText("");
        textView.setTypeface(Typeface.MONOSPACE);
        App.getExecutorService().submit(() -> flash(view, positiveButton));
    }

    private void flash(View view, Button button) {
        try {
            var pipe = ParcelFileDescriptor.createReliablePipe();
            var readSide = pipe[0];
            var writeSide = pipe[1];

            ConfigManager.flashZip(zipPath, writeSide);
            writeSide.close();

            var inputStream = new ParcelFileDescriptor.AutoCloseInputStream(readSide);
            var reader = new BufferedReader(new InputStreamReader(inputStream));

            for (var line = ""; line != null; line = reader.readLine()) {
                if (line.length() > 0) {
                    var showLine = line + "\n";
                    view.post(() -> {
                        textView.append(showLine);
                        rootView.fullScroll(View.FOCUS_DOWN);
                    });
                }
            }

            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "flash", e);
            view.post(() -> textView.append("\n\n" + e.getMessage()));
            rootView.fullScroll(View.FOCUS_DOWN);
        }

        view.post(() -> button.setEnabled(true));
    }
}
