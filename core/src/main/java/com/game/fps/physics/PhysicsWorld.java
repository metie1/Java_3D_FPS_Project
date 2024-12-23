package com.game.fps.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.github.antzGames.gdx.ode4j.ode.*;
import com.game.fps.GameObject;
import com.game.fps.Settings;
import com.game.fps.World;

import static com.github.antzGames.gdx.ode4j.ode.OdeConstants.*;


// 강체 역학 및 충돌의 월드
//
public class PhysicsWorld implements Disposable {

    static final float TIME_STEP = 0.025f;  // 고정된 물리 시간 단계

    DWorld world;
    public DSpace space;
    private final DJointGroup contactGroup;
    private final World gameWorld;
    private float timeElapsed;

    public PhysicsWorld(World gameWorld) {
        this.gameWorld = gameWorld;
        OdeHelper.initODE2(0);
        Gdx.app.log("ODE version", OdeHelper.getVersion());
        Gdx.app.log("ODE config", OdeHelper.getConfiguration());
        contactGroup = OdeHelper.createJointGroup();
        reset();
    }


    // 월드를 리셋합니다. 이는 모든 강체와 지오메트리가 고아 상태가 되므로 게임 오브젝트를 삭제하는 것과 함께 사용해야 합니다.
    public void reset() {
        if(world != null)
            world.destroy();
        if(space != null)
            space.destroy();

        world = OdeHelper.createWorld();
        space = OdeHelper.createSapSpace( null, DSapSpace.AXES.XZY );

        world.setGravity (0,  Settings.gravity, 0);
        world.setCFM (1e-5);
        world.setERP (0.4);
        world.setQuickStepNumIterations (40);
        world.setAngularDamping(0.5f);

        // 자동 비활성화 매개변수를 설정하여 비활성 객체가 수면으로 들어가도록 합니다.
        world.setAutoDisableFlag(true);
        world.setAutoDisableLinearThreshold(0.5);
        world.setAutoDisableAngularThreshold(0.5);
        world.setAutoDisableTime(1);

        timeElapsed = 0;
    }

    // 물리를 업데이트합니다.
    // quickStep()의 시간 단계는 고정 크기여야 합니다.
    public void update(float deltaTime) {
        timeElapsed += deltaTime;

        while(timeElapsed > TIME_STEP) {
            try{
                space.collide(null, nearCallback);
            } catch (Exception e) {}

            world.quickStep(TIME_STEP);
            contactGroup.empty();

            timeElapsed -= TIME_STEP;
        }
    }

    private final DGeom.DNearCallback nearCallback = new DGeom.DNearCallback() {

        @Override
        public void call(Object data, DGeom o1, DGeom o2) {
            DBody b1 = o1.getBody();
            DBody b2 = o2.getBody();
            if (b1 != null && b2 != null && OdeHelper.areConnected(b1, b2))
                return;

            // GameObject 데이터를 가져와 CollisionShapeType 검사
            GameObject obj1 = (GameObject) o1.getData();
            GameObject obj2 = (GameObject) o2.getData();

            if ((obj1 != null && obj1.getShapeType() == CollisionShapeType.NOTHING) ||
                (obj2 != null && obj2.getShapeType() == CollisionShapeType.NOTHING)) {
                // NOTHING 타입은 충돌 무시
                return;
            }

            final int N = 8;
            DContactBuffer contacts = new DContactBuffer(N);

            int n = OdeHelper.collide(o1, o2, N, contacts.getGeomBuffer());
            if (n > 0) {
                gameWorld.onCollision((GameObject)o1.getData(), (GameObject)o2.getData());        // 월드에 콜백

                for (int i = 0; i < n; i++) {
                    DContact contact = contacts.get(i);
                    contact.surface.mode = dContactSlip1 | dContactSlip2 | dContactSoftERP | dContactSoftCFM | dContactApprox1;
                    if (o1 instanceof DSphere || o2 instanceof DSphere || o1 instanceof DCapsule || o2 instanceof DCapsule)
                        contact.surface.mu = 0.01;  // 볼 및 캡슐에 대한 낮은 마찰
                    else
                        contact.surface.mu = 0.5;
                    contact.surface.slip1 = 0.0;
                    contact.surface.slip2 = 0.0;
                    contact.surface.soft_erp = 0.8;
                    contact.surface.soft_cfm = 0.01;

                    DJoint c = OdeHelper.createContactJoint(world, contactGroup, contact);
                    c.attach(o1.getBody(), o2.getBody());
                }
            }
        }
    };

    @Override
    public void dispose() {
        contactGroup.destroy();
        space.destroy();
        world.destroy();
        OdeHelper.closeODE();
    }

}
