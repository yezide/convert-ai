package com.convertai.service;

@FunctionalInterface
public interface LogSink {
    void info(String message);
}
