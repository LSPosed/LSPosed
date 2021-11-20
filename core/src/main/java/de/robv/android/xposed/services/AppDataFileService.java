package de.robv.android.xposed.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import io.github.xposed.xposedservice.IXposedService;

public class AppDataFileService extends BaseService {
    private IXposedService service;

    public AppDataFileService(IXposedService service) {
        this.service = service;
    }

    @Override
    public boolean checkFileAccess(String filename, int mode) {
        ensureAbsolutePath(filename);
        return false;
    }

    @Override
    public FileResult statFile(String filename) {
        ensureAbsolutePath(filename);
        return null;
    }

    @Override
    public byte[] readFile(String filename) {
        ensureAbsolutePath(filename);
        return new byte[0];
    }

    @Override
    public FileResult readFile(String filename, long previousSize, long previousTime) {
        ensureAbsolutePath(filename);
        return null;
    }

    @Override
    public FileResult readFile(String filename, int offset, int length, long previousSize, long previousTime) {
        ensureAbsolutePath(filename);
        return null;
    }

    @Override
    public InputStream getFileInputStream(String filename) {
        ensureAbsolutePath(filename);
        return null;
    }

    @Override
    public FileResult getFileInputStream(String filename, long previousSize, long previousTime) {
        ensureAbsolutePath(filename);
        return null;
    }
}
