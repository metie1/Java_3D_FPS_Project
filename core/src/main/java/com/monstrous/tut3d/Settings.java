package com.monstrous.tut3d;


import com.badlogic.gdx.math.Vector3;

public class Settings {
    static public float eyeHeight = 2.5f;   // meters

    static public float walkSpeed = 5f;    // m/s
    static public float runFactor = 3f;    // m/s
    static public float turnSpeed = 120f;   // degrees/s
    static public float jumpForce = 10.0f;
    static public float groundRayLength = 1.2f;


    static public boolean invertLook = false;
    static public boolean freeLook = true;
    static public float headBobDuration = 0.6f; // s
    static public float headBobHeight = 0.04f;  // m
    static public float degreesPerPixel = 0.1f; // mouse sensitivity

    static public float gravity = -9.8f; // meters / s^2

    static public final int shadowMapSize = 4096;

    static public float ballMass = 0.2f;
    static public float ballForce = 100f;

    static public float playerMass = 1.0f;
    static public float playerLinearDamping = 0.05f;
    static public float playerAngularDamping = 0.5f;

    static public float panMass = 0.5f;
    static public float panForce = 135f;
    static public float gunForce = 80f;

    static public Vector3 gunPosition = new Vector3(-1.1f, 1.9f, 1.8f); // gun position in gun camera view
    static public float gunScale = 3.0f;

    static public final String GLTF_FILE = "models/step12.gltf";
}
