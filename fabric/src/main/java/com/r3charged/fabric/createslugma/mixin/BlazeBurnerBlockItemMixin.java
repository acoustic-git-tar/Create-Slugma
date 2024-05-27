package com.r3charged.fabric.createslugma.mixin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.r3charged.fabric.createslugma.CreateSlugma;
import com.r3charged.fabric.createslugma.NBTHelper;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockItem;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlazeBurnerBlockItem.class)
public class BlazeBurnerBlockItemMixin {

    @Inject(
            method = "interactLivingEntity",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onInteractLivingEntity(ItemStack heldItem, Player player, LivingEntity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (entity instanceof PokemonEntity) {

            PokemonEntity pokemon = (PokemonEntity) entity;
            if (!NBTHelper.isSlugma(pokemon.getPokemon())) {
                cir.setReturnValue(InteractionResult.PASS);
                return;
            }

            if (pokemon.getOwnerUUID() == null || !pokemon.getOwnerUUID().equals(player.getUUID()) || pokemon.isBattling()) {
                spawnErrorEffects(player.level(), entity.position());
                cir.setReturnValue(InteractionResult.PASS);
                return;
            }
            Level world = player.level();
            pokemon.cry();
            ((BlazeBurnerBlockItemAccessor)this).invokeSpawnCaptureEffects(world, entity.position());
            if (world.isClientSide) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }

            giveSlugmaBurnerItemTo(player, heldItem, hand, pokemon.getPokemon());

            entity.discard();
            PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty((ServerPlayer) player);
            party.remove(pokemon.getPokemon());

            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
    }

    protected void giveSlugmaBurnerItemTo(Player player, ItemStack heldItem, InteractionHand hand, Pokemon pokemon) {
        ItemStack filled = NBTHelper.getSlugmaBurnerItem(pokemon);
        if (!player.isCreative())
            heldItem.shrink(1);
        if (heldItem.isEmpty()) {
            player.setItemInHand(hand, filled);
            return;
        }
        player.getInventory()
                .placeItemBackInInventory(filled);
    }

    private void spawnErrorEffects(Level world, Vec3 vec) {
        if (world.isClientSide) {
            for (int i = 0; i < 40; i++) {
                Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, world.random, .125f);
                world.addParticle(ParticleTypes.SMOKE, vec.x, vec.y, vec.z, motion.x, motion.y, motion.z);
                Vec3 circle = motion.multiply(1, 0, 1)
                        .normalize()
                        .scale(.5f);
                world.addParticle(ParticleTypes.SMOKE, circle.x, vec.y, circle.z, 0, -0.125, 0);
            }
            return;
        }

        BlockPos soundPos = BlockPos.containing(vec);
        world.playSound(null, soundPos, SoundEvents.BLAZE_HURT, SoundSource.HOSTILE, .25f, .75f);
        world.playSound(null, soundPos, SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, .5f, .75f);
    }
}
