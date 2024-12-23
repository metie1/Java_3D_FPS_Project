package com.game.fps.net.message;

import com.badlogic.gdx.math.Vector3;

public class ShootMessage extends NetworkMessage {
    public int playerId;           // 총 쏜 플레이어 ID
    public Vector3 shootPosition;  // 총알 시작 위치
    public String weaponType;      // 무기 종류 (예: PISTOL, AK47)

    public ShootMessage() {
        shootPosition = new Vector3();
    }
}
