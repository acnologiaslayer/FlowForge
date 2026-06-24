package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.model.ExecutionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes text content to a file on disk. Both the path and the content may
 * contain {@code ${variable}} placeholders resolved at run time, so a
 * workflow can, for example, write out a total computed by an earlier step.
 */
public class WriteFileTask extends Task {

    private final String path;
    private final String content;
    private final boolean append;

    public WriteFileTask(String name, String path, String content, boolean append)
            throws InvalidTaskConfigurationException {
        super(name);
        if (path == null || path.isBlank()) {
            throw new InvalidTaskConfigurationException("File path must not be blank.");
        }
        this.path = path.trim();
        this.content = content == null ? "" : content;
        this.append = append;
    }

    public String getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }

    public boolean isAppend() {
        return append;
    }

    @Override
    public TaskType getType() {
        return TaskType.WRITE_FILE;
    }

    @Override
    protected String execute(ExecutionContext context) throws IOException {
        Path target = Path.of(context.interpolate(path));
        String resolved = context.interpolate(content);
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        if (append) {
            Files.writeString(target, resolved + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } else {
            Files.writeString(target, resolved);
        }
        return (append ? "Appended to " : "Wrote ") + target.toAbsolutePath();
    }

    @Override
    public String summary() {
        return "Write File: " + path + (append ? " (append)" : "");
    }

    @Override
    public Map<String, String> toFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("path", path);
        fields.put("content", content);
        fields.put("append", String.valueOf(append));
        return fields;
    }
}
