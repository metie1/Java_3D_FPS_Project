package com.monstrous.tut3d;

import com.badlogic.gdx.math.Vector3;
import com.monstrous.tut3d.physics.CollisionShapeType;

public class Populator {

    public static void populate(World world) {
        world.clear();

        world.spawnObject(GameObjectType.TYPE_STATIC, "brickcube", null, CollisionShapeType.BOX, false, Vector3.Zero, 1);
        world.spawnObject(GameObjectType.TYPE_STATIC, "groundbox", null, CollisionShapeType.BOX, false, Vector3.Zero, 1f);
        world.spawnObject(GameObjectType.TYPE_STATIC, "brickcube.001", null, CollisionShapeType.BOX,false, Vector3.Zero, 1f);
        world.spawnObject(GameObjectType.TYPE_STATIC, "brickcube.002", null, CollisionShapeType.BOX,false, Vector3.Zero, 1f);
        world.spawnObject(GameObjectType.TYPE_STATIC, "brickcube.003", null, CollisionShapeType.BOX,false, Vector3.Zero, 1f);
        world.spawnObject(GameObjectType.TYPE_STATIC, "brickcube.004", null, CollisionShapeType.BOX,false, Vector3.Zero, 1f);
        world.spawnObject(GameObjectType.TYPE_STATIC, "wall", null, CollisionShapeType.BOX,false, Vector3.Zero, 1f);
        world.spawnObject(GameObjectType.TYPE_STATIC, "wall.001", null, CollisionShapeType.BOX,false, Vector3.Zero, 1f);
        world.spawnObject(GameObjectType.TYPE_STATIC, "wall.002", null, CollisionShapeType.BOX,false, Vector3.Zero, 1f);

        world.spawnObject(GameObjectType.TYPE_STATIC,"arch",  null, CollisionShapeType.MESH, false, Vector3.Zero, 1f);

        world.spawnObject(GameObjectType.TYPE_STATIC,"stairs", "stairsProxy",  CollisionShapeType.MESH, false, Vector3.Zero, 1f);

        world.spawnObject(GameObjectType.TYPE_STATIC,"ramp",  null, CollisionShapeType.MESH, false, Vector3.Zero, 1f);

        world.spawnObject(GameObjectType.TYPE_DYNAMIC, "ball", null, CollisionShapeType.SPHERE, true, new Vector3(0,4,-2), Settings.ballMass);
        world.spawnObject(GameObjectType.TYPE_DYNAMIC, "ball", null, CollisionShapeType.SPHERE, true, new Vector3(-1,5,-2), Settings.ballMass);
        world.spawnObject(GameObjectType.TYPE_DYNAMIC, "ball", null, CollisionShapeType.SPHERE, true, new Vector3(-2,6,-2), Settings.ballMass);

        world.spawnObject(GameObjectType.TYPE_PICKUP_COIN, "coin",  null, CollisionShapeType.BOX, true, new Vector3(-5,1,0), 1);
        world.spawnObject(GameObjectType.TYPE_PICKUP_COIN, "coin",  null,  CollisionShapeType.BOX, true, new Vector3(5, 1, 15), 1);
        world.spawnObject(GameObjectType.TYPE_PICKUP_COIN, "coin",  null, CollisionShapeType.BOX, true, new Vector3(-12, 1, 13), 1);
        world.spawnObject(GameObjectType.TYPE_PICKUP_HEALTH, "healthpack",null, CollisionShapeType.BOX, true, new Vector3(26, 0.1f, -26), 1);
        world.spawnObject(GameObjectType.TYPE_PICKUP_HEALTH, "healthpack",  null, CollisionShapeType.BOX, true, new Vector3(-26, 0.1f, 26), 1);

        world.spawnObject(GameObjectType.TYPE_ENEMY, "cook",  "cookProxy", CollisionShapeType.CAPSULE, true, new Vector3(-15, 1f, -18), Settings.playerMass  );  // bad guy
        world.spawnObject(GameObjectType.TYPE_ENEMY, "cook",  "cookProxy", CollisionShapeType.CAPSULE, true, new Vector3(15, 1f, 18), Settings.playerMass  );  // bad guy
        world.spawnObject(GameObjectType.TYPE_ENEMY, "cook",  "cookProxy", CollisionShapeType.CAPSULE, true, new Vector3(-25, 1f, 25), Settings.playerMass  );  // bad guy
        world.spawnObject(GameObjectType.TYPE_ENEMY, "cook",  "cookProxy", CollisionShapeType.CAPSULE, true, new Vector3(25, 1f, 25), Settings.playerMass  );  // bad guy

        GameObject go = world.spawnObject(GameObjectType.TYPE_PLAYER, "ducky",null, CollisionShapeType.CAPSULE, true, new Vector3(0,1,0), Settings.playerMass);
        world.setPlayer(go);
    }
}
