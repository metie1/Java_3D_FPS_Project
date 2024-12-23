package com.game.fps.effects;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;

public class BulletTracer {
    private static final float LIFETIME = 0.15f;

    public final ModelInstance tracerModel;
    public final Vector3 start;
    public final Vector3 end;
    public float timeLeft;
    private final Vector3 direction = new Vector3();
    private final Quaternion rotation = new Quaternion();

    public BulletTracer(ModelInstance tracerModel, Vector3 start, Vector3 end) {
        this.tracerModel = tracerModel;
        this.start = new Vector3(start);
        this.end = new Vector3(end);
        this.timeLeft = LIFETIME;

        // 궤적의 방향과 길이 계산
        direction.set(end).sub(start);
        float length = direction.len();
        direction.nor();

        // 쿼터니언을 사용하여 방향 설정
        rotation.setFromCross(Vector3.Z, direction);

        // 변환 매트릭스 설정
        tracerModel.transform.setToTranslation(start);
        tracerModel.transform.rotate(rotation);
        tracerModel.transform.scale(length, 0.02f, 0.02f);
    }

    public void update(float deltaTime) {
        timeLeft -= deltaTime;
        float alpha = Math.min(1.0f, timeLeft / LIFETIME);
        // 시간이 지날수록 투명해짐 (y, z 스케일만 조정)
        tracerModel.transform.scale(1, alpha, alpha);
    }

    public boolean isExpired() {
        return timeLeft <= 0;
    }
}
