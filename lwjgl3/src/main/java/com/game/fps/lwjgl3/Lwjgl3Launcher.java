package com.game.fps.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.game.fps.Main;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {

        // 각 클라이언트마다 고유한 임시 디렉토리 사용
        String timestamp = String.valueOf(System.currentTimeMillis());
        String tempDir = System.getProperty("java.io.tmpdir") + "/libGDX-temp-" + timestamp;
        System.setProperty("java.io.tmpdir", tempDir);

        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new Main(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

        // 해상도 낮춤
        configuration.setWindowedMode(800, 600);

        // 메모리 사용량 최적화를 위한 설정
        configuration.setBackBufferConfig(8, 8, 8, 8, 16, 0, 2);
        configuration.useVsync(true);
        configuration.setForegroundFPS(30);

        // 오디오 설정 최적화(메모리 풀 크기 제한)
        configuration.setAudioConfig(16, 512, 9);

        String clientId = String.valueOf(System.currentTimeMillis() % 1000);
        configuration.setTitle("Tut3D - Client " + clientId);

        // 각 클라이언트마다 다른 위치에 창 생성
        configuration.setWindowPosition(
            (int) (Math.random() * 100),
            (int) (Math.random() * 100)
        );

        // 아이콘 설정
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}
