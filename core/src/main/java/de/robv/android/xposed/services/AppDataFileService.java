package de.robv.android.xposed.services;

import android.os.IBinder;

import java.io.IOException;

import io.github.xposed.xposedservice.IXposedService;

public class AppDataFileService extends BaseService{
    public AppDataFileService(IXposedService service) {

    }
    @Override
    public boolean checkFileAccess(String filename, int mode) {
        return false;
    }

    @Override
    public FileResult statFile(String filename) throws IOException {
        return null;
    }

    @Override
    public byte[] readFile(String filename) throws IOException {
        return new byte[0];
    }

    @Override
    public FileResult readFile(String filename, long previousSize, long previousTime) throws IOException {
        return null;
    }

    @Override
    public FileResult readFile(String filename, int offset, int length, long previousSize, long previousTime) throws IOException {
        return null;
    }
}
