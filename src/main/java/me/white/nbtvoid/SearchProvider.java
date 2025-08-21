package me.white.nbtvoid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.command.argument.NbtPathArgumentType.NbtPath;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.component.DataComponentTypes;

@Environment(value=EnvType.CLIENT)
public class SearchProvider {
    public static record SearchQuery(List<String> nameQueries, List<String> idQueries, List<String> nbtQueries) {}

    public static SearchQuery parseQuery(String query) {
        List<String> nameQueries = new ArrayList<>();
        List<String> idQueries = new ArrayList<>();
        List<String> nbtQueries = new ArrayList<>();

        StringReader reader = new StringReader(query);

        while (reader.canRead()) switch (reader.peek()) {
            case ' ':
                reader.skip();
                break;
            case '$':
                reader.skip();
                if (reader.canRead()) nbtQueries.add(reader.readString());
                break;
            case '&':
                reader.skip();
                if (reader.canRead()) idQueries.add(reader.readString().toLowerCase(Locale.ROOT));
                break;
            default:
                nameQueries.add(reader.readString().toLowerCase(Locale.ROOT));
        }

        return new SearchQuery(nameQueries, idQueries, nbtQueries);
    }

    public static HashSet<VoidEntry> search(Collection<VoidEntry> items, SearchQuery query) {
        HashSet<VoidEntry> result = new HashSet<>();
        itemLoop: for (VoidEntry entry : items) {
            // ID check
            if (Config.getInstance().getIdCheck() == Config.CheckType.ANY) {
                idCheck: if (query.idQueries.size() != 0) {
                    for (String idQuery : query.idQueries) {
                        if (matchIdentifier(entry.getItem(), idQuery)) break idCheck;
                    }
                    // damn if only there were for-else like in python
                    continue;
                }
            } else {
                for (String idQuery : query.idQueries) {
                    if (!matchIdentifier(entry.getItem(), idQuery)) continue itemLoop;
                }
            }
            // Name check
            if (Config.getInstance().getNameCheck() == Config.CheckType.ANY) {
                nameCheck: if (query.nameQueries.size() != 0) {
                    for (String nameQuery : query.nameQueries) {
                        if (matchName(entry.getItem(), nameQuery)) break nameCheck;
                    }
                    continue;
                }
            } else {
                for (String nameQuery : query.nameQueries) {
                    if (!matchName(entry.getItem(), nameQuery)) continue itemLoop;
                }
            }
            // NBT check
            if (Config.getInstance().getNbtCheck() == Config.CheckType.ANY) {
                nbtCheck: if (query.nbtQueries.size() != 0) {
                    for (String nbtQuery : query.nbtQueries) {
                        if (matchNbt(entry.getItem(), nbtQuery)) break nbtCheck;
                    }
                    continue;
                }
            } else {
                for (String nbtQuery : query.nbtQueries) {
                    if (!matchNbt(entry.getItem(), nbtQuery)) continue itemLoop;
                }
            }

            // extra careful here
            if (entry == null) continue;
            if (entry.getItem() == null) continue;
            
            result.add(entry);
        }

        return result;
    }

    public static boolean matchNbt(ItemStack item, String query) {
        // TODO:
        // Do so it can autocomplete queries, e.g.:
        //   Show items containing "CustomModelData" tag if "CustomModel" path given,
        //   Correct incorrect list path ("list[" -> "list[]")
		try {
			// For 1.21.3, check custom data component
			NbtCompound customData = item.contains(DataComponentTypes.CUSTOM_DATA) ? 
				item.get(DataComponentTypes.CUSTOM_DATA).getNbt() : new NbtCompound();
			if (customData.isEmpty()) return false;
			
			NbtPath path = new NbtPathArgumentType().parse(new com.mojang.brigadier.StringReader(query));
			int count = path.count(customData);
			return count != 0;
		} catch (Exception e) {
            NbtVoid.LOGGER.info("Invalid path: '" + query + "'");
			return false;
		}
	}

    private static boolean matchName(ItemStack item, String query) {
        boolean isInName = item.getName().getString().toLowerCase(Locale.ROOT).contains(query);
        if (isInName) return true;
        
        // Check custom name component
        if (item.contains(DataComponentTypes.CUSTOM_NAME)) {
            Text customName = item.get(DataComponentTypes.CUSTOM_NAME);
            if (customName != null && customName.getString().toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        
        // Check lore component
        if (item.contains(DataComponentTypes.LORE)) {
            List<Text> lore = item.get(DataComponentTypes.LORE).lines();
            for (Text loreLine : lore) {
                if (loreLine.getString().toLowerCase(Locale.ROOT).contains(query)) return true;
            }
        }
        
        return false;
    }

    private static boolean matchIdentifier(ItemStack item, String query) {
        return Registries.ITEM.getId(item.getItem()).toString().contains(query);
    }

    // I'm pretty sure there's already is something like that in Java, but i couldn't find it
    public static class StringReader {
        private String s;
        private int i;

        public StringReader(String s, int i) {
            this.s = s;
            this.i = i;
        }

        public StringReader(String s) {
            this(s, 0);
        }

        public boolean canRead() {
            return i < s.length() && i >= 0;
        }

        public char peek() {
            if (!canRead()) return '\0';
            return s.charAt(i);
        }

        public char read() {
            if (!canRead()) return '\0';
            return s.charAt(i++);
        }

        public void skip() {
            i += 1;
        }

        public String readString(char delimeter) {
            StringBuilder builder = new StringBuilder();
            boolean inQuotes = false;

            while (canRead() && !(peek() == delimeter && !inQuotes)) {
                if (peek() == '"') inQuotes = !inQuotes;
                if (peek() == '\\') skip();
                if (!canRead()) break;

                builder.append(read());
            }
            return builder.toString();
        }

        public String readString() {
            return readString(' ');
        }
    }
}