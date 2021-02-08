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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extension.platform.Watchdog;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.internal.block.BlockStateIdAccess;
import com.sk89q.worldedit.internal.wna.WorldNativeAccess;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.IntegerProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.io.file.SafeFiles;
import com.sk89q.worldedit.util.nbt.BinaryTag;
import com.sk89q.worldedit.util.nbt.ByteArrayBinaryTag;
import com.sk89q.worldedit.util.nbt.ByteBinaryTag;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.util.nbt.DoubleBinaryTag;
import com.sk89q.worldedit.util.nbt.EndBinaryTag;
import com.sk89q.worldedit.util.nbt.FloatBinaryTag;
import com.sk89q.worldedit.util.nbt.IntArrayBinaryTag;
import com.sk89q.worldedit.util.nbt.IntBinaryTag;
import com.sk89q.worldedit.util.nbt.ListBinaryTag;
import com.sk89q.worldedit.util.nbt.LongArrayBinaryTag;
import com.sk89q.worldedit.util.nbt.LongBinaryTag;
import com.sk89q.worldedit.util.nbt.ShortBinaryTag;
import com.sk89q.worldedit.util.nbt.StringBinaryTag;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.RegenOptions;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.item.ItemType;
import net.minecraft.server.v1_16_R3.BiomeBase;
import net.minecraft.server.v1_16_R3.BiomeStorage;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.BlockStateBoolean;
import net.minecraft.server.v1_16_R3.BlockStateDirection;
import net.minecraft.server.v1_16_R3.BlockStateEnum;
import net.minecraft.server.v1_16_R3.BlockStateInteger;
import net.minecraft.server.v1_16_R3.BlockStateList;
import net.minecraft.server.v1_16_R3.Blocks;
import net.minecraft.server.v1_16_R3.Chunk;
import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R3.ChunkProviderServer;
import net.minecraft.server.v1_16_R3.ChunkStatus;
import net.minecraft.server.v1_16_R3.Convertable;
import net.minecraft.server.v1_16_R3.DedicatedServer;
import net.minecraft.server.v1_16_R3.DynamicOpsNBT;
import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.EnumDirection;
import net.minecraft.server.v1_16_R3.EnumHand;
import net.minecraft.server.v1_16_R3.EnumInteractionResult;
import net.minecraft.server.v1_16_R3.GeneratorSettings;
import net.minecraft.server.v1_16_R3.IAsyncTaskHandler;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.IBlockState;
import net.minecraft.server.v1_16_R3.IChunkAccess;
import net.minecraft.server.v1_16_R3.INamable;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.IRegistryCustom;
import net.minecraft.server.v1_16_R3.Item;
import net.minecraft.server.v1_16_R3.ItemActionContext;
import net.minecraft.server.v1_16_R3.ItemStack;
import net.minecraft.server.v1_16_R3.MinecraftKey;
import net.minecraft.server.v1_16_R3.MinecraftServer;
import net.minecraft.server.v1_16_R3.MovingObjectPositionBlock;
import net.minecraft.server.v1_16_R3.NBTBase;
import net.minecraft.server.v1_16_R3.NBTTagByte;
import net.minecraft.server.v1_16_R3.NBTTagByteArray;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NBTTagDouble;
import net.minecraft.server.v1_16_R3.NBTTagEnd;
import net.minecraft.server.v1_16_R3.NBTTagFloat;
import net.minecraft.server.v1_16_R3.NBTTagInt;
import net.minecraft.server.v1_16_R3.NBTTagIntArray;
import net.minecraft.server.v1_16_R3.NBTTagList;
import net.minecraft.server.v1_16_R3.NBTTagLong;
import net.minecraft.server.v1_16_R3.NBTTagLongArray;
import net.minecraft.server.v1_16_R3.NBTTagShort;
import net.minecraft.server.v1_16_R3.NBTTagString;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityStatus;
import net.minecraft.server.v1_16_R3.PacketPlayOutTileEntityData;
import net.minecraft.server.v1_16_R3.PlayerChunk;
import net.minecraft.server.v1_16_R3.RegistryReadOps;
import net.minecraft.server.v1_16_R3.ResourceKey;
import net.minecraft.server.v1_16_R3.SystemUtils;
import net.minecraft.server.v1_16_R3.TileEntity;
import net.minecraft.server.v1_16_R3.Vec3D;
import net.minecraft.server.v1_16_R3.World;
import net.minecraft.server.v1_16_R3.WorldDataServer;
import net.minecraft.server.v1_16_R3.WorldDimension;
import net.minecraft.server.v1_16_R3.WorldLoadListener;
import net.minecraft.server.v1_16_R3.WorldServer;
import net.minecraft.server.v1_16_R3.WorldSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.generator.ChunkGenerator;
import org.spigotmc.SpigotConfig;
import org.spigotmc.WatchdogThread;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public final class Spigot_v1_16_R3 implements BukkitImplAdapter {

    private final Logger logger = Logger.getLogger(getClass().getCanonicalName());

    private final Field nbtListTagListField;
    private final Field serverWorldsField;
    private final Method getChunkFutureMethod;
    private final Field chunkProviderExecutorField;
    private final Watchdog watchdog;

    // ------------------------------------------------------------------------
    // Code that may break between versions of Minecraft
    // ------------------------------------------------------------------------

    public Spigot_v1_16_R3() throws NoSuchFieldException, NoSuchMethodException {
        // A simple test
        CraftServer.class.cast(Bukkit.getServer());


        int dataVersion = CraftMagicNumbers.INSTANCE.getDataVersion();
        if (dataVersion != 2584 && dataVersion != 2586) throw new UnsupportedClassVersionError("Not 1.16.4/1.16.5!");

        // The list of tags on an NBTTagList
        nbtListTagListField = NBTTagList.class.getDeclaredField("list");
        nbtListTagListField.setAccessible(true);

        serverWorldsField = CraftServer.class.getDeclaredField("worlds");
        serverWorldsField.setAccessible(true);

        getChunkFutureMethod = ChunkProviderServer.class.getDeclaredMethod("getChunkFutureMainThread",
            int.class, int.class, ChunkStatus.class, boolean.class);
        getChunkFutureMethod.setAccessible(true);

        chunkProviderExecutorField = ChunkProviderServer.class.getDeclaredField("serverThreadQueue");
        chunkProviderExecutorField.setAccessible(true);

        new DataConverters_1_16_R3(CraftMagicNumbers.INSTANCE.getDataVersion(), this).build(ForkJoinPool.commonPool());

        Watchdog watchdog;
        try {
            Class.forName("org.spigotmc.WatchdogThread");
            watchdog = new SpigotWatchdog();
        } catch (ClassNotFoundException e) {
            try {
                watchdog = new MojangWatchdog(((CraftServer) Bukkit.getServer()).getServer());
            } catch (NoSuchFieldException ex) {
                watchdog = null;
            }
        }
        this.watchdog = watchdog;

        try {
            Class.forName("org.spigotmc.SpigotConfig");
            SpigotConfig.config.set("world-settings.worldeditregentempworld.verbose", false);
        } catch (ClassNotFoundException ignored) {}
    }

    @Override
    public DataFixer getDataFixer() {
        return DataConverters_1_16_R3.INSTANCE;
    }

    /**
     * Read the given NBT data into the given tile entity.
     *
     * @param tileEntity the tile entity
     * @param tag the tag
     */
    static void readTagIntoTileEntity(NBTTagCompound tag, TileEntity tileEntity) {
        tileEntity.load(tileEntity.getBlock(), tag);
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
        entity.load(tag);
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

    private static Block getBlockFromType(BlockType blockType) {
        return IRegistry.BLOCK.get(MinecraftKey.a(blockType.getId()));
    }

    private static Item getItemFromType(ItemType itemType) {
        return IRegistry.ITEM.get(MinecraftKey.a(itemType.getId()));
    }

    @Override
    public OptionalInt getInternalBlockStateId(BlockData data) {
        IBlockData state = ((CraftBlockData) data).getState();
        int combinedId = Block.getCombinedId(state);
        return combinedId == 0 && state.getBlock() != Blocks.AIR ? OptionalInt.empty() : OptionalInt.of(combinedId);
    }

    @Override
    public OptionalInt getInternalBlockStateId(BlockState state) {
        Block mcBlock = getBlockFromType(state.getBlockType());
        IBlockData newState = mcBlock.getBlockData();
        Map<Property<?>, Object> states = state.getStates();
        newState = applyProperties(mcBlock.getStates(), newState, states);
        final int combinedId = Block.getCombinedId(newState);
        return combinedId == 0 && state.getBlockType() != BlockTypes.AIR ? OptionalInt.empty() : OptionalInt.of(combinedId);
    }

    @Override
    public BaseBlock getBlock(Location location) {
        checkNotNull(location);

        CraftWorld craftWorld = ((CraftWorld) location.getWorld());
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        final WorldServer handle = craftWorld.getHandle();
        Chunk chunk = handle.getChunkAt(x >> 4, z >> 4);
        final BlockPosition blockPos = new BlockPosition(x, y, z);
        final IBlockData blockData = chunk.getType(blockPos);
        int internalId = Block.getCombinedId(blockData);
        BlockState state = BlockStateIdAccess.getBlockStateById(internalId);
        if (state == null) {
            org.bukkit.block.Block bukkitBlock = location.getBlock();
            state = BukkitAdapter.adapt(bukkitBlock.getBlockData());
        }

        // Read the NBT data
        TileEntity te = chunk.a(blockPos, Chunk.EnumTileEntityState.CHECK);
        if (te != null) {
            NBTTagCompound tag = new NBTTagCompound();
            readTileEntityIntoTag(te, tag); // Load data
            return state.toBaseBlock((CompoundBinaryTag) toNative(tag));
        }

        return state.toBaseBlock();
    }

    @Override
    public WorldNativeAccess<?, ?, ?> createWorldNativeAccess(org.bukkit.World world) {
        return new WorldNativeAccess_v1_16_R3(this,
            new WeakReference<>(((CraftWorld) world).getHandle()));
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
                    throw new IllegalStateException("Enum property " + property.getName() + " does not contain " + enumName);
                });
            }

            newState = newState.set((IBlockState) property, (Comparable) value);
        }
        return newState;
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
            return new BaseEntity(com.sk89q.worldedit.world.entity.EntityTypes.get(id), (CompoundBinaryTag) toNative(tag));
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
            CompoundBinaryTag nativeTag = state.getNbt();
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

    @Override
    public Component getRichBlockName(BlockType blockType) {
        return TranslatableComponent.of(getBlockFromType(blockType).i());
    }

    @Override
    public Component getRichItemName(ItemType itemType) {
        return TranslatableComponent.of(getItemFromType(itemType).getName());
    }

    @Override
    public Component getRichItemName(BaseItemStack itemStack) {
        return TranslatableComponent.of(CraftItemStack.asNMSCopy(BukkitAdapter.adapt(itemStack)).j());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, ? extends Property<?>> getProperties(BlockType blockType) {
        Map<String, Property<?>> properties = Maps.newTreeMap(String::compareTo);
        Block block = getBlockFromType(blockType);
        BlockStateList<Block, IBlockData> blockStateList = block.getStates();
        for (IBlockState state : blockStateList.d()) {
            Property property;
            if (state instanceof BlockStateBoolean) {
                property = new BooleanProperty(state.getName(), ImmutableList.copyOf(state.getValues()));
            } else if (state instanceof BlockStateDirection) {
                property = new DirectionalProperty(state.getName(),
                        (List<Direction>) state.getValues().stream().map(e -> Direction.valueOf(((INamable) e).getName().toUpperCase())).collect(Collectors.toList()));
            } else if (state instanceof BlockStateEnum) {
                property = new EnumProperty(state.getName(),
                        (List<String>) state.getValues().stream().map(e -> ((INamable) e).getName()).collect(Collectors.toList()));
            } else if (state instanceof BlockStateInteger) {
                property = new IntegerProperty(state.getName(), ImmutableList.copyOf(state.getValues()));
            } else {
                throw new IllegalArgumentException("WorldEdit needs an update to support " + state.getClass().getSimpleName());
            }

            properties.put(property.getName(), property);
        }
        return properties;
    }

    @Override
    public void sendFakeNBT(Player player, BlockVector3 pos, CompoundBinaryTag nbtData) {
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
        stack.setTag(((NBTTagCompound) fromNative(item.getNbt())));
        return CraftItemStack.asCraftMirror(stack);
    }

    @Override
    public BaseItemStack adapt(org.bukkit.inventory.ItemStack itemStack) {
        final ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
        final BaseItemStack weStack = new BaseItemStack(BukkitAdapter.asItemType(itemStack.getType()), itemStack.getAmount());
        weStack.setNbt(((CompoundBinaryTag) toNative(nmsStack.getTag())));
        return weStack;
    }

    private LoadingCache<WorldServer, FakePlayer_v1_16_R3> fakePlayers
            = CacheBuilder.newBuilder().weakKeys().softValues().build(CacheLoader.from(FakePlayer_v1_16_R3::new));

    @Override
    public boolean simulateItemUse(org.bukkit.World world, BlockVector3 position, BaseItem item, Direction face) {
        CraftWorld craftWorld = (CraftWorld) world;
        WorldServer worldServer = craftWorld.getHandle();
        ItemStack stack = CraftItemStack.asNMSCopy(BukkitAdapter.adapt(item instanceof BaseItemStack
                ? ((BaseItemStack) item) : new BaseItemStack(item.getType(), item.getNbt(), 1)));
        stack.setTag((NBTTagCompound) fromNative(item.getNbt()));

        FakePlayer_v1_16_R3 fakePlayer;
        try {
            fakePlayer = fakePlayers.get(worldServer);
        } catch (ExecutionException ignored) {
            return false;
        }
        fakePlayer.a(EnumHand.MAIN_HAND, stack);
        fakePlayer.setLocation(position.getBlockX(), position.getBlockY(), position.getBlockZ(),
                (float) face.toVector().toYaw(), (float) face.toVector().toPitch());

        final BlockPosition blockPos = new BlockPosition(position.getBlockX(), position.getBlockY(), position.getBlockZ());
        final Vec3D blockVec = Vec3D.b(blockPos);
        final EnumDirection enumFacing = adapt(face);
        MovingObjectPositionBlock rayTrace = new MovingObjectPositionBlock(blockVec, enumFacing, blockPos, false);
        ItemActionContext context = new ItemActionContext(fakePlayer, EnumHand.MAIN_HAND, rayTrace);
        EnumInteractionResult result = stack.placeItem(context, EnumHand.MAIN_HAND);
        if (result != EnumInteractionResult.SUCCESS) {
            if (worldServer.getType(blockPos).interact(worldServer, fakePlayer, EnumHand.MAIN_HAND, rayTrace).a()) {
                result = EnumInteractionResult.SUCCESS;
            } else {
                result = stack.getItem().a(worldServer, fakePlayer, EnumHand.MAIN_HAND).a();
            }
        }

        return result == EnumInteractionResult.SUCCESS;
    }

    @Override
    public boolean canPlaceAt(org.bukkit.World world, BlockVector3 position, BlockState blockState) {
        int internalId = BlockStateIdAccess.getBlockStateId(blockState);
        IBlockData blockData = Block.getByCombinedId(internalId);
        return blockData.canPlace(((CraftWorld) world).getHandle(), new BlockPosition(position.getX(), position.getY(), position.getZ()));
    }

    @Override
    public boolean regenerate(org.bukkit.World bukkitWorld, Region region, Extent extent, RegenOptions options) {
        try {
            doRegen(bukkitWorld, region, extent, options);
        } catch (Exception e) {
            throw new IllegalStateException("Regen failed.", e);
        }

        return true;
    }

    private void doRegen(org.bukkit.World bukkitWorld, Region region, Extent extent, RegenOptions options) throws Exception {
        Environment env = bukkitWorld.getEnvironment();
        ChunkGenerator gen = bukkitWorld.getGenerator();

        Path tempDir = Files.createTempDirectory("WorldEditWorldGen");
        Convertable convertable = Convertable.a(tempDir);
        ResourceKey<WorldDimension> worldDimKey = getWorldDimKey(env);
        try (Convertable.ConversionSession session = convertable.c("worldeditregentempworld", worldDimKey)) {
            WorldServer originalWorld = ((CraftWorld) bukkitWorld).getHandle();
            WorldDataServer originalWorldData = originalWorld.worldDataServer;

            long seed = options.getSeed().orElse(originalWorld.getSeed());

            WorldDataServer levelProperties = (WorldDataServer) originalWorld.getServer().getServer().getSaveData();
            RegistryReadOps<NBTBase> nbtRegOps = RegistryReadOps.a(
                DynamicOpsNBT.a,
                originalWorld.getServer().getServer().dataPackResources.h(),
                IRegistryCustom.b()
            );

            GeneratorSettings newOpts = GeneratorSettings.a
                .encodeStart(nbtRegOps, levelProperties.getGeneratorSettings())
                .flatMap(tag ->
                    GeneratorSettings.a.parse(
                        recursivelySetSeed(new Dynamic<>(nbtRegOps, tag), seed, new HashSet<>())
                    )
                )
                .result()
                .orElseThrow(() -> new IllegalStateException("Unable to map GeneratorOptions"));


            WorldSettings newWorldSettings = new WorldSettings("worldeditregentempworld",
                originalWorldData.b.getGameType(),
                originalWorldData.b.hardcore,
                originalWorldData.b.getDifficulty(),
                originalWorldData.b.e(),
                originalWorldData.b.getGameRules(),
                originalWorldData.b.g());
            WorldDataServer newWorldData = new WorldDataServer(newWorldSettings, newOpts, Lifecycle.stable());

            WorldServer freshWorld = new WorldServer(
                originalWorld.getMinecraftServer(),
                originalWorld.getMinecraftServer().executorService,
                session, newWorldData,
                originalWorld.getDimensionKey(),
                originalWorld.getDimensionManager(),
                //originalWorld.getTypeKey(),
                new NoOpWorldLoadListener(),
                newOpts.d().a(worldDimKey).c(),
                originalWorld.isDebugWorld(),
                seed,
                ImmutableList.of(),
                false,
                env, gen
            );
            try {
                regenForWorld(region, extent, freshWorld, options);
            } finally {
                freshWorld.getChunkProvider().close(false);
            }
        } finally {
            try {
                Map<String, org.bukkit.World> map = (Map<String, org.bukkit.World>) serverWorldsField.get(Bukkit.getServer());
                map.remove("worldeditregentempworld");
            } catch (IllegalAccessException ignored) {
            }
            SafeFiles.tryHardToDeleteDir(tempDir);
        }
    }

    @SuppressWarnings("unchecked")
    private Dynamic<NBTBase> recursivelySetSeed(Dynamic<NBTBase> dynamic, long seed, Set<Dynamic<NBTBase>> seen) {
        if (!seen.add(dynamic)) {
            return dynamic;
        }
        return dynamic.updateMapValues(pair -> {
            if (pair.getFirst().asString("").equals("seed")) {
                return pair.mapSecond(v -> v.createLong(seed));
            }
            if (pair.getSecond().getValue() instanceof NBTTagCompound) {
                return pair.mapSecond(v -> recursivelySetSeed((Dynamic<NBTBase>) v, seed, seen));
            }
            return pair;
        });
    }

    private BiomeType adapt(WorldServer serverWorld, BiomeBase origBiome) {
        MinecraftKey key = serverWorld.r().b(IRegistry.ay).getKey(origBiome);
        if (key == null) {
            return null;
        }
        return BiomeTypes.get(key.toString());
    }

    private void regenForWorld(Region region, Extent extent, WorldServer serverWorld, RegenOptions options) throws WorldEditException {
        List<CompletableFuture<IChunkAccess>> chunkLoadings = submitChunkLoadTasks(region, serverWorld);
        IAsyncTaskHandler executor;
        try {
            executor = (IAsyncTaskHandler) chunkProviderExecutorField.get(serverWorld.getChunkProvider());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Couldn't get executor for chunk loading.", e);
        }
        executor.awaitTasks(() -> {
            // bail out early if a future fails
            if (chunkLoadings.stream().anyMatch(ftr ->
                ftr.isDone() && Futures.getUnchecked(ftr) == null
            )) {
                return false;
            }
            return chunkLoadings.stream().allMatch(CompletableFuture::isDone);
        });
        Map<ChunkCoordIntPair, IChunkAccess> chunks = new HashMap<>();
        for (CompletableFuture<IChunkAccess> future : chunkLoadings) {
            @Nullable
            IChunkAccess chunk = future.getNow(null);
            checkState(chunk != null, "Failed to generate a chunk, regen failed.");
            chunks.put(chunk.getPos(), chunk);
        }

        for (BlockVector3 vec : region) {
            BlockPosition pos = new BlockPosition(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ());
            IChunkAccess chunk = chunks.get(new ChunkCoordIntPair(pos));
            final IBlockData blockData = chunk.getType(pos);
            int internalId = Block.getCombinedId(blockData);
            BlockStateHolder<?> state = BlockStateIdAccess.getBlockStateById(internalId);
            TileEntity blockEntity = chunk.getTileEntity(pos);
            if (blockEntity != null) {
                NBTTagCompound tag = new NBTTagCompound();
                blockEntity.save(tag);
                state = state.toBaseBlock(((CompoundBinaryTag) toNative(tag)));
            }
            extent.setBlock(vec, state.toBaseBlock());
            if (options.shouldRegenBiomes()) {
                BiomeStorage biomeIndex = chunk.getBiomeIndex();
                if (biomeIndex != null) {
                    BiomeBase origBiome = biomeIndex.getBiome(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ());
                    BiomeType adaptedBiome = adapt(serverWorld, origBiome);
                    if (adaptedBiome != null) {
                        extent.setBiome(vec, adaptedBiome);
                    }
                }
            }
        }
    }

    private List<CompletableFuture<IChunkAccess>> submitChunkLoadTasks(Region region, WorldServer serverWorld) {
        ChunkProviderServer chunkManager = serverWorld.getChunkProvider();
        List<CompletableFuture<IChunkAccess>> chunkLoadings = new ArrayList<>();
        // Pre-gen all the chunks
        for (BlockVector2 chunk : region.getChunks()) {
            try {
                //noinspection unchecked
                chunkLoadings.add(
                    ((CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>>)
                        getChunkFutureMethod.invoke(chunkManager, chunk.getX(), chunk.getZ(), ChunkStatus.FEATURES, true))
                            .thenApply(either -> either.left().orElse(null))
                );
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Couldn't load chunk for regen.", e);
            }
        }
        return chunkLoadings;
    }

    private ResourceKey<WorldDimension> getWorldDimKey(Environment env) {
        switch (env) {
            case NETHER:
                return WorldDimension.THE_NETHER;
            case THE_END:
                return WorldDimension.THE_END;
            case NORMAL:
            default:
                return WorldDimension.OVERWORLD;
        }
    }

    private static final Set<SideEffect> SUPPORTED_SIDE_EFFECTS = Sets.immutableEnumSet(
            SideEffect.NEIGHBORS,
            SideEffect.LIGHTING,
            SideEffect.VALIDATION,
            SideEffect.ENTITY_AI,
            SideEffect.EVENTS,
            SideEffect.UPDATE
    );

    @Override
    public Set<SideEffect> getSupportedSideEffects() {
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
    BinaryTag toNative(NBTBase foreign) {
        if (foreign == null) {
            return null;
        }
        if (foreign instanceof NBTTagCompound) {
            Map<String, BinaryTag> values = new HashMap<>();
            Set<String> foreignKeys = ((NBTTagCompound) foreign).getKeys(); // map.keySet

            for (String str : foreignKeys) {
                NBTBase base = ((NBTTagCompound) foreign).get(str);
                values.put(str, toNative(base));
            }
            return CompoundBinaryTag.from(values);
        } else if (foreign instanceof NBTTagByte) {
            return ByteBinaryTag.of(((NBTTagByte) foreign).asByte());
        } else if (foreign instanceof NBTTagByteArray) {
            return ByteArrayBinaryTag.of(((NBTTagByteArray) foreign).getBytes());
        } else if (foreign instanceof NBTTagDouble) {
            return DoubleBinaryTag.of(((NBTTagDouble) foreign).asDouble());
        } else if (foreign instanceof NBTTagFloat) {
            return FloatBinaryTag.of(((NBTTagFloat) foreign).asFloat());
        } else if (foreign instanceof NBTTagInt) {
            return IntBinaryTag.of(((NBTTagInt) foreign).asInt());
        } else if (foreign instanceof NBTTagIntArray) {
            return IntArrayBinaryTag.of(((NBTTagIntArray) foreign).getInts());
        } else if (foreign instanceof NBTTagLongArray) {
            return LongArrayBinaryTag.of(((NBTTagLongArray) foreign).getLongs());
        } else if (foreign instanceof NBTTagList) {
            try {
                return toNativeList((NBTTagList) foreign);
            } catch (Throwable e) {
                logger.log(Level.WARNING, "Failed to convert NBTTagList", e);
                return ListBinaryTag.empty();
            }
        } else if (foreign instanceof NBTTagLong) {
            return LongBinaryTag.of(((NBTTagLong) foreign).asLong());
        } else if (foreign instanceof NBTTagShort) {
            return ShortBinaryTag.of(((NBTTagShort) foreign).asShort());
        } else if (foreign instanceof NBTTagString) {
            return StringBinaryTag.of(foreign.asString());
        } else if (foreign instanceof NBTTagEnd) {
            return EndBinaryTag.get();
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
    private ListBinaryTag toNativeList(NBTTagList foreign) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        ListBinaryTag.Builder values = ListBinaryTag.builder();

        List foreignList;
        foreignList = (List) nbtListTagListField.get(foreign);
        for (int i = 0; i < foreign.size(); i++) {
            NBTBase element = (NBTBase) foreignList.get(i);
            values.add(toNative(element)); // List elements shouldn't have names
        }

        return values.build();
    }

    /**
     * Converts a WorldEdit-native NBT structure to a NMS structure.
     *
     * @param foreign structure to convert
     * @return non-native structure
     */
    NBTBase fromNative(BinaryTag foreign) {
        if (foreign == null) {
            return null;
        }
        if (foreign instanceof CompoundBinaryTag) {
            NBTTagCompound tag = new NBTTagCompound();
            for (String key : ((CompoundBinaryTag) foreign).keySet()) {
                tag.set(key, fromNative(((CompoundBinaryTag) foreign).get(key)));
            }
            return tag;
        } else if (foreign instanceof ByteBinaryTag) {
            return NBTTagByte.a(((ByteBinaryTag) foreign).value());
        } else if (foreign instanceof ByteArrayBinaryTag) {
            return new NBTTagByteArray(((ByteArrayBinaryTag) foreign).value());
        } else if (foreign instanceof DoubleBinaryTag) {
            return NBTTagDouble.a(((DoubleBinaryTag) foreign).value());
        } else if (foreign instanceof FloatBinaryTag) {
            return NBTTagFloat.a(((FloatBinaryTag) foreign).value());
        } else if (foreign instanceof IntBinaryTag) {
            return NBTTagInt.a(((IntBinaryTag) foreign).value());
        } else if (foreign instanceof IntArrayBinaryTag) {
            return new NBTTagIntArray(((IntArrayBinaryTag) foreign).value());
        } else if (foreign instanceof LongArrayBinaryTag) {
            return new NBTTagLongArray(((LongArrayBinaryTag) foreign).value());
        } else if (foreign instanceof ListBinaryTag) {
            NBTTagList tag = new NBTTagList();
            ListBinaryTag foreignList = (ListBinaryTag) foreign;
            for (BinaryTag t : foreignList) {
                tag.add(fromNative(t));
            }
            return tag;
        } else if (foreign instanceof LongBinaryTag) {
            return NBTTagLong.a(((LongBinaryTag) foreign).value());
        } else if (foreign instanceof ShortBinaryTag) {
            return NBTTagShort.a(((ShortBinaryTag) foreign).value());
        } else if (foreign instanceof StringBinaryTag) {
            return NBTTagString.a(((StringBinaryTag) foreign).value());
        } else if (foreign instanceof EndBinaryTag) {
            return NBTTagEnd.b;
        } else {
            throw new IllegalArgumentException("Don't know how to make NMS " + foreign.getClass().getCanonicalName());
        }
    }

    @Override
    public boolean supportsWatchdog() {
        return watchdog != null;
    }

    @Override
    public void tickWatchdog() {
        watchdog.tick();
    }

    private static class SpigotWatchdog implements Watchdog {
        @Override
        public void tick() {
            WatchdogThread.tick();
        }
    }

    private static class MojangWatchdog implements Watchdog {
        private final DedicatedServer server;
        private final Field tickField;

        MojangWatchdog(DedicatedServer server) throws NoSuchFieldException {
            this.server = server;
            Field tickField = MinecraftServer.class.getDeclaredField("nextTick");
            tickField.setAccessible(true);
            this.tickField = tickField;
        }

        @Override
        public void tick() {
            try {
                tickField.set(server, SystemUtils.getMonotonicMillis());
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    private static class NoOpWorldLoadListener implements WorldLoadListener {
        @Override
        public void a(ChunkCoordIntPair chunkCoordIntPair) {
        }

        @Override
        public void a(ChunkCoordIntPair chunkCoordIntPair, @Nullable ChunkStatus chunkStatus) {
        }

        @Override
        public void b() {
        }
    }
}
