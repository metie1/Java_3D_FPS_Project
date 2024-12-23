package com.game.fps;

public enum WeaponType {

   BALL (0.2f),
   // GUN (0.5f),
   PISTOL(0.5f), // 권총
    AK47(0.2f),
    SG553(0.15f),
    AR15(0.12f),
    KNIFE(0.3f), // 기본 무기
    BOMB(1f);
   public final float repeatRate; // 발사 간격(초)
   WeaponType(float repeatRate ){
       this.repeatRate = repeatRate;
   }
}
