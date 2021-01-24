package de.robv.android.xposed;


/**
 * Hook the initialization of Java-based command-line tools (like pm).
 *
 * @hide Xposed no longer hooks command-line tools, therefore this interface shouldn't be
 * implemented anymore.
 */
public interface IXposedHookCmdInit extends IXposedMod {
	/**
	 * Called very early during startup of a command-line tool.
	 * @param startupParam Details about the module itself and the started process.
	 * @throws Throwable Everything is caught, but it will prevent further initialization of the module.
	 */
	void initCmdApp(StartupParam startupParam) throws Throwable;

	/** Data holder for {@link #initCmdApp}. */
	final class StartupParam {
		/*package*/ StartupParam() {}

		/** The path to the module's APK. */
		public String modulePath;

		/** The class name of the tools that the hook was invoked for. */
		public String startClassName;
	}
}
