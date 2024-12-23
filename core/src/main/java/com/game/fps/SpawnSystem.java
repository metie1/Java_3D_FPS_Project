package com.game.fps;

import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpawnSystem {
    private final List<Vector3> teamASpawnPoints;
    private final List<Vector3> teamBSpawnPoints;
    private final Random random;

    public SpawnSystem() {
        teamASpawnPoints = new ArrayList<>();
        teamBSpawnPoints = new ArrayList<>();
        random = new Random();

        // 스폰 포인트 초기화
        initializeSpawnPoints();
    }

    private void initializeSpawnPoints() {
        // 수비팀: x:155.746 m y:5 z:-293.402 m
        // 공격팀  x:-13.435 m y: 5 z:-30.6744 m

        // Team A 스폰 포인트
        teamASpawnPoints.add(new Vector3(-13, 5, -30));

        // Team B 스폰 포인트
        teamBSpawnPoints.add(new Vector3(155, 0, -293));
    }

    public Vector3 getRandomSpawnPoint(boolean isTeamA) {
        List<Vector3> spawnPoints = isTeamA ? teamASpawnPoints : teamBSpawnPoints;
        int index = random.nextInt(spawnPoints.size());
        return spawnPoints.get(index).cpy();
    }
}
