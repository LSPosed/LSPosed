package de.robv.android.xposed.callbacks;

import de.robv.android.xposed.IXposedHookZygoteInit;

/**
 * This class is only used for internal purposes, except for the {@link StartupParam}
 * subclass.
 */
public abstract class XC_InitZygote extends XCallback implements IXposedHookZygoteInit {

    /**
     * Creates a new callback with default priority.
     *
     * @hide
     */
    @SuppressWarnings("deprecation")
    public XC_InitZygote() {
        super();
    }

    /**
     * Creates a new callback with a specific priority.
     *
     * @param priority See {@link XCallback#priority}.
     * @hide
     */
    public XC_InitZygote(int priority) {
        super(priority);
    }

    /**
     * @hide
     */
    @Override
    protected void call(Param param) throws Throwable {
        if (param instanceof StartupParam)
            initZygote((StartupParam) param);
    }
}
