package com.game.fps;


import com.badlogic.gdx.math.Vector3;

public class Settings {
    static public boolean supportControllers = true;       // disable in case it causes issues
    static public float verticalReadjustSpeed = 4f;

    static public float eyeHeight = 2.5f;   // meters
    static public float realeyeHeight = 2.3f;

    static public float walkSpeed = 100f;    // m/s
    static public float runFactor = 2f;     // 달릴때 속도에 곱함
    static public float turnSpeed = 120f;   // degrees/s
    static public float jumpForce = 0.5f;
    static public float groundRayLength = 3.13f;



    static public boolean invertLook = false;
    static public boolean freeLook = true;
    static public float headBobDuration = 0.6f; // s
    static public float headBobHeight = 0.03f;  // m
    static public float degreesPerPixel = 0.1f; // 마우스 감도

    static public float gravity = -9.8f; // meters / s^2

    static public final int shadowMapSize = 4096;


    static public float playerLinearDamping = 0.05f;
    static public float playerAngularDamping = 0.5f;

    static public float gunForce = 40f;


    static public Vector3 gunPosition = new Vector3(-1.1f, 1.9f, 1.8f); // 건의 카메라 위치
    static public Vector3 knifePosition = new Vector3(-0.2f, 1.9f, 1.8f);
    static public float gunScale = 3.0f;

    static public final String GLTF_FILE = "models/step21.gltf";
    public static float bombScale = 1.0f;
}
