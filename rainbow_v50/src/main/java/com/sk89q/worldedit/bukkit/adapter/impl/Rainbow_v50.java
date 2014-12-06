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

import PluginBukkitBridge.FakeCraftServer;
import PluginBukkitBridge.FakeWorld;
import PluginBukkitBridge.Util;
import PluginBukkitBridge.block.FakeBlock;
import PluginBukkitBridge.entity.FakeEntity;
import WrapperObjects.PluginHelper;
import WrapperObjects.WorldWrapper;
import WrapperObjects.Entities.EntityWrapper;

import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.EndTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.IntArrayTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.internal.Constants;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import javax.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import joebkt.*;
import static com.google.common.base.Preconditions.checkNotNull;

public final class Rainbow_v50 implements BukkitImplAdapter {

    private final Logger logger = Logger.getLogger(getClass().getCanonicalName());

    private final Field nbtListTagListField;
    private final Method nbtCreateTagMethod;

    // ------------------------------------------------------------------------
    // Code that may break between versions of Minecraft
    // ------------------------------------------------------------------------

    public Rainbow_v50() throws NoSuchFieldException, NoSuchMethodException {
        // A simple test
        FakeCraftServer.class.cast(Bukkit.getServer());

        // The list of tags on an NBTTagList
        nbtListTagListField = NBTTagList.class.getDeclaredField("c_list");
        nbtListTagListField.setAccessible(true);

        // The method to create an NBTBase tag given its type ID
        nbtCreateTagMethod = NBTRelatedTypeIndicator.class.getDeclaredMethod("a", byte.class);
        nbtCreateTagMethod.setAccessible(true);
    }

    /**
     * Get the {@code Block} object from Minecraft given the ID.
     *
     * @param type the type
     * @return the block
     */
    private static BlockObject getBlockFromTypeId(int type) {
        return BlockObject.c(type);
    }

    /**
     * Read the given NBT data into the given tile entity.
     *
     * @param tileEntity the tile entity
     * @param tag the tag
     */
    private static void readTagIntoTileEntity(NBTTagCompound tag, SpecialBlockRelatedClass tileEntity) {
        tileEntity.populateClassFromNBT(tag);
    }

    /**
     * Write the tile entity's NBT data to the given tag.
     *
     * @param tileEntity the tile entity
     * @param tag the tag
     */
    private static void readTileEntityIntoTag(SpecialBlockRelatedClass tileEntity, NBTTagCompound tag) {
        tileEntity.serializeClassToNBT(tag);
    }

    /**
     * Get the ID string of the given entity.
     *
     * @param entity the entity
     * @return the entity ID or null if one is not known
     */
    @Nullable
    private static String getEntityId(EntityGeneric entity) {
        return EntityClassMapper.b(entity);
    }

    /**
     * Create an entity using the given entity ID.
     *
     * @param id the entity ID
     * @param world the world
     * @return an entity or null
     */
    @Nullable
    private static EntityGeneric createEntityFromId(String id, World world) {
        return EntityClassMapper.getNewEntityInstance(id, world);
    }

    /**
     * Write the given NBT data into the given entity.
     *
     * @param entity the entity
     * @param tag the tag
     */
    private static void readTagIntoEntity(NBTTagCompound tag, EntityGeneric entity) {
        entity.loadEntity(tag);
    }

    /**
     * Write the entity's NBT data to the given tag.
     *
     * @param entity the entity
     * @param tag the tag
     */
    private static void readEntityIntoTag(EntityGeneric entity, NBTTagCompound tag) {
        entity.saveEntity(tag);
    }

    // ------------------------------------------------------------------------
    // Code that is less likely to break
    // ------------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    @Override
    public int getBlockId(Material material) {
        return material.getId();
    }

    @SuppressWarnings("deprecation")
    @Override
    public Material getMaterial(int id) {
        return Material.getMaterial(id);
    }

    @Override
    public int getBiomeId(Biome biome) {
        BiomeBase mcBiome = PluginHelper.TranslateBiomeBase(Util.wrapBiome(biome));
        return mcBiome != null ? mcBiome.id : 0;
    }

    @Override
    public Biome getBiome(int id) {
        BiomeBase mcBiome = BiomeBase.getBiomeByIdxWithDefault(id, BiomeBase.Ocean);
        return Util.wrapBiome(PluginHelper.TranslateBiomeBase(mcBiome)); // Defaults to ocean if it's an invalid ID
    }

    @SuppressWarnings("deprecation")
    @Override
    public BaseBlock getBlock(Location location) {
        checkNotNull(location);

        World world = ((WorldWrapper) ((FakeWorld) location.getWorld()).world).world;
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        IntegerCoordinates coords = new IntegerCoordinates(x, y, z);

        org.bukkit.block.Block bukkitBlock = location.getBlock();
        BaseBlock block = new BaseBlock(bukkitBlock.getTypeId(), bukkitBlock.getData());

        // Read the NBT data
        SpecialBlockRelatedClass te = world.getSpecialBlockAtCoord(coords);
        if (te != null) {
            NBTTagCompound tag = new NBTTagCompound();
            readTileEntityIntoTag(te, tag); // Load data
            block.setNbtData((CompoundTag) toNative(tag));
        }

        return block;
    }

    @Override
    public boolean setBlock(Location location, BaseBlock block, boolean notifyAndLight) {
        checkNotNull(location);
        checkNotNull(block);

        World world = ((WorldWrapper) ((FakeWorld) location.getWorld()).world).world;
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        IntegerCoordinates coords = new IntegerCoordinates(x, y, z);
        BlockObject nativeBlock = getBlockFromTypeId(block.getId());

        // Set the block ID and data
        boolean changed = world.setTypeIdWithFlag(coords, nativeBlock.getBlockStateFromSubtype(block.getData()), 0);

        // Copy NBT data for the block
        CompoundTag nativeTag = block.getNbtData();
        if (nativeTag != null) {
            // We will assume that the tile entity was created for us,
            // though we do not do this on the Forge version
            SpecialBlockRelatedClass tileEntity = world.getSpecialBlockAtCoord(coords);
            if (tileEntity != null) {
                NBTTagCompound tag = (NBTTagCompound) fromNative(nativeTag);
                tag.putInt("x", x);
                tag.putInt("y", y);
                tag.putInt("z", z);
                readTagIntoTileEntity(tag, tileEntity); // Load data
            }
        }

        // Update, notify, and light
        if (changed && notifyAndLight) {
            world.notifyBlockDataChangedAt(coords);
            world.updateNeighbors(coords, nativeBlock);
        }

        return changed;
    }

    @Override
    public BaseEntity getEntity(org.bukkit.entity.Entity entity) {
        checkNotNull(entity);

        EntityGeneric mcEntity = ((EntityWrapper) ((FakeEntity) entity).m_ent).ent;

        String id = getEntityId(mcEntity);

        if (id != null) {
            NBTTagCompound tag = new NBTTagCompound();
            readEntityIntoTag(mcEntity, tag);
            return new BaseEntity(id, (CompoundTag) toNative(tag));
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public org.bukkit.entity.Entity createEntity(Location location, BaseEntity state) {
        checkNotNull(location);
        checkNotNull(state);

        World world = ((WorldWrapper) ((FakeWorld) location.getWorld()).world).world;

        EntityGeneric createdEntity = createEntityFromId(state.getTypeId(), world);

        if (createdEntity != null) {
            CompoundTag nativeTag = state.getNbtData();
            if (nativeTag != null) {
                NBTTagCompound tag = (NBTTagCompound) fromNative(nativeTag);
                for (String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                    tag.removeElement(name);
                }
                readTagIntoEntity(tag, createdEntity);
            }

            createdEntity.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

            world.addEntity(createdEntity);
            return new FakeEntity(new EntityWrapper(createdEntity));
        } else {
            return null;
        }
    }

    /**
     * Converts from a non-native NMS NBT structure to a native WorldEdit NBT
     * structure.
     *
     * @param foreign non-native NMS NBT structure
     * @return native WorldEdit NBT structure
     */
    @SuppressWarnings("unchecked")
    private Tag toNative(NBTRelatedTypeIndicator foreign) {
        if (foreign == null) {
            return null;
        }
        if (foreign instanceof NBTTagCompound) {
            Map<String, Tag> values = new HashMap<String, Tag>();
            Collection<Object> foreignKeys = ((NBTTagCompound) foreign).c();

            for (Object obj : foreignKeys) {
                String key = (String) obj;
                NBTRelatedTypeIndicator base = ((NBTTagCompound) foreign).a(key);
                values.put(key, toNative(base));
            }
            return new CompoundTag(values);
        } else if (foreign instanceof ByteNumerical) {
            return new ByteTag(((ByteNumerical) foreign).getByteVersion()); // getByte
        } else if (foreign instanceof NBTDataIO) { // NBTByteArray
            return new ByteArrayTag(((NBTDataIO) foreign).c()); // data
        } else if (foreign instanceof DoubleNumerical) {
            return new DoubleTag(((DoubleNumerical) foreign).getDoubleVersion()); // getDouble
        } else if (foreign instanceof FloatNumerical) {
            return new FloatTag(((FloatNumerical) foreign).h()); // getFloat
        } else if (foreign instanceof IntNumerical) {
            return new IntTag(((IntNumerical) foreign).getIntegerVersion()); // getInt
        } else if (foreign instanceof ft) { // NBTIntArray
            return new IntArrayTag(((ft) foreign).c()); // data
        } else if (foreign instanceof NBTTagList) {
            try {
                return toNativeList((NBTTagList) foreign);
            } catch (Throwable e) {
                logger.log(Level.WARNING, "Failed to convert NBTTagList", e);
                return new ListTag(ByteTag.class, new ArrayList<ByteTag>());
            }
        } else if (foreign instanceof LongNumerical) {
            return new LongTag(((LongNumerical) foreign).getLongVersion()); // getLong
        } else if (foreign instanceof ShortNumerical) {
            return new ShortTag(((ShortNumerical) foreign).getShortVersion()); // getShort
        } else if (foreign instanceof StringData) {
            return new StringTag(((StringData) foreign).a_()); // data
        } else if (foreign instanceof NbtEndMarker) {
            return new EndTag();
        } else {
            throw new IllegalArgumentException("Don't know how to make native " + foreign.getClass().getCanonicalName());
        }
    }

    /**
     * Convert a foreign NBT list tag into a native WorldEdit one.
     *
     * @param foreign the foreign tag
     * @return the converted tag
     * @throws NoSuchFieldException on error
     * @throws SecurityException on error
     * @throws IllegalArgumentException on error
     * @throws IllegalAccessException on error
     */
    private ListTag toNativeList(NBTTagList foreign) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        List<Tag> values = new ArrayList<Tag>();
        int type = foreign.f();

        List<?> foreignList;
        foreignList = (List<?>) nbtListTagListField.get(foreign);
        for (int i = 0; i < foreignList.size(); i++) {
            NBTRelatedTypeIndicator element = (NBTRelatedTypeIndicator) foreignList.get(i);
            values.add(toNative(element)); // List elements shouldn't have names
        }

        Class<? extends Tag> cls = NBTConstants.getClassFromType(type);
        return new ListTag(cls, values);
    }

    /**
     * Converts a WorldEdit-native NBT structure to a NMS structure.
     *
     * @param foreign structure to convert
     * @return non-native structure
     */
    private NBTRelatedTypeIndicator fromNative(Tag foreign) {
        if (foreign == null) {
            return null;
        }
        if (foreign instanceof CompoundTag) {
            NBTTagCompound tag = new NBTTagCompound();
            for (Map.Entry<String, Tag> entry : ((CompoundTag) foreign)
                    .getValue().entrySet()) {
                tag.putTypeIndicator(entry.getKey(), fromNative(entry.getValue()));
            }
            return tag;
        } else if (foreign instanceof ByteTag) {
            return new ByteNumerical(((ByteTag) foreign).getValue());
        } else if (foreign instanceof ByteArrayTag) {
            return new NBTDataIO(((ByteArrayTag) foreign).getValue());
        } else if (foreign instanceof DoubleTag) {
            return new DoubleNumerical(((DoubleTag) foreign).getValue());
        } else if (foreign instanceof FloatTag) {
            return new FloatNumerical(((FloatTag) foreign).getValue());
        } else if (foreign instanceof IntTag) {
            return new IntNumerical(((IntTag) foreign).getValue());
        } else if (foreign instanceof IntArrayTag) {
            return new ft(((IntArrayTag) foreign).getValue());
        } else if (foreign instanceof ListTag) {
            NBTTagList tag = new NBTTagList();
            ListTag foreignList = (ListTag) foreign;
            for (Tag t : foreignList.getValue()) {
                tag.addToTagListMaybe(fromNative(t));
            }
            return tag;
        } else if (foreign instanceof LongTag) {
            return new LongNumerical(((LongTag) foreign).getValue());
        } else if (foreign instanceof ShortTag) {
            return new ShortNumerical(((ShortTag) foreign).getValue());
        } else if (foreign instanceof StringTag) {
            return new StringData(((StringTag) foreign).getValue());
        } else if (foreign instanceof EndTag) {
            try {
                return (NBTRelatedTypeIndicator) nbtCreateTagMethod.invoke(null, (byte) 0);
            } catch (Exception e) {
                return null;
            }
        } else {
            throw new IllegalArgumentException("Don't know how to make NMS " + foreign.getClass().getCanonicalName());
        }
    }

}