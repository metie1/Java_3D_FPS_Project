package com.game.fps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.game.fps.bomb.BombManager;
import com.game.fps.effects.BulletEffectsManager;
import com.game.fps.inputs.PlayerController;
import com.game.fps.physics.*;
import com.game.fps.physics.*;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

public class World implements Disposable {
    private final Array<GameObject> gameObjects;
    public GameStats stats;
    private final SceneAsset sceneAsset;
    private final PhysicsWorld physicsWorld;
    private final PhysicsBodyFactory factory;
    private final PlayerController playerController;
    public final PhysicsRayCaster rayCaster;
    public final WeaponState weaponState;
    private final Vector3 muzzlePosition;
    private final Vector3 spawnPos;
    private final Vector3 shootForce;
    private final Vector3 impulse;
    private int prevNode = -1;

    protected final BulletEffectsManager effectsManager;  // protected로 변경
    protected GameObject player;    // protected로 변경

    public final RoundSystem roundSystem;
    public final SpawnSystem spawnSystem;


    public World() {
        gameObjects = new Array<>();
        stats = new GameStats();
        sceneAsset = Main.assets.sceneAsset;
        sceneAsset.maxBones = 100;
        physicsWorld = new PhysicsWorld(this);
        factory = new PhysicsBodyFactory(physicsWorld);
        rayCaster = new PhysicsRayCaster(physicsWorld);
        playerController = new PlayerController(this);
        weaponState = new WeaponState();

        // 벡터 초기화
        muzzlePosition = new Vector3();
        spawnPos = new Vector3();
        shootForce = new Vector3();
        impulse = new Vector3();

        // 총알 효과 매니저 초기화
        effectsManager = new BulletEffectsManager(
            createBulletHoleModel(),
            createTracerModel()
        );

        roundSystem = new RoundSystem();
        spawnSystem = new SpawnSystem();
    }

    private Model createBulletHoleModel() {
        ModelBuilder modelBuilder = new ModelBuilder();
        Material material = new Material();
        material.set(new ColorAttribute(ColorAttribute.Diffuse, new Color(0.2f, 0.2f, 0.2f, 0.8f)));

        // 원형 탄흔 생성 (디스크 형태)
        return modelBuilder.createCylinder(
            0.2f,  // 지름
            0.01f, // 두께
            0.2f,  // 지름
            16,    // 분할 수 (더 매끄러운 원형을 위해)
            material,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
    }

    private Model createTracerModel() {
        ModelBuilder modelBuilder = new ModelBuilder();
        Material material = new Material();
        material.set(new ColorAttribute(ColorAttribute.Diffuse, Color.YELLOW));

        return modelBuilder.createCylinder(
            1f, 0.02f, 0.02f, 8,
            material,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
    }


    public void clear() {
        physicsWorld.reset();
        playerController.reset();
        stats.reset();
        weaponState.reset();
        gameObjects.clear();
        player = null;
        prevNode = -1;
    }

    public int getNumGameObjects() {
        return gameObjects.size;
    }

    public GameObject getGameObject(int index) {
        return gameObjects.get(index);
    }

    public GameObject getPlayer() {
        return player;
    }

    public void setPlayer( GameObject player ){
        this.player = player;
        player.body.setCapsuleCharacteristics();
        //navMesh.updateDistances(player.getPosition());
    }



    public PlayerController getPlayerController() {
        return playerController;
    }
    public BombManager getBombManager() { return playerController.getBombManager();}
    public GameObject spawnObject(GameObjectType type, String name, String proxyName, CollisionShapeType shapeType, boolean resetPosition, Vector3 position){

        Scene scene = loadNode( name, resetPosition, position );

        ModelInstance collisionInstance = scene.modelInstance;
        if(proxyName != null) {
            Scene proxyScene = loadNode( proxyName, resetPosition, position );
            collisionInstance = proxyScene.modelInstance;
        }
        PhysicsBody body = null;
//        if(type == GameObjectType.TYPE_NAVMESH){
//            return null;
//        }
        body = factory.createBody(collisionInstance, shapeType, type.isStatic,name);
        GameObject go = new GameObject(type, scene, body);
        gameObjects.add(go);
        if(go.type == GameObjectType.TYPE_OTHER_PLAYER)
            stats.numEnemies++;


        return go;
    }

    private Scene loadNode( String nodeName, boolean resetPosition, Vector3 position ) {
        try {
            Gdx.app.log("World", "Attempting to load node: " + nodeName);
            Scene scene = new Scene(sceneAsset.scene, nodeName);

            if (scene.modelInstance.nodes.size == 0) {
                Gdx.app.error("World", "Node not found in GLTF file: " + nodeName);
                throw new RuntimeException("Cannot find node in GLTF file: " + nodeName);
            }

            Gdx.app.log("World", "Successfully loaded node: " + nodeName);
            applyNodeTransform(resetPosition, scene.modelInstance, scene.modelInstance.nodes.first());
            scene.modelInstance.transform.translate(position);
            return scene;
        } catch (Exception e) {
            Gdx.app.error("World", "Error loading node: " + nodeName, e);
            throw e;
        }
    }

    private void applyNodeTransform(boolean resetPosition, ModelInstance modelInstance, Node node ){
        if(!resetPosition)
            modelInstance.transform.mul(node.globalTransform);
        node.translation.set(0,0,0);
        node.scale.set(1,1,1);
        node.rotation.idt();
        modelInstance.calculateTransforms();
    }

    public void removeObject(GameObject gameObject){
        gameObject.health = 0;

        if(gameObject.type == GameObjectType.TYPE_OTHER_PLAYER){
            stats.numEnemies--;
        }

        gameObjects.removeValue(gameObject, true);
        gameObject.dispose();

    }



    public void update( float deltaTime ) {
        if(stats.numEnemies > 0 || stats.coinsCollected < stats.numCoins)
            stats.gameTime += deltaTime;
        else {
            if(!stats.levelComplete)
                Main.assets.sounds.GAME_COMPLETED.play();
            stats.levelComplete = true;
        }

        // 총알 효과 업데이트
        effectsManager.update(deltaTime);

        weaponState.update(deltaTime);
        try{
            playerController.update(player, deltaTime);
            physicsWorld.update(deltaTime);
        } catch(Exception e){}
        syncToPhysics();
        roundSystem.update(deltaTime);

        for(GameObject go : gameObjects) {
            if(go.getPosition().y < -50)
                removeObject(go);
            go.update(this, deltaTime);

        }
    }

    private void syncToPhysics() {
        for(GameObject go : gameObjects) {
            if(go.body != null && go.body.geom.getBody() != null) {
                if(go.type == GameObjectType.TYPE_PLAYER) {
                    player.scene.modelInstance.transform.setToRotation(Vector3.Z, playerController.getForwardDirection());
                    player.scene.modelInstance.transform.setTranslation(go.body.getPosition());
                }
                else if(go.type == GameObjectType.TYPE_OTHER_PLAYER) {
                    //go.scene.modelInstance.transform.setToRotation(Vector3.Z, enemyController.getRotation); enemy rotation을 얻는것을 멀티로 구현 클래스도 새로 구현해야함
                    go.scene.modelInstance.transform.setTranslation(go.body.getPosition());
                }
                else {
                    go.scene.modelInstance.transform.set(go.body.getPosition(), go.body.getOrientation());
                }
            }
        }
    }

    // fire current weapon
    public void fireWeapon(Vector3 viewingDirection,  PhysicsRayCaster.HitPoint hitPoint) {
        if(player.isDead())
            return;
        if(!weaponState.isWeaponReady())
            return;
        weaponState.firing = true;

        switch(weaponState.currentWeaponType) {
            case KNIFE: // 단검 공격 방식
                Main.assets.sounds.KNIFE_STAB.play(); // 단검 사운드 재생

                if (hitPoint.hit) {
                    GameObject hitObject = hitPoint.refObject;

                    if (hitObject.type.isEnemy || hitObject.type.isTarget) {
                        bulletHit(hitObject); // 적중 처리
                        effectsManager.addBulletHole(hitPoint.worldContactPoint, hitPoint.normal); // 타격 효과
                    }
                }
                break;

            case PISTOL:
            case AK47:
            case SG553:
            case AR15:
                Main.assets.sounds.GUN_SHOT.play();
                muzzlePosition.set(player.getPosition()).add(0, Settings.eyeHeight, 0);

                if(hitPoint.hit) {
                    effectsManager.addBulletHole(hitPoint.worldContactPoint, hitPoint.normal);
                    effectsManager.addBulletTracer(muzzlePosition, hitPoint.worldContactPoint);

                    GameObject hitObject = hitPoint.refObject;
                    if(hitObject.type.isEnemy)
                        bulletHit(hitObject);
                    if(hitObject.type.isTarget)
                        bulletHit(hitObject);

                    impulse.set(hitObject.getPosition()).sub(player.getPosition()).nor().scl(Settings.gunForce);
                    if(hitObject.body.geom.getBody() != null) {
                        hitObject.body.geom.getBody().enable();
                        hitObject.body.applyForceAtPos(impulse, hitPoint.worldContactPoint);
                    }
                } else {
                    Vector3 missPosition = new Vector3(viewingDirection).scl(100f).add(muzzlePosition);
                    effectsManager.addBulletTracer(muzzlePosition, missPosition);
                }
                break;
        }
    }

    // BulletEffectsManager getter 추가
    public BulletEffectsManager getEffectsManager() {
        return effectsManager;
    }

    public void onCollision(GameObject go1, GameObject go2){
        // try either order
        if(go1.type.isStatic || go2.type.isStatic)
            return;

        handleCollision(go1, go2);
        handleCollision(go2, go1);
    }

    private void handleCollision(GameObject go1, GameObject go2) {
        if (go1.type.isPlayer && go2.type.canPickup) {
            pickup(go1, go2);
        }
        if (go1.type.isPlayer && go2.type.isEnemyBullet) {
            removeObject(go2);
            bulletHit(go1);
        }

        if(go1.type.isEnemy && go2.type.isFriendlyBullet) {
            removeObject(go2);
            bulletHit(go1);
        }
    }

    private void pickup(GameObject character, GameObject pickup){
        if (pickup.type == GameObjectType.TYPE_PICKUP_GUN) {
            String modelName = pickup.scene.modelInstance.nodes.first().id;

            // 이미 주무기가 있다면 현재 무기를 떨어뜨림
            if (weaponState.hasMainWeapon()) {
                weaponState.removeMainWeapons();
            }

            // 새 무기 획득
            switch (modelName) {
                case "AkArmature":
                    weaponState.haveAK47 = true;
                    weaponState.currentWeaponType = WeaponType.AK47;
                    break;
                case "SgArmature":
                    weaponState.haveSG553 = true;
                    weaponState.currentWeaponType = WeaponType.SG553;
                    break;
                case "ArArmature":
                    weaponState.haveAR15 = true;
                    weaponState.currentWeaponType = WeaponType.AR15;
                    break;
            }
            Main.assets.sounds.UPGRADE.play();
        }

        removeObject(pickup);
    }

    private void bulletHit(GameObject character) {
        character.health -= 0.25f;      // - 25% health
        Main.assets.sounds.HIT.play();
        if(character.isDead()) {
            removeObject(character);
            if (character.type.isPlayer)
                Main.assets.sounds.GAME_OVER.play();
        }
    }

    public void addBulletHole(Vector3 position, Vector3 normal) {
        if (effectsManager != null) {
            effectsManager.addBulletHole(position, normal);
        }
    }

    public void addBulletTracer(Vector3 start, Vector3 end) {
        if (effectsManager != null) {
            effectsManager.addBulletTracer(start, end);
        }
    }

    public boolean isPlayerDead() {
        return player != null && player.isDead();
    }

    @Override
    public void dispose() {
        if (physicsWorld != null) {
            physicsWorld.dispose();
        }
        if (rayCaster != null) {
            rayCaster.dispose();
        }
        if (effectsManager != null) {
            effectsManager.dispose();
        }
        // GameObject들의 리소스도 정리
        for (GameObject go : gameObjects) {
            if (go != null) {
                go.dispose();
            }
        }
        gameObjects.clear();
    }
}
