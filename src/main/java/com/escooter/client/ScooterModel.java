package com.escooter.client;

import com.escooter.EScooterMod;
import com.escooter.entity.ElectricScooterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Модель самоката на ванильных ModelPart (без GeckoLib).
 * Начало координат модели: y = 24 — земля, перёд самоката смотрит в -z.
 * Раскладка текстуры 64x64 совпадает с tools/make_textures.py — менять только вместе.
 */
public class ScooterModel extends EntityModel<ElectricScooterEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(EScooterMod.MODID, "electric_scooter"), "main");

    private final ModelPart root;
    private final ModelPart front;      // руль + вилка, поворачивается
    private final ModelPart frontWheel;
    private final ModelPart rearWheel;

    public ScooterModel(ModelPart root) {
        this.root = root;
        this.front = root.getChild("front");
        this.frontWheel = this.front.getChild("front_wheel");
        this.rearWheel = root.getChild("rear_wheel");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Дека: x -3.5..3.5, y 17..19 (над землёй 5..7 px), z -10..10
        root.addOrReplaceChild("deck",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -7.0F, -10.0F, 7.0F, 2.0F, 20.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        // Заднее колесо: центр на высоте 2.5 px, z = +12
        root.addOrReplaceChild("rear_wheel",
                CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, -2.5F, -2.5F, 2.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 21.5F, 12.0F));

        // Передняя часть: ось поворота руля проходит через (0, *, -11)
        PartDefinition front = root.addOrReplaceChild("front",
                CubeListBuilder.create()
                        // стойка
                        .texOffs(16, 24).addBox(-1.0F, -18.0F, -1.0F, 2.0F, 17.0F, 2.0F)
                        // руль
                        .texOffs(26, 24).addBox(-6.0F, -20.0F, -1.0F, 12.0F, 2.0F, 2.0F)
                        // фара
                        .texOffs(0, 36).addBox(-1.5F, -16.0F, -3.0F, 3.0F, 3.0F, 2.0F)
                        // батарея на стойке
                        .texOffs(26, 30).addBox(-2.0F, -12.0F, 1.0F, 4.0F, 8.0F, 2.0F),
                PartPose.offset(0.0F, 24.0F, -11.0F));

        // Переднее колесо — дочернее к «front», поворачивается вместе с рулём
        front.addOrReplaceChild("front_wheel",
                CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, -2.5F, -2.5F, 2.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, -2.5F, -1.5F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Вызывается рендерером перед отрисовкой. Углы в градусах. */
    public void pose(float wheelDegrees, float steerDegrees) {
        float wheel = wheelDegrees * Mth.DEG_TO_RAD;
        this.rearWheel.xRot = wheel;
        this.frontWheel.xRot = wheel;
        this.front.yRot = steerDegrees * Mth.DEG_TO_RAD;
    }

    @Override
    public void setupAnim(ElectricScooterEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Анимация задаётся из рендерера через pose(), т.к. нужен partialTick.
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
