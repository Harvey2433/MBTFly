package me.maple_bamboo_team.mbtfly.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class AimingUtils {

    /**
     * 计算玩家到目标点的偏航角 (yaw)。
     * @param playerPos 玩家当前位置
     * @param targetPos 目标位置
     * @return 偏航角
     */
    public static double getYaw(Vec3d playerPos, Vec3d targetPos) {
        Vec3d direction = targetPos.subtract(playerPos);
        return Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0;
    }

    /**
     * 计算玩家到目标点的俯仰角 (pitch)。
     * @param playerPos 玩家当前位置
     * @param targetPos 目标位置
     * @return 俯仰角
     */
    public static double getPitch(Vec3d playerPos, Vec3d targetPos) {
        Vec3d direction = targetPos.subtract(playerPos);
        return -Math.toDegrees(Math.asin(direction.y / direction.length()));
    }

    /**
     * 直接将玩家的视角对准目标。
     * @param player 玩家实例
     * @param target 目标位置
     */
    public static void aimAt(ClientPlayerEntity player, Vec3d target) {
        Vec3d eyePos = player.getEyePos();
        Vec3d direction = target.subtract(eyePos).normalize();
        double yaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0;
        double pitch = -Math.toDegrees(Math.asin(direction.y));
        player.setYaw((float) yaw);
        player.setPitch((float) pitch);
    }
}