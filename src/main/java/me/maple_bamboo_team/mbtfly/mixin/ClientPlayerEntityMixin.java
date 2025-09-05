package me.maple_bamboo_team.mbtfly.mixin;

import me.maple_bamboo_team.mbtfly.util.AimingUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.maple_bamboo_team.mbtfly.client.MBTFlyClient;
import me.maple_bamboo_team.mbtfly.client.flight.FlightControl;
import java.time.Instant;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.client.gui.screen.TitleScreen;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Unique
    private static final double Y_TOLERANCE = 0.5;
    @Unique
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.SIMPLIFIED_CHINESE).withZone(ZoneId.systemDefault());
    @Unique
    private static final Text PREFIX = Text.literal("§b[Maple Client] §e[MBTFly] ");
    @Unique
    private int autoExitCountdown = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        if (client == null || client.world == null || player == null) {
            return;
        }

        if (FlightControl.enabled) {
            // 在自动飞行期间, 禁用 WASD, 左 Shift 和空格键的输入
            client.options.forwardKey.setPressed(false);
            client.options.leftKey.setPressed(false);
            client.options.rightKey.setPressed(false);
            client.options.backKey.setPressed(false);
            client.options.jumpKey.setPressed(false);
            client.options.sneakKey.setPressed(false);
        }

        if (autoExitCountdown > 0) {
            autoExitCountdown--;
            if (autoExitCountdown % 20 == 0) {
                player.sendMessage(PREFIX.copy().append("§6在 §f" + (autoExitCountdown / 20) + " §6秒后自动退出..."), false);
            }
            if (autoExitCountdown <= 0) {
                player.sendMessage(PREFIX.copy().append("§6正在退出"), false);
                // 延迟1秒后退出游戏
                new Thread(() -> {
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // 在主线程执行退出操作
                    client.execute(() -> {
                        if (client.world != null) {
                            client.world.disconnect();
                            client.disconnect();
                            client.setScreen(new TitleScreen());

                        }
                    });
                }).start();
                // 确保只执行一次退出逻辑
                autoExitCountdown = -1;
            }
            return;
        }

        if (FlightControl.enabled && MBTFlyClient.destination != null) {
            Vec3d playerPos = player.getPos();

            // 计算平坦距离（忽略Y轴）
            Vec3d flatPlayerPos = new Vec3d(playerPos.x, MBTFlyClient.destination.y, playerPos.z);
            double flatDistance = flatPlayerPos.distanceTo(MBTFlyClient.destination);

            double totalDistance = playerPos.distanceTo(MBTFlyClient.destination);

            boolean ascentIssue = flatDistance <= MBTFlyClient.detectionRange && playerPos.y < MBTFlyClient.destination.y - Y_TOLERANCE;
            boolean reachedDestination = totalDistance <= MBTFlyClient.detectionRange;

            if (ascentIssue || reachedDestination) {
                // 立即停止所有操作
                FlightControl.enabled = false;

                client.options.forwardKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
                client.options.sneakKey.setPressed(false);

                Instant endTime = Instant.now();
                Duration flightDuration = Duration.between(MBTFlyClient.startTime, endTime);
                double originalTotalDistance = MBTFlyClient.startPos.distanceTo(MBTFlyClient.destination);

                long totalSeconds = flightDuration.getSeconds();
                long days = totalSeconds / (24 * 3600);
                long hours = (totalSeconds % (24 * 3600)) / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;

                StringBuilder timeString = new StringBuilder();
                if (days > 0) timeString.append(days).append("天");
                if (hours > 0) timeString.append(hours).append("时");
                if (minutes > 0) timeString.append(minutes).append("分");
                timeString.append(seconds).append("秒");

                player.sendMessage(PREFIX.copy().append("§a============================================="), false);
                player.sendMessage(PREFIX.copy().append("§a            MBTFly 自动飞行数据统计             "), false);
                player.sendMessage(PREFIX.copy().append("§a============================================="), false);
                player.sendMessage(PREFIX.copy().append("§b玩家名: §f" + MBTFlyClient.playerName), false);
                player.sendMessage(PREFIX.copy().append("§b开始时间: §f" + formatter.format(MBTFlyClient.startTime)), false);
                player.sendMessage(PREFIX.copy().append("§b结束时间: §f" + formatter.format(endTime)), false);
                player.sendMessage(PREFIX.copy().append("§b总耗时: §f" + timeString.toString()), false);
                player.sendMessage(PREFIX.copy().append(String.format("§b总路程: §f%.2f 格", originalTotalDistance)), false);
                player.sendMessage(PREFIX.copy().append("§a============================================="), false);

                if (ascentIssue) {
                    player.sendMessage(PREFIX.copy().append("\n§c没有足够的距离拉升到目标高度,请指定足够远的距离以拉升高度"), false);
                }

                if (MBTFlyClient.autoExitEnabled && !MBTFlyClient.autoExitTriggered) {
                    MBTFlyClient.autoExitTriggered = true;
                    autoExitCountdown = 200;
                    player.sendMessage(PREFIX.copy().append("§6已到达目的地, 10秒后自动退出游戏"), false);
                }
            } else if (flatDistance <= MBTFlyClient.detectionRange) {
                // 如果只在水平范围内，停止水平移动和转头，只执行垂直移动
                client.options.forwardKey.setPressed(false);
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);
                client.options.backKey.setPressed(false);

                // 垂直移动逻辑
                if (playerPos.y < MBTFlyClient.destination.y - Y_TOLERANCE) {
                    client.options.jumpKey.setPressed(true);
                    client.options.sneakKey.setPressed(false);
                } else if (playerPos.y > MBTFlyClient.destination.y + Y_TOLERANCE) {
                    client.options.jumpKey.setPressed(false);
                    client.options.sneakKey.setPressed(true);
                } else {
                    client.options.jumpKey.setPressed(false);
                    client.options.sneakKey.setPressed(false);
                }
            } else {
                // 正常飞行模式
                // 平滑转头逻辑
                double yaw = AimingUtils.getYaw(playerPos, MBTFlyClient.destination);
                double pitch = AimingUtils.getPitch(playerPos, MBTFlyClient.destination);
                double yawDiff = yaw - player.getYaw();
                double pitchDiff = pitch - player.getPitch();

                if (yawDiff > 180) {
                    yawDiff -= 360;
                } else if (yawDiff < -180) {
                    yawDiff += 360;
                }

                float newYaw = (float) (player.getYaw() + yawDiff);
                float newPitch = (float) (player.getPitch() + pitchDiff);

                player.setYaw(newYaw);
                player.setPitch(newPitch);

                // 全速前进, 直到进入停止范围
                client.options.forwardKey.setPressed(true);

                // 垂直移动逻辑
                if (playerPos.y < MBTFlyClient.destination.y - Y_TOLERANCE) {
                    client.options.jumpKey.setPressed(true);
                    client.options.sneakKey.setPressed(false);
                } else if (playerPos.y > MBTFlyClient.destination.y + Y_TOLERANCE) {
                    client.options.jumpKey.setPressed(false);
                    client.options.sneakKey.setPressed(true);
                } else {
                    client.options.jumpKey.setPressed(false);
                    client.options.sneakKey.setPressed(false);
                }
            }
        }
    }
}