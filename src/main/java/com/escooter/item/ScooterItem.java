package com.escooter.item;

import com.escooter.entity.ElectricScooterEntity;
import com.escooter.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Предмет-самокат. ПКМ по блоку ставит сущность самоката. Заряд хранится в NBT ("Charge").
 */
public class ScooterItem extends Item {
    public static final String TAG_CHARGE = "Charge";

    private final int variant;

    public ScooterItem(Properties properties, int variant) {
        super(properties);
        this.variant = variant;
    }

    /** Заряд в тиках езды. Если тега нет (самокат только что скрафчен) — половина батареи. */
    public static int getCharge(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(TAG_CHARGE)) {
            return stack.getTag().getInt(TAG_CHARGE);
        }
        return ElectricScooterEntity.MAX_CHARGE / 2;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        ElectricScooterEntity scooter = ModEntities.ELECTRIC_SCOOTER.get().create(serverLevel);
        if (scooter == null) {
            return InteractionResult.FAIL;
        }

        scooter.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                player != null ? player.getYRot() : 0.0F, 0.0F);
        scooter.setCharge(getCharge(stack));
        scooter.setVariant(this.variant);

        if (!serverLevel.noCollision(scooter, scooter.getBoundingBox())) {
            return InteractionResult.FAIL;
        }

        serverLevel.addFreshEntity(scooter);
        serverLevel.playSound(null, pos, SoundEvents.IRON_GOLEM_STEP, SoundSource.PLAYERS, 0.8F, 1.0F);
        serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, pos);

        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int percent = Math.round(100.0F * getCharge(stack) / ElectricScooterEntity.MAX_CHARGE);
        tooltip.add(Component.translatable("tooltip.escooter.charge", percent)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.escooter.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
