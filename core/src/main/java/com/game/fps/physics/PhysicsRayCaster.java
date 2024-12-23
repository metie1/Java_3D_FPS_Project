package com.game.fps.physics;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.github.antzGames.gdx.ode4j.math.DVector3;
import com.github.antzGames.gdx.ode4j.ode.*;
import com.game.fps.GameObject;
import com.game.fps.Settings;

public class PhysicsRayCaster implements Disposable {

    private final PhysicsWorld physicsWorld;       // 참조 전용
    private final DRay groundRay;
    private final DRay shootRay;
    private GameObject player;

    public PhysicsRayCaster(PhysicsWorld physicsWorld) {
        this.physicsWorld = physicsWorld;
        groundRay = OdeHelper.createRay(1);        // 레이가 사용될 때 길이가 덮어쓰기됨
        shootRay = OdeHelper.createRay(1);         // 레이가 사용될 때 길이가 덮어쓰기됨
    }

    public boolean isGrounded(GameObject player, Vector3 playerPos, float rayLength, Vector3 groundNormal ) {
        this.player = player;
        groundRay.setLength(rayLength);
        groundRay.set(playerPos.x, playerPos.y, playerPos.z, 0, -1, 0); // 레이를 아래로 향하게 설정
        groundRay.setFirstContact(true);
        groundRay.setBackfaceCull(true);

        groundNormal.set(0,0,0);    // 유효하지 않은 값으로 설정
        OdeHelper.spaceCollide2(physicsWorld.space, groundRay, groundNormal, callback);
        return !groundNormal.isZero();
    }

    private final DGeom.DNearCallback callback = new DGeom.DNearCallback() {

        @Override
        public void call(Object data, DGeom o1, DGeom o2) {
            GameObject go;
            final int N = 1;
            DContactBuffer contacts = new DContactBuffer(N);
            int n = OdeHelper.collide (o1,o2,N,contacts.getGeomBuffer());
            if (n > 0) {

                float sign = 1;
                if(o2 instanceof DRay ) {
                    go = (GameObject) o1.getData();
                    sign = -1f;
                }
                else
                    go = (GameObject) o2.getData();
                //Gdx.app.log("ray cast",""+go.scene.modelInstance.nodes.first().id);
                if(go == player)      // 플레이어 자신과의 충돌은 무시
                    return;

                DVector3 normal = contacts.get(0).getContactGeom().normal;
                ((Vector3)data).set((float) (sign*normal.get(0)), (float)(sign*normal.get(1)), (float)(sign*normal.get(2)));
            }
        }
    };

    // 히트 포인트의 세부 정보를 담는 클래스
    public static class HitPoint {
        public boolean hit;
        public float distance;
        public GameObject refObject;
        public Vector3 normal;
        public Vector3 worldContactPoint;

        public HitPoint() {
            normal = new Vector3();
            worldContactPoint = new Vector3();
        }
    }

    // 레이 캐스팅을 사용하여 조준선이 타겟 게임 오브젝트 위에 있는지 확인
    public boolean findTarget(Vector3 playerPos, Vector3 viewDir, HitPoint hitPoint, float maxRange) {
        shootRay.setLength(maxRange); // 단검 등 공격 범위를 제한
        shootRay.set(playerPos.x, playerPos.y + Settings.realeyeHeight, playerPos.z, viewDir.x, viewDir.y, viewDir.z); // 레이 설정

        // TriMesh와의 충돌 관련 설정
        shootRay.setFirstContact(true);     // TriMesh와의 첫 번째 충돌만 검사
        shootRay.setBackfaceCull(true);     // TriMesh의 뒤쪽 면 무시
        shootRay.setClosestHit(false);      // 첫 번째 충돌만 처리

        hitPoint.hit = false;   // 초기화
        hitPoint.distance = Float.MAX_VALUE;

        // 충돌 검사 실행
        OdeHelper.spaceCollide2(physicsWorld.space, shootRay, hitPoint, shootCallback);

        return hitPoint.hit;
    }

    private final DGeom.DNearCallback shootCallback = new DGeom.DNearCallback() {
        @Override
        public void call(Object data, DGeom o1, DGeom o2) {
            HitPoint hitPoint = (HitPoint)data;

            final int N = 1;    // 레이는 이 geom과 단 한 번만 접촉함
            DContactBuffer contacts = new DContactBuffer(N);
            if( OdeHelper.collide (o1,o2,N,contacts.getGeomBuffer()) > 0 ) {    // 충돌?
                // 레이가 아닌 DGeom은 어느 것인가?
                GameObject go;
                if (o2 instanceof DRay)
                    go = (GameObject) o1.getData();
                else
                    go = (GameObject) o2.getData();
                if(go.type.isPlayer)       // 레이가 플레이어 자신을 맞추는 경우 무시
                    return;
                double d = contacts.get(0).getContactGeom().depth;
                // 가장 가까운 접촉 유지
                if(d < hitPoint.distance) {
                    hitPoint.hit = true;
                    hitPoint.distance = (float)d;
                    hitPoint.refObject = go;
                    DVector3 normal = contacts.get(0).getContactGeom().normal;
                    hitPoint.normal.set((float) normal.get(0),(float) normal.get(1), (float) normal.get(2));
                    DVector3 pos = contacts.get(0).getContactGeom().pos;
                    hitPoint.worldContactPoint.set((float) pos.get(0), (float) pos.get(1), (float) pos.get(2));
                }
            }
        }
    };

    @Override
    public void dispose() {
        groundRay.destroy();
        shootRay.destroy();
    }
}
