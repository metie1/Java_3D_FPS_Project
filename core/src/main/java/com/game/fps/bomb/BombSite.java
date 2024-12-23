package com.game.fps.bomb;

import com.badlogic.gdx.math.Vector3;

public class BombSite {
    private Vector3 minCorner; // 설치 가능 영역의 최소 좌표
    private Vector3 maxCorner; // 설치 가능 영역의 최대 좌표
    private float fixedHeight; // 해당 BombSite의 고정 높이

    public BombSite(Vector3 corner1, Vector3 corner2, float fixedHeight) {
        this.minCorner = new Vector3(
            Math.min(corner1.x, corner2.x),
            Float.NEGATIVE_INFINITY, // y는 제한하지 않음
            Math.min(corner1.z, corner2.z)
        );
        this.maxCorner = new Vector3(
            Math.max(corner1.x, corner2.x),
            Float.POSITIVE_INFINITY, // y는 제한하지 않음
            Math.max(corner1.z, corner2.z)
        );
        this.fixedHeight = fixedHeight;
    }

    // 위치가 설치 가능 영역 내에 있는지 확인
    public boolean isWithinBounds(Vector3 position) {
        return position.x >= minCorner.x && position.x <= maxCorner.x &&
            position.z >= minCorner.z && position.z <= maxCorner.z;
    }

    // 고정 높이 반환
    public float getFixedHeight() {
        return fixedHeight;
    }
}
