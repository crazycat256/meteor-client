/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.InteractItemEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class ElytraBoost extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> dontConsumeFirework = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-consume")
        .description("Prevents fireworks from being consumed when using Elytra Boost.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> fireworkLevel = sgGeneral.add(new IntSetting.Builder()
        .name("firework-duration")
        .description("The duration of the firework.")
        .defaultValue(0)
        .range(0, 255)
        .sliderMax(255)
        .build()
    );

    private final Setting<Boolean> playSound = sgGeneral.add(new BoolSetting.Builder()
        .name("play-sound")
        .description("Plays the firework sound when a boost is triggered.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
        .name("keybind")
        .description("The keybind to boost.")
        .action(this::boost)
        .build()
    );

    private final Setting<Boolean> controls = sgGeneral.add(new BoolSetting.Builder()
        .name("controls")
        .description("Allows you to boost you with the controls.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("The speed to boost you with the controls.")
        .defaultValue(1.7)
        .sliderRange(0, 10)
        .visible(controls::get)
        .build()
    );

    private final Setting<Double> maneuverability = sgGeneral.add(new DoubleSetting.Builder()
        .name("maneuverability")
        .description("The maneuverability of the elytra.")
        .defaultValue(0.025)
        .sliderRange(0, 1)
        .visible(controls::get)
        .build()
    );

    private final List<FireworkRocketEntity> fireworks = new ArrayList<>();

    public ElytraBoost() {
        super(Categories.Movement, "elytra-boost", "Boosts your elytra as if you used a firework.");
    }

    @Override
    public void onDeactivate() {
        fireworks.clear();
    }

    @EventHandler
    private void onInteractItem(InteractItemEvent event) {
        ItemStack itemStack = mc.player.getStackInHand(event.hand);

        if (itemStack.getItem() instanceof FireworkRocketItem && dontConsumeFirework.get()) {
            event.toReturn = ActionResult.PASS;

            boost();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        fireworks.removeIf(Entity::isRemoved);

        if (mc.player != null && mc.player.isFallFlying() && controls.get()) {
            Vec3d direction = mc.player.getRotationVector();
            Vec3d velocity = mc.player.getVelocity();
            double length = velocity.length();
            float yaw = mc.player.getYaw();
            double multiplier = speed.get() * maneuverability.get();

            if (mc.options.forwardKey.isPressed()) {
                velocity = velocity.add(direction.multiply(multiplier));
            }
            if (mc.options.backKey.isPressed()) {
                velocity = velocity.add(direction.multiply(-multiplier));
            }
            if (mc.options.leftKey.isPressed()) {
                velocity = velocity.add(new Vec3d(Math.cos(Math.toRadians(yaw)), 0, Math.sin(Math.toRadians(yaw))).multiply(multiplier));
            }
            if (mc.options.rightKey.isPressed()) {
                velocity = velocity.add(new Vec3d(Math.cos(Math.toRadians(yaw + 180)), 0, Math.sin(Math.toRadians(yaw - 90))).multiply(multiplier));
            }
            if (mc.options.jumpKey.isPressed()) {
                velocity = velocity.add(0, multiplier, 0);
            }
            if (mc.options.sneakKey.isPressed()) {
                velocity = velocity.add(0, -multiplier, 0);
            }

            if (velocity.length() > speed.get()) {
                velocity = velocity.normalize().multiply(length);
            }
            mc.player.setVelocity(velocity);
        }

        /*if (controls.get() && mc.player != null && mc.player.getPose() == EntityPose.FALL_FLYING) {
            Vec3d direction = mc.player.getRotationVector();
            float yaw = mc.player.getYaw();
            double multiplier = speed.get() * 0.05;

            if (mc.options.forwardKey.isPressed()) {
                mc.player.addVelocity(direction.multiply(multiplier));
            }
            if (mc.options.backKey.isPressed()) {
                mc.player.addVelocity(direction.multiply(-multiplier));
            }
            if (mc.options.leftKey.isPressed()) {
                mc.player.addVelocity(new Vec3d(Math.cos(Math.toRadians(yaw + 90)), 0, Math.sin(Math.toRadians(yaw + 90))).multiply(multiplier));
            }
            if (mc.options.rightKey.isPressed()) {
                mc.player.addVelocity(new Vec3d(Math.cos(Math.toRadians(yaw - 90)), 0, Math.sin(Math.toRadians(yaw - 90))).multiply(multiplier));
            }
            if (mc.options.jumpKey.isPressed()) {
                mc.player.addVelocity(0, multiplier, 0);
            }
            if (mc.options.sneakKey.isPressed()) {
                mc.player.addVelocity(0, -multiplier, 0);
            }
        }
        */
    }

    private void boost() {
        if (!Utils.canUpdate()) return;

        if (mc.player.isFallFlying() && mc.currentScreen == null) {
            ItemStack itemStack = Items.FIREWORK_ROCKET.getDefaultStack();
            itemStack.getOrCreateSubNbt("Fireworks").putByte("Flight", fireworkLevel.get().byteValue());

            FireworkRocketEntity entity = new FireworkRocketEntity(mc.world, itemStack, mc.player);
            fireworks.add(entity);
            if (playSound.get()) mc.world.playSoundFromEntity(mc.player, entity, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.AMBIENT, 3.0F, 1.0F);
            mc.world.addEntity(entity);
        }
    }

    public boolean isFirework(FireworkRocketEntity firework) {
        return isActive() && fireworks.contains(firework);
    }
}
