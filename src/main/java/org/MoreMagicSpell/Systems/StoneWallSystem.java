package org.MoreMagicSpell.Systems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;
import org.MoreMagicSpell.Components.StoneWallComponent;
import org.MoreMagicSpell.Spells.StoneWallSpell;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.heightmap.HeightmapColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;

public class StoneWallSystem extends EntityTickingSystem<EntityStore> {

  private final ComponentType<EntityStore, StoneWallComponent> stoneWallComponentType;

  private static final HytaleLogger LOGGER = HytaleLogger.getLogger();

  public StoneWallSystem(ComponentType<EntityStore, StoneWallComponent> stoneWallComponentType) {
    this.stoneWallComponentType = stoneWallComponentType;
  }

  @Override
  public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
      @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

    // Get StoneWallComponent and Ref
    StoneWallComponent stoneWall = archetypeChunk.getComponent(index, stoneWallComponentType);
    Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
    World world = commandBuffer.getExternalData().getWorld();

    if (stoneWall.isExpired()) {

        // Remove all Invisible Blocks
        if (stoneWall.getInvisibleBlocks() != null) {
          clearInvisibleBlocks(world, stoneWall.getInvisibleBlocks());
        }

        // Remove StoneWallComponent
        commandBuffer.removeComponent(ref, stoneWallComponentType);

        // Remove StoneWall entity after a short delay to allow for any despawn animations
        world.execute(() -> {
            commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
        });

        LOGGER.atInfo().log("StoneWall entity expired: " + ref.toString());
    }
  }

  private static void clearInvisibleBlocks(World world, List<Vector3i> blocks) {
    var chunkStore = world.getChunkStore();
    var invisibleBlockId = BlockType.getAssetMap().getIndex(StoneWallSpell.INVISIBLE_BLOCK);

    var blocksBySection = new HashMap<Vector3i, List<Vector3i>>();
    for (var block : blocks) {
      var sectionPos = new Vector3i(
          ChunkUtil.chunkCoordinate(block.x),
          ChunkUtil.chunkCoordinate(block.y),
          ChunkUtil.chunkCoordinate(block.z));
      blocksBySection.computeIfAbsent(sectionPos, k -> new ArrayList<>()).add(block);
    }

    blocksBySection.forEach((sectionPos, sectionBlocks) ->
        chunkStore.getChunkSectionReferenceAsync(sectionPos.x, sectionPos.y, sectionPos.z)
            .thenAcceptAsync(sectionRef -> clearSection(chunkStore, sectionRef, sectionBlocks, invisibleBlockId), world)
            .exceptionally(t -> {
              LOGGER.atWarning().withCause(t).log("Failed to clear StoneWall blocks in section " + sectionPos);
              return null;
            }));
  }

  private static void clearSection(ChunkStore chunkStore, Ref<ChunkStore> sectionRef, List<Vector3i> blocks,
      int invisibleBlockId) {
    if (sectionRef == null || !sectionRef.isValid()) return;

    var store = chunkStore.getStore();
    var section = store.getComponent(sectionRef, ChunkSection.getComponentType());
    var blockSection = store.getComponent(sectionRef, BlockSection.getComponentType());
    if (section == null || blockSection == null) return;
    var blockComponentSection = store.getComponent(sectionRef, BlockComponentSection.getComponentType());
    var heightmapColumn = store.getComponent(section.getChunkColumnReference(), HeightmapColumn.getComponentType());
    if (heightmapColumn == null) return;

    for (var block : blocks) {
      if (blockSection.get(block.x, block.y, block.z) != invisibleBlockId) continue;
      BlockOperations.setBlock(chunkStore, sectionRef, section, blockSection, blockComponentSection, heightmapColumn,
          block.x, block.y, block.z, BlockType.EMPTY_ID, BlockType.EMPTY,
          RotationTuple.NONE_INDEX, FillerBlockUtil.NO_FILLER, SetBlockSettings.NONE);
    }
  }

  @Nonnull
  @Override
  public Query<EntityStore> getQuery() {
    return Query.and(this.stoneWallComponentType);
  }

}
