package com.game.fps.net.message;

public class BombPlantedMessage extends NetworkMessage {
    public boolean bombPlanted;

    public BombPlantedMessage() {
    } // KryoNet에서 사용되는 기본 생성자

    public BombPlantedMessage(boolean bombPlanted) {
        this.bombPlanted = bombPlanted;
    }
}
