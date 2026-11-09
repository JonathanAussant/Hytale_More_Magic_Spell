package org.MoreMagicSpell.Interactions;

import org.MoreMagicSpell.Spells.StoneWallSpell;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.joml.Vector3d;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Interaction that spawns a stone wall entity in front of the player when used.
 */
public class SpawnStoneWallInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<SpawnStoneWallInteraction> CODEC = BuilderCodec.builder(
            SpawnStoneWallInteraction.class, SpawnStoneWallInteraction::new, SimpleInstantInteraction.CODEC).build();

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float WALL_SPAWN_DISTANCE = 5.0f;

    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType,
            @NonNullDecl InteractionContext interactionContext, @NonNullDecl CooldownHandler cooldownHandler) {

        // Get CommandBuffer
        var commandBuffer = interactionContext.getCommandBuffer();

        // Verfication Checks for CommandBuffer, Player, and ItemStack
        if (commandBuffer == null) {
            interactionContext.getState().state = InteractionState.Failed;
            LOGGER.atInfo().log("CommandBuffer is null");
            return;
        }

        var target = interactionContext.getOwningEntity();

        if (target == null || !target.isValid()) return;

        var headRotation = commandBuffer.getComponent(target, HeadRotation.getComponentType());
        if (headRotation == null) return;
        var direction = headRotation.getRotation().y(); // facing direction of player

        // Set up the wall position
        var offsetX = -Math.sin(direction) * WALL_SPAWN_DISTANCE;
        var offsetZ = -Math.cos(direction) * WALL_SPAWN_DISTANCE;

        var playerTransform = commandBuffer.getComponent(target, TransformComponent.getComponentType());
        var wallPosition = new Vector3d(playerTransform.getPosition());
        wallPosition.add(
                new Vector3d(
                        offsetX,
                        0,
                        offsetZ));
        wallPosition = new Vector3d(((int) wallPosition.x) + 0.5, (int) wallPosition.y, ((int) wallPosition.z) + 0.5); // Center
                                                                                                                       // wall
                                                                                                                       // on
                                                                                                                       // block
                                                                                                                       // grid

        interactionContext.getState().state = StoneWallSpell.CastSpell(commandBuffer, wallPosition, headRotation.getRotation(), 8000);

        return;

    }

}
