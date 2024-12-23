package com.game.fps.bomb;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.game.fps.GameObject;
import com.game.fps.GameObjectType;
import com.game.fps.World;
import com.game.fps.physics.CollisionShapeType;

public class BombManager {
    private Array<BombSite> bombSites; // 여러 BombSite를 저장
    private Array<GameObject> bombs; // 설치된 폭탄 목록

    public BombManager() {
        this.bombSites = new Array<>();
        this.bombs = new Array<>();
    }

    // BombSite 추가 메서드
    public void addBombSite(Vector3 corner1, Vector3 corner2, float fixedHeight) {
        bombSites.add(new BombSite(corner1, corner2, fixedHeight));
    }

    public boolean isBombSite(Vector3 position) {
        for (BombSite bombSite : bombSites) {
            return bombSite.isWithinBounds(position);
        }
        return false;
    }


    public boolean placeBomb(Vector3 position, World world) {
        for (BombSite bombSite : bombSites) {
            if (bombSite.isWithinBounds(position)) {
                // BombSite에 따라 높이 설정
                position.y = bombSite.getFixedHeight();

                GameObject bomb = world.spawnObject(
                    GameObjectType.TYPE_STATIC, // 정적 오브젝트
                    "bomb", // 폭탄 모델 이름
                    null, // 애니메이션 없음
                    CollisionShapeType.BOX, // 충돌 형태
                    true, // 물리 활성화
                    position // 폭탄 설치 위치
                );

                bombs.add(bomb);
                Gdx.app.log("BombManager", "Bomb placed at: " + position);
                return true; // 설치 성공
            }
        }

        Gdx.app.log("BombManager", "Cannot place bomb at: " + position + " (Outside all BombSites)");
        return false; // 설치 실패
    }

    // 설치된 폭탄 반환 메서드
    public GameObject getBomb() {
        if (bombs.size > 0) {
            return bombs.get(0); // 첫 번째 폭탄 반환
        }
        return null; // 폭탄이 없으면 null 반환
    }

}
