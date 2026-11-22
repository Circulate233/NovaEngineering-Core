package github.kasuminova.novaeng.common.logging.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingAgentConfigTest {
    @TempDir
    Path directory;

    private boolean read(final String text) throws Exception {
        return LoggingAgentConfig.read(Files.writeString(directory.resolve("novaeng_core.cfg"), text));
    }

    @Test
    void defaultPathMatchesForgeMissingFilesStayDisabledAndReadErrorsRemainVisible() throws Exception {
        assertEquals(Path.of("config", "novaeng_core.cfg"), LoggingAgentConfig.configurationPath(null));
        assertEquals(Path.of("config", "novaeng_core.cfg"), LoggingAgentConfig.configurationPath("  "));
        assertFalse(LoggingAgentConfig.isEnabled("config=" + directory.resolve("missing.cfg")));
        assertThrows(IOException.class, () -> LoggingAgentConfig.isEnabled("config=" + directory));
    }

    @Test
    void customAbsolutePathPreservesSpacesAndEqualsSigns() throws Exception {
        final Path file = Files.createDirectories(directory.resolve("test directory"))
            .resolve("custom=profile.cfg");
        Files.writeString(file, "general {\n performance {\n B:DisableLogOutput=true\n }\n}\n");
        assertEquals(file, LoggingAgentConfig.configurationPath("config=" + file));
        assertTrue(LoggingAgentConfig.isEnabled("config=" + file));
        assertThrows(IllegalArgumentException.class, () -> LoggingAgentConfig.configurationPath("config="));
        assertThrows(IllegalArgumentException.class, () -> LoggingAgentConfig.configurationPath("enable=true"));
    }

    @Test
    void onlyTheExactBooleanPropertyInGeneralPerformanceEnablesSuppression() throws Exception {
        assertTrue(read("general {\n performance {\n B:DisableLogOutput=true\n }\n}\n"));
        assertFalse(read("general {\n client {\n B:DisableLogOutput=true\n }\n}\n"));
        assertFalse(read("performance {\n B:DisableLogOutput=true\n}\n"));
        assertFalse(read("other {\n performance {\n B:DisableLogOutput=true\n }\n}\n"));
        assertFalse(read("general {\n performance {\n nested {\n B:DisableLogOutput=true\n }\n }\n}\n"));
        assertFalse(read("general {\n performance {\n S:DisableLogOutput=true\n }\n}\n"));
        assertFalse(read("general {\n performance {\n B:disableLogOutput=true\n }\n}\n"));
    }

    @Test
    void falseMissingAndMalformedValuesStayDisabled() throws Exception {
        assertFalse(read(""));
        assertFalse(read("general {\n performance {\n }\n}\n"));
        assertFalse(read("general {\n performance {\n B:DisableLogOutput=false\n }\n}\n"));
        assertFalse(read("general {\n performance {\n B:DisableLogOutput=1\n }\n}\n"));
        assertFalse(read("general {\n performance {\n B:DisableLogOutput=true\n"));
    }

    @Test
    void forgeQuotedNamesCrLfAndInlineCommentsAreSupported() throws Exception {
        assertTrue(read("# Configuration file\r\n\"general\" {\r\n \"performance\" {\r\n"
            + " B:\"DisableLogOutput\" = TrUe # only this property counts\r\n }\r\n}\r\n"));
    }

    @Test
    void commentsAndListValuesCannotActAsConfigurationProperties() throws Exception {
        assertFalse(read("""
            general {
                performance {
                    # B:DisableLogOutput=true
                    S:examples <
                        B:DisableLogOutput=true
                        general {
                        performance {
                    >
                    B:DisableLogOutput=false # B:DisableLogOutput=true
                    S:text="some#text"
                }
            }
            """));
    }

    @Test
    void laterExplicitValuesWinWithinTheExactCategory() throws Exception {
        assertFalse(read("general {\n performance {\n B:DisableLogOutput=true\n"
            + " B:DisableLogOutput=false\n }\n}\n"));
        assertTrue(read("general {\n performance {\n B:DisableLogOutput=false\n"
            + " B:DisableLogOutput=true\n }\n}\n"));
    }
}
