package com.game.fps.views;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.game.fps.net.NetworkWorld;
import com.game.fps.SpectatorSystem;
import com.game.fps.GameObject;
import com.game.fps.Settings;

import com.badlogic.gdx.graphics.g3d.ModelBatch;

import com.game.fps.World;
import com.game.fps.inputs.CameraController;
import com.game.fps.physics.CollisionShapeType;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;



public class GameView implements Disposable {
    private final World world;                                // reference to World
    private final SceneManager sceneManager;
    private final PerspectiveCamera cam;
    private final Cubemap diffuseCubemap;
    private final Cubemap environmentCubemap;
    private final Cubemap specularCubemap;
    private final Texture brdfLUT;
    private SceneSkybox skybox;
    private final CameraController camController;

    private final ModelBatch modelBatch;

    private final boolean isOverlay;
    private float bobAngle;     // angle in the camera bob cycle (radians)
    private final float bobScale;     // scale factor for camera bobbing
    private boolean thirdPersonMode;  // 변수 추가

    private ShaderProvider shaderProvider;
    // if the view is an overlay, we don't clear screen on render, only depth buffer
    //
    public GameView(World world, boolean overlay, float near, float far, float bobScale) {
        this.world = world;
        this.isOverlay = overlay;
        this.bobScale = bobScale;
        this.thirdPersonMode = false;  // 초기값 설정

        sceneManager = new SceneManager(100);
        modelBatch = new ModelBatch();

        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(),  Gdx.graphics.getHeight());
        cam.position.set(0f, Settings.eyeHeight, 0f);
        cam.lookAt(0,Settings.eyeHeight,10f);
        cam.near = near;
        cam.far = far;
        cam.update();

        sceneManager.setCamera(cam);
        camController = new CameraController(cam);
        camController.setThirdPersonMode(true);

        // setup light
        DirectionalLightEx light = new net.mgsx.gltf.scene3d.lights.DirectionalShadowLight(Settings.shadowMapSize, Settings.shadowMapSize)
            .setViewport(50,50,10f,100);
        light.direction.set(1, -3, 1).nor();
        light.color.set(Color.WHITE);
        light.intensity = 3f;
        sceneManager.environment.add(light);

        // setup quick IBL (image based lighting)
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
        environmentCubemap = iblBuilder.buildEnvMap(1024);
        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();

        // This texture is provided by the library, no need to have it in your assets.
        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        sceneManager.setAmbientLight(0.1f);
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));
        sceneManager.environment.set(new PBRFloatAttribute(PBRFloatAttribute.ShadowBias, 1f/512f)); // reduce shadow acne

        // setup skybox
        if(!isOverlay) {
            skybox = new SceneSkybox(environmentCubemap);
            sceneManager.setSkyBox(skybox);
        }
    }

    public void update(World world, float deltaTime) {
        if (world instanceof NetworkWorld) {
            NetworkWorld netWorld = (NetworkWorld) world;
            SpectatorSystem spectatorSystem = netWorld.getSpectatorSystem();

            if (spectatorSystem.isSpectating()) {
                // 관전 모드일 때 카메라 위치와 방향 업데이트
                cam.position.set(spectatorSystem.getSpectatorPosition());
                cam.direction.set(spectatorSystem.getSpectatorDirection());
            } else {
                // 일반 플레이어 시점
                GameObject player = world.getPlayer();
                if (player != null) {
                    updateNormalCamera(player);
                }
            }
        }
        cam.update(true);
    }

    private void updateNormalCamera(GameObject player) {
        cam.position.set(player.getPosition());
        cam.position.y += Settings.eyeHeight;

        // 플레이어의 회전 상태를 카메라에 반영
        if (!thirdPersonMode) {
            cam.direction.set(player.getDirection());
            cam.up.set(Vector3.Y);
            cam.update();
        }
        // ... 나머지 카메라 업데이트 로직
    }

    public void setThirdPersonMode(boolean mode) {
        this.thirdPersonMode = mode;
    }

    public PerspectiveCamera getCamera() {
        return cam;
    }

    public void setFieldOfView( float fov ){
        cam.fieldOfView = fov;
        cam.update();
    }

    public CameraController getCameraController() {
        return camController;
    }


    public void refresh() {
        sceneManager.getRenderableProviders().clear();



        // 모든 게임 오브젝트의 회전 상태를 반영
        int num = world.getNumGameObjects();

        for(int i = 0; i < num; i++) {
            GameObject go = world.getGameObject(i);

            Scene scene = go.scene;

            if (go.visible) {
                // Scene scene = go.scene;
                // 회전 상태 적용
                if(go.body.getShapeType() == CollisionShapeType.CAPSULE)
                    scene.modelInstance.transform.set(go.getPosition(), go.body.getBodyOrientation());
                sceneManager.addScene(scene, false);
            }
        }


    }



    public boolean inThirdPersonMode() {
        return camController.getThirdPersonMode();
    }

    public void render(float delta, float speed ) {
        if(world instanceof NetworkWorld) {
            NetworkWorld netWorld = (NetworkWorld) world;
            SpectatorSystem spectatorSystem = netWorld.getSpectatorSystem();

            if (spectatorSystem.isSpectating()) {
                // 관전 모드일 때 카메라 위치와 방향 업데이트
                cam.position.set(spectatorSystem.getSpectatorPosition());
                cam.direction.set(spectatorSystem.getSpectatorDirection());
            } else {
                // 일반 플레이어 시점
                GameObject player = world.getPlayer();
                if (player != null) {
                    updateNormalCamera(player);
                }
            }
            cam.update(true);
        }
        if(!isOverlay)
            camController.update(world.getPlayer().getPosition(), world.getPlayerController().getViewingDirection());
        else
            cam.position.y = Settings.eyeHeight;

        addHeadBob(delta, speed);
        cam.update();
        refresh();
        sceneManager.update(delta);

        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);   // clear depth buffer only
        sceneManager.render();

        // 총알 효과 렌더링
        modelBatch.begin(cam);
        world.getEffectsManager().render(modelBatch);
        modelBatch.end();
    }

    private void addHeadBob(float deltaTime, float speed ) {

        if( speed > 0.1f ) {
            bobAngle += speed * deltaTime * Math.PI / Settings.headBobDuration;

            // move the head up and down in a sine wave
            cam.position.y += bobScale * Settings.headBobHeight * (float)Math.sin(bobAngle);
        }

    }

    public void resize(int width, int height){
        sceneManager.updateViewport(width, height);
    }



    @Override
    public void dispose() {
        sceneManager.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        modelBatch.dispose();
        if(!isOverlay)
            skybox.dispose();
    }
}
