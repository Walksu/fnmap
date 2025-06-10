//package org.example.fnmap.function;
//
//import javax.swing.*;
//import java.awt.*;
//
//public class TodoManage {
//    public void showTodoManageWindow() {
//        JFrame frame = new JFrame("待办管理");
//        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//        frame.setSize(400, 300);
//        frame.setLocationRelativeTo(null); // 居中显示
//
//        JPanel panel = new JPanel(new BorderLayout());
//        panel.add(new JLabel("这里是待办管理窗口的内容"), BorderLayout.CENTER);
//
//        frame.add(panel);
//        frame.setVisible(true);
//    }
//}


package org.example.fnmap.function;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class TodoManage {
    private Project project;

    public TodoManage() {}

    public TodoManage(Project project) {
        this.project = project;
    }

    public void showTodoManageWindow() {
        JFrame frame = new JFrame("待办管理");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null); // 居中显示

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("这里是待办管理窗口的内容"), BorderLayout.CENTER);

        frame.add(panel);
        frame.setVisible(true);
    }

    /**
     * 为指定的组件添加鼠标右键弹出菜单功能。
     * 当鼠标右键点击组件时，会显示一个包含预定义选项的弹出菜单。
     *
     * @param component 需要添加右键菜单功能的组件。
     * @param onAddTodo 添加待办时的回调函数
     */
    public void addRightClickPopupMenu(JComponent component, Consumer<TodoItem> onAddTodo) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    JPopupMenu popupMenu = new JPopupMenu();
                    JMenuItem add = new JMenuItem("添加待办");
                    add.addActionListener(actionEvent -> {
                        // 获取当前文件和行号
                        TodoItem todoItem = getCurrentFileInfo();
                        if (todoItem != null) {
                            // 弹出输入对话框让用户输入待办内容
                            String content = JOptionPane.showInputDialog(
                                    component,
                                    "请输入待办内容:",
                                    "添加待办",
                                    JOptionPane.PLAIN_MESSAGE
                            );

                            if (content != null && !content.trim().isEmpty()) {
                                todoItem.setContent(content.trim());
                                onAddTodo.accept(todoItem);
                            }
                        } else {
                            JOptionPane.showMessageDialog(component, "无法获取当前文件信息");
                        }
                    });
                    popupMenu.add(add);

                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    /**
     * 获取当前文件信息
     */
    private TodoItem getCurrentFileInfo() {
        if (project == null) return null;

        try {
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            Editor editor = fileEditorManager.getSelectedTextEditor();

            if (editor != null) {
                Document document = editor.getDocument();
                VirtualFile virtualFile = fileEditorManager.getSelectedFiles()[0];

                // 获取当前光标行号
                int caretOffset = editor.getCaretModel().getOffset();
                int lineNumber = document.getLineNumber(caretOffset) + 1; // 行号从1开始

                // 获取当前行内容作为预览
                int lineStartOffset = document.getLineStartOffset(lineNumber - 1);
                int lineEndOffset = document.getLineEndOffset(lineNumber - 1);
                String lineContent = document.getText().substring(lineStartOffset, lineEndOffset).trim();

                return new TodoItem(
                        virtualFile.getPath(),
                        virtualFile.getName(),
                        lineNumber,
                        "",  // 待办内容稍后设置
                        lineContent,
                        TodoItem.Priority.MEDIUM, // 默认优先级
                        false // 默认未完成
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 跳转到指定文件的指定行
     */
    public void navigateToTodo(TodoItem todo) {
        if (project == null) return;

        try {
            VirtualFile virtualFile = project.getBaseDir().getFileSystem()
                    .findFileByPath(todo.getFilePath());

            if (virtualFile != null && virtualFile.exists()) {
                OpenFileDescriptor descriptor = new OpenFileDescriptor(
                        project,
                        virtualFile,
                        todo.getLineNumber() - 1,  // OpenFileDescriptor的行号从0开始
                        0
                );
                descriptor.navigate(true);
            } else {
                JOptionPane.showMessageDialog(null, "文件不存在: " + todo.getFilePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "跳转失败: " + e.getMessage());
        }
    }

    /**
     * 待办项数据类
     */
    public static class TodoItem {
        private String filePath;
        private String fileName;
        private int lineNumber;
        private String content;
        private String linePreview;
        private Priority priority;
        private boolean completed;
        private long createdTime;

        public enum Priority {
            HIGH("高", Color.RED),
            MEDIUM("中", Color.ORANGE),
            LOW("低", Color.GREEN);

            private final String displayName;
            private final Color color;

            Priority(String displayName, Color color) {
                this.displayName = displayName;
                this.color = color;
            }

            public String getDisplayName() { return displayName; }
            public Color getColor() { return color; }
        }

        public TodoItem(String filePath, String fileName, int lineNumber, String content,
                        String linePreview, Priority priority, boolean completed) {
            this.filePath = filePath;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.content = content;
            this.linePreview = linePreview;
            this.priority = priority;
            this.completed = completed;
            this.createdTime = System.currentTimeMillis();
        }

        // Getters and Setters
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getLinePreview() { return linePreview; }
        public void setLinePreview(String linePreview) { this.linePreview = linePreview; }

        public Priority getPriority() { return priority; }
        public void setPriority(Priority priority) { this.priority = priority; }

        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }

        public long getCreatedTime() { return createdTime; }
        public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }

        @Override
        public String toString() {
            return (completed ? "[完成] " : "") + content + " (" + fileName + ":" + lineNumber + ")";
        }
    }
}
