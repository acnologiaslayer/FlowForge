package com.flowforge.gui;

import com.flowforge.exception.WorkflowException;
import com.flowforge.model.RunReport;
import com.flowforge.model.StepResult;
import com.flowforge.model.Workflow;
import com.flowforge.model.ExecutionContext;
import com.flowforge.model.task.Task;
import com.flowforge.model.task.TaskType;
import com.flowforge.service.WorkflowEngine;
import com.flowforge.service.WorkflowExecutionListener;
import com.flowforge.service.WorkflowManager;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

/**
 * The main FlowForge window: a custom, undecorated, fully themed frame.
 * <p>
 * Layout: a custom {@link TitleBar} on top, a left sidebar listing
 * workflows, a centre showing the selected workflow's steps with a toolbar,
 * and a bottom run log that streams live progress via a
 * {@link WorkflowExecutionListener}. The window talks only to the service
 * layer ({@link WorkflowManager} / {@link WorkflowEngine}), never to the
 * persistence classes directly.
 */
public class FlowForgeApp extends JFrame {

    private final WorkflowManager manager;
    private final WorkflowEngine engine;

    private final DefaultListModel<Workflow> workflowModel = new DefaultListModel<>();
    private final JList<Workflow> workflowList = new JList<>(workflowModel);
    private final DefaultListModel<Task> stepModel = new DefaultListModel<>();
    private final JList<Task> stepList = new JList<>(stepModel);
    private final JTextPane runLog = new JTextPane();

    private final JLabel headerTitle = new JLabel("Select a workflow");
    private final JLabel headerSubtitle = new JLabel(" ");
    private final JLabel statusLabel = new JLabel("Ready");

    private TitleBar titleBar;
    private final java.util.List<JPanel> surfaces = new ArrayList<>();
    private final java.util.List<FlowButton> buttons = new ArrayList<>();

    public FlowForgeApp(WorkflowManager manager, WorkflowEngine engine) {
        super("FlowForge");
        this.manager = manager;
        this.engine = engine;

        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1040, 680);
        setMinimumSize(new Dimension(820, 540));
        setLocationRelativeTo(null);

        buildUI();
        refreshWorkflowList();
        applyTheme(FlowTheme.active());
    }

    // ---------- UI construction ----------

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createLineBorder(FlowTheme.active().palette().accent(), 1));
        setContentPane(root);

        titleBar = new TitleBar(this, "FlowForge  -  Workflow Automation Studio");
        root.add(titleBar, BorderLayout.NORTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildSidebar(), buildWorkflowArea());
        mainSplit.setDividerLocation(260);
        mainSplit.setBorder(null);
        mainSplit.setDividerSize(6);
        root.add(mainSplit, BorderLayout.CENTER);

        root.add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildSidebar() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 6));
        surfaces.add(panel);

        JLabel heading = sectionLabel("WORKFLOWS");
        panel.add(heading, BorderLayout.NORTH);

        workflowList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        workflowList.setCellRenderer(new WorkflowCellRenderer());
        workflowList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedWorkflow();
            }
        });
        JScrollPane scroll = new JScrollPane(workflowList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(2, 1, 0, 6));
        actions.setOpaque(false);
        FlowButton newButton = primaryButton("+  New Workflow");
        FlowButton deleteButton = dangerButton("Delete Workflow");
        newButton.addActionListener(e -> createWorkflow());
        deleteButton.addActionListener(e -> deleteSelectedWorkflow());
        actions.add(newButton);
        actions.add(deleteButton);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildWorkflowArea() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        surfaces.add(panel);

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildStepsPanel(), buildRunLogPanel());
        verticalSplit.setResizeWeight(0.62);
        verticalSplit.setDividerLocation(360);
        verticalSplit.setBorder(null);
        verticalSplit.setDividerSize(6);
        panel.add(verticalSplit, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStepsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 10, 8, 12));
        surfaces.add(panel);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        headerTitle.setFont(headerTitle.getFont().deriveFont(Font.BOLD, 18f));
        headerSubtitle.setFont(headerSubtitle.getFont().deriveFont(Font.PLAIN, 12f));
        JPanel titleStack = new JPanel(new GridLayout(2, 1));
        titleStack.setOpaque(false);
        titleStack.add(headerTitle);
        titleStack.add(headerSubtitle);
        header.add(titleStack, BorderLayout.WEST);

        FlowButton runButton = primaryButton("\u25B6  Run");
        runButton.addActionListener(e -> runSelectedWorkflow());
        JPanel runWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        runWrap.setOpaque(false);
        runWrap.add(runButton);
        header.add(runWrap, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        stepList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stepList.setCellRenderer(new StepCellRenderer());
        JScrollPane scroll = new JScrollPane(stepList);
        scroll.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        toolbar.setOpaque(false);
        FlowButton addButton = secondaryButton("Add Step");
        FlowButton editButton = secondaryButton("Edit");
        FlowButton upButton = secondaryButton("\u25B2 Up");
        FlowButton downButton = secondaryButton("\u25BC Down");
        FlowButton removeButton = dangerButton("Remove");
        addButton.addActionListener(e -> addStep());
        editButton.addActionListener(e -> editStep());
        upButton.addActionListener(e -> moveStep(-1));
        downButton.addActionListener(e -> moveStep(1));
        removeButton.addActionListener(e -> removeStep());
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(upButton);
        toolbar.add(downButton);
        toolbar.add(removeButton);
        panel.add(toolbar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildRunLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 12));
        surfaces.add(panel);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(sectionLabel("RUN LOG"), BorderLayout.WEST);
        FlowButton clear = secondaryButton("Clear");
        clear.addActionListener(e -> runLog.setText(""));
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wrap.setOpaque(false);
        wrap.add(clear);
        header.add(wrap, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        runLog.setEditable(false);
        runLog.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(runLog);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        surfaces.add(bar);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 12f));
        bar.add(statusLabel, BorderLayout.WEST);

        ThemeSelector selector = new ThemeSelector(this::applyTheme);
        bar.add(selector, BorderLayout.EAST);
        this.themeSelector = selector;
        return bar;
    }

    private ThemeSelector themeSelector;

    /** Dev/test hook: selects the first workflow so screenshots show steps. */
    public void selectFirstWorkflowForShot() {
        if (workflowModel.getSize() > 0) {
            workflowList.setSelectedIndex(0);
        }
    }

    // ---------- actions ----------

    private void createWorkflow() {
        FormDialog form = new FormDialog(this, "New Workflow")
                .addText("name", "Name", "")
                .addTextArea("description", "Description", "");
        var values = form.showDialog();
        if (values == null) {
            return;
        }
        try {
            Workflow created = manager.createWorkflow(values.get("name"), values.get("description"));
            refreshWorkflowList();
            selectWorkflow(created);
            setStatus("Created workflow '" + created.getName() + "'");
        } catch (WorkflowException e) {
            MessageDialog.show(this, "Could not create workflow", e.getMessage(),
                    MessageDialog.Kind.ERROR);
        }
    }

    private void deleteSelectedWorkflow() {
        Workflow workflow = workflowList.getSelectedValue();
        if (workflow == null) {
            MessageDialog.show(this, "No selection", "Select a workflow to delete first.",
                    MessageDialog.Kind.WARNING);
            return;
        }
        if (!ConfirmDialog.ask(this, "Delete workflow",
                "Delete '" + workflow.getName() + "'? This cannot be undone.")) {
            return;
        }
        try {
            manager.delete(workflow.getId());
            refreshWorkflowList();
            stepModel.clear();
            headerTitle.setText("Select a workflow");
            headerSubtitle.setText(" ");
            setStatus("Deleted workflow '" + workflow.getName() + "'");
        } catch (WorkflowException e) {
            MessageDialog.show(this, "Could not delete", e.getMessage(), MessageDialog.Kind.ERROR);
        }
    }

    private void addStep() {
        Workflow workflow = workflowList.getSelectedValue();
        if (workflow == null) {
            MessageDialog.show(this, "No workflow", "Create or select a workflow first.",
                    MessageDialog.Kind.WARNING);
            return;
        }
        TaskType type = chooseTaskType();
        if (type == null) {
            return;
        }
        Task task = TaskEditorDialog.createTask(this, type);
        if (task == null) {
            return;
        }
        workflow.addStep(task);
        persist(workflow);
        showSelectedWorkflow();
        stepList.setSelectedIndex(stepModel.getSize() - 1);
        setStatus("Added step '" + task.getName() + "'");
    }

    private void editStep() {
        Workflow workflow = workflowList.getSelectedValue();
        int index = stepList.getSelectedIndex();
        if (workflow == null || index < 0) {
            MessageDialog.show(this, "No step", "Select a step to edit.", MessageDialog.Kind.WARNING);
            return;
        }
        Task updated = TaskEditorDialog.editTask(this, workflow.getStep(index));
        if (updated == null) {
            return;
        }
        workflow.replaceStep(index, updated);
        persist(workflow);
        showSelectedWorkflow();
        stepList.setSelectedIndex(index);
        setStatus("Updated step '" + updated.getName() + "'");
    }

    private void moveStep(int direction) {
        Workflow workflow = workflowList.getSelectedValue();
        int index = stepList.getSelectedIndex();
        if (workflow == null || index < 0) {
            return;
        }
        if (direction < 0) {
            workflow.moveStepUp(index);
        } else {
            workflow.moveStepDown(index);
        }
        persist(workflow);
        showSelectedWorkflow();
        int newIndex = Math.max(0, Math.min(stepModel.getSize() - 1, index + direction));
        stepList.setSelectedIndex(newIndex);
    }

    private void removeStep() {
        Workflow workflow = workflowList.getSelectedValue();
        int index = stepList.getSelectedIndex();
        if (workflow == null || index < 0) {
            MessageDialog.show(this, "No step", "Select a step to remove.",
                    MessageDialog.Kind.WARNING);
            return;
        }
        Task removed = workflow.removeStep(index);
        persist(workflow);
        showSelectedWorkflow();
        setStatus("Removed step '" + removed.getName() + "'");
    }

    private void runSelectedWorkflow() {
        Workflow workflow = workflowList.getSelectedValue();
        if (workflow == null) {
            MessageDialog.show(this, "No workflow", "Select a workflow to run.",
                    MessageDialog.Kind.WARNING);
            return;
        }
        appendLog("\n=== Running '" + workflow.getName() + "' ===\n", LogStyle.HEADER);
        setStatus("Running '" + workflow.getName() + "'...");

        // Run off the EDT so a Delay step does not freeze the UI; stream
        // progress back onto the EDT through the listener.
        new SwingWorker<RunReport, String>() {
            @Override
            protected RunReport doInBackground() throws Exception {
                return engine.run(workflow, new ExecutionContext(), new LiveListener());
            }

            @Override
            protected void done() {
                try {
                    RunReport report = get();
                    if (report.isSuccess()) {
                        appendLog(String.format("Completed %d/%d steps in %d ms.%n",
                                        report.getCompletedSteps(), workflow.stepCount(),
                                        report.getTotalDurationMillis()),
                                LogStyle.SUCCESS);
                        setStatus("Run succeeded");
                    } else {
                        StepResult failure = report.getFailure();
                        appendLog("Run failed"
                                        + (failure == null ? "." : ": " + failure.getMessage()) + "\n",
                                LogStyle.ERROR);
                        setStatus("Run failed");
                    }
                } catch (Exception ex) {
                    appendLog("Run error: " + ex.getMessage() + "\n", LogStyle.ERROR);
                    setStatus("Run error");
                }
            }
        }.execute();
    }

    /** Streams engine callbacks into the run log on the EDT. */
    private class LiveListener implements WorkflowExecutionListener {
        @Override
        public void onStepStarted(int index, Workflow workflow) {
            SwingUtilities.invokeLater(() ->
                    appendLog(String.format("  > step %d: %s%n", index + 1,
                            workflow.getStep(index).getName()), LogStyle.INFO));
        }

        @Override
        public void onStepFinished(StepResult result) {
            SwingUtilities.invokeLater(() -> appendLog(
                    String.format("    %s %s (%d ms)%n",
                            result.isSuccess() ? "\u2714" : "\u2716",
                            result.getMessage(), result.getDurationMillis()),
                    result.isSuccess() ? LogStyle.SUCCESS : LogStyle.ERROR));
        }
    }

    private TaskType chooseTaskType() {
        String[] labels = new String[TaskType.values().length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = TaskType.values()[i].getLabel();
        }
        var values = new FormDialog(this, "Add Step")
                .addCombo("type", "Step type", labels, labels[0])
                .showDialog();
        if (values == null) {
            return null;
        }
        return TaskType.fromLabel(values.get("type"));
    }

    // ---------- data binding ----------

    private void refreshWorkflowList() {
        Workflow selected = workflowList.getSelectedValue();
        workflowModel.clear();
        for (Workflow workflow : manager.listWorkflows()) {
            workflowModel.addElement(workflow);
        }
        if (selected != null) {
            selectWorkflow(selected);
        }
    }

    private void selectWorkflow(Workflow workflow) {
        for (int i = 0; i < workflowModel.size(); i++) {
            if (workflowModel.get(i).getId().equals(workflow.getId())) {
                workflowList.setSelectedIndex(i);
                return;
            }
        }
    }

    private void showSelectedWorkflow() {
        Workflow workflow = workflowList.getSelectedValue();
        stepModel.clear();
        if (workflow == null) {
            headerTitle.setText("Select a workflow");
            headerSubtitle.setText(" ");
            return;
        }
        headerTitle.setText(workflow.getName());
        headerSubtitle.setText(describe(workflow));
        for (Task task : workflow.getSteps()) {
            stepModel.addElement(task);
        }
    }

    private String describe(Workflow workflow) {
        String desc = workflow.getDescription();
        String steps = workflow.stepCount() + " step" + (workflow.stepCount() == 1 ? "" : "s");
        String updated = "updated " + relativeTime(workflow.getUpdatedAt());
        return (desc == null || desc.isBlank() ? steps : desc + "  -  " + steps) + "  -  " + updated;
    }

    private static String relativeTime(Instant when) {
        Duration ago = Duration.between(when, Instant.now());
        long seconds = Math.max(0, ago.getSeconds());
        if (seconds < 60) {
            return "just now";
        }
        if (seconds < 3600) {
            return (seconds / 60) + " min ago";
        }
        if (seconds < 86400) {
            return (seconds / 3600) + " h ago";
        }
        return (seconds / 86400) + " d ago";
    }

    private void persist(Workflow workflow) {
        try {
            manager.save(workflow);
        } catch (WorkflowException e) {
            MessageDialog.show(this, "Could not save", e.getMessage(), MessageDialog.Kind.ERROR);
        }
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    // ---------- run log styling ----------

    private enum LogStyle {HEADER, INFO, SUCCESS, ERROR}

    private void appendLog(String text, LogStyle style) {
        FlowTheme.Palette p = FlowTheme.active().palette();
        Color color = switch (style) {
            case HEADER -> p.accent();
            case INFO -> p.foreground();
            case SUCCESS -> p.success();
            case ERROR -> p.danger();
        };
        StyledDocument doc = runLog.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        StyleConstants.setBold(attrs, style == LogStyle.HEADER);
        try {
            doc.insertString(doc.getLength(), text, attrs);
            runLog.setCaretPosition(doc.getLength());
        } catch (BadLocationException ignored) {
            // appending at the end never throws in practice
        }
    }

    // ---------- theming ----------

    private void applyTheme(FlowTheme theme) {
        try {
            theme.apply();
        } catch (Exception e) {
            MessageDialog.show(this, "Theme error",
                    "Could not apply theme: " + e.getMessage(), MessageDialog.Kind.ERROR);
            return;
        }
        SwingUtilities.updateComponentTreeUI(this);
        repaintChrome(theme.palette());
        setStatus("Theme: " + theme.getLabel());
    }

    private void repaintChrome(FlowTheme.Palette p) {
        ((JPanel) getContentPane()).setBorder(BorderFactory.createLineBorder(p.accent(), 1));
        getContentPane().setBackground(p.background());
        titleBar.applyPalette(p);
        titleBar.applyFont(FlowTheme.active().headingFont(Font.BOLD, 14f));

        for (JPanel surface : surfaces) {
            surface.setBackground(p.background());
        }
        headerTitle.setForeground(p.foreground());
        headerSubtitle.setForeground(FlowTheme.blend(p.foreground(), p.background(), 0.35f));
        statusLabel.setForeground(FlowTheme.blend(p.foreground(), p.background(), 0.25f));
        runLog.setBackground(FlowTheme.blend(p.surface(), p.background(), 0.3f));

        for (FlowButton button : buttons) {
            button.applyPalette(p);
        }
        if (themeSelector != null) {
            themeSelector.applyPalette(p);
        }
        repaint();
    }

    // ---------- styled helpers ----------

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
        label.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 0));
        return label;
    }

    private FlowButton primaryButton(String text) {
        FlowButton b = new FlowButton(text, FlowButton.Style.PRIMARY);
        buttons.add(b);
        return b;
    }

    private FlowButton secondaryButton(String text) {
        FlowButton b = new FlowButton(text, FlowButton.Style.SECONDARY);
        buttons.add(b);
        return b;
    }

    private FlowButton dangerButton(String text) {
        FlowButton b = new FlowButton(text, FlowButton.Style.DANGER);
        buttons.add(b);
        return b;
    }

    // ---------- cell renderers ----------

    private static class WorkflowCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean selected, boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected, focused);
            Workflow workflow = (Workflow) value;
            FlowTheme.Palette p = FlowTheme.active().palette();
            setText("<html><b>" + workflow.getName() + "</b><br><span style='font-size:9px'>"
                    + workflow.stepCount() + " step" + (workflow.stepCount() == 1 ? "" : "s")
                    + "</span></html>");
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            if (selected) {
                setBackground(p.accent());
                setForeground(p.accentText());
            } else {
                setBackground(p.background());
                setForeground(p.foreground());
            }
            return this;
        }
    }

    private static class StepCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean selected, boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected, focused);
            Task task = (Task) value;
            FlowTheme.Palette p = FlowTheme.active().palette();
            setText("<html><b>" + (index + 1) + ".  " + escape(task.getName())
                    + "</b>  <span style='font-size:10px'>[" + task.getType().getLabel()
                    + "]</span><br><span style='font-size:10px'>"
                    + escape(task.summary()) + "</span></html>");
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            if (selected) {
                setBackground(p.accent());
                setForeground(p.accentText());
            } else {
                setBackground(FlowTheme.blend(p.surface(), p.background(), 0.5f));
                setForeground(p.foreground());
            }
            return this;
        }

        private static String escape(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
