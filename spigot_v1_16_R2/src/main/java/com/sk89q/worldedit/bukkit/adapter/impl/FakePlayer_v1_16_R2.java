/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.bukkit.adapter.impl;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.v1_16_R2.ChatMessageType;
import net.minecraft.server.v1_16_R2.Container;
import net.minecraft.server.v1_16_R2.DamageSource;
import net.minecraft.server.v1_16_R2.Entity;
import net.minecraft.server.v1_16_R2.EntityPlayer;
import net.minecraft.server.v1_16_R2.IChatBaseComponent;
import net.minecraft.server.v1_16_R2.ITileInventory;
import net.minecraft.server.v1_16_R2.PacketPlayInSettings;
import net.minecraft.server.v1_16_R2.PlayerInteractManager;
import net.minecraft.server.v1_16_R2.Statistic;
import net.minecraft.server.v1_16_R2.TileEntitySign;
import net.minecraft.server.v1_16_R2.Vec3D;
import net.minecraft.server.v1_16_R2.WorldServer;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import javax.annotation.Nullable;
import java.util.OptionalInt;
import java.util.UUID;

class FakePlayer_v1_16_R2 extends EntityPlayer {
    private static final GameProfile FAKE_WORLDEDIT_PROFILE = new GameProfile(UUID.nameUUIDFromBytes("worldedit".getBytes()), "[WorldEdit]");

    FakePlayer_v1_16_R2(WorldServer world) {
        super(world.getMinecraftServer(), world, FAKE_WORLDEDIT_PROFILE, new PlayerInteractManager(world));
    }

    @Override
    public Vec3D getPositionVector() {
        return Vec3D.a;
    }

    @Override
    public void tick() {
    }

    @Override
    public void die(DamageSource damagesource) {
    }

    @Override
    public Entity b(WorldServer worldserver, TeleportCause cause) {
        return this;
    }

    @Override
    public OptionalInt openContainer(@Nullable ITileInventory itileinventory) {
        return OptionalInt.empty();
    }

    @Override
    public void a(PacketPlayInSettings packetplayinsettings) {
    }

    @Override
    public void a(IChatBaseComponent ichatbasecomponent, boolean flag) {
    }

    @Override
    public void a(IChatBaseComponent ichatbasecomponent, ChatMessageType chatmessagetype, UUID uuid) {
    }

    @Override
    public void a(Statistic<?> statistic, int i) {
    }

    @Override
    public void a(Statistic<?> statistic) {
    }

    @Override
    public boolean isInvulnerable(DamageSource damagesource) {
        return true;
    }

    @Override
    public boolean q(boolean flag) { // canEat, search for foodData usage
        return true;
    }

    @Override
    public void updateInventory(Container container) {
    }

    @Override
    public void openSign(TileEntitySign tileentitysign) {
    }
}
