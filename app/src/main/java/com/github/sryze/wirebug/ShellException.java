package com.github.sryze.wirebug;

public class ShellException extends Exception {

    public ShellException(Throwable throwable) {
        super("Error while executing command", throwable);
    }
}
