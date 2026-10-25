package io.github.edilrey.nearlycraftable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/** The dev environment must use the same versions as the Beyond Depth instance. */
class PackVersionsTest {
    private static Properties load(InputStream in) throws IOException {
        Properties p = new Properties();
        try (in) {
            p.load(in);
        }
        return p;
    }

    @Test
    void projectVersionsMatchThePack() throws IOException {
        Properties project = load(Files.newInputStream(Path.of("gradle.properties")));
        Properties pack = load(getClass().getResourceAsStream("/pack-versions.properties"));
        for (String key : new String[] {"forge_version", "emi_version", "minecraft_version"}) {
            assertEquals(pack.getProperty(key), project.getProperty(key), key);
        }
    }
}
