package com.game.fps.physics;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.github.antzGames.gdx.ode4j.math.DQuaternion;
import com.github.antzGames.gdx.ode4j.math.DQuaternionC;
import com.github.antzGames.gdx.ode4j.math.DVector3C;
import com.github.antzGames.gdx.ode4j.ode.DBody;
import com.github.antzGames.gdx.ode4j.ode.DGeom;
import com.game.fps.Settings;

public class PhysicsBody {

    public final DGeom geom;
    private final Vector3 position;               // 편의를 위해 geom.getPosition()과 일치하지만 Vector3로 변환됨
    private final Quaternion quaternion;          // 편의를 위해 geom.getQuaternion()과 일치하지만 LibGDX Quaternion으로 변환됨
    public final ModelInstance debugInstance;    // 디버그 보기용 충돌 형태 시각화
    private final DQuaternion tmpQ;
    private final Vector3 linearVelocity;
    private final CollisionShapeType shapeType;

    public PhysicsBody(DGeom geom, ModelInstance debugInstance, CollisionShapeType shapeType) {
        this.geom = geom;
        this.debugInstance = debugInstance;
        this.shapeType = shapeType;
        position = new Vector3();
        linearVelocity = new Vector3();
        quaternion = new Quaternion();
        tmpQ = new DQuaternion();
    }

    public CollisionShapeType getShapeType() {
        return shapeType;
    }

    public Vector3 getPosition() {
        DVector3C pos = geom.getPosition();
        position.x = (float) pos.get0();
        position.y = (float) pos.get1();
        position.z = (float) pos.get2();
        return position;
    }

    public void setPosition(Vector3 pos) {
        geom.setPosition(pos.x, pos.y, pos.z);
        // geom이 강체에 부착된 경우 위치도 변경됨
    }

    public Quaternion getOrientation() {
        DQuaternionC odeQ = geom.getQuaternion();
        float ow = (float) odeQ.get0();
        float ox = (float) odeQ.get1();
        float oy = (float) odeQ.get2();
        float oz = (float) odeQ.get3();
        quaternion.set(ox, oy, oz, ow);
        return quaternion;
    }

    public Quaternion getBodyOrientation() {
        DQuaternionC odeQ;
        if (geom.getBody() == null)      // geom에 부착된 강체가 없으면 geom 방향으로 대체
            odeQ = geom.getQuaternion();
        else
            odeQ = geom.getBody().getQuaternion();
        float ow = (float) odeQ.get0();
        float ox = (float) odeQ.get1();
        float oy = (float) odeQ.get2();
        float oz = (float) odeQ.get3();
        quaternion.set(ox, oy, oz, ow);
        return quaternion;
    }

    public void setOrientation(Quaternion q) {
        tmpQ.set(q.w, q.x, q.y, q.z);       // ODE 쿼터니언으로 변환
        geom.setQuaternion(tmpQ);
        // geom이 강체에 부착된 경우 회전도 변경됨
    }

    public void applyForce(Vector3 force) {
        DBody rigidBody = geom.getBody();
        rigidBody.addForce(force.x, force.y, force.z);
    }

    public void applyForceAtPos(Vector3 force, Vector3 pos) {
        DBody rigidBody = geom.getBody();
        rigidBody.addForceAtPos(force.x, force.y, force.z, pos.x, pos.y, pos.z);
    }

    public void applyTorque(Vector3 torque) {
        DBody rigidBody = geom.getBody();
        rigidBody.addTorque(torque.x, torque.y, torque.z);
    }

    public Vector3 getVelocity() {
        if (geom.getBody() == null)
            linearVelocity.set(Vector3.Zero);
        else {
            DVector3C v = geom.getBody().getLinearVel();
            linearVelocity.set((float) v.get0(), (float) v.get1(), (float) v.get2());
        }
        return linearVelocity;
    }

    public void setCapsuleCharacteristics() {
        DBody rigidBody = geom.getBody();
        rigidBody.setDamping(Settings.playerLinearDamping, Settings.playerAngularDamping);
        rigidBody.setAutoDisableFlag(false);        // 플레이어가 비활성화되지 않도록 설정
        rigidBody.setMaxAngularSpeed(0);            // 회전을 허용하지 않아 캡슐을 똑바로 유지
    }

    public void destroy() {
        if (geom.getBody() != null)
            geom.getBody().destroy();
        geom.destroy();
    }
}
