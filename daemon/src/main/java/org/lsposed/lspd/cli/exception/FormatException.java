package org.lsposed.lspd.cli.exception;

import androidx.annotation.Nullable;

public class FormatException extends Exception {
    private final String message;

    public FormatException(String message) {
        super(message);
        this.message = message;
    }

    @Nullable
    @Override
    public String getMessage() {
        return message;
    }
}
