package com.game.fps;

public class GameObjectType {
    public final static GameObjectType TYPE_STATIC = new GameObjectType("static", true, false, false, false, false, false, false, false);
    public final static GameObjectType TYPE_PLAYER = new GameObjectType("player", false, true, false, false, false, false, false, false);

    public final static GameObjectType TYPE_PICKUP_GUN = new GameObjectType("gun", false, false, true, false , false, false, false, false);
    public final static GameObjectType TYPE_OTHER_PLAYER = new GameObjectType("enemy", false, false, false, true, false, false, false, false);
    public final static GameObjectType TYPE_FRIENDLY_BULLET = new GameObjectType("bullet", false, false, false, false, true,false, false, false);

    public final static GameObjectType TYPE_TARGET = new GameObjectType("target",true,false,false,false,false,false,false, true);

    public String typeName;
    public boolean isStatic;
    public boolean isPlayer;
    public boolean canPickup;
    public boolean isEnemy;
    public boolean isFriendlyBullet;
    public boolean isEnemyBullet;
    public boolean isNavMesh;
    public boolean isTarget;

    public GameObjectType(String typeName, boolean isStatic, boolean isPlayer, boolean canPickup, boolean isEnemy, boolean isFriendlyBullet, boolean isEnemyBullet, boolean isNavMesh,boolean isTarget) {
        this.typeName = typeName;
        this.isStatic = isStatic;
        this.isPlayer = isPlayer;
        this.canPickup = canPickup;
        this.isEnemy = isEnemy;
        this.isFriendlyBullet = isFriendlyBullet;
        this.isEnemyBullet = isEnemyBullet;
        this.isNavMesh = isNavMesh;
        this.isTarget= isTarget;

    }
}
