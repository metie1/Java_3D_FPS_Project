package com.game.fps.net;

public class NetworkConstants {
    // 모든 네트워크 인터페이스에서 연결을 받기 위해 "0.0.0.0" 사용
    // public static final String DEFAULT_HOST = "127.0.0.1";  // localhost 대신 IP 주소 사용
    public static final String DEFAULT_HOST = "24.ip.gl.ply.gg";
    public static final int TCP_PORT = 14517;
    public static final int UDP_PORT = 14517;

    public static final int BUFFER_SIZE = 16384;
    public static final int OBJECT_BUFFER_SIZE = 8192;

    // Connection settings
    public static final int CONNECTION_TIMEOUT = 10000;  // 10 seconds
    public static final int CONNECTION_RETRY_DELAY = 1000;  // 1 second
    public static final int MAX_CONNECTION_RETRIES = 3;
}
