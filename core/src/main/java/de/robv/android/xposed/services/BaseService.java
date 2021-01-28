package de.robv.android.xposed.services;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import de.robv.android.xposed.SELinuxHelper;

/**
 * General definition of a file access service provided by the Xposed framework.
 *
 * <p>References to a concrete subclass should generally be retrieved from {@link SELinuxHelper}.
 */
public abstract class BaseService {
	/** Flag for {@link #checkFileAccess}: Read access. */
	public static final int R_OK = 4;
	/** Flag for {@link #checkFileAccess}: Write access. */
	public static final int W_OK = 2;
	/** Flag for {@link #checkFileAccess}: Executable access. */
	public static final int X_OK = 1;
	/** Flag for {@link #checkFileAccess}: File/directory exists. */
	public static final int F_OK = 0;

	/**
	 * Checks whether the services accesses files directly (instead of using IPC).
	 *
	 * @return {@code true} in case direct access is possible.
	 */
	public boolean hasDirectFileAccess() {
		return false;
	}

	/**
	 * Check whether a file is accessible. SELinux might enforce stricter checks.
	 *
	 * @param filename The absolute path of the file to check.
	 * @param mode The mode for POSIX's {@code access()} function.
	 * @return The result of the {@code access()} function.
	 */
	public abstract boolean checkFileAccess(String filename, int mode);

	/**
	 * Check whether a file exists.
	 *
	 * @param filename The absolute path of the file to check.
	 * @return The result of the {@code access()} function.
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean checkFileExists(String filename) {
		return checkFileAccess(filename, F_OK);
	}

	/**
	 * Determine the size and modification time of a file.
	 *
	 * @param filename The absolute path of the file to check.
	 * @return A {@link FileResult} object holding the result.
	 * @throws IOException In case an error occurred while retrieving the information.
	 */
	public abstract FileResult statFile(String filename) throws IOException;

	/**
	 * Determine the size time of a file.
	 *
	 * @param filename The absolute path of the file to check.
	 * @return The file size.
	 * @throws IOException In case an error occurred while retrieving the information.
	 */
	public long getFileSize(String filename) throws IOException {
		return statFile(filename).size;
	}

	/**
	 * Determine the size time of a file.
	 *
	 * @param filename The absolute path of the file to check.
	 * @return The file modification time.
	 * @throws IOException In case an error occurred while retrieving the information.
	 */
	public long getFileModificationTime(String filename) throws IOException {
		return statFile(filename).mtime;
	}

	/**
	 * Read a file into memory.
	 *
	 * @param filename The absolute path of the file to read.
	 * @return A {@code byte} array with the file content.
	 * @throws IOException In case an error occurred while reading the file.
	 */
	public abstract byte[] readFile(String filename) throws IOException;

	/**
	 * Read a file into memory, but only if it has changed since the last time.
	 *
	 * @param filename The absolute path of the file to read.
	 * @param previousSize File size of last read.
	 * @param previousTime File modification time of last read.
	 * @return A {@link FileResult} object holding the result.
	 *         <p>The {@link FileResult#content} field might be {@code null} if the file
	 *         is unmodified ({@code previousSize} and {@code previousTime} are still valid).
	 * @throws IOException In case an error occurred while reading the file.
	 */
	public abstract FileResult readFile(String filename, long previousSize, long previousTime) throws IOException;

	/**
	 * Read a file into memory, optionally only if it has changed since the last time.
	 *
	 * @param filename The absolute path of the file to read.
	 * @param offset Number of bytes to skip at the beginning of the file.
	 * @param length Number of bytes to read (0 means read to end of file).
	 * @param previousSize Optional: File size of last read.
	 * @param previousTime Optional: File modification time of last read.
	 * @return A {@link FileResult} object holding the result.
	 *         <p>The {@link FileResult#content} field might be {@code null} if the file
	 *         is unmodified ({@code previousSize} and {@code previousTime} are still valid).
	 * @throws IOException In case an error occurred while reading the file.
	 */
	public abstract FileResult readFile(String filename, int offset, int length,
			long previousSize, long previousTime) throws IOException;

	/**
	 * Get a stream to the file content.
	 * Depending on the service, it may or may not be read completely into memory.
	 *
	 * @param filename The absolute path of the file to read.
	 * @return An {@link InputStream} to the file content.
	 * @throws IOException In case an error occurred while reading the file.
	 */
	public InputStream getFileInputStream(String filename) throws IOException {
		return new ByteArrayInputStream(readFile(filename));
	}

	/**
	 * Get a stream to the file content, but only if it has changed since the last time.
	 * Depending on the service, it may or may not be read completely into memory.
	 *
	 * @param filename The absolute path of the file to read.
	 * @param previousSize Optional: File size of last read.
	 * @param previousTime Optional: File modification time of last read.
	 * @return A {@link FileResult} object holding the result.
	 *         <p>The {@link FileResult#stream} field might be {@code null} if the file
	 *         is unmodified ({@code previousSize} and {@code previousTime} are still valid).
	 * @throws IOException In case an error occurred while reading the file.
	 */
	public FileResult getFileInputStream(String filename, long previousSize, long previousTime) throws IOException {
		FileResult result = readFile(filename, previousSize, previousTime);
		if (result.content == null)
			return result;
		return new FileResult(new ByteArrayInputStream(result.content), result.size, result.mtime);
	}


	// ----------------------------------------------------------------------------
	/*package*/ BaseService() {}

	/*package*/ static void ensureAbsolutePath(String filename) {
		if (!filename.startsWith("/")) {
			throw new IllegalArgumentException("Only absolute filenames are allowed: " + filename);
		}
	}

	/*package*/ static void throwCommonIOException(int errno, String errorMsg, String filename, String defaultText) throws IOException {
		switch (errno) {
			case 1: // EPERM
			case 13: // EACCES
				throw new FileNotFoundException(errorMsg != null ? errorMsg : "Permission denied: " + filename);
			case 2: // ENOENT
				throw new FileNotFoundException(errorMsg != null ? errorMsg : "No such file or directory: " + filename);
			case 12: // ENOMEM
				throw new OutOfMemoryError(errorMsg);
			case 21: // EISDIR
				throw new FileNotFoundException(errorMsg != null ? errorMsg : "Is a directory: " + filename);
			default:
				throw new IOException(errorMsg != null ? errorMsg : "Error " + errno + defaultText + filename);
		}
	}
}
