package com.game.fps.server;

import com.game.fps.net.GameServer;
import com.game.fps.net.NetworkConstants;

public class ServerLauncher {
    private static GameServer server;
    private static volatile boolean running = true;

    public static void main(String[] args) {
        try {
            System.out.println("Initializing game server...");

            // 서버 생성
            server = new GameServer();
            if (server == null) {
                throw new RuntimeException("Failed to create GameServer instance");
            }

            // 종료 훅 등록
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down server...");
                running = false;
                if (server != null) {
                    server.stop();
                }
            }));

            // 서버 시작
            try {
                server.start();
                System.out.println("Server is running on:");
                System.out.println("TCP port: " + NetworkConstants.TCP_PORT);
                System.out.println("UDP port: " + NetworkConstants.UDP_PORT);
                System.out.println("Press Ctrl+C to stop the server");

                // 서버 실행 상태 유지
                while (running) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        running = false;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error starting server: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("Fatal server error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
