package github.kasuminova.novaeng.mixin;

import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;


@SuppressWarnings("unused")
public class NovaEngCoreEarlyMixinLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {
    public static final Logger LOG = LogManager.getLogger("NOVAENG_CORE_PRE");
    public static final String LOG_PREFIX = "[NOVAENG_CORE_PRE]" + ' ';

    static {
        if (isCleanroomLoader()) {
            LOG.info(LOG_PREFIX + "CleanroomLoader detected.");
            if (FMLLaunchHandler.side().isClient()){
                checkLauncher();
            }
        }
    }

    /**
     * <a href="https://github.com/GBLodb/PreventCrappyLauncher/blob/retro/src/main/java/gblodb/preventCrappyLauncher/PreventCrappyLauncher.java">Original Source Code</a><br/>
     * <br/>
     * 你说得对，但是不得不写。<br/>
     * <a href="https://github.com/Hex-Dragon/PCL2/discussions/3004#discussioncomment-8741822">PCL2 Discussion Link</a><br/>
     */
    public static void checkLauncher() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            return;
        }

        boolean detected = false;
        List<String> lines = new ArrayList<>();
        try {
            String queryCmd = "tasklist.exe" + " /FO csv /FI \"STATUS eq RUNNING\" | findstr /R /C:\"Plain Craft Launcher 2\"";
            String cmd = "cmd";
            Process pr = new ProcessBuilder(cmd, "/C", queryCmd).start();
            SequenceInputStream sis = new SequenceInputStream(pr.getInputStream(), pr.getErrorStream());
            InputStreamReader inst = new InputStreamReader(sis, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(inst);
            String line;
            while ((line = br.readLine()) != null) {
                detected |= line.startsWith("\"");
            }
        } catch (Exception e) {
            LOG.warn(LOG_PREFIX + "Launcher check failed.", e);
            return;
        }

        if (detected) {
            if (!Desktop.isDesktopSupported()) {
                LOG.warn(LOG_PREFIX + "Crappy launcher detected, but desktop is unsupported.");
                return;
            }
            int input = JOptionPane.showConfirmDialog(null,
                    """
                            客户端已侦测到 CleanroomLoader，但是你***可能***正在使用不兼容的启动器来启动客户端。
                            使用不兼容的启动器会出现预期外的问题，并导致性能下降，甚至崩溃。
                            如果可能，请检查你的整合包的版本是否是最新版本。你可以点击“确定”强制启动客户端，但是这可能会导致大概率游戏崩溃。
                            """, "侦测到不兼容的启动器",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (input != JOptionPane.YES_OPTION) {
                throw new RuntimeException("Unsupported launcher detected.");
            }
        } else {
            LOG.info(LOG_PREFIX + "Launcher check passed.");
        }
    }

    private static final String RESOURCE_BUNDLE = "messages";
    static ResourceBundle bundler;

    public static String getString(String key){
        return new String(bundler.getString(key).getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }

    /**
     * 愚蠢的一切，为什么要用十年前的java？
     */
    public static void checkJavaVersion() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            return;
        }

        String version = SystemUtils.JAVA_VERSION;
        LOG.info(RESOURCE_BUNDLE + ".{}", version);

        try {
            bundler = ResourceBundle.getBundle(RESOURCE_BUNDLE);
            String[] mainParts = version.split("\\.");
            if (mainParts.length < 3) {
                logWarning(RESOURCE_BUNDLE + ".java.version.invalid", version);
                return;
            }

            ResourceBundle bundle = ResourceBundle.getBundle(
                    "messages",
                    Locale.getDefault(),
                    Thread.currentThread().getContextClassLoader()
            );

            int major = Integer.parseInt(mainParts[0]);
            int minor = Integer.parseInt(mainParts[1]);
            String[] updateParts = mainParts[2].split("_");

            if (updateParts.length < 1) {
                logWarning(RESOURCE_BUNDLE + ".java.version.invalid.format", mainParts[2]);
                return;
            }

            int update = Integer.parseInt(updateParts[1]);

            if (major == 1 && minor == 8 && update < 271) {
                String warningMessage = getString("java.version.warning");
                String detail = String.format(getString("java.version.warning.detail"), version);
                String recommendation = getString("java.version.warning.recommend");
                String risk = getString("java.version.warning.risk");
                String confirm = getString("java.version.warning.confirm");

                showWarningDialog(warningMessage + "\n" + detail + "\n" + recommendation + "\n" + risk + "\n\n" + confirm);
            }
        } catch (NumberFormatException e) {
            logError("messages.java.version.parse.error",version, e);
        }
    }

    private static void showWarningDialog(String message) {
        if (!Desktop.isDesktopSupported()) {
            logWarning(RESOURCE_BUNDLE + ".desktop.unsupported");
            return;
        }

        int input = JOptionPane.showConfirmDialog(null, message,
                getString("java.version.warning.title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (input != JOptionPane.YES_OPTION) {
            throw new RuntimeException(getString("java.version.blocked"));
        }
    }

    private static void logWarning(String key, Object... params) {
        LOG.warn(getString(key), params);
    }

    private static void logError(String key,Object... params) {
        LOG.error(getString(key), params);
    }

    public static boolean isCleanroomLoader() {
        try {
            Class.forName("com.cleanroommc.boot.Main");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public List<String> getMixinConfigs() {
        return Arrays.asList(
                "mixins.novaeng_core_vanilla.json",
                "mixins.novaeng_core.json"
        );
    }

    // Noop

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(final Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
