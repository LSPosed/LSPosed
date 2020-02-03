package org.meowcat.edxposed.manager.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import androidx.fragment.app.FragmentManager;

import org.meowcat.edxposed.manager.CompileDialogFragment;
import org.meowcat.edxposed.manager.R;

public class CompileUtil {

    private static final String COMPILE_COMMAND_PREFIX = "cmd package ";
    private static final String COMPILE_RESET_COMMAND = COMPILE_COMMAND_PREFIX + "compile --reset ";
    private static final String COMPILE_SPEED_COMMAND = COMPILE_COMMAND_PREFIX + "compile -f -m speed ";
    private static final String COMPILE_DEXOPT_COMMAND = COMPILE_COMMAND_PREFIX + "force-dex-opt ";
    private static final String TAG_COMPILE_DIALOG = "compile_dialog";

    public static void reset(Context context, FragmentManager fragmentManager,
                             ApplicationInfo info) {
        compilePackageInBg(fragmentManager, info,
                context.getString(R.string.compile_reset_msg), COMPILE_RESET_COMMAND);
    }

    public static void compileSpeed(Context context, FragmentManager fragmentManager,
                                    ApplicationInfo info) {
        compilePackageInBg(fragmentManager, info,
                context.getString(R.string.compile_speed_msg), COMPILE_SPEED_COMMAND);
    }

    public static void compileDexopt(Context context, FragmentManager fragmentManager,
                                     ApplicationInfo info) {
        compilePackageInBg(fragmentManager, info,
                context.getString(R.string.compile_speed_msg), COMPILE_DEXOPT_COMMAND);
    }

    private static void compilePackageInBg(FragmentManager fragmentManager,
                                           ApplicationInfo info, String msg, String... commands) {
        CompileDialogFragment fragment = CompileDialogFragment.newInstance(info, msg, commands);
        fragment.show(fragmentManager, TAG_COMPILE_DIALOG);
    }

}
