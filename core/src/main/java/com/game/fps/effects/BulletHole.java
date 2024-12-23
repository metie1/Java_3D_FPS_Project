package com.game.fps.effects;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Matrix4;

public class BulletHole {
    private static final float LIFETIME = 10f;
    private static final float HOLE_OFFSET = 0.005f; // 벽으로부터의 거리

    public final ModelInstance decal;
    public final Vector3 position;
    public final Vector3 normal;
    public float timeLeft;
    private final Quaternion rotation = new Quaternion();

    public BulletHole(ModelInstance decal, Vector3 position, Vector3 normal) {
        this.decal = decal;
        this.position = new Vector3(position);
        this.normal = new Vector3(normal);
        this.timeLeft = LIFETIME;

        // 법선 벡터를 기준으로 회전 계산
        rotation.setFromCross(Vector3.Y, normal);

        // 변환 설정
        decal.transform.setToTranslation(position.cpy().add(normal.cpy().scl(HOLE_OFFSET)));
        decal.transform.rotate(rotation);

        // 초기 크기 설정 (약간 더 작게)
        float initialScale = 1.5f;
        decal.transform.scale(initialScale, initialScale, initialScale);
    }

    public void update(float deltaTime) {
        timeLeft -= deltaTime;

        // 시간이 지날수록 천천히 사라지도록
        if (timeLeft < 2.0f) {
            float alpha = timeLeft / 2.0f;
            float scale = 0.15f * alpha;
            decal.transform.setToTranslation(position.cpy().add(normal.cpy().scl(HOLE_OFFSET)));
            decal.transform.rotate(rotation);
            decal.transform.scale(scale, scale, scale);
        }
    }

    public boolean isExpired() {
        return timeLeft <= 0;
    }
}
