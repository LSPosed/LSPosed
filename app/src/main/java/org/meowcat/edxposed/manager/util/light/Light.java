package org.meowcat.edxposed.manager.util.light;

import android.annotation.SuppressLint;
import android.graphics.HardwareRenderer;
import android.os.Build;
import android.view.View;


@SuppressWarnings({"unchecked", "ConstantConditions"})
@SuppressLint("PrivateApi")
public class Light {

    public static boolean setLightSourceAlpha(View view, float ambientShadowAlpha, float spotShadowAlpha) {
        try {
            @SuppressWarnings("rawtypes") Class threadedRendererClass = Class.forName("android.view.ThreadedRenderer");

            Object threadedRenderer = Hack.into(View.class)
                    .method("getThreadedRenderer")
                    .returning(threadedRendererClass)
                    .withoutParams()
                    .invoke()
                    .on(view);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ((HardwareRenderer) threadedRenderer).setLightSourceAlpha(ambientShadowAlpha, spotShadowAlpha);
            } else {
                long mNativeProxy = (long) Hack.into(threadedRendererClass)
                        .field("mNativeProxy").ofType(long.class).get(threadedRenderer);

                float mLightRadius = (float) Hack.into(threadedRendererClass)
                        .field("mLightRadius")
                        .ofType(float.class)
                        .get(threadedRenderer);

                Hack.into(threadedRendererClass)
                        .staticMethod("nSetup")
                        .withParams(long.class, float.class, int.class, int.class)
                        .invoke(mNativeProxy, mLightRadius,
                                (int) (255 * ambientShadowAlpha + 0.5f), (int) (255 * spotShadowAlpha + 0.5f))
                        .statically();
            }
            return true;
        } catch (Throwable tr) {
            tr.printStackTrace();
            return false;
        }
    }
}
