package dev.gallon.services;

import dev.gallon.domain.BreedingStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds out whether horses can breed.
 * <p>
 * The game only tells the server about the breeding cooldown and the love time: the client only knows if a horse is a
 * baby. In singleplayer, and when hosting a LAN world, the built-in server is in the same process, so its horses are read
 * from there. The server thread owns them, so they are read on that thread, a few times per second, and the client
 * only ever looks at the last answer. On another server nothing can be read, and the status is unknown.
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

    /**
     * @return the status of the horse, with the time left counting down between two reads of the server
     */
    public @NotNull BreedingStatus statusOf(@NotNull AbstractHorse horse) {
        boolean ridden = horse.isVehicle() || horse.isPassenger();
        boolean hurt = horse.getHealth() < horse.getMaxHealth();

        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return BreedingStatus.of(new BreedingStatus.Inputs(horse.isBaby(), horse.isTamed(), hurt, ridden, null, null));
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

        long now = System.nanoTime();
        snapshots.values().removeIf(snapshot -> now - snapshot.readAt() > FORGET_NANOS);
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
