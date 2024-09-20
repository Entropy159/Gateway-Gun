package com.entropy.misc;

import net.minecraft.block.Block;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import qouteall.q_misc_util.Helper;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record BlockList(List<String> list) {
    public static BlockList createDefault() {
        return new BlockList(new ArrayList<>());
    }

    public static BlockList fromTag(NbtList tag) {
        return new BlockList(Helper.listTagDeserialize(tag, NbtString::asString, NbtString.class));
    }

    public BiPredicate<World, BlockPos> getWallPredicate() {
        if (list().isEmpty()) {
            return (w, p) -> w.getBlockState(p).isSolidBlock(w, p);
        }

        Set<Block> allowedBlocks = asStream().collect(Collectors.toSet());

        return (w, p) -> allowedBlocks.contains(w.getBlockState(p).getBlock());
    }

    public NbtList toTag() {
        return Helper.listTagSerialize(list, NbtString::of);
    }

    public Stream<Block> asStream() {
        return list.stream().flatMap(s -> parseBlockStr(s).stream());
    }

    public static Collection<Block> parseBlockStr(String str) {
        if (str.startsWith("#")) {
            TagKey<Block> tagKey = TagKey.of(
                    Registries.BLOCK.getKey(),
                    new Identifier(str.substring(1))
            );
            Optional<RegistryEntryList.Named<Block>> named = Registries.BLOCK.getEntryList(tagKey);
            if (named.isEmpty()) {
                return Collections.emptyList();
            } else {
                RegistryEntryList.Named<Block> holderSet = named.get();
                return holderSet.stream().map(RegistryEntry::value).toList();
            }
        } else {
            Optional<Block> optional = Registries.BLOCK.getOrEmpty(new Identifier(str));
            return optional.<Collection<Block>>map(Collections::singletonList).orElse(Collections.emptyList());
        }
    }
}
