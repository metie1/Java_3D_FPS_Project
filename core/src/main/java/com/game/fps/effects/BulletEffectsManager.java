package com.game.fps.effects;

import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.math.Vector3;

public class BulletEffectsManager implements Disposable {
    private final Array<BulletHole> bulletHoles;
    private final Array<BulletTracer> bulletTracers;
    private final Model bulletHoleModel;
    private final Model tracerModel;

    public BulletEffectsManager(Model bulletHoleModel, Model tracerModel) {
        this.bulletHoles = new Array<>();
        this.bulletTracers = new Array<>();
        this.bulletHoleModel = bulletHoleModel;
        this.tracerModel = tracerModel;
    }

    public void addBulletHole(Vector3 position, Vector3 normal) {
        ModelInstance decal = new ModelInstance(bulletHoleModel);
        bulletHoles.add(new BulletHole(decal, position, normal));
    }

    public void addBulletTracer(Vector3 start, Vector3 end) {
        ModelInstance tracerInstance = new ModelInstance(tracerModel);
        bulletTracers.add(new BulletTracer(tracerInstance, start, end));
    }

    public void update(float deltaTime) {
        for (int i = bulletHoles.size - 1; i >= 0; i--) {
            BulletHole hole = bulletHoles.get(i);
            hole.update(deltaTime);
            if (hole.isExpired()) {
                bulletHoles.removeIndex(i);
            }
        }

        for (int i = bulletTracers.size - 1; i >= 0; i--) {
            BulletTracer tracer = bulletTracers.get(i);
            tracer.update(deltaTime);
            if (tracer.isExpired()) {
                bulletTracers.removeIndex(i);
            }
        }
    }

    public void render(ModelBatch modelBatch) {
        for (BulletHole hole : bulletHoles) {
            modelBatch.render(hole.decal);
        }

        for (BulletTracer tracer : bulletTracers) {
            modelBatch.render(tracer.tracerModel);
        }
    }

    @Override
    public void dispose() {
        bulletHoleModel.dispose();
        tracerModel.dispose();
    }
}
