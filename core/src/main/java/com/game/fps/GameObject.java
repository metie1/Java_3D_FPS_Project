package com.game.fps;

import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.game.fps.physics.CollisionShapeType;
import com.game.fps.physics.PhysicsBody;
import net.mgsx.gltf.scene3d.scene.Scene;

public class GameObject implements Disposable {
    public final GameObjectType type;
    public final Scene scene;
    public final PhysicsBody body;
    public final Vector3 direction;
    public boolean visible;
    public float health;
    public boolean isCrouch;
    public boolean nohealth=false;

    // 애니메이션 컨트롤러 및 현재 애니메이션 이름 필드 추가
    public AnimationController animationController;
    private String currentAnimation;
    private Object userData;  // 추가: 사용자 데이터 저장용 필드

    public void setUserData(Object data) {
        this.userData = data;
    }

    public Object getUserData() {
        return userData;
    }

    public GameObject(GameObjectType type, Scene scene, PhysicsBody body) {
        this.type = type;
        this.scene = scene;
        this.body = body;
        if (body != null)
            body.geom.setData(this); // 충돌 처리를 위해 geom에 사용자 데이터를 설정
        visible = true;
        direction = new Vector3();
        health = 1f;
        isCrouch = false;

        // 애니메이션 컨트롤러 초기화
        if (scene != null) {
            this.animationController = new AnimationController(scene.modelInstance);
        }
        currentAnimation = "idle"; // 초기 애니메이션 상태 설정
    }


    public void update(World world, float deltaTime) {
        // 애니메이션 업데이트
        if (animationController != null) {
            animationController.update(deltaTime);
        }
        if(health<=0){
            nohealth = true;
        }
        else{
            nohealth = false;
        }
    }

    public void setAnimation(String animationName, int loopCount) {
        if (animationController != null) {
            animationController.setAnimation(animationName, loopCount);
            currentAnimation = animationName; // 현재 애니메이션 이름 업데이트
        }
    }

    public String getCurrentAnimation() {
        return currentAnimation;
    }


    public void setAlive(){
        nohealth=false;
    }

    public boolean isDead() {
        return nohealth;
    }

    public Vector3 getPosition() {
        if (body == null)
            return Vector3.Zero;
        return body.getPosition();
    }

    public Vector3 getDirection() {
        direction.set(0, 0, 1);
        direction.mul(body.getOrientation());
        return direction;
    }

    public CollisionShapeType getShapeType() {
        if (body != null) {
            return body.getShapeType();
        }
        return CollisionShapeType.NOTHING; // body가 없는 경우 NOTHING 반환
    }
    @Override
    public void dispose() {
        body.destroy();
    }
}
