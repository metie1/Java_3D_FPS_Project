package com.monstrous.tut3d.nav;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.monstrous.tut3d.Settings;

public class NavMesh {
    public Array<NavNode> navNodes;         // node in nav mesh (triangles)
    private Vector3 goal;                   // target used for calculating distances


    // create a navigation mesh from the mesh of a model instance
    //
    public NavMesh( ModelInstance instance ) {
        Mesh mesh = instance.nodes.first().parts.first().meshPart.mesh;
        int primitiveType = instance.nodes.first().parts.first().meshPart.primitiveType;
        if(primitiveType != GL20.GL_TRIANGLES)
            throw new RuntimeException("Nav mesh must be GL_TRIANGLES");

        int numVertices = mesh.getNumVertices();
        int numIndices = mesh.getNumIndices();
        int stride = mesh.getVertexSize()/4;        // floats per vertex in mesh, e.g. for position, normal, textureCoordinate, etc.

        float[] vertices = new float[numVertices*stride];
        short[] indices = new short[numIndices];
        // find offset of position floats per vertex, they are not necessarily the first 3 floats
        int posOffset = mesh.getVertexAttributes().findByUsage(VertexAttributes.Usage.Position).offset / 4;

        mesh.getVertices(vertices);
        mesh.getIndices(indices);

        navNodes = new Array<>();
        Vector3 a = new Vector3();
        Vector3 b = new Vector3();
        Vector3 c = new Vector3();
        int numTriangles = numIndices/3;

        for(int i = 0; i < numIndices; i+=3) {

            int index = indices[i];
            float x = vertices[stride*index+posOffset];
            float y = vertices[stride*index+1+posOffset];
            float z = vertices[stride*index+2+posOffset];
            a.set(x, y, z);

            index = indices[i+1];
            x = vertices[stride*index+posOffset];
            y = vertices[stride*index+1+posOffset];
            z = vertices[stride*index+2+posOffset];
            b.set(x, y, z);

            index = indices[i+2];
            x = vertices[stride*index+posOffset];
            y = vertices[stride*index+1+posOffset];
            z = vertices[stride*index+2+posOffset];
            c.set(x, y, z);

            NavNode node = new NavNode(i/3, a, b, c);
            navNodes.add(node);
        }
        Gdx.app.log("Nav Nodes:", ""+navNodes.size);

        // now determine connectivity between triangles, i.e. which triangles share two vertices?
        int links = 0;
        for(int i = 0; i < numTriangles; i++) {
            // note: multiple vertices can be equivalent, i.e. have same position, so check on position equivalence
            // not just if triangles use the same indices.
            // (Alternative would be to deduplicate such vertices before to share the same index)
            Vector3 p0 = navNodes.get(i).p0;
            Vector3 p1 = navNodes.get(i).p1;
            Vector3 p2 = navNodes.get(i).p2;
            for(int j = 0; j < i; j++) {
                Vector3 q0 = navNodes.get(j).p0;
                Vector3 q1 = navNodes.get(j).p1;
                Vector3 q2 = navNodes.get(j).p2;
                int matches = 0;
                if(p0.epsilonEquals(q0) || p0.epsilonEquals(q1) || p0.epsilonEquals(q2))
                    matches++;
                if(p1.epsilonEquals(q0) || p1.epsilonEquals(q1) || p1.epsilonEquals(q2))
                    matches++;
                if(p2.epsilonEquals(q0) || p2.epsilonEquals(q1) || p2.epsilonEquals(q2))
                    matches++;
                if(matches == 3)
                    throw new RuntimeException("Duplicate triangles");
                if(matches == 2){
                    navNodes.get(i).addNeighbour(navNodes.get(j));
                    navNodes.get(j).addNeighbour(navNodes.get(i));
                    Gdx.app.log("connection", ""+i+" & "+j);
                    links++;
                }
            }
        }
        goal = new Vector3();

        StringBuilder sb = new StringBuilder();
        Gdx.app.log("Nav Connections:", ""+links);
        for(int i = 0; i < numTriangles; i++) {
            NavNode node = navNodes.get(i);
            sb.setLength(0);
            for(int j = 0; j < node.neighbours.size; j++) {
                sb.append(" ");
                sb.append(node.neighbours.get(j).id);
            }
            sb.append("\t[");
            sb.append(node.p0.toString());
            sb.append(node.p1.toString());
            sb.append(node.p2.toString());
            sb.append("]");
            Gdx.app.log("triangle", ""+i+" nbors: "+node.neighbours.size+" = "+sb.toString());

        }
        sb.setLength(0);
    }

    public NavNode findNode( Vector3 point, float maxDist ){
        for(NavNode node : navNodes ) {
            if(node.isPointInTriangle(point, maxDist)) {
                return node;
            }
        }
        return null;
    }

    // find closest node - in case point is not in a node (off-piste)
    public NavNode findClosestNode( Vector3 point ){
        float minDist = Float.MAX_VALUE;
        NavNode closest = null;
        for(NavNode node : navNodes ) {
            float len2 = point.dst2(node.centre);
            if(len2 < minDist) {
                minDist = len2;
                closest = node;
            }
        }
        return closest;
    }

    // todo: use size of node for cost because a large node should be more costly to cross

    // update distance for each node to the target point
    public void updateDistances( Vector3 targetPoint ) {
        goal.set(targetPoint);

        NavNode target = findNode( targetPoint, Settings.navHeight );
        if(target == null) {
              // Gdx.app.error("warning: updateDistances: start not in node", "");
               target = findClosestNode(targetPoint);      // navigate towards centre of closest node to get back in the game
        }

        // Dijkstra's algorithm
        //   here we are not looking for the shortest path, but to update each node with distance value to the target
        Array<NavNode> Q = new Array<>();

        for(int i = 0; i < navNodes.size; i++){
            NavNode node = navNodes.get(i);
            node.steps = Integer.MAX_VALUE;
            node.prev = null;
            if(!node.degenerate)
                Q.add(node);
        }
        target.steps = 0;

        while(Q.size > 0) {
            int minSteps = Integer.MAX_VALUE;
            NavNode u = null;
            for(NavNode n : Q){
                if(n.steps < minSteps){
                    minSteps = n.steps;
                    u = n;
                }
            }
            Q.removeValue(u, true);
            for(int i = 0; i < u.neighbours.size; i++){
                NavNode nbor = u.neighbours.get(i);
                if(Q.contains(nbor, true)){
                    int alt = u.steps + 1;
                    if(alt < nbor.steps){
                        nbor.steps = alt;
                        nbor.prev = u;
                    }
                }
            }
        } // while
    }

    private boolean  findPath( Vector3 startPoint, Array<NavNode> path ) {

        NavNode node = findNode(startPoint, Settings.navHeight);
        if (node == null) {
            node = findClosestNode(startPoint);      // navigate towards centre of closest node to get back in the game
            //Gdx.app.error("warning: findPath: start not in node", "going to node:"+node.id);
         }
        path.clear();
        path.add(node);
        while( node.steps != 0) {

            // find neighbour with the closest distance to target
            int shortest = Integer.MAX_VALUE;
            NavNode choice = null;
            for(NavNode nbor : node.neighbours) {
                if(nbor.steps < shortest) {
                    shortest = nbor.steps;
                    choice = nbor;
                }
            }
            path.add(choice);
            node = choice;
        }

        StringBuilder sb = new StringBuilder();
        for(NavNode n : path ) {
            sb.append(" ");
            sb.append(n.id);
        }
       // Gdx.app.log("path", " id:" +sb.toString());
        return true;
    }


    // very simple: make a path using node centres, looks rubbish
    public void makePathViaNodeCentres( Vector3 startPoint, Vector3 targetPoint, Array<NavNode> nodePath, Array<Vector3> pointPath ) {

        pointPath.clear();
        pointPath.add(new Vector3(startPoint));
        for(NavNode node : nodePath ) {
            Vector3 centre = new Vector3(node.p0);
            centre.add(node.p1);
            centre.add(node.p2);
            centre.scl(1/3f);
            pointPath.add(  centre );
        }
        pointPath.add(  targetPoint );
    }

    // make a path by going through the mid-point of connecting edges
    // looks marginally better
    public void makePathMidPoints( Vector3 startPoint, Vector3 targetPoint, Array<NavNode> nodePath, Array<Vector3> pointPath ) {

        Vector3 edgeStart = new Vector3();
        Vector3 edgeEnd = new Vector3();
        pointPath.clear();
        pointPath.add(new Vector3(startPoint));
        for(int i = 0; i < nodePath.size-1; i++ ) {
            NavNode node = nodePath.get(i);
            NavNode nextNode = nodePath.get(i+1);
            getEdge(node, nextNode, edgeStart, edgeEnd);

            Vector3 centre = new Vector3(edgeStart);
            centre.add(edgeEnd);
            centre.scl(1/2f);
            pointPath.add(  centre );
        }
        pointPath.add(  targetPoint );
    }

    // get the edge between two triangles that we know are connected
    private void getEdge(NavNode a, NavNode b, Vector3 start, Vector3 end ){

        boolean p0matches = false;
        boolean p1matches = false;
        boolean p2matches = false;

        if(a.p0.epsilonEquals(b.p0) || a.p0.epsilonEquals(b.p1)|| a.p0.epsilonEquals(b.p2) )
            p0matches = true;
        if(a.p1.epsilonEquals(b.p0) || a.p1.epsilonEquals(b.p1)|| a.p1.epsilonEquals(b.p2) )
            p1matches = true;
        if(a.p2.epsilonEquals(b.p0) || a.p2.epsilonEquals(b.p1)|| a.p2.epsilonEquals(b.p2) )
            p2matches = true;

        if(p0matches && p1matches){
            start.set(a.p0);
            end.set(a.p1);
        }
        else if(p1matches && p2matches){
            start.set(a.p1);
            end.set(a.p2);
        }
        else if(p2matches && p0matches){
            start.set(a.p2);
            end.set(a.p0);
        }
        else
            throw new RuntimeException("Cannot match edges");
    }


    // returns true of path was rebuilt
    public boolean  makePath( Vector3 startPoint, Vector3 targetPoint, Array<Vector3> pointPath ) {

        if(pointPath.size > 0) {
            Vector3 endPoint = new Vector3(targetPoint);
            NavNode endNode = findNode(endPoint, Settings.navHeight);
            if(endNode == null) {
                endNode = findClosestNode(endPoint);        // we cannot get to the target, go to the centre of the closest node
                endPoint.set(endNode.centre);
            }
            endPoint.y = endNode.p0.y;
            Vector3 end = pointPath.get(pointPath.size - 1);
            if (end.dst(endPoint) < 1f)      // if existing path leads (close to) to target point, we don't need to recalculate
                return false;
        }

        Array<NavNode> navNodePath = new Array<>();
        Gdx.app.log("find path", "recalculating path");
        findPath(startPoint, navNodePath);
        makePath(startPoint, targetPoint, navNodePath, pointPath);
        return true;
    }


    // string pulling algo:
    // "simple stupid funnel algorithm" by Mikko Mononen
    //

    // a portal is an edge between two adjacent nodes (i.e. triangles) on the path
    public static class Portal {
        public Vector3 left;
        public Vector3 right;
        public boolean slopeChange;     // indicator if portal connects nodes with different slope

        public Portal(Vector3 left, Vector3 right, boolean slopeChange ) {
            this.left = new Vector3(left);
            this.right = new Vector3(right);
            this.slopeChange = slopeChange;
        }
    }

    private void makePath( Vector3 startPoint, Vector3 targetPoint, Array<NavNode> nodePath, Array<Vector3> pointPath ) {

        Array<Portal> portals = new Array<>();
        Vector3 edgeStart = new Vector3();
        Vector3 edgeEnd = new Vector3();

        // build a list of portals, i.e. edges between triangles on the node path to the goal
        portals.clear();
        NavNode startNode = findNode(startPoint, Settings.navHeight);
        if(startNode != null)
            startPoint.y = startNode.p0.y;                                          // make sure to use node (floor) height, not character height
        portals.add(new Portal(startPoint, startPoint, false));
        for (int i = 0; i < nodePath.size - 1; i++) {
            NavNode node = nodePath.get(i);
            NavNode nextNode = nodePath.get(i + 1);
            getEdge(node, nextNode, edgeStart, edgeEnd);
            boolean slopeChange = ( node.normal.dot(nextNode.normal) < 0.99f ); // use dot product of normals to detect slope change
            portals.add(new Portal(edgeEnd, edgeStart, slopeChange));
        }
        Vector3 endPoint = new Vector3(targetPoint);
        NavNode endNode = findNode(endPoint, Settings.navHeight);
        if(endNode == null) {
            endNode = findClosestNode(endPoint);        // we cannot get to the target, go to the centre of the closest node
            endPoint.set(endNode.centre);
        }
        endPoint.y = endNode.p0.y;
        portals.add(new Portal(endPoint, endPoint, false));

        // use the portals to create a list of way points
        pointPath.clear();
        pointPath.add(new Vector3(startPoint));

        // define a funnel with an apex, a left foot and a right foot
        Vector3 apex = startPoint;
        Vector3 leftFoot = startPoint;
        Vector3 rightFoot = startPoint;
        int apexIndex = 0, leftIndex = 0, rightIndex = 0;

        for (int i = 1; i < portals.size; i++) {
            Portal portal = portals.get(i);

            // update right leg
            if ( area(apex, rightFoot, portal.right) <= 0) {
                if (apex.epsilonEquals(rightFoot) || area(apex, leftFoot, portal.right) > 0f) {
                    // tighten the funnel
                    rightFoot = portal.right;
                    rightIndex = i;
                } else {
                    // right over left,insert left into path and restart scan from left foot
                    pointPath.add(new Vector3(leftFoot));
                    apex = leftFoot;
                    apexIndex = leftIndex;
                    // reset portal
                    leftFoot = apex;
                    rightFoot = apex;
                    leftIndex = apexIndex;
                    rightIndex = apexIndex;
                    i = apexIndex;
                    continue;
                }
            }
            // update left leg
            if (area(apex, leftFoot, portal.left) >= 0) {
                if (apex.epsilonEquals(leftFoot) || area(apex, rightFoot, portal.left) < 0f) {
                    // tighten the funnel
                    leftFoot = portal.left;
                    leftIndex = i;
                } else {
                    // left over right, insert right into path and restart scan from right foot
                    pointPath.add(new Vector3(rightFoot));
                    apex = rightFoot;
                    apexIndex = rightIndex;
                    // reset portal
                    leftFoot = apex;
                    rightFoot = apex;
                    leftIndex = apexIndex;
                    rightIndex = apexIndex;
                    i = apexIndex;
                    continue;
                }
            }

            // force a way point on a slope change so that the path follows the slopes (e.g. over a bridge)
            // this is an addition to SSFA
            if(portal.slopeChange){
                Vector3 wayPoint = new Vector3(portal.left).add(portal.right).scl(0.5f);    // mid point of portal
                pointPath.add( wayPoint );
                apex = wayPoint;
                apexIndex = i;
                // reset portal
                leftFoot = apex;
                rightFoot = apex;
                leftIndex = apexIndex;
                rightIndex = apexIndex;
                continue;
            }

        }
        pointPath.add(endPoint);
    }


    // 2d function to test if triangle a,b,c has positive or negative area, negative means the funnel legs are crossed
    private float area(Vector3 a, Vector3 b, Vector3 c) {
        float ax = b.x - a.x;
        float az = b.z - a.z;
        float bx = c.x - a.x;
        float bz = c.z - a.z;
        return - (bx*az - ax*bz);
    }
}
