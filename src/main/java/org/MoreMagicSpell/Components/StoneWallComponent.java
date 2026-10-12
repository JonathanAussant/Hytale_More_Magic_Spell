package org.MoreMagicSpell.Components;

import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import org.MoreMagicSpell.MoreMagicSpell;
import org.joml.Vector3i;

public class StoneWallComponent implements Component<EntityStore> {

    // Saving necessary data for petrification effect
    public static final BuilderCodec<StoneWallComponent> CODEC =
        BuilderCodec.builder(StoneWallComponent.class, StoneWallComponent::new)
            .append(new KeyedCodec<Long>("EndTime", Codec.LONG),
                (c, v) -> c.endTime = v, c -> c.endTime)
            .add()
            .append(new KeyedCodec<>("InvisibleBlocks", new ArrayCodec<>(Vector3iUtil.CODEC, Vector3i[]::new)),
                (c, v) -> c.InvisibleBlocks = List.of(v),
                c -> c.InvisibleBlocks == null ? null : c.InvisibleBlocks.toArray(Vector3i[]::new))
            .add()
            .build();

    // Serialized fields
    private long endTime;
    private List<Vector3i> InvisibleBlocks;

    public StoneWallComponent() {}

    public StoneWallComponent(long durationMs, List<Vector3i> InvisibleBlocks) {
        this.endTime = System.currentTimeMillis() + durationMs;
        this.InvisibleBlocks = InvisibleBlocks;
    }

    @Nonnull
    public static ComponentType<EntityStore, StoneWallComponent> getComponentType() {
        return MoreMagicSpell.get().getStoneWallComponentType();
    }

    // Getters and other
    public long getEndTime() { return endTime; }
    public boolean isExpired() {
        return System.currentTimeMillis() > endTime;
    }
    public List<Vector3i> getInvisibleBlocks() { return InvisibleBlocks; }

    @Override
    public Component<EntityStore> clone() {
        StoneWallComponent copy = new StoneWallComponent(endTime - System.currentTimeMillis(), InvisibleBlocks);
        return copy;
    }
}