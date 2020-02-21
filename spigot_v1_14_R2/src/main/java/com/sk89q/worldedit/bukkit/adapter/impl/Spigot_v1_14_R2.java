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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.EndTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.IntArrayTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.LongArrayTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.internal.block.BlockStateIdAccess;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.IntegerProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectApplier;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import net.minecraft.server.v1_14_R1.Block;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.BlockStateBoolean;
import net.minecraft.server.v1_14_R1.BlockStateDirection;
import net.minecraft.server.v1_14_R1.BlockStateEnum;
import net.minecraft.server.v1_14_R1.BlockStateInteger;
import net.minecraft.server.v1_14_R1.BlockStateList;
import net.minecraft.server.v1_14_R1.Chunk;
import net.minecraft.server.v1_14_R1.Entity;
import net.minecraft.server.v1_14_R1.EntityTypes;
import net.minecraft.server.v1_14_R1.EnumDirection;
import net.minecraft.server.v1_14_R1.IBlockData;
import net.minecraft.server.v1_14_R1.IBlockState;
import net.minecraft.server.v1_14_R1.INamable;
import net.minecraft.server.v1_14_R1.IRegistry;
import net.minecraft.server.v1_14_R1.ItemStack;
import net.minecraft.server.v1_14_R1.MinecraftKey;
import net.minecraft.server.v1_14_R1.NBTBase;
import net.minecraft.server.v1_14_R1.NBTTagByte;
import net.minecraft.server.v1_14_R1.NBTTagByteArray;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.NBTTagDouble;
import net.minecraft.server.v1_14_R1.NBTTagEnd;
import net.minecraft.server.v1_14_R1.NBTTagFloat;
import net.minecraft.server.v1_14_R1.NBTTagInt;
import net.minecraft.server.v1_14_R1.NBTTagIntArray;
import net.minecraft.server.v1_14_R1.NBTTagList;
import net.minecraft.server.v1_14_R1.NBTTagLong;
import net.minecraft.server.v1_14_R1.NBTTagLongArray;
import net.minecraft.server.v1_14_R1.NBTTagShort;
import net.minecraft.server.v1_14_R1.NBTTagString;
import net.minecraft.server.v1_14_R1.PacketPlayOutEntityStatus;
import net.minecraft.server.v1_14_R1.PacketPlayOutTileEntityData;
import net.minecraft.server.v1_14_R1.PlayerChunk;
import net.minecraft.server.v1_14_R1.TileEntity;
import net.minecraft.server.v1_14_R1.World;
import net.minecraft.server.v1_14_R1.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.CraftServer;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Spigot_v1_14_R2 implements BukkitImplAdapter {

    private final Logger logger = Logger.getLogger(getClass().getCanonicalName());

    private final Field nbtListTagListField;
    private final Method nbtCreateTagMethod;

    // ------------------------------------------------------------------------
    // Code that may break between versions of Minecraft
    // ------------------------------------------------------------------------

    public Spigot_v1_14_R2() throws NoSuchFieldException, NoSuchMethodException {
        // A simple test
        CraftServer.class.cast(Bukkit.getServer());


        if (getDataVersion() != 1957) throw new UnsupportedClassVersionError("Not 1.14.1!");

        // The list of tags on an NBTTagList
        nbtListTagListField = NBTTagList.class.getDeclaredField("list");
        nbtListTagListField.setAccessible(true);

        // The method to create an NBTBase tag given its type ID
        nbtCreateTagMethod = NBTBase.class.getDeclaredMethod("createTag", byte.class);
        nbtCreateTagMethod.setAccessible(true);

        new DataConverters_1_14_R2(getDataVersion(), this).build(ForkJoinPool.commonPool());
    }

    @Override
    public int getDataVersion() {
        return CraftMagicNumbers.INSTANCE.getDataVersion();
    }

    @Override
    public DataFixer getDataFixer() {
        return DataConverters_1_14_R2.INSTANCE;
    }

    /**
     * Read the given NBT data into the given tile entity.
     *
     * @param tileEntity the tile entity
     * @param tag the tag
     */
    private static void readTagIntoTileEntity(NBTTagCompound tag, TileEntity tileEntity) {
        tileEntity.load(tag);
    }

    /**
     * Write the tile entity's NBT data to the given tag.
     *
     * @param tileEntity the tile entity
     * @param tag the tag
     */
    private static void readTileEntityIntoTag(TileEntity tileEntity, NBTTagCompound tag) {
        tileEntity.save(tag);
    }

    /**
     * Get the ID string of the given entity.
     *
     * @param entity the entity
     * @return the entity ID or null if one is not known
     */
    @Nullable
    private static String getEntityId(Entity entity) {
        MinecraftKey minecraftkey = EntityTypes.getName(entity.getEntityType());

        return minecraftkey == null ? null : minecraftkey.toString();
    }

    /**
     * Create an entity using the given entity ID.
     *
     * @param id the entity ID
     * @param world the world
     * @return an entity or null
     */
    @Nullable
    private static Entity createEntityFromId(String id, World world) {
        return EntityTypes.a(id).map(t -> t.a(world)).orElse(null);
    }

    /**
     * Write the given NBT data into the given entity.
     *
     * @param entity the entity
     * @param tag the tag
     */
    private static void readTagIntoEntity(NBTTagCompound tag, Entity entity) {
        entity.f(tag);
    }

    /**
     * Write the entity's NBT data to the given tag.
     *
     * @param entity the entity
     * @param tag the tag
     */
    private static void readEntityIntoTag(Entity entity, NBTTagCompound tag) {
        entity.save(tag);
    }

    @Override
    public BaseBlock getBlock(Location location) {
        checkNotNull(location);

        CraftWorld craftWorld = ((CraftWorld) location.getWorld());
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        org.bukkit.block.Block bukkitBlock = location.getBlock();
        BlockState state = BukkitAdapter.adapt(bukkitBlock.getBlockData());

        // Read the NBT data
        TileEntity te = craftWorld.getHandle().getTileEntity(new BlockPosition(x, y, z));
        if (te != null) {
            NBTTagCompound tag = new NBTTagCompound();
            readTileEntityIntoTag(te, tag); // Load data
            return state.toBaseBlock((CompoundTag) toNative(tag));
        }

        return state.toBaseBlock();
    }

    private static final EnumDirection[] neighbourOrder = {
            EnumDirection.WEST, EnumDirection.EAST,
            EnumDirection.DOWN, EnumDirection.UP,
            EnumDirection.NORTH, EnumDirection.SOUTH
    };

    /**
     * This is a heavily modified function stripped from MC to apply worldedit-modifications.
     *
     * @see World#notifyAndUpdatePhysics
     */
    private void updateNeighbours(WorldServer world, BlockPosition blockposition, Chunk chunk, IBlockData oldBlock, IBlockData newBlock,
            IBlockData actualBlock, SideEffectApplier sideEffectApplier) {
        if (actualBlock == newBlock) {
            if (oldBlock != actualBlock) {
                // This seems to do nothing?
                world.m(blockposition);
            }

            // Remove redundant branches
            if (world.isClientSide || chunk == null || chunk.getState() != null && chunk.getState().a(PlayerChunk.State.TICKING)) {
                if (sideEffectApplier.shouldApply(SideEffect.ENTITY_AI)) {
                    world.notify(blockposition, oldBlock, newBlock, 1 | 2);
                } else {
                    // If we want to skip entity AI, just call the chunk dirty flag.
                    world.getChunkProvider().flagDirty(blockposition);
                }
            }

            if (!world.isClientSide && sideEffectApplier.shouldApply(SideEffect.NEIGHBORS)) {
                if (sideEffectApplier.shouldApply(SideEffect.PLUGIN_EVENTS)) {
                    world.update(blockposition, oldBlock.getBlock());
                } else {
                    // When we don't want events, manually run the physics without them.
                    // Un-nest neighbour updating
                    for (EnumDirection direction : neighbourOrder) {
                        BlockPosition shifted = blockposition.shift(direction);
                        world.getType(shifted).doPhysics(world, shifted, oldBlock.getBlock(), blockposition, false);
                    }
                }

                if (newBlock.isComplexRedstone()) {
                    world.updateAdjacentComparators(blockposition, newBlock.getBlock());
                }
            }

            // Make connection updates optional
            if (sideEffectApplier.shouldApply(SideEffect.CONNECTIONS)) {
                // Seems to notify observers
                oldBlock.b(world, blockposition, 2);
                if (sideEffectApplier.shouldApply(SideEffect.PLUGIN_EVENTS)) {
                    CraftWorld craftWorld = world.getWorld();
                    if (craftWorld != null) {
                        BlockPhysicsEvent
                                event = new BlockPhysicsEvent(craftWorld.getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ()), CraftBlockData.fromData(newBlock));
                        world.getServer().getPluginManager().callEvent(event);
                        if (event.isCancelled()) {
                            return;
                        }
                    }
                }

                // Seems to update connections, eg fences
                newBlock.a(world, blockposition, 2);
                // Re-notifies observers as new block
                newBlock.b(world, blockposition, 2);
            }

            // This appears to just be some villager stuff / debug code
            // world.a(blockposition, oldBlock, actualBlock);
        }
    }

    @Override
    public boolean setBlock(Location location, BlockStateHolder<?> state, SideEffectApplier sideEffectApplier) {
        checkNotNull(location);
        checkNotNull(state);

        CraftWorld craftWorld = ((CraftWorld) location.getWorld());
        WorldServer world = craftWorld.getHandle();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        // First set the block
        Chunk chunk = world.getChunkAt(x >> 4, z >> 4);
        BlockPosition blockPos = new BlockPosition(x, y, z);
        IBlockData old = chunk.getType(blockPos);
        IBlockData newState;
        final int blockStateId = BlockStateIdAccess.getBlockStateId(state.toImmutableState());
        if (BlockStateIdAccess.isValidInternalId(blockStateId)) {
            newState = Block.getByCombinedId(blockStateId);
        } else {
            Block mcBlock = IRegistry.BLOCK.get(MinecraftKey.a(state.getBlockType().getId()));
            newState = mcBlock.getBlockData();
            Map<Property<?>, Object> states = state.getStates();
            newState = applyProperties(mcBlock.getStates(), newState, states);
        }
        IBlockData successState = chunk.setType(blockPos, newState, false, sideEffectApplier.shouldApply(SideEffect.NEIGHBORS));
        boolean successful = successState != null;

        // Create the TileEntity
        if (successful || old == newState) {
            if (state instanceof BaseBlock) {
                CompoundTag nativeTag = ((BaseBlock) state).getNbtData();
                if (nativeTag != null) {
                    // We will assume that the tile entity was created for us,
                    // though we do not do this on the Forge version
                    TileEntity tileEntity = world.getTileEntity(blockPos);
                    if (tileEntity != null) {
                        NBTTagCompound tag = (NBTTagCompound) fromNative(nativeTag);
                        tag.set("x", new NBTTagInt(x));
                        tag.set("y", new NBTTagInt(y));
                        tag.set("z", new NBTTagInt(z));
                        readTagIntoTileEntity(tag, tileEntity); // Load data
                        successful = true;
                    }
                }
            }
        }

        if (successful) {
            if (sideEffectApplier.shouldApply(SideEffect.LIGHTING)) {
                world.getChunkProvider().getLightEngine().a(blockPos); // server should do lighting for us
            }
            updateNeighbours(world, blockPos, chunk, old, newState, newState, sideEffectApplier);
        }

        return successful;
    }

    private static EnumDirection adapt(Direction face) {
        switch (face) {
            case NORTH: return EnumDirection.NORTH;
            case SOUTH: return EnumDirection.SOUTH;
            case WEST: return EnumDirection.WEST;
            case EAST: return EnumDirection.EAST;
            case DOWN: return EnumDirection.DOWN;
            case UP:
            default:
                return EnumDirection.UP;
        }
    }

    private IBlockData applyProperties(BlockStateList<Block, IBlockData> stateContainer, IBlockData newState, Map<Property<?>, Object> states) {
        for (Map.Entry<Property<?>, Object> state : states.entrySet()) {
            IBlockState<?> property = stateContainer.a(state.getKey().getName());
            Comparable<?> value = (Comparable) state.getValue();
            // we may need to adapt this value, depending on the source prop
            if (property instanceof BlockStateDirection) {
                Direction dir = (Direction) value;
                value = adapt(dir);
            } else if (property instanceof BlockStateEnum) {
                String enumName = (String) value;
                value = ((BlockStateEnum<?>) property).b((String) value).orElseGet(() -> {
                    throw new IllegalStateException("Enum property " + property.a() + " does not contain " + enumName);
                });
            }

            newState = newState.set((IBlockState) property, (Comparable) value);
        }
        return newState;
    }

    @Override
    public void notifyAndLightBlock(Location position, BlockState previousType, SideEffectApplier sideEffectApplier) {
        CraftWorld craftWorld = ((CraftWorld) position.getWorld());

        BlockPosition blockPosition = new BlockPosition(position.getBlockX(), position.getBlockY(), position.getBlockZ());
        IBlockData oldData = ((CraftBlockData) BukkitAdapter.adapt(previousType)).getState();
        IBlockData newData = craftWorld.getHandle().getType(blockPosition);

        if (sideEffectApplier.shouldApply(SideEffect.LIGHTING)) {
            craftWorld.getHandle().getChunkProvider().getLightEngine().a(blockPosition); // server should do lighting for us
        }
        updateNeighbours(craftWorld.getHandle(), blockPosition, null, oldData, newData, newData, sideEffectApplier); // Update
    }

    @Override
    public BaseEntity getEntity(org.bukkit.entity.Entity entity) {
        checkNotNull(entity);

        CraftEntity craftEntity = ((CraftEntity) entity);
        Entity mcEntity = craftEntity.getHandle();

        String id = getEntityId(mcEntity);

        if (id != null) {
            NBTTagCompound tag = new NBTTagCompound();
            readEntityIntoTag(mcEntity, tag);
            return new BaseEntity(com.sk89q.worldedit.world.entity.EntityTypes.get(id), (CompoundTag) toNative(tag));
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public org.bukkit.entity.Entity createEntity(Location location, BaseEntity state) {
        checkNotNull(location);
        checkNotNull(state);

        CraftWorld craftWorld = ((CraftWorld) location.getWorld());
        WorldServer worldServer = craftWorld.getHandle();

        Entity createdEntity = createEntityFromId(state.getType().getId(), craftWorld.getHandle());

        if (createdEntity != null) {
            CompoundTag nativeTag = state.getNbtData();
            if (nativeTag != null) {
                NBTTagCompound tag = (NBTTagCompound) fromNative(nativeTag);
                for (String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                    tag.remove(name);
                }
                readTagIntoEntity(tag, createdEntity);
            }

            createdEntity.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

            worldServer.addEntity(createdEntity, SpawnReason.CUSTOM);
            return createdEntity.getBukkitEntity();
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, ? extends Property<?>> getProperties(BlockType blockType) {
        Map<String, Property<?>> properties = Maps.newTreeMap(String::compareTo);
        Block block = IRegistry.BLOCK.get(MinecraftKey.a(blockType.getId()));
        if (block == null) {
            logger.warning("Failed to find properties for " + blockType.getId());
            return properties;
        }
        BlockStateList<Block, IBlockData> blockStateList = block.getStates();
        for (IBlockState state : blockStateList.d()) {
            Property property;
            if (state instanceof BlockStateBoolean) {
                property = new BooleanProperty(state.a(), ImmutableList.copyOf(state.d()));
            } else if (state instanceof BlockStateDirection) {
                property = new DirectionalProperty(state.a(),
                        (List<Direction>) state.d().stream().map(e -> Direction.valueOf(((INamable) e).getName().toUpperCase())).collect(Collectors.toList()));
            } else if (state instanceof BlockStateEnum) {
                property = new EnumProperty(state.a(),
                        (List<String>) state.d().stream().map(e -> ((INamable) e).getName()).collect(Collectors.toList()));
            } else if (state instanceof BlockStateInteger) {
                property = new IntegerProperty(state.a(), ImmutableList.copyOf(state.d()));
            } else {
                throw new IllegalArgumentException("WorldEdit needs an update to support " + state.getClass().getSimpleName());
            }

            properties.put(property.getName(), property);
        }
        return properties;
    }

    @Override
    public void sendFakeNBT(Player player, BlockVector3 pos, CompoundTag nbtData) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutTileEntityData(
                new BlockPosition(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()),
                7,
                (NBTTagCompound) fromNative(nbtData)
        ));
    }

    @Override
    public void sendFakeOP(Player player) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityStatus(
                ((CraftPlayer) player).getHandle(), (byte) 28
        ));
    }

    @Override
    public org.bukkit.inventory.ItemStack adapt(BaseItemStack item) {
        ItemStack stack = new ItemStack(IRegistry.ITEM.get(MinecraftKey.a(item.getType().getId())), item.getAmount());
        stack.setTag(((NBTTagCompound) fromNative(item.getNbtData())));
        return CraftItemStack.asCraftMirror(stack);
    }

    @Override
    public BaseItemStack adapt(org.bukkit.inventory.ItemStack itemStack) {
        final ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
        final BaseItemStack weStack = new BaseItemStack(BukkitAdapter.asItemType(itemStack.getType()), itemStack.getAmount());
        weStack.setNbtData(((CompoundTag) toNative(nmsStack.getTag())));
        return weStack;
    }

    private static final Collection<SideEffect> SUPPORTED_SIDE_EFFECTS = ImmutableSet.copyOf(EnumSet.of(
            SideEffect.NEIGHBORS,
            SideEffect.LIGHTING,
            SideEffect.CONNECTIONS,
            SideEffect.ENTITY_AI,
            SideEffect.PLUGIN_EVENTS
    ));

    @Override
    public Collection<SideEffect> getSupportedSideEffects() {
        return SUPPORTED_SIDE_EFFECTS;
    }

    // ------------------------------------------------------------------------
    // Code that is less likely to break
    // ------------------------------------------------------------------------

    /**
     * Converts from a non-native NMS NBT structure to a native WorldEdit NBT
     * structure.
     *
     * @param foreign non-native NMS NBT structure
     * @return native WorldEdit NBT structure
     */
    @SuppressWarnings("unchecked")
    Tag toNative(NBTBase foreign) {
        if (foreign == null) {
            return null;
        }
        if (foreign instanceof NBTTagCompound) {
            Map<String, Tag> values = new HashMap<>();
            Set<String> foreignKeys = ((NBTTagCompound) foreign).getKeys(); // map.keySet

            for (String str : foreignKeys) {
                NBTBase base = ((NBTTagCompound) foreign).get(str);
                values.put(str, toNative(base));
            }
            return new CompoundTag(values);
        } else if (foreign instanceof NBTTagByte) {
            return new ByteTag(((NBTTagByte) foreign).asByte());
        } else if (foreign instanceof NBTTagByteArray) {
            return new ByteArrayTag(((NBTTagByteArray) foreign).getBytes()); // data
        } else if (foreign instanceof NBTTagDouble) {
            return new DoubleTag(((NBTTagDouble) foreign).asDouble()); // getDouble
        } else if (foreign instanceof NBTTagFloat) {
            return new FloatTag(((NBTTagFloat) foreign).asFloat());
        } else if (foreign instanceof NBTTagInt) {
            return new IntTag(((NBTTagInt) foreign).asInt());
        } else if (foreign instanceof NBTTagIntArray) {
            return new IntArrayTag(((NBTTagIntArray) foreign).getInts()); // data
        } else if (foreign instanceof NBTTagLongArray) {
            return new LongArrayTag(((NBTTagLongArray) foreign).getLongs()); // data
        } else if (foreign instanceof NBTTagList) {
            try {
                return toNativeList((NBTTagList) foreign);
            } catch (Throwable e) {
                logger.log(Level.WARNING, "Failed to convert NBTTagList", e);
                return new ListTag(ByteTag.class, new ArrayList<ByteTag>());
            }
        } else if (foreign instanceof NBTTagLong) {
            return new LongTag(((NBTTagLong) foreign).asLong());
        } else if (foreign instanceof NBTTagShort) {
            return new ShortTag(((NBTTagShort) foreign).asShort());
        } else if (foreign instanceof NBTTagString) {
            return new StringTag(foreign.asString());
        } else if (foreign instanceof NBTTagEnd) {
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
        List<Tag> values = new ArrayList<>();
        int type = foreign.a_();

        List foreignList;
        foreignList = (List) nbtListTagListField.get(foreign);
        for (int i = 0; i < foreign.size(); i++) {
            NBTBase element = (NBTBase) foreignList.get(i);
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
    NBTBase fromNative(Tag foreign) {
        if (foreign == null) {
            return null;
        }
        if (foreign instanceof CompoundTag) {
            NBTTagCompound tag = new NBTTagCompound();
            for (Map.Entry<String, Tag> entry : ((CompoundTag) foreign)
                    .getValue().entrySet()) {
                tag.set(entry.getKey(), fromNative(entry.getValue()));
            }
            return tag;
        } else if (foreign instanceof ByteTag) {
            return new NBTTagByte(((ByteTag) foreign).getValue());
        } else if (foreign instanceof ByteArrayTag) {
            return new NBTTagByteArray(((ByteArrayTag) foreign).getValue());
        } else if (foreign instanceof DoubleTag) {
            return new NBTTagDouble(((DoubleTag) foreign).getValue());
        } else if (foreign instanceof FloatTag) {
            return new NBTTagFloat(((FloatTag) foreign).getValue());
        } else if (foreign instanceof IntTag) {
            return new NBTTagInt(((IntTag) foreign).getValue());
        } else if (foreign instanceof IntArrayTag) {
            return new NBTTagIntArray(((IntArrayTag) foreign).getValue());
        } else if (foreign instanceof LongArrayTag) {
            return new NBTTagLongArray(((LongArrayTag) foreign).getValue());
        } else if (foreign instanceof ListTag) {
            NBTTagList tag = new NBTTagList();
            ListTag foreignList = (ListTag) foreign;
            for (Tag t : foreignList.getValue()) {
                tag.add(fromNative(t));
            }
            return tag;
        } else if (foreign instanceof LongTag) {
            return new NBTTagLong(((LongTag) foreign).getValue());
        } else if (foreign instanceof ShortTag) {
            return new NBTTagShort(((ShortTag) foreign).getValue());
        } else if (foreign instanceof StringTag) {
            return new NBTTagString(((StringTag) foreign).getValue());
        } else if (foreign instanceof EndTag) {
            try {
                return (NBTBase) nbtCreateTagMethod.invoke(null, (byte) 0);
            } catch (Exception e) {
                return null;
            }
        } else {
            throw new IllegalArgumentException("Don't know how to make NMS " + foreign.getClass().getCanonicalName());
        }
    }

}
