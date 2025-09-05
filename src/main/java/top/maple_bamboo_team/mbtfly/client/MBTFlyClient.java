package top.maple_bamboo_team.mbtfly.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import top.maple_bamboo_team.mbtfly.client.flight.FlightControl;
import java.time.Instant;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class MBTFlyClient implements ClientModInitializer {

    public static Vec3d destination = null;
    public static Instant startTime = null;
    public static String playerName = null;
    public static Vec3d startPos = null;
    public static float detectionRange = 1.8f;
    public static boolean autoExitEnabled = false;
    public static boolean autoExitTriggered = false;
    private static final Text PREFIX = Text.literal("§b[Maple Client]§e[MBTFly] ");

    private void resetAllStates() {
        FlightControl.enabled = false;
        destination = null;
        startTime = null;
        startPos = null;
        detectionRange = 1.8f;
        autoExitEnabled = false;
    }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(literal("mbtfly")
                .executes(context -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        client.player.sendMessage(PREFIX.copy().append("欢迎使用MBTFly v1"), false);
                        client.player.sendMessage(PREFIX.copy().append("帮助菜单:"), false);
                        client.player.sendMessage(PREFIX.copy().append("- /mbtfly <x> <y> <z> [range] [quit] §7: 自动飞行到指定坐标"), false);
                        client.player.sendMessage(PREFIX.copy().append("- /mbtfly <x> ~ <z> [range] [quit] §7: 自动飞行到指定XZ坐标, 保持当前Y坐标"), false);
                        client.player.sendMessage(PREFIX.copy().append("§7[range] 为目的地检测范围, 默认为 1.8 格进入此范围后, 飞行将停止"), false);
                        client.player.sendMessage(PREFIX.copy().append("§7[quit] 为可选参数到达目的地后, 如果所有坐标均不为0, 则10秒后自动退出游戏"), false);
                        client.player.sendMessage(PREFIX.copy().append("- /mbtfly stop §7: 停止当前飞行"), false);
                        client.player.sendMessage(PREFIX.copy().append("§c本模组没有任何自动避障功能, 建议在末地或下界顶层使用"), false);
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(literal("stop")
                        .executes(context -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client.player != null) {
                                if (FlightControl.enabled) {
                                    FlightControl.enabled = false;
                                    client.player.sendMessage(PREFIX.copy().append("§6自动飞行已停止"), false);
                                } else {
                                    client.player.sendMessage(PREFIX.copy().append("§c当前没有进行中的自动飞行"), false);
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(argument("x", FloatArgumentType.floatArg())
                        .then(argument("y", FloatArgumentType.floatArg())
                                .then(argument("z", FloatArgumentType.floatArg())
                                        .executes(context -> {
                                            resetAllStates();
                                            autoExitTriggered = false;
                                            MinecraftClient client = MinecraftClient.getInstance();
                                            if (client.player != null) {
                                                float x = FloatArgumentType.getFloat(context, "x");
                                                float y = FloatArgumentType.getFloat(context, "y");
                                                float z = FloatArgumentType.getFloat(context, "z");

                                                if (y < -64 || y > 1280) {
                                                    client.player.sendMessage(PREFIX.copy().append("§c错误: Y坐标 §f" + y + " §c超出了有效范围 (-64 ~ 1279)"), false);
                                                    return Command.SINGLE_SUCCESS;
                                                }

                                                destination = new Vec3d(x, y, z);
                                                startTime = Instant.now();
                                                playerName = client.player.getName().getString();
                                                startPos = client.player.getPos();

                                                FlightControl.enabled = true;
                                                client.player.sendMessage(PREFIX.copy().append("§a开始自动飞行至 §f(" + x + ", " + y + ", " + z + ")"), false);
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .then(argument("range", FloatArgumentType.floatArg())
                                                .executes(context -> {
                                                    resetAllStates();
                                                    autoExitTriggered = false;
                                                    MinecraftClient client = MinecraftClient.getInstance();
                                                    if (client.player != null) {
                                                        float x = FloatArgumentType.getFloat(context, "x");
                                                        float y = FloatArgumentType.getFloat(context, "y");
                                                        float z = FloatArgumentType.getFloat(context, "z");
                                                        float range = FloatArgumentType.getFloat(context, "range");

                                                        if (y < -64 || y > 1280) {
                                                            client.player.sendMessage(PREFIX.copy().append("§c错误: Y坐标 §f" + y + " §c超出了有效范围 (-64 ~ 1279)"), false);
                                                            return Command.SINGLE_SUCCESS;
                                                        }

                                                        if (range < 1) {
                                                            client.player.sendMessage(PREFIX.copy().append("§crange不可以小于1"), false);
                                                            return Command.SINGLE_SUCCESS;
                                                        }

                                                        destination = new Vec3d(x, y, z);
                                                        startTime = Instant.now();
                                                        playerName = client.player.getName().getString();
                                                        startPos = client.player.getPos();
                                                        detectionRange = range;

                                                        FlightControl.enabled = true;
                                                        client.player.sendMessage(PREFIX.copy().append("§a开始自动飞行至 §f(" + x + ", " + y + ", " + z + ") §a, 检测范围为 §f" + range), false);
                                                    }
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                                .then(literal("quit")
                                                        .executes(context -> {
                                                            resetAllStates();
                                                            autoExitTriggered = false;
                                                            MinecraftClient client = MinecraftClient.getInstance();
                                                            if (client.player != null) {
                                                                float x = FloatArgumentType.getFloat(context, "x");
                                                                float y = FloatArgumentType.getFloat(context, "y");
                                                                float z = FloatArgumentType.getFloat(context, "z");
                                                                float range = FloatArgumentType.getFloat(context, "range");

                                                                if (y < -64 || y > 1280) {
                                                                    client.player.sendMessage(PREFIX.copy().append("§c错误: Y坐标 §f" + y + " §c超出了有效范围 (-64 ~ 1279)"), false);
                                                                    return Command.SINGLE_SUCCESS;
                                                                }

                                                                if (range < 1) {
                                                                    client.player.sendMessage(PREFIX.copy().append("§crange不可以小于1"), false);
                                                                    return Command.SINGLE_SUCCESS;
                                                                }

                                                                destination = new Vec3d(x, y, z);
                                                                startTime = Instant.now();
                                                                playerName = client.player.getName().getString();
                                                                startPos = client.player.getPos();
                                                                detectionRange = range;
                                                                autoExitEnabled = true;

                                                                FlightControl.enabled = true;
                                                                client.player.sendMessage(PREFIX.copy().append("§a开始自动飞行至 §f(" + x + ", " + y + ", " + z + ") §a, 检测范围为 §f" + range + "§a, 本次飞行已启用目的地自动退出"), false);
                                                            }
                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                        )
                                        .then(literal("quit")
                                                .executes(context -> {
                                                    resetAllStates();
                                                    autoExitTriggered = false;
                                                    MinecraftClient client = MinecraftClient.getInstance();
                                                    if (client.player != null) {
                                                        float x = FloatArgumentType.getFloat(context, "x");
                                                        float y = FloatArgumentType.getFloat(context, "y");
                                                        float z = FloatArgumentType.getFloat(context, "z");

                                                        destination = new Vec3d(x, y, z);
                                                        startTime = Instant.now();
                                                        playerName = client.player.getName().getString();
                                                        startPos = client.player.getPos();
                                                        autoExitEnabled = true;

                                                        FlightControl.enabled = true;
                                                        client.player.sendMessage(PREFIX.copy().append("§a开始自动飞行至 §f(" + x + ", " + y + ", " + z + ") §a, 本次飞行已启用目的地自动退出"), false);
                                                    }
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
                        .then(literal("~")
                                .then(argument("z", FloatArgumentType.floatArg())
                                        .executes(context -> {
                                            resetAllStates();
                                            autoExitTriggered = false;
                                            MinecraftClient client = MinecraftClient.getInstance();
                                            if (client.player != null) {
                                                float x = FloatArgumentType.getFloat(context, "x");
                                                float y = (float) Math.round(client.player.getY());
                                                float z = FloatArgumentType.getFloat(context, "z");

                                                destination = new Vec3d(x, y, z);
                                                startTime = Instant.now();
                                                playerName = client.player.getName().getString();
                                                startPos = client.player.getPos();

                                                FlightControl.enabled = true;
                                                client.player.sendMessage(PREFIX.copy().append("§a开始自动飞行至 §f(" + x + ", " + y + ", " + z + ")"), false);
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .then(argument("range", FloatArgumentType.floatArg())
                                                .executes(context -> {
                                                    resetAllStates();
                                                    autoExitTriggered = false;
                                                    MinecraftClient client = MinecraftClient.getInstance();
                                                    if (client.player != null) {
                                                        float x = FloatArgumentType.getFloat(context, "x");
                                                        float y = (float) Math.round(client.player.getY());
                                                        float z = FloatArgumentType.getFloat(context, "z");
                                                        float range = FloatArgumentType.getFloat(context, "range");

                                                        if (range < 1) {
                                                            client.player.sendMessage(PREFIX.copy().append("§crange不可以小于1"), false);
                                                            return Command.SINGLE_SUCCESS;
                                                        }

                                                        destination = new Vec3d(x, y, z);
                                                        startTime = Instant.now();
                                                        playerName = client.player.getName().getString();
                                                        startPos = client.player.getPos();
                                                        detectionRange = range;

                                                        FlightControl.enabled = true;
                                                        client.player.sendMessage(PREFIX.copy().append("§a开始自动飞行至 §f(" + x + ", " + y + ", " + z + ") §a, 检测范围为 §f" + range), false);
                                                    }
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                                .then(literal("quit")
                                                        .executes(context -> {
                                                            resetAllStates();
                                                            autoExitTriggered = false;
                                                            MinecraftClient client = MinecraftClient.getInstance();
                                                            if (client.player != null) {
                                                                float x = FloatArgumentType.getFloat(context, "x");
                                                                float y = (float) Math.round(client.player.getY());
                                                                float z = FloatArgumentType.getFloat(context, "z");
                                                                float range = FloatArgumentType.getFloat(context, "range");

                                                                if (range < 1) {
                                                                    client.player.sendMessage(PREFIX.copy().append("§crange不可以小于1"), false);
                                                                    return Command.SINGLE_SUCCESS;
                                                                }

                                                                destination = new Vec3d(x, y, z);
                                                                startTime = Instant.now();
                                                                playerName = client.player.getName().getString();
                                                                startPos = client.player.getPos();
                                                                detectionRange = range;
                                                                autoExitEnabled = true;

                                                                FlightControl.enabled = true;
                                                                client.player.sendMessage(PREFIX.copy().append("§a开始自动飞行至 §f(" + x + ", " + y + ", " + z + ") §a, 检测范围为 §f" + range + "§a, 本次飞行已启用目的地自动退出"), false);
                                                            }
                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                        )
                                        .then(literal("quit")
                                                .executes(context -> {
                                                    resetAllStates();
                                                    autoExitTriggered = false;
                                                    MinecraftClient client = MinecraftClient.getInstance();
                                                    if (client.player != null) {
                                                        float x = FloatArgumentType.getFloat(context, "x");
                                                        float y = (float) Math.round(client.player.getY());
                                                        float z = FloatArgumentType.getFloat(context, "z");

                                                        destination = new Vec3d(x, y, z);
                                                        startTime = Instant.now();
                                                        playerName = client.player.getName().getString();
                                                        startPos = client.player.getPos();
                                                        autoExitEnabled = true;

                                                        FlightControl.enabled = true;
                                                        client.player.sendMessage(PREFIX.copy().append("§a开始自动飞行至 §f(" + x + ", " + y + ", " + z + ") §a, 本次飞行已启用目的地自动退出"), false);
                                                    }
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
                )
        ));
    }
}