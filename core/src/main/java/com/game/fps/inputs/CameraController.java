package com.game.fps.inputs;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.game.fps.Settings;

public class CameraController extends InputAdapter {

    private final Camera camera;
    private boolean thirdPersonMode = false;
    private final Vector3 offset = new Vector3();
    private float distance = 5f;

    public CameraController(Camera camera ) {
        this.camera = camera;
        offset.set(0, 2, -3);
    }

    public void setThirdPersonMode(boolean mode){
        thirdPersonMode = mode;
    }

    public boolean getThirdPersonMode() { return thirdPersonMode; }

    public void update ( Vector3 playerPosition, Vector3 viewDirection ) {
        playerPosition.y += Settings.realeyeHeight;

        camera.position.set(playerPosition);

        if(thirdPersonMode) {
            // 플레이어 위치로부터 카메라의 오프셋
            offset.set(viewDirection).scl(-1);      // 시선 방향 반전
            offset.y = Math.max(0, offset.y);             // 플레이어 아래로 가지 않도록 설정
            offset.nor().scl(distance);                   // 카메라 거리 조정
            camera.position.add(offset);

            camera.lookAt(playerPosition);
            camera.up.set(Vector3.Y);
        }
        else {
            camera.direction.set(viewDirection);
        }
        camera.update(true);
    }

    @Override
    public boolean scrolled (float amountX, float amountY) {
        return zoom(amountY );
    }

    private boolean zoom (float amount) {
        if(amount < 0 && distance < 5f)
            return false;
        if(amount > 0 && distance > 50f)
            return false;
        distance += amount;
        return true;
    }
}
