package com.game.fps.net.message;

import com.badlogic.gdx.math.Vector3;

public class SpawnBombMessage extends NetworkMessage {
    public Vector3 position;

    public SpawnBombMessage() {
        position = new Vector3();
    } // KryoNet에서 사용되는 기본 생성자


}
