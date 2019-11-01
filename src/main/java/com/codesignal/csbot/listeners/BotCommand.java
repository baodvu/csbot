package com.codesignal.csbot.listeners;

public class BotCommand {
    private String commandName;
    private String commandParams;

    public String getCommandName() {
        return commandName;
    }

    BotCommand withCommandName(String commandName) {
        this.commandName = commandName;
        return this;
    }

    public String getCommandParams() {
        return commandParams;
    }

    BotCommand withCommandParams(String commandParams) {
        this.commandParams = commandParams;
        return this;
    }
}
