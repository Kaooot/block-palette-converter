package com.github.kaooot.converter;

import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.Tag;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;

/**
 * @author Kaooot
 * @version 1.0
 */
public class BlockPaletteConverter {

    public static void main(String[] args) {
        if (args.length < 2) {
            return;
        }

        final String paletteFileName = args[0];
        final String outputFileName = args[1];

        try (final InputStream stream = BlockPaletteConverter.class.getClassLoader()
            .getResourceAsStream(paletteFileName)) {
            if (stream == null) {
                return;
            }

            try (final BufferedInputStream bufferedInputStream = new BufferedInputStream(stream)) {
                final List<NbtMap> blocks = new ObjectArrayList<>();

                while (bufferedInputStream.available() > 0) {
                    final CompoundTag compoundTag =
                        NBTIO.read(bufferedInputStream, ByteOrder.BIG_ENDIAN, true);

                    final String name = compoundTag.getString("name");
                    final int version = compoundTag.getInt("version");
                    final CompoundTag states = compoundTag.getCompound("states");

                    final NbtMapBuilder statesBuilder = NbtMap.builder();

                    for (Map.Entry<String, Tag> entry : states.getTags().entrySet()) {
                        final Tag value = entry.getValue();

                        if (value.parseValue() instanceof Byte b) {
                            statesBuilder.putByte(entry.getKey(), b);
                        } else if (value.parseValue() instanceof Integer integer) {
                            statesBuilder.putInt(entry.getKey(), integer);
                        } else if (value.parseValue() instanceof String string) {
                            statesBuilder.putString(entry.getKey(), string);
                        }
                    }

                    blocks.add(NbtMap.builder()
                        .putString("name", name)
                        .putInt("version", version)
                        .putCompound("states", statesBuilder.build())
                        .build());
                }

                final NbtMap palette =
                    NbtMap.builder().putList("blocks", NbtType.COMPOUND, blocks).build();

                try (final NBTOutputStream nbtOutputStream = new NBTOutputStream(
                    new DataOutputStream(
                        new GZIPOutputStream(Files.newOutputStream(Paths.get(outputFileName)))))) {
                    nbtOutputStream.writeTag(palette);

                    final int version = blocks.get(0).getInt("version");

                    System.out.println(paletteFileName + " => " + outputFileName + " (" +
                        ((version >> 24) & 0xff) + "." +
                        ((version >> 16) & 0xff) + "." +
                        ((version >> 8) & 0xff) + "." +
                        (version & 0xff) + ")");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}