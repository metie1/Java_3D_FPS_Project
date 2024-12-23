package com.game.fps.net;

import com.badlogic.gdx.math.Quaternion;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.badlogic.gdx.math.Vector3;
import com.game.fps.WeaponType;
import com.game.fps.net.message.*;

import java.io.IOException;
import java.net.BindException;
import java.util.HashMap;
import java.util.Map;

public class GameServer {
    private final Server server;
    private final Map<Integer, String[]> connectedPlayers = new HashMap<>();
    private boolean isRunning;

    public GameServer() {
        System.out.println("Creating GameServer...");
        server = new Server(NetworkConstants.BUFFER_SIZE, NetworkConstants.OBJECT_BUFFER_SIZE);
        isRunning = false;

        System.out.println("Registering Kryo classes...");
        Kryo kryo = server.getKryo();
        registerClasses(kryo);

        System.out.println("Setting up server listener...");
        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                System.out.println("Client connected: " + connection.getID());
            }

            @Override
            public void disconnected(Connection connection) {
                handlePlayerDisconnect(connection);
            }

            @Override
            public void received(Connection connection, Object object) {
                handleMessage(connection, object);
            }
        });
    }

    private void registerClasses(Kryo kryo) {
        try {
            // 상위 클래스를 먼저 등록
            kryo.register(NetworkMessage.class);

            // 메시지 클래스들
            kryo.register(PlayerConnectMessage.class);
            kryo.register(PlayerDisconnectMessage.class);
            kryo.register(PlayerStateMessage.class);
            kryo.register(BombPlantedMessage.class);
            kryo.register(SpawnBombMessage.class);
            kryo.register(DamageMessage.class);
            kryo.register(StartRoundMessage.class); // 추가
            kryo.register(BombDefusedMessage.class);
            kryo.register(ShootMessage.class);

            // 기타 필요한 클래스들
            kryo.register(Vector3.class);
            kryo.register(String.class);
            kryo.register(float[].class); // Vector3 serialization에 필요
            kryo.register(Quaternion.class); // Quaternion 클래스 등록 추가
            kryo.register(WeaponType.class);
            // serializer 추가
            kryo.register(float[].class);
            kryo.register(Quaternion.class, new KryoQuaternionSerializer());

            System.out.println("Kryo classes registered successfully");
        } catch (Exception e) {
            System.err.println("Error registering Kryo classes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void start() {
        if (isRunning) {
            System.out.println("Server is already running");
            return;
        }

        try {
            System.out.println("Starting server...");
            server.start();

            try {
                System.out.println("Binding server to ports - TCP: " + NetworkConstants.TCP_PORT +
                    ", UDP: " + NetworkConstants.UDP_PORT);
                server.bind(NetworkConstants.TCP_PORT, NetworkConstants.UDP_PORT);
            } catch (BindException e) {
                System.err.println("Failed to bind server - ports might be in use");
                System.err.println("Please ensure ports " + NetworkConstants.TCP_PORT +
                    " and " + NetworkConstants.UDP_PORT + " are available");
                stop();
                throw e;
            }

            isRunning = true;
            System.out.println("Server started and bound successfully");

        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            stop();
            throw new RuntimeException("Server startup failed", e);
        }
    }

    private void handlePlayerDisconnect(Connection connection) {
        String[] playerName = connectedPlayers.remove(connection.getID());
        if (playerName != null) {
            System.out.println("Player disconnected: " + playerName + " (ID: " + connection.getID() + ")");
            PlayerDisconnectMessage msg = new PlayerDisconnectMessage();
            msg.playerId = connection.getID();
            server.sendToAllExceptTCP(connection.getID(), msg);
        }
    }

    private void handleMessage(Connection connection, Object object) {
        try {
            if (object instanceof PlayerConnectMessage) {
                PlayerConnectMessage msg = (PlayerConnectMessage) object;
                int newPlayerId = connection.getID();

                // 새로운 플레이어 정보 저장
                connectedPlayers.put(newPlayerId, new String[]{msg.playerName,msg.playerTeam});

                // 새로운 플레이어에게 기존 플레이어들의 정보 전송
                for (Map.Entry<Integer, String[]> entry : connectedPlayers.entrySet()) {
                    if (entry.getKey() != newPlayerId) {
                        PlayerConnectMessage existingPlayerMsg = new PlayerConnectMessage();
                        existingPlayerMsg.playerId = entry.getKey();
                        existingPlayerMsg.playerName = entry.getValue()[0];
                        existingPlayerMsg.playerTeam = entry.getValue()[1]; // 팀 정보 포함
                        connection.sendTCP(existingPlayerMsg);
                    }
                }

                // 다른 모든 플레이어에게 새로운 플레이어 정보 전송
                msg.playerId = newPlayerId;
                server.sendToAllExceptTCP(connection.getID(), msg);

                System.out.println("Player connected: " + msg.playerName + " (ID: " + newPlayerId + ")");
            }
            else if (object instanceof PlayerStateMessage) {
                PlayerStateMessage msg = (PlayerStateMessage) object;
                msg.playerId = connection.getID();  // 연결 ID를 플레이어 ID로 사용
                server.sendToAllExceptUDP(connection.getID(), msg);
            }
            else if (object instanceof BombPlantedMessage) { //폭탄 설치
                BombPlantedMessage msg = (BombPlantedMessage) object;

                // 모든 클라이언트에게 폭탄 설치 메시지 브로드캐스트
                server.sendToAllTCP(msg);
                System.out.println("Broadcasting BombPlantedMessage: " + msg.bombPlanted);
            }
            else if (object instanceof SpawnBombMessage) {
                SpawnBombMessage msg = (SpawnBombMessage) object;
                // 모든 클라이언트에게 폭탄 위치 브로드캐스트
                server.sendToAllExceptTCP(connection.getID(), msg);
                System.out.println("Bomb spawned at: " + msg.position);
            }
            else if (object instanceof StartRoundMessage) { // StartRoundMessage 처리
                StartRoundMessage msg = (StartRoundMessage) object;
                server.sendToAllTCP(msg); // 모든 클라이언트에 라운드 시작 알림
                System.out.println("Broadcasting StartRoundMessage for round: " + msg.roundNumber);
            }
            else if (object instanceof BombDefusedMessage) {
                BombDefusedMessage msg = (BombDefusedMessage) object;
                server.sendToAllTCP(msg); // 모든 클라이언트에 폭탄 해체 메시지 전달
                System.out.println("Broadcasting BombDefusedMessage");
            }
            if (object instanceof DamageMessage) {
                DamageMessage msg = (DamageMessage) object;
                msg.attackerId = connection.getID();  // 공격자 ID 설정
                System.out.println("Player " + msg.attackerId + " dealt " + msg.damage +
                    " damage to Player " + msg.targetId);
                server.sendToAllTCP(msg);  // 모든 클라이언트에게 전송
            }else if (object instanceof ShootMessage) {
                ShootMessage msg = (ShootMessage) object;
                msg.playerId = connection.getID(); // 발사한 플레이어 ID 설정

                // 모든 클라이언트에게 브로드캐스트
                server.sendToAllExceptTCP(connection.getID(), msg);
                System.out.println("Player " + msg.playerId + " fired weapon: " + msg.weaponType);
            }

        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        if (!isRunning) {
            return;
        }

        try {
            System.out.println("Stopping server...");
            server.stop();
            isRunning = false;
            System.out.println("Server stopped successfully");
        } catch (Exception e) {
            System.err.println("Error stopping server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
}
