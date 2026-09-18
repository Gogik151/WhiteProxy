package ru.white.proxymod.render.shader;

import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class ShaderStore {

    private ShaderStore() {}

    private static final byte[] KEY = "Nightix//shader-veil//2026".getBytes(StandardCharsets.UTF_8);
    private static final String NAMESPACE = "proxymod";
    private static final String INCLUDE_DIR = "shaders/include/";

    public static String getSource(Identifier id, ShaderType type) {
        if (id == null || !NAMESPACE.equals(id.getNamespace())) return null;

        String ext = (type == ShaderType.VERTEX) ? "vsh" : "fsh";
        String key = id.getPath() + "|" + ext;

        String enc = ShaderData.SOURCES.get(key);
        if (enc == null) return null;

        String raw = decrypt(enc);
        Identifier owner = Identifier.of(id.getNamespace(), "shaders/" + id.getPath() + "." + ext);
        String resolved = resolveImports(raw, owner, new HashSet<>());
        return dedupeVersion(resolved);
    }

    private static String dedupeVersion(String source) {
        StringBuilder out = new StringBuilder(source.length());
        boolean versionSeen = false;
        for (String line : source.split("\n", -1)) {
            if (line.trim().startsWith("#version")) {
                if (versionSeen) continue;
                versionSeen = true;
            }
            out.append(line).append('\n');
        }
        return out.toString();
    }

    private static String decrypt(String base64) {
        byte[] data = Base64.getDecoder().decode(base64);
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (byte) (data[i] ^ KEY[i % KEY.length]);
        }
        return new String(out, StandardCharsets.UTF_8);
    }

    private static String resolveImports(String source, Identifier owner, Set<String> seen) {
        StringBuilder out = new StringBuilder(source.length() + 256);
        for (String line : source.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#moj_import")) {
                Identifier inc = parseImport(trimmed, owner);
                if (inc != null) {
                    if (seen.add(inc.toString())) {
                        String incSrc = readResource(inc);
                        if (incSrc != null) {
                            out.append(resolveImports(incSrc, inc, seen));
                            if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
                                out.append('\n');
                            }
                        }
                    }
                    continue;
                }
            }
            out.append(line).append('\n');
        }
        return out.toString();
    }

    private static Identifier parseImport(String line, Identifier owner) {
        int lt = line.indexOf('<');
        int gt = line.indexOf('>');
        if (lt >= 0 && gt > lt) {
            String ref = line.substring(lt + 1, gt).trim();
            Identifier id = Identifier.of(ref);
            return Identifier.of(id.getNamespace(), INCLUDE_DIR + id.getPath());
        }
        int q1 = line.indexOf('"');
        int q2 = line.lastIndexOf('"');
        if (q1 >= 0 && q2 > q1) {
            String ref = line.substring(q1 + 1, q2).trim();
            String ownerPath = owner.getPath();
            int slash = ownerPath.lastIndexOf('/');
            String dir = slash >= 0 ? ownerPath.substring(0, slash + 1) : "";
            return Identifier.of(owner.getNamespace(), dir + ref);
        }
        return null;
    }

    private static String readResource(Identifier id) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null) return null;
            ResourceManager rm = mc.getResourceManager();
            if (rm == null) return null;
            Optional<Resource> res = rm.getResource(id);
            if (res.isEmpty()) return null;
            try (InputStream in = res.get().getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
