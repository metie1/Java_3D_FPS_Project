package com.game.fps.server;

import com.game.fps.net.GameServer;
import com.game.fps.net.NetworkConstants;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class ServerLauncher {
    private static GameServer server;
    private static volatile boolean running = true;

    public static void main(String[] args) {
        try {
            System.out.println("Initializing game server...");

            // 서버의 IP 주소들을 출력
            printServerIPs();

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
                System.out.println("\nServer is running on:");
                System.out.println("TCP port: " + NetworkConstants.TCP_PORT);
                System.out.println("UDP port: " + NetworkConstants.UDP_PORT);
                System.out.println("\nPlayers should connect using your IP address and these ports");
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

    private static void printServerIPs() {
        try {
            System.out.println("\nServer IP Addresses:");
            System.out.println("-------------------");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // 활성화된 네트워크 인터페이스만 확인
                if (iface.isUp() && !iface.isLoopback()) {
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        // IPv4 주소만 출력
                        if (addr.getHostAddress().indexOf(':') == -1) {
                            System.out.println(iface.getDisplayName() + ": " + addr.getHostAddress());
                        }
                    }
                }
            }
            System.out.println("-------------------");
        } catch (Exception e) {
            System.err.println("Error getting network interfaces: " + e.getMessage());
        }
    }
}
