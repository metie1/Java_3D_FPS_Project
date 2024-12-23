package com.game.fps.net.message;

public class DamageMessage extends NetworkMessage {
    public int targetId;  // 데미지를 받는 플레이어 ID
    public int attackerId; // 데미지를 주는 플레이어 ID
    public float damage;   // 데미지 양

    public DamageMessage() {
    }

    public DamageMessage(int targetId, int attackerId, float damage) {
        this.targetId = targetId;
        this.attackerId = attackerId;
        this.damage = damage;
    }
}
