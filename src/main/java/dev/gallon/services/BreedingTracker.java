package dev.gallon.services;

import dev.gallon.domain.BreedingLog;
import dev.gallon.domain.BreedingStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds out whether horses can breed.
 * <p>
 * The game only tells the server about the breeding cooldown and the love time: the client only knows if a horse is a
 * baby. In singleplayer, and when hosting a LAN world, the built-in server is in the same process, so its horses are read
 * from there. The server thread owns them, so they are read on that thread, a few times per second, and the client
 * only ever looks at the last answer.
 * <p>
 * On another server nothing can be read. What a client does see is a horse being fed and a horse breeding, and the
 * breeding cooldown and the love time are worked out from that by a {@link BreedingLog}. Those are estimates, and only
 * exist for what happened in front of the player.
 */
public final class BreedingTracker {
    private static final long NANOS_PER_TICK = 50_000_000L;
    private static final long REFRESH_NANOS = 4 * NANOS_PER_TICK;
    private static final long FORGET_NANOS = 600 * NANOS_PER_TICK;

    private record Snapshot(boolean found, int age, int inLove, long readAt) {
    }

    private final Map<Integer, Snapshot> snapshots = new ConcurrentHashMap<>();
    private final Set<Integer> pending = ConcurrentHashMap.newKeySet();
    private @Nullable IntegratedServer lastServer;

    private record PendingLoad(int id, double x, double y, double z, long tick) {
    }

    private final BreedingLog log = new BreedingLog();
    private final List<PendingLoad> pendingLoads = new ArrayList<>();
    private @Nullable Object lastConnection;

    /**
     * @return the status of the horse, with the time left counting down between two reads of the server
     */
    public @NotNull BreedingStatus statusOf(@NotNull AbstractHorse horse) {
        boolean ridden = horse.isVehicle() || horse.isPassenger();
        boolean hurt = horse.getHealth() < horse.getMaxHealth();

        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return estimatedStatusOf(horse, hurt, ridden);
        }

        long now = System.nanoTime();
        Snapshot snapshot = snapshots.get(horse.getId());
        if (snapshot == null || now - snapshot.readAt() >= REFRESH_NANOS) {
            request(server, horse);
        }
        if (snapshot == null) {
            return BreedingStatus.checking();
        }

        BreedingStatus.Inputs inputs = snapshot.found()
                ? new BreedingStatus.Inputs(horse.isBaby(), horse.isTamed(), hurt, ridden, snapshot.age(), snapshot.inLove())
                : new BreedingStatus.Inputs(horse.isBaby(), horse.isTamed(), hurt, ridden, null, null);
        return BreedingStatus.of(inputs).elapsed((int) ((now - snapshot.readAt()) / NANOS_PER_TICK));
    }

    private @NotNull BreedingStatus estimatedStatusOf(AbstractHorse horse, boolean hurt, boolean ridden) {
        Optional<BreedingLog.Estimate> estimate = gameTime().flatMap(now -> log.estimate(horse.getId(), now));
        BreedingStatus status = BreedingStatus.of(new BreedingStatus.Inputs(
                horse.isBaby(),
                horse.isTamed(),
                hurt,
                ridden,
                estimate.map(BreedingLog.Estimate::cooldownTicks).orElse(null),
                estimate.map(BreedingLog.Estimate::loveTicks).orElse(null)
        ));

        boolean derivedFromTheLog = status.state() != BreedingStatus.State.FOAL
                && status.state() != BreedingStatus.State.NOT_TAMED
                && status.state() != BreedingStatus.State.UNKNOWN;
        return derivedFromTheLog ? status.asEstimate() : status;
    }

    /**
     * Called when hearts burst above a horse: it was fed, or it just had a foal.
     */
    public void onHearts(@NotNull AbstractHorse horse) {
        if (Minecraft.getInstance().getSingleplayerServer() == null && !horse.isBaby()) {
            gameTime().ifPresent(now -> log.hearts(horse.getId(), horse.getX(), horse.getY(), horse.getZ(), now));
        }
    }

    /**
     * Called when a horse appears on the client. Whether it is a baby isn't known yet: the game sends it right after.
     */
    public void onLoaded(@NotNull AbstractHorse horse) {
        if (Minecraft.getInstance().getSingleplayerServer() == null) {
            gameTime().ifPresent(now -> pendingLoads.add(new PendingLoad(horse.getId(), horse.getX(), horse.getY(), horse.getZ(), now)));
        }
    }

    private static Optional<Long> gameTime() {
        var level = Minecraft.getInstance().level;
        return level == null ? Optional.empty() : Optional.of(level.getGameTime());
    }

    /**
     * Called every client tick to forget the horses that nobody looked at for a while, and the answers of a world that
     * was left (entity ids are used again by the next world).
     */
    public void tick() {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != lastServer) {
            snapshots.clear();
            pending.clear();
            lastServer = server;
        }

        var connection = Minecraft.getInstance().getConnection();
        if (connection != lastConnection) {
            log.clear();
            pendingLoads.clear();
            lastConnection = connection;
        }

        long now = System.nanoTime();
        snapshots.values().removeIf(snapshot -> now - snapshot.readAt() > FORGET_NANOS);

        if (server == null) {
            processLog();
        }
    }

    private void processLog() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        long now = level.getGameTime();
        pendingLoads.removeIf(load -> {
            if (!(level.getEntity(load.id()) instanceof AbstractHorse horse)) {
                return true;
            }
            if (horse.isBaby()) {
                log.baby(load.id(), load.x(), load.y(), load.z(), load.tick());
                return true;
            }
            // The game sends the baby flag right after the horse itself
            return now - load.tick() > 6;
        });

        log.process(now, (x, y, z, radius) -> level
                .getEntitiesOfClass(AbstractHorse.class, new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius), horse -> !horse.isBaby() && horse.isAlive())
                .stream()
                .map(horse -> new BreedingLog.Neighbour(horse.getId(), horse.getX(), horse.getY(), horse.getZ()))
                .toList());
    }

    private void request(IntegratedServer server, AbstractHorse horse) {
        int id = horse.getId();
        if (!pending.add(id)) {
            return;
        }

        var dimension = horse.level().dimension();
        var uuid = horse.getUUID();
        server.execute(() -> {
            try {
                ServerLevel level = server.getLevel(dimension);
                Entity entity = level == null ? null : level.getEntity(id);
                snapshots.put(id, entity instanceof Animal animal && animal.getUUID().equals(uuid)
                        ? new Snapshot(true, animal.getAge(), animal.getInLoveTime(), System.nanoTime())
                        : new Snapshot(false, 0, 0, System.nanoTime()));
            } finally {
                pending.remove(id);
            }
        });
    }
}
