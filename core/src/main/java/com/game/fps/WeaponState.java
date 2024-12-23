package com.game.fps;

import com.badlogic.gdx.math.Vector3;
import com.game.fps.physics.CollisionShapeType;

import java.util.HashMap;
import java.util.Map;

public class WeaponState {
    // 보유 여부
    public boolean havePistol;
    public boolean haveAK47;
    public boolean haveSG553;
    public boolean haveAR15;

    public WeaponType currentWeaponType;
    public float fireTimer;
    public boolean firing;
    public boolean scopeMode;

    private final Map<WeaponType, AmmoInfo> ammoMap;

    // World 참조를 위한 setter
    private World world;
    public void setWorld(World world) {
        this.world = world;
    }

    public WeaponState() {
        ammoMap = new HashMap<>();
        reset();
    }

    public void reset(){

        // 초기에는 칼만 소지
        havePistol = false;
        haveAK47 = false;
        haveSG553 = false;
        haveAR15 = false;

        currentWeaponType = WeaponType.KNIFE; // 기본 무기는 칼

        fireTimer = 0;
        scopeMode = false;
        firing = false;

        // 각 무기의 탄약 정보 초기화
        ammoMap.clear();
        ammoMap.put(WeaponType.PISTOL, new AmmoInfo(WeaponConstants.PISTOL_MAX_AMMO, WeaponConstants.PISTOL_MAX_RESERVE));
        ammoMap.put(WeaponType.AK47, new AmmoInfo(WeaponConstants.AK47_MAX_AMMO, WeaponConstants.AK47_MAX_RESERVE));
        ammoMap.put(WeaponType.SG553, new AmmoInfo(WeaponConstants.SG553_MAX_AMMO, WeaponConstants.SG553_MAX_RESERVE));
        ammoMap.put(WeaponType.AR15, new AmmoInfo(WeaponConstants.AR15_MAX_AMMO, WeaponConstants.AR15_MAX_RESERVE));
    }

    public boolean canShoot() {
        if (currentWeaponType == WeaponType.KNIFE || currentWeaponType == WeaponType.BOMB) {
            return true;
        }
        AmmoInfo ammoInfo = ammoMap.get(currentWeaponType);
        return ammoInfo != null && ammoInfo.canShoot();
    }

    public void shoot() {
        if (currentWeaponType != WeaponType.KNIFE && currentWeaponType != WeaponType.BOMB) {
            AmmoInfo ammoInfo = ammoMap.get(currentWeaponType);
            if (ammoInfo != null) {
                ammoInfo.shoot();
            }
        }
    }

    public boolean reload() {
        AmmoInfo ammoInfo = ammoMap.get(currentWeaponType);
        return ammoInfo != null && ammoInfo.reload();
    }

    public AmmoInfo getCurrentAmmoInfo() {
        return ammoMap.get(currentWeaponType);
    }

    public boolean isWeaponReady() {
        if(fireTimer > 0)
            return false;
        if (!canShoot())
            return false;
        fireTimer = currentWeaponType.repeatRate;
        return true;
    }

    public boolean hasMainWeapon() {
        return haveAK47 || haveSG553 || haveAR15;
    }

    public void removeMainWeapons() {
        // 현재 들고 있는 주무기 정보 저장
        WeaponType currentMain = null;
        if (haveAK47) currentMain = WeaponType.AK47;
        else if (haveSG553) currentMain = WeaponType.SG553;
        else if (haveAR15) currentMain = WeaponType.AR15;

        // 주무기 상태 초기화
        haveAK47 = false;
        haveSG553 = false;
        haveAR15 = false;

        // 현재 들고 있던 주무기를 떨어뜨림
        if (currentMain != null) {
            dropMainWeapon(currentMain);
        }

        // 현재 무기가 주무기였다면 나이프로 전환
        if (currentWeaponType == WeaponType.AK47 ||
            currentWeaponType == WeaponType.SG553 ||
            currentWeaponType == WeaponType.AR15) {
            currentWeaponType = WeaponType.KNIFE;
        }
    }

    private void dropMainWeapon(WeaponType weaponType) {
        // World에 접근하기 위한 참조 필요
        if (world != null && world.getPlayer() != null) {
            Vector3 dropPosition = new Vector3(world.getPlayer().getPosition());
            // 플레이어 앞쪽으로 약간 떨어진 위치에 무기 생성
            Vector3 forward = world.getPlayerController().getForwardDirection();
            dropPosition.add(forward.scl(2f)); // 플레이어 앞 2미터 위치

            String modelName = "";
            switch (weaponType) {
                case AK47:
                    modelName = "AkArmature";
                    break;
                case SG553:
                    modelName = "SgArmature";
                    break;
                case AR15:
                    modelName = "ArArmature";
                    break;
            }

            if (!modelName.isEmpty()) {
                world.spawnObject(
                    GameObjectType.TYPE_PICKUP_GUN,
                    modelName,
                    null,
                    CollisionShapeType.BOX,
                    true,
                    dropPosition
                );
            }
        }
    }


    public void switchWeapon() {
        // 보유한 무기만 순환
        if (currentWeaponType == WeaponType.KNIFE) {
            if (havePistol)
                currentWeaponType = WeaponType.PISTOL;
            else if (haveAK47)
                currentWeaponType = WeaponType.AK47;
            else if (haveSG553)
                currentWeaponType = WeaponType.SG553;
            else if (haveAR15)
                currentWeaponType = WeaponType.AR15;
        }
        else if (currentWeaponType == WeaponType.PISTOL) {
            if (haveAK47)
                currentWeaponType = WeaponType.AK47;
            else if (haveSG553)
                currentWeaponType = WeaponType.SG553;
            else if (haveAR15)
                currentWeaponType = WeaponType.AR15;
            else
                currentWeaponType = WeaponType.KNIFE;
        }
        else if (currentWeaponType == WeaponType.AK47) {
            if (haveSG553)
                currentWeaponType = WeaponType.SG553;
            else if (haveAR15)
                currentWeaponType = WeaponType.AR15;
            else
                currentWeaponType = WeaponType.KNIFE;
        }
        else if (currentWeaponType == WeaponType.SG553) {
            if (haveAR15)
                currentWeaponType = WeaponType.AR15;
            else
                currentWeaponType = WeaponType.KNIFE;
        }
        else if (currentWeaponType == WeaponType.AR15) {
            currentWeaponType = WeaponType.KNIFE;
        }
    }

    public void update(float deltaTime) {
        fireTimer -= deltaTime;
    }
}
