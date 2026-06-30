package com.flowforge;

import com.flowforge.gui.FlowForgeApp;
import com.flowforge.gui.FlowTheme;
import com.flowforge.model.User;
import com.flowforge.persistence.SqliteWorkflowRepository;
import com.flowforge.service.WorkflowEngine;
import com.flowforge.service.WorkflowExecutionService;
import com.flowforge.service.WorkflowManager;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

/**
 * Dev-only harness (not part of the app) that renders the FlowForge window
 * for each theme to a PNG, so the GUI can be visually verified on machines
 * where external screenshot tools are unavailable (e.g. Wayland).
 */
public final class RenderShot {

    public static void main(String[] args) throws Exception {
        String dataDir = args.length > 0 ? args[0] : "data";
        String outDir = args.length > 1 ? args[1] : "/tmp";

        WorkflowManager manager = new WorkflowManager(new SqliteWorkflowRepository(Path.of(dataDir)));
        WorkflowExecutionService executionService = new WorkflowExecutionService(new WorkflowEngine());
        User shotUser = new User("screenshot", java.time.Instant.now());

        for (FlowTheme theme : FlowTheme.values()) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    theme.apply();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                FlowForgeApp app = new FlowForgeApp(manager, executionService, shotUser);
                app.setSize(1040, 680);
                app.selectFirstWorkflowForShot();
                app.setVisible(true);
                app.toFront();
                appRef[0] = app;
            });

            // Let the EDT lay out and paint the now-visible window.
            Thread.sleep(700);

            SwingUtilities.invokeAndWait(() -> {
                FlowForgeApp app = appRef[0];
                BufferedImage image = new BufferedImage(
                        app.getWidth(), app.getHeight(), BufferedImage.TYPE_INT_RGB);
                var g = image.createGraphics();
                app.paint(g);
                g.dispose();

                try {
                    File out = new File(outDir, "flowforge-" + theme.name().toLowerCase() + ".png");
                    ImageIO.write(image, "png", out);
                    System.out.println("Wrote " + out + " (" + out.length() + " bytes)");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                app.dispose();
            });
        }
        System.exit(0);
    }

    private static final FlowForgeApp[] appRef = new FlowForgeApp[1];

    private RenderShot() {
    }
}
