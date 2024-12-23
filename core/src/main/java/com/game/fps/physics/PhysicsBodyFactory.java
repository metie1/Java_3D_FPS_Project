package com.game.fps.physics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CapsuleShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.github.antzGames.gdx.ode4j.math.DQuaternion;
import com.github.antzGames.gdx.ode4j.ode.*;

public class PhysicsBodyFactory implements Disposable {

    public static final long CATEGORY_STATIC  = 1;      // 충돌 플래그
    public static final long CATEGORY_DYNAMIC  = 2;     // 충돌 플래그

    private final PhysicsWorld physicsWorld;
    private final DMass massInfo;
    private final Vector3 position;
    private final Quaternion q;
    private final ModelBuilder modelBuilder;
    private final Material material;
    private final Array<Disposable> disposables;


    public PhysicsBodyFactory(PhysicsWorld physicsWorld) {
        this.physicsWorld = physicsWorld;
        massInfo = OdeHelper.createMass();
        position = new Vector3();
        q = new Quaternion();
        modelBuilder = new ModelBuilder();
        material = new Material(ColorAttribute.createDiffuse(Color.WHITE));
        disposables = new Array<>();
    }

    public PhysicsBody createBody(ModelInstance collisionInstance, CollisionShapeType shapeType, boolean isStatic, String name) {
        BoundingBox bbox = new BoundingBox();
        Node node = collisionInstance.nodes.first();
        node.calculateBoundingBox(bbox, false); // 변환 없이 경계 상자 계산
        float w = bbox.getWidth();
        float h = bbox.getHeight();
        float d = bbox.getDepth();

        DGeom geom;
        ModelInstance instance;
        float diameter = 0;
        float radius = 0;
        float len;

        float mulheight = 1f;
        switch(shapeType) {
            case BOX:
                if(name.equals("groundbox"))
                    mulheight = 1f;
                else
                    mulheight = 2f;
                geom = OdeHelper.createBox(physicsWorld.space, w , h*mulheight, d );
                massInfo.setBox(1, w , h*mulheight , d );
                break;
            case SPHERE:
                diameter = Math.max(Math.max(w, d), h);
                radius = diameter/2f;
                geom = OdeHelper.createSphere(physicsWorld.space, radius);
                massInfo.setSphere(1, radius);
                break;
            case CAPSULE:
                diameter = Math.max(w, d);
                radius = diameter/2f; // 캡의 반지름
                len = h - 2*radius;     // 양 끝 캡 사이의 실린더 높이
                geom = OdeHelper.createCapsule(physicsWorld.space, radius, len);
                massInfo.setCapsule(1, 2, radius, len);
                break;
            case CYLINDER:
                diameter = Math.max(w, d);
                radius = diameter/2f; // 캡의 반지름
                len = h;     // 양 끝 캡 사이의 실린더 높이
                geom = OdeHelper.createCylinder(physicsWorld.space, radius, len);
                massInfo.setCylinder(1, 2, radius, len);
                break;
            case MESH:
                // 제공된 modelInstance에서 TriMesh 생성
                DTriMeshData triData = OdeHelper.createTriMeshData();
                fillTriData(triData, collisionInstance);
                geom = OdeHelper.createTriMesh(physicsWorld.space, triData, null, null, null);
                massInfo.setBox(1, w, h, d);
                break;
            case NOTHING:
                geom = OdeHelper.createBox(physicsWorld.space, w, h*mulheight, d);
                massInfo.setBox(1, w, h*mulheight, d);
                break;

            default:
                throw new RuntimeException("Unknown shape type");
        }

        if(isStatic) {
            geom.setCategoryBits(CATEGORY_STATIC);   // 이 객체의 카테고리 설정
            geom.setCollideBits(0);                  // 충돌할 카테고리 설정
            // 참고: 정적 객체의 geom에는 강체가 부착되지 않음
        } else {
            DBody rigidBody = OdeHelper.createBody(physicsWorld.world);
            rigidBody.setMass(massInfo);
            rigidBody.enable();
            rigidBody.setAutoDisableDefaults();
            rigidBody.setGravityMode(true);
            rigidBody.setDamping(0.01, 0.1);

            geom.setBody(rigidBody);
            geom.setCategoryBits(CATEGORY_DYNAMIC);
            geom.setCollideBits(CATEGORY_DYNAMIC|CATEGORY_STATIC);

            if(shapeType == CollisionShapeType.CYLINDER || shapeType == CollisionShapeType.CAPSULE) {
                DQuaternion Q = DQuaternion.fromEulerDegrees(90, 0, 0);     // X축을 기준으로 90도 회전
                geom.setOffsetQuaternion(Q);    // 강체에서 geom으로의 표준 회전 설정
            }
        }


        // 충돌 geom 형태에 맞는 디버그 모델 생성
        modelBuilder.begin();
        MeshPartBuilder meshBuilder;
        meshBuilder = modelBuilder.part("part", GL20.GL_LINES, VertexAttributes.Usage.Position , material);

        switch(shapeType) {
            case BOX:
                BoxShapeBuilder.build(meshBuilder, w, h*mulheight, d);
                break;
            case SPHERE:
                SphereShapeBuilder.build(meshBuilder, diameter, diameter, diameter , 8, 8);
                break;
            case CAPSULE:
                CapsuleShapeBuilder.build(meshBuilder, radius, h, 12);
                break;
            case CYLINDER:
                CylinderShapeBuilder.build(meshBuilder, diameter, h, diameter, 12);
                break;
            case MESH:
                buildLineMesh(meshBuilder, collisionInstance);
                break;
        }
        Model modelShape = modelBuilder.end();
        disposables.add(modelShape);
        instance = new ModelInstance(modelShape, Vector3.Zero);

        PhysicsBody body = new PhysicsBody(geom, instance, shapeType);

        // modelInstance에서 본체로 위치와 방향 복사
        collisionInstance.transform.getTranslation(position);
        collisionInstance.transform.getRotation(q);
        body.setPosition(position);
        body.setOrientation(q);
        return body;
    }

    // 충돌 모델 인스턴스의 와이어프레임 메쉬 생성
    private void buildLineMesh(MeshPartBuilder meshBuilder, ModelInstance instance) {
        Mesh mesh = instance.nodes.first().parts.first().meshPart.mesh;

        int numVertices = mesh.getNumVertices();
        int numIndices = mesh.getNumIndices();
        int stride = mesh.getVertexSize()/4;        // 메쉬의 정점당 플로트 수, 예: 위치, 노멀, 텍스처 좌표 등

        float[] origVertices = new float[numVertices*stride];
        short[] origIndices = new short[numIndices];
        // 정점당 위치 플로트의 오프셋 찾기, 반드시 처음 3 플로트가 아님
        int posOffset = mesh.getVertexAttributes().findByUsage(VertexAttributes.Usage.Position).offset / 4;

        mesh.getVertices(origVertices);
        mesh.getIndices(origIndices);

        meshBuilder.ensureVertices(numVertices);
        for(int v = 0; v < numVertices; v++) {
            float x = origVertices[stride*v+posOffset];
            float y = origVertices[stride*v+1+posOffset];
            float z = origVertices[stride*v+2+posOffset];
            meshBuilder.vertex(x, y, z);
        }
        meshBuilder.ensureTriangleIndices(numIndices/3);
        for(int i = 0; i < numIndices; i+=3) {
            meshBuilder.triangle(origIndices[i], origIndices[i+1], origIndices[i+2]);
        }
    }

    // libGDX 메쉬를 ODE TriMeshData로 변환
    private void fillTriData(DTriMeshData triData, ModelInstance instance ) {
        Mesh mesh = instance.nodes.first().parts.first().meshPart.mesh;

        int numVertices = mesh.getNumVertices();
        int numIndices = mesh.getNumIndices();
        int stride = mesh.getVertexSize()/4;        // 메쉬의 정점당 플로트 수, 예: 위치, 노멀, 텍스처 좌표 등

        float[] origVertices = new float[numVertices*stride];
        short[] origIndices = new short[numIndices];
        // 정점당 위치 플로트의 오프셋 찾기, 반드시 처음 3 플로트가 아님
        int posOffset = mesh.getVertexAttributes().findByUsage(VertexAttributes.Usage.Position).offset / 4;

        mesh.getVertices(origVertices);
        mesh.getIndices(origIndices);

        // trimesh용 데이터
        float[] vertices = new float[3*numVertices];
        int[] indices = new int[numIndices];

        for(int v = 0; v < numVertices; v++) {
            // x, y, z
            vertices[3*v] = origVertices[stride*v+posOffset];
            vertices[3*v+1] = origVertices[stride*v+1+posOffset];
            vertices[3*v+2] = origVertices[stride*v+2+posOffset];
        }
        for(int i = 0; i < numIndices; i++)         // 쇼트를 인트로 변환
            indices[i] = origIndices[i];

        triData.build(vertices, indices);
        triData.preprocess();
    }


    @Override
    public void dispose() {
        for(Disposable d : disposables)
            d.dispose();
    }
}
