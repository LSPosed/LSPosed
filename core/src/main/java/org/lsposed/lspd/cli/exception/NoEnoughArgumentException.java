package org.lsposed.lspd.cli.exception;

public class NoEnoughArgumentException extends Exception {

    private final String info;

    public NoEnoughArgumentException(String info) {
        super(info);
        this.info = info;
    }

    public String getInfo() {
        return info;
    }
}
