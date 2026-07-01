package net.example.mod;

import fi.dy.masa.litematica.data.DataManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2S;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class LitematicaEasyBypass {

    public static boolean isEnabled = true; 
    private static int cooldownTicks = 0;   

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null || !isEnabled) return;

            if (cooldownTicks > 0) {
                cooldownTicks--;
            }

            if (client.options.useKey.isPressed() && cooldownTicks == 0) {
                if (client.crosshairTarget instanceof BlockHitResult hitResult) {
                    BlockPos targetPos = hitResult.getBlockPos().offset(hitResult.getSide());
                    executeEasyPlaceBypass(client, targetPos, hitResult);
                }
            }
        });
    }

    private static void executeEasyPlaceBypass(MinecraftClient client, BlockPos targetPos, BlockHitResult hitResult) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        BlockState schematicState = DataManager.getSchematicPlacementManager()
                .getSchematicStateAtPosition(targetPos);

        if (schematicState == null || schematicState.isAir()) return;

        Direction targetDirection = null;
        if (schematicState.contains(Properties.FACING)) {
            targetDirection = schematicState.get(Properties.FACING);
        } else if (schematicState.contains(Properties.HORIZONTAL_FACING)) {
            targetDirection = schematicState.get(Properties.HORIZONTAL_FACING);
        }

        if (targetDirection == null) return;

        float targetYaw = player.getYaw();
        float targetPitch = player.getPitch();

        switch (targetDirection) {
            case NORTH -> { targetYaw = 0; targetPitch = 0; }
            case SOUTH -> { targetYaw = 180; targetPitch = 0; }
            case EAST  -> { targetYaw = 90; targetPitch = 0; }
            case WEST  -> { targetYaw = -90; targetPitch = 0; }
            case UP    -> { targetYaw = player.getYaw(); targetPitch = 90; }   
            case DOWN  -> { targetYaw = player.getYaw(); targetPitch = -90; }  
        }

        float originalYaw = player.getYaw();
        float originalPitch = player.getPitch();

        player.networkHandler.sendPacket(new PlayerMoveC2S.LookAndOnGround(targetYaw, targetPitch, player.isOnGround()));

        player.setYaw(targetYaw);
        player.setPitch(targetPitch);

        client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hitResult);
        player.swingHand(Hand.MAIN_HAND);

        player.setYaw(originalYaw);
        player.setPitch(originalPitch);

        cooldownTicks = 2; 
    }
}
