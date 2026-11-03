package com.escooter.entity;

import com.escooter.item.ScooterItem;
import com.escooter.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Электросамокат.
 *
 * Управление (как у лошади/свиньи — водитель сам считает движение, сервер принимает позицию):
 *   W — разгон (тратит заряд), S — тормоз / задний ход, A/D — руль.
 *
 * Заряд (0..MAX_CHARGE) синхронизируется с клиентами. ПКМ батареей — полная зарядка.
 * Удар игрока — самокат превращается обратно в предмет (с сохранённым зарядом).
 */
public class ElectricScooterEntity extends Entity {

    /** Заряд в тиках разгона: 6000 тиков = 5 минут непрерывного газа. */
    public static final int MAX_CHARGE = 6000;

    /** Цвета самоката. */
    public static final int VARIANT_YELLOW = 0;
    public static final int VARIANT_PURPLE = 1;
    public static final int VARIANT_COUNT = 2;
    public static final String TAG_VARIANT = "Variant";

    private static final EntityDataAccessor<Integer> DATA_CHARGE =
            SynchedEntityData.defineId(ElectricScooterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(ElectricScooterEntity.class, EntityDataSerializers.INT);

    // Параметры езды (блоки/тик). 0.45 блока/тик = 9 блоков/сек.
    private static final double MAX_SPEED = 0.45D;
    private static final double MAX_REVERSE_SPEED = 0.10D;
    private static final double ACCELERATION = 0.022D;
    private static final double BRAKE = 0.06D;
    private static final double COAST_FRICTION = 0.985D;
    private static final float MAX_TURN_PER_TICK = 4.5F;

    // Плавная интерполяция для всех, кто сам не управляет самокатом (как у лодки).
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private double lerpYRot, lerpXRot;

    // Серверный счётчик для редкого обновления заряда (меньше пакетов).
    private int drainBuffer;

    // Анимация колёс — только визуальная, на клиенте.
    public float wheelRot;
    public float wheelRotO;
    public float steerVisual;
    public float steerVisualO;

    public ElectricScooterEntity(EntityType<? extends ElectricScooterEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.setMaxUpStep(1.0F); // заезжает на ступеньки и полублоки как на бордюр
    }

    // ---------------------------------------------------------------- данные

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_CHARGE, MAX_CHARGE / 2);
        this.entityData.define(DATA_VARIANT, VARIANT_YELLOW);
    }

    public int getVariant() {
        return Mth.clamp(this.entityData.get(DATA_VARIANT), 0, VARIANT_COUNT - 1);
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_VARIANT, Mth.clamp(variant, 0, VARIANT_COUNT - 1));
    }

    public int getCharge() {
        return this.entityData.get(DATA_CHARGE);
    }

    public void setCharge(int charge) {
        this.entityData.set(DATA_CHARGE, Mth.clamp(charge, 0, MAX_CHARGE));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains(ScooterItem.TAG_CHARGE)) {
            setCharge(tag.getInt(ScooterItem.TAG_CHARGE));
        }
        setVariant(tag.getInt(TAG_VARIANT));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt(ScooterItem.TAG_CHARGE, getCharge());
        tag.putInt(TAG_VARIANT, getVariant());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // ---------------------------------------------------------------- пассажир

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity first = this.getFirstPassenger();
        return first instanceof LivingEntity living ? living : null;
    }

    /** Игрок стоит на самокате, а не сидит. */
    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    /** Ноги игрока на уровне деки (0.44 блока от земли; смещение игрока -0.35). */
    @Override
    public double getPassengersRidingOffset() {
        return 0.79D;
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        passenger.setYBodyRot(this.getYRot());
        float diff = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
        float clamped = Mth.clamp(diff, -110.0F, 110.0F);
        passenger.yRotO += clamped - diff;
        passenger.setYRot(passenger.getYRot() + clamped - diff);
        passenger.setYHeadRot(passenger.getYRot());
    }

    /** Игрок слезает прямо на месте самоката: так не возникает проблем в тоннелях под потолком. */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return new Vec3(this.getX(), this.getY(), this.getZ());
    }

    // ---------------------------------------------------------------- взаимодействие

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public ItemStack getPickResult() {
        return createItemStack();
    }

    public ItemStack createItemStack() {
        ItemStack stack = new ItemStack(ModItems.scooterForVariant(getVariant()));
        stack.getOrCreateTag().putInt(ScooterItem.TAG_CHARGE, getCharge());
        return stack;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (held.is(ModItems.BATTERY.get())) {
            if (getCharge() >= MAX_CHARGE) {
                if (!this.level().isClientSide) {
                    player.displayClientMessage(Component.translatable("message.escooter.full"), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            if (!this.level().isClientSide) {
                setCharge(MAX_CHARGE);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7F, 1.4F);
                player.displayClientMessage(Component.translatable("message.escooter.charged"), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        if (!this.level().isClientSide) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (!this.level().isClientSide && !this.isRemoved()) {
            if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) { // /kill, пустота
                this.discard();
                return true;
            }
            if (source.getEntity() instanceof Player player) {
                if (!player.getAbilities().instabuild) {
                    this.spawnAtLocation(createItemStack());
                }
                this.discard();
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- тик и физика

    @Override
    public void tick() {
        super.tick();
        tickLerp();

        if (this.isControlledByLocalInstance()) {
            // Это клиент водителя (или сервер, когда самокат пустой): считаем движение сами.
            driveTick();
            this.move(MoverType.SELF, this.getDeltaMovement());
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }

        if (!this.level().isClientSide) {
            serverTick();
        } else {
            animationTick();
        }

        this.checkInsideBlocks();
    }

    private Vec3 forwardVector() {
        float rad = this.getYRot() * ((float) Math.PI / 180F);
        return new Vec3(-Mth.sin(rad), 0.0D, Mth.cos(rad));
    }

    private void driveTick() {
        LivingEntity rider = this.getControllingPassenger();
        Vec3 forward = forwardVector();
        Vec3 motion = this.getDeltaMovement();

        // Скорость вдоль направления самоката (со знаком). Боковой снос гасим — колёса держат сцепление.
        double speed = motion.x * forward.x + motion.z * forward.z;
        boolean inWater = this.isInWater();

        if (rider != null && !inWater) {
            float throttle = rider.zza;
            float steer = rider.xxa;
            boolean hasCharge = getCharge() > 0;

            if (throttle > 0.0F && hasCharge) {
                speed = Math.min(Math.max(speed, 0.0D) + ACCELERATION, MAX_SPEED);
            } else if (throttle < 0.0F) {
                if (speed > 0.0D) {
                    speed = Math.max(0.0D, speed - BRAKE);
                } else if (hasCharge) {
                    speed = Math.max(speed - ACCELERATION * 0.5D, -MAX_REVERSE_SPEED);
                } else {
                    speed *= 0.8D;
                }
            } else {
                speed *= COAST_FRICTION;
            }

            // Руль: на месте поворачивает слабо, на скорости — сильнее.
            if (steer != 0.0F) {
                double factor = 0.35D + 0.65D * Math.min(1.0D, Math.abs(speed) / MAX_SPEED);
                float turn = (float) (steer * MAX_TURN_PER_TICK * factor);
                this.setYRot(this.getYRot() - turn);
            }
        } else {
            // Без водителя (или в воде) катится по инерции и быстро останавливается.
            speed *= inWater ? 0.8D : 0.9D;
            if (Math.abs(speed) < 0.003D) {
                speed = 0.0D;
            }
        }

        Vec3 dir = forwardVector(); // направление после поворота руля
        double y = motion.y;
        y -= inWater ? 0.01D : 0.04D;
        y *= inWater ? 0.8D : 0.98D;

        this.setDeltaMovement(dir.x * speed, y, dir.z * speed);

        // Удар о препятствие гасит скорость.
        if (this.horizontalCollision && Math.abs(speed) > 0.05D) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.5D, 1.0D, 0.5D));
        }
    }

    private void serverTick() {
        LivingEntity rider = this.getControllingPassenger();

        // Расход заряда: пока водитель жмёт «вперёд» (zza приходит на сервер пакетом ввода).
        if (rider != null && rider.zza > 0.0F && getCharge() > 0 && !this.isInWater()) {
            this.drainBuffer++;
            if (this.drainBuffer >= 10) {
                setCharge(getCharge() - this.drainBuffer);
                this.drainBuffer = 0;
            }
        }

        // Заряд на экране (action bar) раз в полсекунды.
        if (rider instanceof ServerPlayer player && this.tickCount % 10 == 0) {
            int percent = Math.round(100.0F * getCharge() / MAX_CHARGE);
            player.displayClientMessage(Component.translatable(
                    percent > 0 ? "message.escooter.charge" : "message.escooter.empty", percent), true);
        }
    }

    private void animationTick() {
        this.wheelRotO = this.wheelRot;
        this.steerVisualO = this.steerVisual;

        double dx = this.getX() - this.xo;
        double dz = this.getZ() - this.zo;
        Vec3 forward = forwardVector();
        double signedDist = dx * forward.x + dz * forward.z;
        // окружность колеса ~0.31 блока (диаметр 5/16)
        this.wheelRot += (float) (signedDist / 0.31D * 360.0D);

        float yawDelta = Mth.wrapDegrees(this.getYRot() - this.yRotO);
        float target = Mth.clamp(yawDelta * 5.0F, -35.0F, 35.0F);
        this.steerVisual = Mth.lerp(0.4F, this.steerVisual, target);
    }

    // ---------------------------------------------------------------- интерполяция (как у лодки)

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = 10;
    }

    private void tickLerp() {
        if (this.isControlledByLocalInstance()) {
            this.lerpSteps = 0;
            this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
        }
        if (this.lerpSteps > 0) {
            double nx = this.getX() + (this.lerpX - this.getX()) / this.lerpSteps;
            double ny = this.getY() + (this.lerpY - this.getY()) / this.lerpSteps;
            double nz = this.getZ() + (this.lerpZ - this.getZ()) / this.lerpSteps;
            double dYaw = Mth.wrapDegrees(this.lerpYRot - this.getYRot());
            this.setYRot(this.getYRot() + (float) dYaw / this.lerpSteps);
            this.setXRot(this.getXRot() + (float) (this.lerpXRot - this.getXRot()) / this.lerpSteps);
            this.lerpSteps--;
            this.setPos(nx, ny, nz);
            this.setRot(this.getYRot(), this.getXRot());
        }
    }
}
