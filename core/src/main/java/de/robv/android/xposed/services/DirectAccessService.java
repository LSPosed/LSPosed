package de.robv.android.xposed.services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/** @hide */
public final class DirectAccessService extends BaseService {
	@Override
	public boolean hasDirectFileAccess() {
		return true;
	}

	@SuppressWarnings("RedundantIfStatement")
	@Override
	public boolean checkFileAccess(String filename, int mode) {
		File file = new File(filename);
		if (mode == F_OK && !file.exists()) return false;
		if ((mode & R_OK) != 0 && !file.canRead()) return false;
		if ((mode & W_OK) != 0 && !file.canWrite()) return false;
		if ((mode & X_OK) != 0 && !file.canExecute()) return false;
		return true;
	}

	@Override
	public boolean checkFileExists(String filename) {
		return new File(filename).exists();
	}

	@Override
	public FileResult statFile(String filename) throws IOException {
		File file = new File(filename);
		return new FileResult(file.length(), file.lastModified());
	}

	@Override
	public byte[] readFile(String filename) throws IOException {
		File file = new File(filename);
		byte content[] = new byte[(int)file.length()];
		FileInputStream fis = new FileInputStream(file);
		fis.read(content);
		fis.close();
		return content;
	}

	@Override
	public FileResult readFile(String filename, long previousSize, long previousTime) throws IOException {
		File file = new File(filename);
		long size = file.length();
		long time = file.lastModified();
		if (previousSize == size && previousTime == time)
			return new FileResult(size, time);
		return new FileResult(readFile(filename), size, time);
	}

	@Override
	public FileResult readFile(String filename, int offset, int length, long previousSize, long previousTime) throws IOException {
		File file = new File(filename);
		long size = file.length();
		long time = file.lastModified();
		if (previousSize == size && previousTime == time)
			return new FileResult(size, time);

		// Shortcut for the simple case
		if (offset <= 0 && length <= 0)
			return new FileResult(readFile(filename), size, time);

		// Check range
		if (offset > 0 && offset >= size) {
			throw new IllegalArgumentException("Offset " + offset + " is out of range for " + filename);
		} else if (offset < 0) {
			offset = 0;
		}

		if (length > 0 && (offset + length) > size) {
			throw new IllegalArgumentException("Length " + length + " is out of range for " + filename);
		} else if (length <= 0) {
			length = (int) (size - offset);
		}

		byte content[] = new byte[length];
		FileInputStream fis = new FileInputStream(file);
		fis.skip(offset);
		fis.read(content);
		fis.close();
		return new FileResult(content, size, time);
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation returns a BufferedInputStream instead of loading the file into memory.
	 */
	@Override
	public InputStream getFileInputStream(String filename) throws IOException {
		return new BufferedInputStream(new FileInputStream(filename), 16*1024);
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation returns a BufferedInputStream instead of loading the file into memory.
	 */
	@Override
	public FileResult getFileInputStream(String filename, long previousSize, long previousTime) throws IOException {
		File file = new File(filename);
		long size = file.length();
		long time = file.lastModified();
		if (previousSize == size && previousTime == time)
			return new FileResult(size, time);
		return new FileResult(new BufferedInputStream(new FileInputStream(filename), 16*1024), size, time);
	}
}
