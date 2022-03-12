package android.content.pm;

import java.io.File;

public class PackageParser {
	public static class PackageLite {
		public final String packageName = null;
	}

	/** Before SDK21 */
	public static PackageLite parsePackageLite(String packageFile, int flags) {
		throw new UnsupportedOperationException("STUB");
	}

	/** Since SDK21 */
	public static PackageLite parsePackageLite(File packageFile, int flags) throws PackageParserException {
		throw new UnsupportedOperationException("STUB");
	}

	/** Since SDK21 */
	@SuppressWarnings("serial")
	public static class PackageParserException extends Exception {
	}
}
