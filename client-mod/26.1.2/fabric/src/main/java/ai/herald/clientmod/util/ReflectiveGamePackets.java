package ai.herald.clientmod.util;

import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import io.netty.buffer.Unpooled;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * Sends version-sensitive packets via reflection so common compiles against both
 * 1.20.1 ({@code protocol.game.*}) and 1.20.4+ ({@code protocol.common.*}).
 */
public final class ReflectiveGamePackets {

    private static final String NOT_AVAILABLE = "Packet not available in this MC version";

    private ReflectiveGamePackets() {}

    public static ActionResult sendClientInformation(
            ClientPacketListener conn,
            String locale,
            int viewDistance,
            ChatVisiblity vis,
            boolean chatColors,
            int skinParts,
            HumanoidArm arm,
            boolean textFiltering,
            boolean allowServerListings) {
        try {
            Object packet = newClientInformationPacket(
                    locale, viewDistance, vis, chatColors, skinParts, arm, textFiltering, allowServerListings);
            if (packet == null) {
                return ActionResult.error(ErrorCode.NOT_IMPLEMENTED, NOT_AVAILABLE);
            }
            send(conn, packet);
            return ActionResult.ok();
        } catch (ClassNotFoundException e) {
            return ActionResult.error(ErrorCode.NOT_IMPLEMENTED, NOT_AVAILABLE);
        } catch (ReflectiveOperationException e) {
            return ActionResult.error(ErrorCode.MAINTHREAD_FAILURE, "Failed to send client information: " + e.getMessage());
        }
    }

    public static ActionResult sendResourcePackResponse(ClientPacketListener conn, String resultEnumName, UUID packId) {
        try {
            Object packet = newResourcePackPacket(resultEnumName, packId);
            if (packet == null) {
                return ActionResult.error(ErrorCode.NOT_IMPLEMENTED, NOT_AVAILABLE);
            }
            send(conn, packet);
            return ActionResult.ok();
        } catch (ClassNotFoundException e) {
            return ActionResult.error(ErrorCode.NOT_IMPLEMENTED, NOT_AVAILABLE);
        } catch (ReflectiveOperationException e) {
            return ActionResult.error(ErrorCode.MAINTHREAD_FAILURE, "Failed to send resource pack response: " + e.getMessage());
        }
    }

    public static ActionResult sendKeepAlive(ClientPacketListener conn, long id) {
        try {
            Object packet = newPacket(
                    "ServerboundKeepAlivePacket",
                    new Class<?>[]{long.class},
                    id);
            if (packet == null) {
                return ActionResult.error(ErrorCode.NOT_IMPLEMENTED, NOT_AVAILABLE);
            }
            send(conn, packet);
            return ActionResult.ok();
        } catch (ClassNotFoundException e) {
            return ActionResult.error(ErrorCode.NOT_IMPLEMENTED, NOT_AVAILABLE);
        } catch (ReflectiveOperationException e) {
            return ActionResult.error(ErrorCode.MAINTHREAD_FAILURE, "Failed to send keep-alive: " + e.getMessage());
        }
    }

    public static ActionResult sendPong(ClientPacketListener conn, int parameter) {
        try {
            Object packet = newPacket(
                    "ServerboundPongPacket",
                    new Class<?>[]{int.class},
                    parameter);
            if (packet == null) {
                return ActionResult.error(ErrorCode.NOT_IMPLEMENTED, NOT_AVAILABLE);
            }
            send(conn, packet);
            return ActionResult.ok();
        } catch (ClassNotFoundException e) {
            return ActionResult.error(ErrorCode.NOT_IMPLEMENTED, NOT_AVAILABLE);
        } catch (ReflectiveOperationException e) {
            return ActionResult.error(ErrorCode.MAINTHREAD_FAILURE, "Failed to send pong: " + e.getMessage());
        }
    }

    public static ActionResult sendCustomPayload(ClientPacketListener conn, ResourceLocation channel, byte[] data) {
        try {
            Object packet = newCustomPayloadPacket(channel, data);
            if (packet == null) {
                return ActionResult.error(ErrorCode.NOT_IMPLEMENTED, NOT_AVAILABLE);
            }
            send(conn, packet);
            return ActionResult.ok();
        } catch (ClassNotFoundException e) {
            return ActionResult.error(ErrorCode.NOT_IMPLEMENTED, NOT_AVAILABLE);
        } catch (ReflectiveOperationException e) {
            return ActionResult.error(ErrorCode.MAINTHREAD_FAILURE, "Failed to send custom payload: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static void send(ClientPacketListener conn, Object packet) {
        conn.send((Packet<?>) packet);
    }

    private static Object newPacket(String simpleName, Class<?>[] paramTypes, Object... args)
            throws ReflectiveOperationException {
        Class<?> clazz = findPacketClass(simpleName);
        if (clazz == null) {
            throw new ClassNotFoundException(simpleName);
        }
        Constructor<?> ctor = clazz.getConstructor(paramTypes);
        return ctor.newInstance(args);
    }

    private static Object newClientInformationPacket(
            String locale,
            int viewDistance,
            ChatVisiblity vis,
            boolean chatColors,
            int skinParts,
            HumanoidArm arm,
            boolean textFiltering,
            boolean allowServerListings) throws ReflectiveOperationException {
        Class<?> gamePkt = tryLoad("net.minecraft.network.protocol.game.ServerboundClientInformationPacket");
        if (gamePkt != null) {
            Constructor<?> ctor = gamePkt.getConstructor(
                    String.class,
                    int.class,
                    ChatVisiblity.class,
                    boolean.class,
                    int.class,
                    HumanoidArm.class,
                    boolean.class,
                    boolean.class);
            return ctor.newInstance(locale, viewDistance, vis, chatColors, skinParts, arm, textFiltering, allowServerListings);
        }
        Class<?> commonPkt = tryLoad("net.minecraft.network.protocol.common.ServerboundClientInformationPacket");
        Class<?> clientInfo = tryLoad("net.minecraft.server.level.ClientInformation");
        if (commonPkt == null || clientInfo == null) {
            return null;
        }
        Object information;
        // 1.21.4+ adds ParticleStatus as 9th param
        Class<?> particleStatus = tryLoad("net.minecraft.server.level.ParticleStatus");
        if (particleStatus != null) {
            try {
                Constructor<?> infoCtor = clientInfo.getConstructor(
                        String.class, int.class, ChatVisiblity.class, boolean.class,
                        int.class, HumanoidArm.class, boolean.class, boolean.class, particleStatus);
                // Default to ALL particles
                Object allParticles = particleStatus.getEnumConstants()[0];
                information = infoCtor.newInstance(
                        locale, viewDistance, vis, chatColors, skinParts, arm, textFiltering, allowServerListings, allParticles);
            } catch (NoSuchMethodException e) {
                // Fallback to 8-arg constructor
                Constructor<?> infoCtor = clientInfo.getConstructor(
                        String.class, int.class, ChatVisiblity.class, boolean.class,
                        int.class, HumanoidArm.class, boolean.class, boolean.class);
                information = infoCtor.newInstance(
                        locale, viewDistance, vis, chatColors, skinParts, arm, textFiltering, allowServerListings);
            }
        } else {
            Constructor<?> infoCtor = clientInfo.getConstructor(
                    String.class, int.class, ChatVisiblity.class, boolean.class,
                    int.class, HumanoidArm.class, boolean.class, boolean.class);
            information = infoCtor.newInstance(
                    locale, viewDistance, vis, chatColors, skinParts, arm, textFiltering, allowServerListings);
        }
        Constructor<?> pktCtor = commonPkt.getConstructor(clientInfo);
        return pktCtor.newInstance(information);
    }

    private static Object newResourcePackPacket(String actionName, UUID packId) throws ReflectiveOperationException {
        Class<?> gamePkt = tryLoad("net.minecraft.network.protocol.game.ServerboundResourcePackPacket");
        if (gamePkt != null) {
            Class<?> actionClass = Class.forName(
                    "net.minecraft.network.protocol.game.ServerboundResourcePackPacket$Action");
            Object action = Enum.valueOf((Class<Enum>) actionClass, actionName);
            Constructor<?> ctor = gamePkt.getConstructor(actionClass);
            return ctor.newInstance(action);
        }
        Class<?> commonPkt = tryLoad("net.minecraft.network.protocol.common.ServerboundResourcePackPacket");
        if (commonPkt == null) {
            return null;
        }
        Class<?> actionClass = Class.forName(
                "net.minecraft.network.protocol.common.ServerboundResourcePackPacket$Action");
        Object action = Enum.valueOf((Class<Enum>) actionClass, actionName);
        UUID id = packId != null ? packId : new UUID(0L, 0L);
        Constructor<?> ctor = commonPkt.getConstructor(UUID.class, actionClass);
        return ctor.newInstance(id, action);
    }

    private static Object newCustomPayloadPacket(ResourceLocation channel, byte[] data)
            throws ReflectiveOperationException {
        Class<?> gamePkt = tryLoad("net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket");
        if (gamePkt != null) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            Constructor<?> ctor = gamePkt.getConstructor(ResourceLocation.class, FriendlyByteBuf.class);
            return ctor.newInstance(channel, buf);
        }
        Class<?> commonPkt = tryLoad("net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket");
        Class<?> payloadIface = tryLoad("net.minecraft.network.protocol.common.custom.CustomPacketPayload");
        if (commonPkt == null || payloadIface == null) {
            return null;
        }
        byte[] payloadBytes = data.clone();
        Object payload = Proxy.newProxyInstance(
                payloadIface.getClassLoader(),
                new Class<?>[]{payloadIface},
                new RawCustomPayloadHandler(channel, payloadBytes));
        Constructor<?> ctor = commonPkt.getConstructor(payloadIface);
        return ctor.newInstance(payload);
    }

    private static Class<?> findPacketClass(String simpleName) throws ClassNotFoundException {
        Class<?> c = tryLoad("net.minecraft.network.protocol.game." + simpleName);
        if (c != null) {
            return c;
        }
        c = tryLoad("net.minecraft.network.protocol.common." + simpleName);
        return c;
    }

    private static Class<?> tryLoad(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static final class RawCustomPayloadHandler implements InvocationHandler {
        private final ResourceLocation id;
        private final byte[] data;

        RawCustomPayloadHandler(ResourceLocation id, byte[] data) {
            this.id = id;
            this.data = data;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "id" -> id;
                case "write" -> {
                    if (args != null && args.length == 1 && args[0] instanceof FriendlyByteBuf buf) {
                        buf.writeBytes(data);
                    }
                    yield null;
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "RawCustomPayload[" + id + ", " + data.length + " bytes]";
                default -> null;
            };
        }
    }
}