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

public class BookMarkManage {
    private Project project;

    public BookMarkManage() {}

    public BookMarkManage(Project project) {
        this.project = project;
    }

    public void showBookmarkManageWindow() {
        JFrame frame = new JFrame("书签管理");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null); // 居中显示

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("这里是书签管理窗口的内容"), BorderLayout.CENTER);

        frame.add(panel);
        frame.setVisible(true);
    }

    /**
     * 为指定的组件添加鼠标右键弹出菜单功能。
     * 当鼠标右键点击组件时，会显示一个包含预定义选项的弹出菜单。
     *
     * @param component 需要添加右键菜单功能的组件。
     * @param onAddBookmark 添加书签时的回调函数
     */
    public void addRightClickPopupMenu(JComponent component, Consumer<BookmarkItem> onAddBookmark) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    JPopupMenu popupMenu = new JPopupMenu();
                    JMenuItem add = new JMenuItem("添加书签");
                    add.addActionListener(actionEvent -> {
                        // 获取当前文件和行号
                        BookmarkItem bookmarkItem = getCurrentFileInfo();
                        if (bookmarkItem != null) {
                            // 弹出输入对话框让用户输入注释
                            String comment = JOptionPane.showInputDialog(
                                    component,
                                    "请输入书签注释:",
                                    "添加书签",
                                    JOptionPane.PLAIN_MESSAGE
                            );

                            if (comment != null && !comment.trim().isEmpty()) {
                                bookmarkItem.setComment(comment.trim());
                                onAddBookmark.accept(bookmarkItem);
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
    private BookmarkItem getCurrentFileInfo() {
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

                return new BookmarkItem(
                        virtualFile.getPath(),
                        virtualFile.getName(),
                        lineNumber,
                        "",  // 注释稍后设置
                        lineContent
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
    public void navigateToBookmark(BookmarkItem bookmark) {
        if (project == null) return;

        try {
            VirtualFile virtualFile = project.getBaseDir().getFileSystem()
                    .findFileByPath(bookmark.getFilePath());

            if (virtualFile != null && virtualFile.exists()) {
                OpenFileDescriptor descriptor = new OpenFileDescriptor(
                        project,
                        virtualFile,
                        bookmark.getLineNumber() - 1,  // OpenFileDescriptor的行号从0开始
                        0
                );
                descriptor.navigate(true);
            } else {
                JOptionPane.showMessageDialog(null, "文件不存在: " + bookmark.getFilePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "跳转失败: " + e.getMessage());
        }
    }

    /**
     * 书签项数据类
     */
    public static class BookmarkItem {
        private String filePath;
        private String fileName;
        private int lineNumber;
        private String comment;
        private String linePreview;

        public BookmarkItem(String filePath, String fileName, int lineNumber, String comment, String linePreview) {
            this.filePath = filePath;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.comment = comment;
            this.linePreview = linePreview;
        }

        // Getters and Setters
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }

        public String getLinePreview() { return linePreview; }
        public void setLinePreview(String linePreview) { this.linePreview = linePreview; }

        @Override
        public String toString() {
            return comment + " (" + fileName + ":" + lineNumber + ")";
        }
    }
}
