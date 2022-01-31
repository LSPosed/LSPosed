package org.lsposed.lspd.cli.exception;

public class UnknownCommandException extends Exception {
    private final String mainCommand;
    private final String subCommand;
    public UnknownCommandException(String mainCommand, String subCommand) {
        super("unknown command: " + mainCommand + "-" + subCommand);
        this.mainCommand = mainCommand;
        this.subCommand = subCommand;
    }

    public String getMainCommand() {
        return mainCommand;
    }

    public String getSubCommand() {
        return subCommand;
    }
}
