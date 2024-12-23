package com.game.fps.net.message;
public class NetworkMessage {
    public int playerId;  // 메시지를 보낸 플레이어의 ID

    // Kryo를 위한 기본 생성자 추가
    public NetworkMessage() {
        playerId = -1;
    }
}
