package com.game.fps.lwjgl3;

import com.badlogic.gdx.ApplicationLogger;

public class NoOpApplicationLogger implements ApplicationLogger {
    @Override
    public void log(String tag, String message) {
        // 아무것도 하지 않음 (로그 무시)
    }

    @Override
    public void log(String tag, String message, Throwable exception) {
        // 아무것도 하지 않음 (로그 무시)
    }

    @Override
    public void error(String tag, String message) {
        // 아무것도 하지 않음 (오류 로그 무시)
    }

    @Override
    public void error(String tag, String message, Throwable exception) {
        // 아무것도 하지 않음 (오류 로그 무시)
    }

    @Override
    public void debug(String tag, String message) {
        // 아무것도 하지 않음 (디버그 로그 무시)
    }

    @Override
    public void debug(String tag, String message, Throwable exception) {
        // 아무것도 하지 않음 (디버그 로그 무시)
    }
}
