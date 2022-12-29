package android.content.pm;

import java.io.File;

public class PackageParser {
	public static class PackageLite {
		public final String packageName = null;
	}

	public final static class Package {
        public ApplicationInfo applicationInfo;
	}

	/** Before SDK21 */
	public static PackageLite parsePackageLite(String packageFile, int flags) {
		throw new UnsupportedOperationException("STUB");
	}

	/** Since SDK21 */
	public static PackageLite parsePackageLite(File packageFile, int flags) throws PackageParserException {
		throw new UnsupportedOperationException("STUB");
	}

	public Package parsePackage(File packageFile, int flags, boolean useCaches)
			throws PackageParserException {
		throw new UnsupportedOperationException("STUB");
	}

	/** Since SDK21 */
	public static class PackageParserException extends Exception {
	}
}
