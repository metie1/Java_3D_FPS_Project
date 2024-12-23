package com.game.fps.net;

import com.badlogic.gdx.math.Quaternion;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class KryoQuaternionSerializer extends Serializer<Quaternion> {
    @Override
    public void write(Kryo kryo, Output output, Quaternion q) {
        output.writeFloat(q.x);
        output.writeFloat(q.y);
        output.writeFloat(q.z);
        output.writeFloat(q.w);
    }

    @Override
    public Quaternion read(Kryo kryo, Input input, Class<? extends Quaternion> type) {
        float x = input.readFloat();
        float y = input.readFloat();
        float z = input.readFloat();
        float w = input.readFloat();
        return new Quaternion(x, y, z, w);
    }
}
