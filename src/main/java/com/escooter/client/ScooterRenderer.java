package com.escooter.client;

import com.escooter.EScooterMod;
import com.escooter.entity.ElectricScooterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ScooterRenderer extends EntityRenderer<ElectricScooterEntity> {
    /** Индекс = ElectricScooterEntity.VARIANT_*: 0 — жёлтый, 1 — фиолетовый. */
    private static final ResourceLocation[] TEXTURES = {
            new ResourceLocation(EScooterMod.MODID, "textures/entity/electric_scooter_yellow.png"),
            new ResourceLocation(EScooterMod.MODID, "textures/entity/electric_scooter_purple.png")
    };

    private final ScooterModel model;

    public ScooterRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.45F;
        this.model = new ScooterModel(context.bakeLayer(ScooterModel.LAYER_LOCATION));
    }

    @Override
    public void render(ElectricScooterEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Та же последовательность, что у ванильных мобов: модель задана «вверх ногами» от y = 24.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0D, -1.5D, 0.0D);

        this.model.pose(
                Mth.lerp(partialTicks, entity.wheelRotO, entity.wheelRot),
                Mth.lerp(partialTicks, entity.steerVisualO, entity.steerVisual));

        VertexConsumer consumer = buffer.getBuffer(this.model.renderType(getTextureLocation(entity)));
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ElectricScooterEntity entity) {
        return TEXTURES[entity.getVariant()];
    }
}
