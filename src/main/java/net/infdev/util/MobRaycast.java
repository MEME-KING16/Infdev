package net.infdev.util;

import net.infdev.api.world.entity.Mob;
import org.joml.Vector3f;

import java.util.List;

public class MobRaycast {

    public static MobHitResult raycast(Vector3f origin, Vector3f direction, List<Mob> mobs, float maxDistance) {
        Mob closestMob = null;
        float closestDistance = maxDistance;

        for (Mob mob : mobs) {
            if (mob.isDead()) continue;

            Vector3f mobPos = mob.getPosition();

            // Create a bounding box for the mob using its real dimensions
            float mobWidth = mob.getHalfWidth() * 2.0f;
            float mobHeight = mob.getHeight();

            // Calculate intersection with mob's bounding box
            Float hitDistance = rayBoxIntersection(origin, direction, mobPos, mobWidth, mobHeight, mobWidth);

            if (hitDistance != null && hitDistance < closestDistance) {
                closestDistance = hitDistance;
                closestMob = mob;
            }
        }

        if (closestMob != null) {
            Vector3f hitPoint = new Vector3f(origin).add(new Vector3f(direction).mul(closestDistance));
            return new MobHitResult(closestMob, hitPoint, closestDistance, true);
        }

        return new MobHitResult(null, null, 0, false);
    }

    private static Float rayBoxIntersection(Vector3f origin, Vector3f direction, Vector3f boxCenter,
                                           float boxWidth, float boxHeight, float boxDepth) {
        // Calculate box bounds
        float minX = boxCenter.x - boxWidth / 2;
        float maxX = boxCenter.x + boxWidth / 2;
        float minY = boxCenter.y;
        float maxY = boxCenter.y + boxHeight;
        float minZ = boxCenter.z - boxDepth / 2;
        float maxZ = boxCenter.z + boxDepth / 2;

        // Calculate intersection distances for each axis
        float tMinX = (minX - origin.x) / direction.x;
        float tMaxX = (maxX - origin.x) / direction.x;
        if (tMinX > tMaxX) {
            float temp = tMinX;
            tMinX = tMaxX;
            tMaxX = temp;
        }

        float tMinY = (minY - origin.y) / direction.y;
        float tMaxY = (maxY - origin.y) / direction.y;
        if (tMinY > tMaxY) {
            float temp = tMinY;
            tMinY = tMaxY;
            tMaxY = temp;
        }

        if ((tMinX > tMaxY) || (tMinY > tMaxX)) {
            return null;
        }

        float tMin = Math.max(tMinX, tMinY);
        float tMax = Math.min(tMaxX, tMaxY);

        float tMinZ = (minZ - origin.z) / direction.z;
        float tMaxZ = (maxZ - origin.z) / direction.z;
        if (tMinZ > tMaxZ) {
            float temp = tMinZ;
            tMinZ = tMaxZ;
            tMaxZ = temp;
        }

        if ((tMin > tMaxZ) || (tMinZ > tMax)) {
            return null;
        }

        tMin = Math.max(tMin, tMinZ);
        tMax = Math.min(tMax, tMaxZ);

        if (tMin < 0) {
            return null;
        }

        return tMin;
    }

    public static class MobHitResult {
        public final Mob mob;
        public final Vector3f hitPoint;
        public final float distance;
        public final boolean hit;

        public MobHitResult(Mob mob, Vector3f hitPoint, float distance, boolean hit) {
            this.mob = mob;
            this.hitPoint = hitPoint;
            this.distance = distance;
            this.hit = hit;
        }
    }
}
