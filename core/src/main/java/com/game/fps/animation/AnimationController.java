package com.game.fps.animation;
import com.game.fps.GameObject;


public class AnimationController {
    private String currentAnimation;
    private boolean isLooping;

    public AnimationController() {
        currentAnimation = "p_idle";
        isLooping = true;
    }

    // 애니메이션 설정
    public void setAnimation(String animationName, boolean looping,GameObject player) {
        if (!animationName.equals(currentAnimation)) {  // 현재 애니메이션과 다를 경우에만 전환
            currentAnimation = animationName;
            isLooping = looping;
            playAnimation(animationName,player);
        }
    }

    // 애니메이션 재생 (이 예제에서는 콘솔 출력으로 대체)
    public void playAnimation(String animationName,GameObject player) {
        player.setAnimation(animationName,-1);
        // 실제 애니메이션 재생 로직 추가 필요
    }

    public String getCurrentAnimation() {
        return currentAnimation;
    }

    public boolean isLooping() {
        return isLooping;
    }
}
