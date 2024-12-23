package com.game.fps.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Quaternion;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryo.Kryo;
import com.badlogic.gdx.math.Vector3;
import com.game.fps.GameObject;
import com.game.fps.WeaponType;
import com.game.fps.GameObjectType;
import com.game.fps.net.message.*;

import com.game.fps.physics.CollisionShapeType;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkClient {
    private static final int TIMEOUT = 10000;
    private static final int RETRY_DELAY = 1000;
    private static final int MAX_RETRIES = 3;

    private final Client client;
    private final String playerName;
    private int playerId;
    private NetworkWorld networkWorld;
    private final AtomicBoolean isConnecting;
    private volatile boolean isConnected;
    private Thread updateThread;

    public NetworkClient(String playerName) {
        Gdx.app.log("NetworkClient", "Creating NetworkClient for player: " + playerName);
        this.playerName = playerName;
        this.playerId = -1;  // 초기값을 -1로 설정

        try {
            this.client = new Client(NetworkConstants.BUFFER_SIZE, NetworkConstants.OBJECT_BUFFER_SIZE);
            this.isConnected = false;
            this.isConnecting = new AtomicBoolean(false);

            Gdx.app.log("NetworkClient", "Registering Kryo classes");
            Kryo kryo = client.getKryo();
            registerClasses(kryo);

            Gdx.app.log("NetworkClient", "Adding network listener");
            client.addListener(new Listener() {
                @Override
                public void connected(Connection connection) {
                    Gdx.app.log("NetworkClient", "Connected to server");
                    isConnected = true;
                    playerId = connection.getID();  // 연결 ID를 플레이어 ID로 설정

                    PlayerConnectMessage msg = new PlayerConnectMessage();
                    msg.playerName = playerName;
                    msg.playerId = playerId;
                    msg.playerTeam = networkWorld.returnTeamName();
                    client.sendTCP(msg);
                }

                @Override
                public void disconnected(Connection connection) {
                    Gdx.app.log("NetworkClient", "Disconnected from server");
                    isConnected = false;
                    stopUpdateThread();
                }

                @Override
                public void received(Connection connection, Object object) {

                    if(networkWorld != null) {
                        handleMessage(object);
                    }
                }
            });

            Gdx.app.log("NetworkClient", "Starting client");
            client.start();

        } catch (Exception e) {
            Gdx.app.error("NetworkClient", "Error creating NetworkClient", e);
            throw new RuntimeException("Failed to create NetworkClient", e);
        }
    }

    public int getPlayerId() {
        return playerId;
    }

    public void connect(String host) throws IOException {
        Gdx.app.log("NetworkClient", "Attempting to connect to host: " + host);
        if (client == null) {
            throw new IllegalStateException("Client is null");
        }

        if (isConnecting.get()) {
            Gdx.app.log("NetworkClient", "Already attempting to connect");
            return;
        }

        isConnecting.set(true);
        int retries = 0;
        Exception lastException = null;

        while (retries < NetworkConstants.MAX_CONNECTION_RETRIES && !isConnected) {
            try {
                Gdx.app.log("NetworkClient", "Connection attempt " + (retries + 1) + "/" + NetworkConstants.MAX_CONNECTION_RETRIES);

                // update 스레드 시작
                startUpdateThread();

                // 실제 연결 시도
                client.connect(NetworkConstants.CONNECTION_TIMEOUT, host, NetworkConstants.TCP_PORT, NetworkConstants.UDP_PORT);

                // 연결 성공
                isConnecting.set(false);
                Gdx.app.log("NetworkClient", "Successfully connected to server");
                return;

            } catch (IOException e) {
                Gdx.app.error("NetworkClient", "Connection attempt failed: " + e.getMessage(), e);
                lastException = e;
                retries++;
                stopUpdateThread();

                if (retries < NetworkConstants.MAX_CONNECTION_RETRIES) {
                    Gdx.app.log("NetworkClient", "Waiting " + (NetworkConstants.CONNECTION_RETRY_DELAY/1000) + " seconds before next attempt");
                    try {
                        Thread.sleep(NetworkConstants.CONNECTION_RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Connection interrupted", ie);
                    }
                }
            }
        }

        isConnecting.set(false);
        throw new IOException("Failed to connect after " + NetworkConstants.MAX_CONNECTION_RETRIES + " attempts", lastException);
    }
    public NetworkWorld getNetworkWorld() {return networkWorld;}
    private void startUpdateThread() {
        stopUpdateThread();
        updateThread = new Thread(() -> {
            Gdx.app.log("NetworkClient", "Update thread started");
            while (!Thread.currentThread().isInterrupted() && (isConnecting.get() || isConnected)) {
                try {
                    try {
                        client.update(16);
                    } catch (IOException e) {
                        Gdx.app.error("NetworkClient", "Error updating client", e);
                        break;
                    }
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            Gdx.app.log("NetworkClient", "Update thread stopped");
        }, "NetworkClient-Update");
        updateThread.setDaemon(true);
        updateThread.start();
    }

    private void stopUpdateThread() {
        if (updateThread != null) {
            updateThread.interrupt();
            try {
                updateThread.join(1000);
            } catch (InterruptedException e) {
                Gdx.app.error("NetworkClient", "Error stopping update thread", e);
            }
            updateThread = null;
        }
    }

    public void setNetworkWorld(NetworkWorld world) {
        this.networkWorld = world;
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

    private void handleMessage(Object object) {
        try {
            if (object instanceof PlayerConnectMessage) {
                PlayerConnectMessage msg = (PlayerConnectMessage) object;

                if (msg.playerId != this.playerId) {
                    networkWorld.addNetworkPlayer(msg.playerId, msg.playerName, new Vector3(0, 5, 0), msg.playerTeam);
                    Gdx.app.log("NetworkClient", "Player connected: " + msg.playerName +
                        " (ID: " + msg.playerId + ")");
                }
            }
            else if (object instanceof PlayerDisconnectMessage) {
                PlayerDisconnectMessage msg = (PlayerDisconnectMessage) object;
                networkWorld.removeNetworkPlayer(msg.playerId);
                Gdx.app.log("NetworkClient", "Player disconnected: ID " + msg.playerId);
            }
            else if (object instanceof PlayerStateMessage) {
                PlayerStateMessage msg = (PlayerStateMessage) object;
                if (msg.playerId != this.playerId) {
                    networkWorld.updateNetworkPlayerState(msg);
                }
            }
            else if (object instanceof SpawnBombMessage) {
                SpawnBombMessage msg = (SpawnBombMessage) object;
                if (networkWorld != null) {
                    // OpenGL 작업을 메인 스레드에서 실행
                    Gdx.app.postRunnable(() -> {
                        Gdx.app.log("NetworkClient", "Bomb spawned at: " + msg.position);
                        GameObject bomb = networkWorld.spawnObject(
                            GameObjectType.TYPE_STATIC,
                            "bomb",
                            null,
                            CollisionShapeType.BOX,
                            true,
                            msg.position
                        );
                        networkWorld.setBombObject(bomb);
                    });
                    networkWorld.setBombPosition(msg.position);
                }
            }
            else if (object instanceof StartRoundMessage) {
                StartRoundMessage msg = (StartRoundMessage) object;
                Gdx.app.log("NetworkClient", "Received StartRoundMessage for round: " + msg.roundNumber);

                if (networkWorld.roundSystem != null) {
                    networkWorld.roundSystem.startRound(); // 라운드 시작
                }
            }
            else if (object instanceof BombDefusedMessage) {
                BombDefusedMessage msg = (BombDefusedMessage) object;
                Gdx.app.log("NetworkClient", "Bomb defused by player: " + msg.playerId);
                GameObject bomb = networkWorld.getBombManager().getBomb();
                // BombDefusedMessage 수신 시 NetworkWorld에서 폭탄 제거
                if (networkWorld != null) {
                    Gdx.app.postRunnable(() -> {
                        networkWorld.removeBomb();
                        networkWorld.roundSystem.defuseBomb();
                        networkWorld.roundSystem.BombDefuseEnd();
                        if(bomb != null) {
                            networkWorld.removeObject(bomb);
                        }
                    });
                }
            }
            if (object instanceof DamageMessage) {
                DamageMessage msg = (DamageMessage) object;
                Gdx.app.log("NetworkClient", String.format(
                    "Received damage: From=%d To=%d Amount=%.2f",
                    msg.attackerId, msg.targetId, msg.damage));

                networkWorld.handleDamage(msg.targetId, msg.attackerId, msg.damage);
            }
            else if (object instanceof BombPlantedMessage) {
                BombPlantedMessage msg = (BombPlantedMessage) object;
                if (networkWorld != null && networkWorld.roundSystem != null) {
                    networkWorld.roundSystem.setBombPlanted(msg.bombPlanted);
                    Gdx.app.log("NetworkClient", "Bomb planted state updated: " + msg.bombPlanted);
                }
            }
            else if (object instanceof ShootMessage) {
                ShootMessage msg = (ShootMessage) object;
                if (networkWorld != null) {
                    networkWorld.handleShootEvent(msg);
                }
            }

        } catch (Exception e) {
            Gdx.app.error("NetworkClient", "Error handling message", e);
        }
    }

    public void sendPlayerState(PlayerStateMessage stateMsg) {
        if (!isConnected) return;

        try {
            stateMsg.playerId = this.playerId;  // 현재 플레이어 ID 설정

            client.sendTCP(stateMsg);  // TCP로 변경하여 신뢰성 확보
        } catch (Exception e) {
            Gdx.app.error("NetworkClient", "Error sending player state", e);
        }
    }
    public void sendBombDefusedMessage() {
        if (!isConnected()) return;

        try {
            BombDefusedMessage message = new BombDefusedMessage();
            client.sendTCP(message);
        } catch (Exception e) {
            Gdx.app.error("NetworkClient", "Error sending BombDefusedMessage", e);
        }
    }


    public void sendDamage(int targetId, float damage) {
        if (!isConnected) return;

        try {
            DamageMessage msg = new DamageMessage();
            msg.targetId = targetId;
            msg.damage = damage;

            Gdx.app.log("NetworkClient", String.format(
                "Sending damage: Target=%d Amount=%.2f",
                targetId, damage));

            client.sendTCP(msg);
        } catch (Exception e) {
            Gdx.app.error("NetworkClient", "Error sending damage", e);
        }
    }

    public void sendBombPosition(Vector3 position) {
        if (!isConnected) return;

        try {
            SpawnBombMessage msg = new SpawnBombMessage();
            msg.position.set(position);
            client.sendTCP(msg);
        } catch (Exception e) {
            Gdx.app.error("NetworkClient", "Error sending BombPositionMessage", e);
        }
    }

    public void sendBombPlanted(boolean bombPlanted) {
        if (!isConnected) return;

        try {
            BombPlantedMessage msg = new BombPlantedMessage(bombPlanted);
            client.sendTCP(msg); // 서버로 전송
        } catch (Exception e) {
            Gdx.app.error("NetworkClient", "Error sending BombPlantedMessage", e);
        }
    }

    public void sendStartRoundMessage(StartRoundMessage msg) {
        if (!isConnected) return;

        try {
            client.sendTCP(msg); // 서버로 메시지 전송
            Gdx.app.log("NetworkClient", "StartRoundMessage sent: Round " + msg.roundNumber);
        } catch (Exception e) {
            Gdx.app.error("NetworkClient", "Error sending StartRoundMessage", e);
        }
    }

    public void sendShootEvent(int playerId, Vector3 shootPosition, String weaponType) {
        if (!isConnected()) return;

        ShootMessage shootMessage = new ShootMessage();
        shootMessage.playerId = playerId;
        shootMessage.shootPosition.set(shootPosition);
        shootMessage.weaponType = weaponType;

        try {
            client.sendTCP(shootMessage);
        } catch (Exception e) {
            Gdx.app.error("NetworkClient", "Failed to send shoot event", e);
        }
    }
    public void disconnect() {
        if (isConnected) {
            PlayerDisconnectMessage msg = new PlayerDisconnectMessage();
            msg.playerId = playerId;
            try {
                client.sendTCP(msg);
            } catch (Exception e) {
                Gdx.app.error("NetworkClient", "Error sending disconnect message", e);
            }
        }
        stopUpdateThread();
        client.stop();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void dispose() {
        disconnect();
    }
}
