package com.game.fps.physics;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.utils.Disposable;
import com.game.fps.GameObject;
import com.game.fps.World;

public class PhysicsView implements Disposable {

    // 활성 및 비활성 geom에 사용할 색상
    static private final Color COLOR_ACTIVE = Color.GREEN;
    static private final Color COLOR_SLEEPING = Color.TEAL;
    static private final Color COLOR_STATIC = Color.GRAY;

    private final ModelBatch modelBatch;
    private final World world;      // 참조

    public PhysicsView(World world) {
        this.world = world;
        modelBatch = new ModelBatch();
    }

    public void render(Camera cam) {
        modelBatch.begin(cam);
        int num = world.getNumGameObjects();
        for(int i = 0; i < num; i++) {
            GameObject go = world.getGameObject(i);
            if (go.visible)
                renderCollisionShape(go.body);
        }
        modelBatch.end();
    }

    private void renderCollisionShape(PhysicsBody body) {
        // geom에 맞춰 디버그 modelInstance의 위치 및 방향 설정
        if(body == null)
            return;

        body.debugInstance.transform.set(body.getPosition(), body.getBodyOrientation());

        // 정적/비활성/활성 객체 및 활성 객체에 따라 다른 색상 사용
        Color color = COLOR_STATIC;
        if (body.geom.getBody() != null) {
            if (body.geom.getBody().isEnabled())
                color = COLOR_ACTIVE;
            else
                color = COLOR_SLEEPING;
        }
        body.debugInstance.materials.first().set(ColorAttribute.createDiffuse(color));   // 재질 색상 설정

        modelBatch.render(body.debugInstance);
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
    }
}
