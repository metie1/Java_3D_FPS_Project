package com.game.fps.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ObjectMap;
import com.game.fps.*;
import com.game.fps.net.message.PlayerStateMessage;
import com.game.fps.net.message.ShootMessage;
import com.game.fps.physics.CollisionShapeType;
import com.game.fps.physics.PhysicsRayCaster;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.game.fps.Populator.*;

// NetworkWorld.java (World 클래스를 확장)
public class NetworkWorld extends World {
    private final Queue<Runnable> mainThreadTasks;
    private NetworkClient networkClient;
    private final ObjectMap<Integer, NetworkPlayer> networkPlayers;
    private float networkUpdateTimer;
    private float stateTime;
    private static final float NETWORK_UPDATE_INTERVAL = 0.016f; // ~60Hz
    private static float DAMAGE_AMOUNT = 0.25f;
    private final Vector3 tmpVec = new Vector3();
    private final Vector3 bombPosition = new Vector3();
    private String team;
    private SpectatorSystem spectatorSystem;

    public NetworkWorld() {
        super();
        this.spectatorSystem = new SpectatorSystem(this);

        this.networkPlayers = new ObjectMap<>();
        this.mainThreadTasks = new ConcurrentLinkedQueue<>();

        Gdx.app.log("NetworkWorld", "Created");
    }

    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
        Gdx.app.log("NetworkWorld", "Network client set");
    }


    public void addNetworkPlayer(int playerId, String playerName, Vector3 position, String playerTeam) {

        // OpenGL 작업을 메인 스레드에 예약
        mainThreadTasks.add(() -> {
            Gdx.app.log("NetworkWorld", "Adding network player: " + playerName + " (ID: " + playerId + ")");

            try {
                if (networkPlayers.containsKey(playerId)) {
                    removeNetworkPlayer(playerId);
                }

                // 팀에 따라 모델 선택
                String playerModel = playerTeam.equals("Team_A") ? "player01" : "player02";
                String idleAnimation = playerTeam.equals("Team_A") ? "p_idle" : "p_idle.agent";

                GameObject playerObject = spawnObject(
                    GameObjectType.TYPE_OTHER_PLAYER,
                    playerModel,  // 팀에 맞는 모델 사용
                    "playerProxy",
                    CollisionShapeType.CAPSULE,
                    true,
                    position
                );

                playerObject.body.setCapsuleCharacteristics(); // 콜리전 뭉개짐 해결

                if (playerObject != null) {
                    playerObject.setAnimation(idleAnimation, -1);  // 팀에 맞는 애니메이션 설정
                    NetworkPlayer netPlayer = new NetworkPlayer(playerId, playerName, playerObject,team,weaponState);
                    networkPlayers.put(playerId, netPlayer);
                    if(playerTeam.equals("Team_A")){
                        roundSystem.getTeamA().add(playerObject);
                    }
                    else if(playerTeam.equals("Team_B")){
                        roundSystem.getTeamB().add(playerObject);
                    }
                    Gdx.app.log("NetworkWorld", "Network player added successfully");
                }

            } catch (Exception e) {
                Gdx.app.error("NetworkWorld", "Error adding network player", e);
            }
        });


    }

    public void getTeamName(String team){
        this.team = team;
    }

    public String returnTeamName(){
        return team;
    }


    public void setTeam() {

        if (team.equals("Team_A")) {
            roundSystem.getTeamA().add(getPlayer());
        } else if (team.equals("Team_B")) {
            roundSystem.getTeamB().add(getPlayer());
        }

        Gdx.app.log("NetworkWorld", "Assigned local player to " + team);
    }


    public void removeNetworkPlayer(int playerId) {
        NetworkPlayer netPlayer = networkPlayers.get(playerId);
        if (netPlayer != null) {
            Gdx.app.log("NetworkWorld", "Removing network player with ID: " + playerId);
            removeObject(netPlayer.getGameObject());
            networkPlayers.remove(playerId);
        }
    }

    @Override
    public void update(float deltaTime) {

        // 메인 스레드에서 예약된 작업 실행
        while (!mainThreadTasks.isEmpty()) {
            try {
                Runnable task = mainThreadTasks.poll();
                if (task != null) {
                    task.run();
                }
            } catch (Exception e) {
                Gdx.app.error("NetworkWorld", "Error executing main thread task", e);
            }
        }

        try{
            super.update(deltaTime);
        } catch(Exception e) { }

        for (NetworkPlayer netPlayer : networkPlayers.values()) {
            if (netPlayer.getGameObject() != null) {
                WeaponType currentWeapon = netPlayer.getCurrentWeaponType();
                WeaponType previousWeapon = netPlayer.getPreviousWeaponType();

                // 이전 무기를 화면 밖으로 이동시켜 비활성화
                if (previousWeapon != null) {
                    switch (previousWeapon) {
                        case PISTOL:
                            Populator.pistol.scene.modelInstance.transform.setTranslation(-1000, -1000, -1000);
                            break;
                        case KNIFE:
                            Populator.knife.scene.modelInstance.transform.setTranslation(-1000, -1000, -1000);
                            break;
                        case AK47:
                            ak47.scene.modelInstance.transform.setTranslation(-1000, -1000, -1000);
                            break;
                        case SG553:
                            Populator.sg553.scene.modelInstance.transform.setTranslation(-1000, -1000, -1000);
                            break;
                        case AR15:
                            Populator.ar15.scene.modelInstance.transform.setTranslation(-1000, -1000, -1000);
                            break;
                        // 필요한 경우 추가 무기
                    }
                }

                // 현재 무기를 플레이어 위치에 배치하여 활성화
                if (currentWeapon != null) {
                    switch (currentWeapon) {
                        case PISTOL:
                            Populator.pistol.scene.modelInstance.transform.set(
                                netPlayer.getPlayerPosition(),
                                netPlayer.getGameObject().body.getOrientation()
                            );
                            break;
                        case KNIFE:
                            Populator.knife.scene.modelInstance.transform.set(
                                netPlayer.getPlayerPosition(),
                                netPlayer.getGameObject().body.getOrientation()
                            );
                            break;
                        case AK47:
                            ak47.scene.modelInstance.transform.set(
                                netPlayer.getPlayerPosition(),
                                netPlayer.getGameObject().body.getOrientation()
                            );
                            break;
                        case SG553:
                            Populator.sg553.scene.modelInstance.transform.set(
                                netPlayer.getPlayerPosition(),
                                netPlayer.getGameObject().body.getOrientation()
                            );
                            break;
                        case AR15:
                            Populator.ar15.scene.modelInstance.transform.set(
                                netPlayer.getPlayerPosition(),
                                netPlayer.getGameObject().body.getOrientation()
                            );
                            break;
                        // 필요한 경우 추가 무기
                    }
                }
            }
        }


        stateTime += deltaTime;

        // 네트워크 플레이어 보간 업데이트
        for (NetworkPlayer netPlayer : networkPlayers.values()) {
            netPlayer.interpolate(deltaTime);
        }

        // 로컬 플레이어 상태 전송
        networkUpdateTimer += deltaTime;
        if (networkUpdateTimer >= NETWORK_UPDATE_INTERVAL) {
            // sendPlayerState() 대신 syncPlayerState() 호출
            if (getPlayer() != null) {
                syncPlayerState(getPlayer(), getPlayer().health);
            }
            networkUpdateTimer = 0;
        }

        // 플레이어가 죽었고 아직 관전 중이 아니라면 관전 시작
        if (getPlayer() != null && getPlayer().isDead() && !spectatorSystem.isSpectating()) {
            Gdx.app.log("NetworkWorld", "Player is dead, starting spectator mode");
            spectatorSystem.startSpectating();
        }

        // 관전 시스템 업데이트
        spectatorSystem.update(deltaTime);

        // 라운드가 새로 시작될 때 관전 모드 해제
        if (roundSystem.getRoundState() == RoundSystem.RoundState.WAITING) {
            spectatorSystem.stopSpectating();
        }
        for (NetworkPlayer netPlayer : networkPlayers.values()) {
            if (netPlayer.getGameObject() != null) {
                {
                    switch(netPlayer.getCurrentWeaponType()){
                        case PISTOL:
                            netPlayer.getGameObject().scene.modelInstance.calculateTransforms();
                            Vector3 playerPosition = netPlayer.getPlayerPosition();
                            Quaternion collisionRotation = netPlayer.getGameObject().body.getOrientation() != null ?
                                netPlayer.getGameObject().body.getOrientation() : new Quaternion();
                            Quaternion undoRotation = new Quaternion(Vector3.X, -90);
                            Quaternion adjustedRotation = new Quaternion(collisionRotation).mul(undoRotation);
                            Matrix4 weaponTransform = new Matrix4();
                            weaponTransform.set(playerPosition, adjustedRotation, new Vector3(1, 1, 1));
                            pistol.scene.modelInstance.transform.set(weaponTransform);
                            break;
                        case KNIFE:
                            netPlayer.getGameObject().scene.modelInstance.calculateTransforms();
                            playerPosition = netPlayer.getPlayerPosition();
                            collisionRotation = netPlayer.getGameObject().body.getOrientation() != null ?
                                netPlayer.getGameObject().body.getOrientation() : new Quaternion();
                            undoRotation = new Quaternion(Vector3.X, -90);
                            adjustedRotation = new Quaternion(collisionRotation).mul(undoRotation);
                            weaponTransform = new Matrix4();
                            weaponTransform.set(playerPosition, adjustedRotation, new Vector3(1, 1, 1));
                            knife.scene.modelInstance.transform.set(weaponTransform);
                            break;
                        case AK47:
                            netPlayer.getGameObject().scene.modelInstance.calculateTransforms();
                            playerPosition = netPlayer.getPlayerPosition();
                            collisionRotation = netPlayer.getGameObject().body.getOrientation() != null ?
                                netPlayer.getGameObject().body.getOrientation() : new Quaternion();
                            undoRotation = new Quaternion(Vector3.X, -90);
                            adjustedRotation = new Quaternion(collisionRotation).mul(undoRotation);
                            weaponTransform = new Matrix4();
                            weaponTransform.set(playerPosition, adjustedRotation, new Vector3(1, 1, 1));
                            ak47.scene.modelInstance.transform.set(weaponTransform);
                            break;
                            case SG553:
                                netPlayer.getGameObject().scene.modelInstance.calculateTransforms();
                                playerPosition = netPlayer.getPlayerPosition();
                                collisionRotation = netPlayer.getGameObject().body.getOrientation() != null ?
                                    netPlayer.getGameObject().body.getOrientation() : new Quaternion();
                                undoRotation = new Quaternion(Vector3.X, -90);
                                adjustedRotation = new Quaternion(collisionRotation).mul(undoRotation);
                                weaponTransform = new Matrix4();
                                weaponTransform.set(playerPosition, adjustedRotation, new Vector3(1, 1, 1));
                                sg553.scene.modelInstance.transform.set(weaponTransform);
                                break;
                                case AR15:
                                    netPlayer.getGameObject().scene.modelInstance.calculateTransforms();
                                    playerPosition = netPlayer.getPlayerPosition();
                                    collisionRotation = netPlayer.getGameObject().body.getOrientation() != null ?
                                        netPlayer.getGameObject().body.getOrientation() : new Quaternion();
                                    undoRotation = new Quaternion(Vector3.X, -90);
                                    adjustedRotation = new Quaternion(collisionRotation).mul(undoRotation);
                                    weaponTransform = new Matrix4();
                                    weaponTransform.set(playerPosition, adjustedRotation, new Vector3(1, 1, 1));
                                    ar15.scene.modelInstance.transform.set(weaponTransform);
                    }

                }
                    }

        }

    }

    public SpectatorSystem getSpectatorSystem() {
        return spectatorSystem;
    }

    public List<NetworkPlayer> getNetworkPlayers() {
        List<NetworkPlayer> players = new ArrayList<>();
        for (NetworkPlayer player : networkPlayers.values()) {
            players.add(player);
        }
        return players;
    }

    private GameObject bombObject; // 폭탄 객체를 추적하기 위한 필드

    public void setBombObject(GameObject bombObject) {
        this.bombObject = bombObject;
    }


    public GameObject getBombObject() {
        return bombObject;
    }

    public void setBombPosition(Vector3 position) {
        bombPosition.set(position);
    }
    public Vector3 getBombPosition(){return bombPosition;}


    public void removeBomb() {
        mainThreadTasks.add(() -> {
            try {

                if (bombObject != null) {
                    removeObject(bombObject); // 폭탄 객체를 월드에서 제거
                    setBombObject(null); // 폭탄 객체 참조 해제
                    Gdx.app.log("NetworkWorld", "Bomb removed from the world.");
                }

                // 폭탄 위치를 초기화
                bombPosition.set(Vector3.Zero);
                Gdx.app.log("NetworkWorld", "Bomb position reset.");
            } catch (Exception e) {
                Gdx.app.error("NetworkWorld", "Error removing bomb", e);
            }
        });
    }

    private void syncPlayerState(GameObject playerObject, float health) {
        if (networkClient != null) {
            PlayerStateMessage stateMsg = new PlayerStateMessage();
            stateMsg.position.set(playerObject.getPosition());
            stateMsg.direction.set(playerObject.getDirection());
            stateMsg.rotation.set(playerObject.body.getBodyOrientation()); // 실제 회전 상태를 가져옴
            stateMsg.health = health;
            stateMsg.currentAnimation = playerObject.getCurrentAnimation();
            stateMsg.isFiring = false;
            stateMsg.currentWeaponType = weaponState.currentWeaponType;

            try {
                networkClient.sendPlayerState(stateMsg);
            } catch (Exception e) {
                Gdx.app.error("NetworkWorld", "Error sending player state", e);
            }
        }
    }

    @Override
    public void onCollision(GameObject go1, GameObject go2) {
        super.onCollision(go1, go2);

        // 네트워크 플레이어 간의 충돌 처리
        if (go1.type == GameObjectType.TYPE_OTHER_PLAYER && go2.type == GameObjectType.TYPE_FRIENDLY_BULLET) {
            handleNetworkPlayerHit(go1, go2);
        }
        else if (go2.type == GameObjectType.TYPE_OTHER_PLAYER && go1.type == GameObjectType.TYPE_FRIENDLY_BULLET) {
            handleNetworkPlayerHit(go2, go1);
        }
    }

    public void handleDamage(int targetId, int attackerId, float damage) {
        Gdx.app.log("NetworkWorld", String.format(
            "Handling damage: Attacker=%d Target=%d Damage=%.2f",
            attackerId, targetId, damage));

        // 로컬 플레이어가 데미지를 받는 경우
        if (networkClient != null && networkClient.getPlayerId() == targetId) {
            GameObject localPlayer = getPlayer();
            if (localPlayer != null) {
                float oldHealth = localPlayer.health;
                localPlayer.health = Math.max(0, oldHealth - damage);

                Gdx.app.log("NetworkWorld", String.format(
                    "Local player health: %.2f -> %.2f",
                    oldHealth, localPlayer.health));

                Main.assets.sounds.HIT.play();

                if (localPlayer.isDead()) {
                    Gdx.app.log("NetworkWorld", "Local player died!");
                    // 죽음 처리 추가
                }
            }
        }
        // 다른 플레이어가 데미지를 받는 경우
        else {
            NetworkPlayer targetPlayer = networkPlayers.get(targetId);
            if (targetPlayer != null) {
                GameObject targetObject = targetPlayer.getGameObject();
                float oldHealth = targetObject.health;
                targetObject.health = Math.max(0, oldHealth - damage);

                Gdx.app.log("NetworkWorld", String.format(
                    "Network player %d health: %.2f -> %.2f",
                    targetId, oldHealth, targetObject.health));

                if (targetObject.isDead()) {
                    Gdx.app.log("NetworkWorld", String.format(
                        "Network player %d died!", targetId));
//                    removeNetworkPlayer(targetId);
                }
            }
        }


    }

    // 플레이어 상태 업데이트도 수정
    public void updateNetworkPlayerState(PlayerStateMessage msg) {
        NetworkPlayer netPlayer = networkPlayers.get(msg.playerId);

        if (netPlayer != null) {
            GameObject playerObject = netPlayer.getGameObject();
            if (playerObject != null) {
                // 체력 업데이트
                if (Math.abs(playerObject.health - msg.health) > 0.001f) {
                    Gdx.app.log("NetworkWorld", String.format(
                        "Updating player %d health: %.2f -> %.2f",
                        msg.playerId, playerObject.health, msg.health));
                    playerObject.health = msg.health;
                }

                // 위치, 방향, 회전 업데이트
                playerObject.body.setPosition(msg.position);
                playerObject.direction.set(msg.direction);
                playerObject.body.setOrientation(msg.rotation);  // 회전 정보 적용

                if (!msg.currentAnimation.equals(playerObject.getCurrentAnimation())) {
                    playerObject.setAnimation(msg.currentAnimation, -1);
                }

                netPlayer.setCrouch(msg.isCrouch);
                netPlayer.setCurrentWeaponType(msg.currentWeaponType);
            }
        }
    }

    private void handleNetworkPlayerHit(GameObject hitPlayer, GameObject bullet) {
        for (NetworkPlayer netPlayer : networkPlayers.values()) {
            if (netPlayer.getGameObject() == hitPlayer) {
                try{
                    networkClient.sendDamage(netPlayer.getPlayerId(), 0.25f);
                } catch (Exception e) {
                    Gdx.app.error("PlayerController", "Error while placing the bomb", e);
                }

                Gdx.app.log("NetworkWorld", "Sent damage to player: " + netPlayer.getPlayerId());
                break;
            }
        }

        // 총알 제거 및 효과
        Vector3 hitPosition = new Vector3(hitPlayer.getPosition());
        addBulletHole(hitPosition, new Vector3(0, 1, 0));
        removeObject(bullet);
        Main.assets.sounds.HIT.play();
    }
    public void handleShootEvent(ShootMessage msg) {
        // 총을 쏜 위치
        Vector3 shootPosition = msg.shootPosition;

        // 현재 플레이어의 위치
        Vector3 playerPosition = getPlayer().getPosition();

        // 거리 계산
        float distance = playerPosition.dst(shootPosition);

        // 볼륨 계산 (거리에 따라 감소, 최대 볼륨 = 1, 최소 볼륨 = 0)
        float maxHearingDistance = 200f; // 플레이어가 소리를 들을 수 있는 최대 거리
        float volume = Math.max(0, 1 - (distance / maxHearingDistance));

        // 볼륨에 따라 총소리 재생
        switch (msg.weaponType) {
            case "PISTOL":
            case "AK47":
            case "SG553":
            case "AR15":
                if (volume > 0) { // 볼륨이 0보다 클 때만 소리를 재생
                    Main.assets.sounds.GUN_SHOT.play(volume);
                    Gdx.app.log("NetworkWorld", String.format("Playing gunshot sound at volume %.2f (distance %.2f)", volume, distance));
                }
                break;

            default:
                Gdx.app.log("NetworkWorld", "Unknown weapon type: " + msg.weaponType);
                break;
        }
    }

    public void fireSound(){
        Main.assets.sounds.GUN_SHOT.play();
        Vector3 shootPosition = new Vector3(getPlayer().getPosition());
        if (networkClient != null) {
            networkClient.sendShootEvent(
                networkClient.getPlayerId(),
                shootPosition,
                weaponState.currentWeaponType.name()
            );
        }
    }

    public void fireWeapon(Vector3 viewingDirection, PhysicsRayCaster.HitPoint hitPoint) {
        if(isPlayerDead())  // World의 public 메서드 사용
            return;
        if(!weaponState.isWeaponReady())
            return;
        weaponState.firing = true;

        switch(weaponState.currentWeaponType){
            case KNIFE:
                Main.assets.sounds.KNIFE_STAB.play();
                DAMAGE_AMOUNT = 0.2f;
                break;
            case PISTOL:
                DAMAGE_AMOUNT = 0.1f;
                fireSound();
                break;
            case AK47:
                DAMAGE_AMOUNT = 0.3f;
                fireSound();
                break;
            case SG553:
                DAMAGE_AMOUNT = 0.5f;
                fireSound();
                break;
            case AR15:
                DAMAGE_AMOUNT = 0.35f;
                fireSound();
                break;
        }
        // hitPoint가 적중했고, 대상이 다른 플레이어인 경우
        if(hitPoint.hit && hitPoint.refObject.type == GameObjectType.TYPE_OTHER_PLAYER) {
            // 피격된 플레이어를 찾아서 데미지 전송
            NetworkPlayer targetPlayer = findNetworkPlayerByGameObject(hitPoint.refObject);
            if (targetPlayer != null) {
                Gdx.app.log("NetworkWorld", "Hit player: " + targetPlayer.getPlayerId());
                try{
                    if(hitPoint.worldContactPoint.y - targetPlayer.getPlayerPosition().y >= 2.03
                        && hitPoint.worldContactPoint.y - targetPlayer.getPlayerPosition().y <= 2.73){ //head shot

                        if (weaponState.currentWeaponType ==WeaponType.PISTOL ||
                            weaponState.currentWeaponType == WeaponType.KNIFE) {
                            networkClient.sendDamage(targetPlayer.getPlayerId(),0.5f);
                        }
                        else{
                            networkClient.sendDamage(targetPlayer.getPlayerId(), 1.0f); //한방
                        }

                    }
                    else if(hitPoint.worldContactPoint.y - targetPlayer.getPlayerPosition().y < 2.03
                        && hitPoint.worldContactPoint.y - targetPlayer.getPlayerPosition().y >= 0.23){ //몸통 샷
                        networkClient.sendDamage(targetPlayer.getPlayerId(),DAMAGE_AMOUNT);
                    }

                    else if(hitPoint.worldContactPoint.y - targetPlayer.getPlayerPosition().y< 0.23
                        && hitPoint.worldContactPoint.y -targetPlayer.getPlayerPosition().y>= -3.77){ //다리 샷
                        networkClient.sendDamage(targetPlayer.getPlayerId(),DAMAGE_AMOUNT/2);
                    }
                } catch (Exception e) { Gdx.app.error("PlayerController", "Error while placing the bomb", e);}


                // 시각 효과
                addBulletHole(hitPoint.worldContactPoint, hitPoint.normal);
                addBulletTracer(getPlayer().getPosition(), hitPoint.worldContactPoint);
                Main.assets.sounds.HIT.play();
            }
        }
    }

    private NetworkPlayer findNetworkPlayerByGameObject(GameObject gameObject) {
        for (NetworkPlayer player : networkPlayers.values()) {
            if (player.getGameObject() == gameObject) {
                return player;
            }
        }
        return null;
    }


    @Override
    public void clear() {
        super.clear();
        networkPlayers.clear();
    }

    @Override
    public void dispose() {
        for (NetworkPlayer netPlayer : networkPlayers.values()) {
            if (netPlayer.getGameObject() != null) {
                removeObject(netPlayer.getGameObject());
            }
        }

        networkPlayers.clear();

        super.dispose();
    }
}
