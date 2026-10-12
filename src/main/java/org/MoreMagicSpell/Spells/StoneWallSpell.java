package org.MoreMagicSpell.Spells;

import java.util.ArrayList;
import java.util.List;

import org.MoreMagicSpell.Components.StoneWallComponent;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3i;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.ActiveAnimationComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.SectionReader;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;

public class StoneWallSpell {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static List<Vector2i> SHAPE_FLAT;
    private static List<Vector2i> SHAPE_CORNER;

    public static final String INVISIBLE_BLOCK = "InvisibleBlock";

    public static InteractionState CastSpell(CommandBuffer<EntityStore> commandBuffer,
            Vector3d origin, Rotation3f rotation, int durationMs) {
        // Get Store and World
        World world = commandBuffer.getExternalData().getWorld();
        var chunkStore = world.getChunkStore();
        var blocks = new SectionReader(chunkStore);

        // Create Entity Holder to make a new Entity
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        // Getting Model Asset and creating Model
        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset("StoneWallConjuration"); // Model in JSON in
                                                                                           // ressources
        Model model = Model.createScaledModel(modelAsset, 1.0f);
        // Getting Information from Player

        TransformComponent wallTransform = new TransformComponent(origin, new Rotation3f(0, rotation.y(), 0)); // Make
                                                                                     // TransformComponent for
                                                                                     // wall

        Vector3i centerOfWall = new Vector3i(
                (int) Math.floor(origin.x),
                (int) Math.floor(origin.y),
                (int) Math.floor(origin.z));

        BlockMaterial wallSpawnMaterial = materialAt(blocks, centerOfWall); // Material of block where the wall will be spawned
        int decale = 1;
        int maxChecks = 5;
        // Check for empty space to spawn wall, if not found, move further away in
        // facing
        BlockMaterial empty = BlockMaterial.Empty;
        if (wallSpawnMaterial.equals(empty)) {
            // If empty, check under for solid ground
            LOGGER.atInfo().log("Spawned wall in empty space");
            while (maxChecks > 0) {
                Vector3i belowPos = new Vector3i(
                        centerOfWall.x,
                        centerOfWall.y - decale,
                        centerOfWall.z);
                if (!materialAt(blocks, belowPos).equals(empty)) {
                    // Found solid ground below
                    wallTransform.getPosition().y -= decale - 1; // Move wall down to be on solid ground
                    centerOfWall.y -= decale - 1;
                    break;
                }
                decale += 1;
                maxChecks -= 1;
            }
        } else {
            // If solid, check above for empty space
            LOGGER.atInfo().log("Warning: Spawning wall in non-empty space");
            while (maxChecks > 0) {
                Vector3i abovePos = new Vector3i(
                        centerOfWall.x,
                        centerOfWall.y + decale,
                        centerOfWall.z);
                if (materialAt(blocks, abovePos).equals(empty)) {
                    // Found empty space above
                    wallTransform.getPosition().y += decale; // Move wall up to be in empty space
                    centerOfWall.y += decale;
                    break;
                }
                decale += 1;
                maxChecks -= 1;
            }
        }
        if (!(materialAt(blocks, centerOfWall).equals(BlockMaterial.Empty)
                && materialAt(blocks, new Vector3i(centerOfWall.x, centerOfWall.y - 1, centerOfWall.z))
                        .equals(BlockMaterial.Solid))) {
            // Could not find suitable space to spawn wall
            LOGGER.atInfo().log("Failed to find suitable space to spawn wall");
            return InteractionState.Failed;
        }

        // Add Components to Entity Holder for the Stone Wall
        holder.addComponent(TransformComponent.getComponentType(), wallTransform);
        holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
        BoundingBox box = new BoundingBox(model.getBoundingBox());
        holder.addComponent(BoundingBox.getComponentType(), box);
        holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(commandBuffer.getExternalData().takeNextNetworkId()));
        // Animation will be used when spawning the wall so we make a variable
        ActiveAnimationComponent activeAnimationComponent = new ActiveAnimationComponent();
        holder.addComponent(ActiveAnimationComponent.getComponentType(), activeAnimationComponent);

        // Create Collision Shape based on Wallshape
        CreateShape();
        List<Vector2i> Wallshape = getShape(rotation.y());
        List<Vector3i> invisibleBlockPositions = new ArrayList<>();
        var invisibleBlockId = BlockType.getAssetMap().getIndex(INVISIBLE_BLOCK);
        var invisibleBlock = BlockType.getAssetMap().getAsset(invisibleBlockId);
        if (invisibleBlock == null) {
            LOGGER.atWarning().log("Missing block type " + INVISIBLE_BLOCK);
            return InteractionState.Failed;
        }
        for (Vector2i v : Wallshape) { // For each position in the wall shape
            for (int y_add = -2; y_add < 3; y_add++) { // Wall height of 3 blocks (+2 under making it 5 blocks)
                Vector3i blockPos = new Vector3i(
                        centerOfWall.x + v.x,
                        centerOfWall.y + y_add,
                        centerOfWall.z + v.y);
                var sectionRef = blocks.sectionAt(blockPos.x, blockPos.y, blockPos.z);
                if (sectionRef != null && materialAt(blocks, blockPos) == BlockMaterial.Empty) { // Only set block if space is
                                                                                                 // empty, otherwise leave
                                                                                                 // existing block
                    invisibleBlockPositions.add(blockPos); // Save position to remove later
                    BlockOperations.setBlock(chunkStore, sectionRef,
                            blockPos.x,
                            blockPos.y,
                            blockPos.z,
                            invisibleBlockId, invisibleBlock,
                            RotationTuple.NONE_INDEX, FillerBlockUtil.NO_FILLER,
                            SetBlockSettings.NONE); // Set block to invisible block for collision only
                }
            }
        }

        holder.addComponent(
                StoneWallComponent.getComponentType(),
                new StoneWallComponent(durationMs, invisibleBlockPositions) // Wall lasts 8 seconds
        );

        // Interaction effect
        world.execute(() -> {
            commandBuffer.addEntity(holder, AddReason.SPAWN);
            activeAnimationComponent.setPlayingAnimation(AnimationSlot.Action, "Spawn");
        });

        return InteractionState.Finished;

    }

    private static BlockMaterial materialAt(SectionReader blocks, Vector3i pos) {
        return BlockType.getAssetMap().getAssetOrDefault(blocks.getBlock(pos.x, pos.y, pos.z), BlockType.EMPTY)
                .getMaterial();
    }

    // Making Shapes for Collision Detection
    private static void CreateShape() {
        // █ █ █ █ █
        // █ █
        SHAPE_FLAT = new java.util.ArrayList<>();
        for (int i = -2; i <= 2; i++) {
            SHAPE_FLAT.add(new Vector2i(i, 0));
        }
        SHAPE_FLAT.add(new Vector2i(-3, 1));
        SHAPE_FLAT.add(new Vector2i(3, 1));

        // █ █
        // █
        // █
        // █
        // █
        // █
        SHAPE_CORNER = new java.util.ArrayList<>();
        for (int i = 2; i <= 3; i++) {
            SHAPE_CORNER.add(new Vector2i(-2, i));
            SHAPE_CORNER.add(new Vector2i(i, -2));
        }
        for (int i = -1; i <= 1; i++) {
            SHAPE_CORNER.add(new Vector2i(i, -i));
        }
    }

    private static List<Vector2i> getShape(float direction) {
        // Direction [-PI, PI]
        // Determine shape based on direction and return rotated shape
        // 0, 90, 180, 270 degree -> flat shape
        // 45, 135, 225, 315 degree -> corner shape
        int shapeIndex = (int) Math.round(direction / (Math.PI / 4)) % 8;
        // absolute value on shapeIndex
        if (shapeIndex < 0)
            shapeIndex += 8;
        List<Vector2i> result = new ArrayList<>();

        switch (shapeIndex) {
            // FLAT SHAPE
            case 0:
                for (Vector2i v : SHAPE_FLAT)
                    result.add(new Vector2i(v.x, v.y)); // North
                break;
            case 2:
                for (Vector2i v : SHAPE_FLAT)
                    result.add(new Vector2i(v.y, -v.x)); // West
                break;
            case 4:
                for (Vector2i v : SHAPE_FLAT)
                    result.add(new Vector2i(-v.x, -v.y)); // South
                break;
            case 6:
                for (Vector2i v : SHAPE_FLAT)
                    result.add(new Vector2i(-v.y, v.x)); // East
                break;

            // CORNER SHAPE
            case 1:
                for (Vector2i v : SHAPE_CORNER)
                    result.add(new Vector2i(v.x, v.y)); // North-West
                break;
            case 3:
                for (Vector2i v : SHAPE_CORNER)
                    result.add(new Vector2i(v.y, -v.x)); // South-West
                break;
            case 5:
                for (Vector2i v : SHAPE_CORNER)
                    result.add(new Vector2i(-v.x, -v.y)); // South-East
                break;
            case 7:
                for (Vector2i v : SHAPE_CORNER)
                    result.add(new Vector2i(-v.y, v.x)); // North-East
                break;
        }
        LOGGER.atInfo().log("Spawn Wall at index " + shapeIndex);
        return result;

    }
}