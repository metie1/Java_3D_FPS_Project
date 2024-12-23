package com.game.fps.net.message;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Quaternion;
import com.game.fps.WeaponType;

public class PlayerStateMessage extends NetworkMessage {
    public Vector3 position;
    public Vector3 direction;
    public Quaternion rotation;  // 회전 정보 추가
    public float health;
    public String currentAnimation;
    public boolean isFiring;
    public boolean isCrouch;
    public WeaponType currentWeaponType;

    // 기본 생성자 추가 (Kryo serialization에 필요)
    public PlayerStateMessage() {
        position = new Vector3();
        direction = new Vector3();
        rotation = new Quaternion();  // 회전 정보 초기화
        currentAnimation = "";
        health = 1.0f;
        isFiring = false;
        isCrouch = false;
        currentWeaponType = null; // 초기값 null
    }
}
