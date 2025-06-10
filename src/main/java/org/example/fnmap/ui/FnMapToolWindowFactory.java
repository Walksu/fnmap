package org.example.fnmap.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class FnMapToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 传入project参数
        FnMapPanel fnMapPanel = new FnMapPanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(fnMapPanel, "FnMap", false);
        toolWindow.getContentManager().addContent(content);
    }
}