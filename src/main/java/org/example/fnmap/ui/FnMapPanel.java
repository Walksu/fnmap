package org.example.fnmap.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import org.example.fnmap.function.FileTree;
import org.example.fnmap.function.BookMarkManage;
import org.example.fnmap.function.TodoManage;
import org.example.fnmap.function.PinnedManage;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FnMapPanel extends JPanel {
    private JPanel cardPanel;
    private CardLayout cardLayout;
    private Project project;
    private FileTree fileTree;
    private JPanel structurePanel1;
    private JPanel structurePanel2;
    private MessageBusConnection connection;

    private java.util.List<BookMarkManage.BookmarkItem> bookmarks = new java.util.ArrayList<>();
    private JPanel bookmarkPanel;
    private JPanel bookmarkPanelAll;
    private BookMarkManage bookMarkManage;

    // 在类的成员变量部分添加：
    private java.util.List<TodoManage.TodoItem> todos = new java.util.ArrayList<>();
    private JPanel todoPanel;
    private JPanel todoPanelAll;
    private TodoManage todoManage;

    public FnMapPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        this.fileTree = new FileTree(project);
        this.bookMarkManage = new BookMarkManage(project); // 初始化时传入project
        this.todoManage = new TodoManage(project);

        initializeUI();
        setupFileEditorListener();

        // 初始加载当前文件结构
        updateFileStructure();
    }

    private void initializeUI() {
        // 按钮
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        Icon UserIcon = IconLoader.getIcon("/icons/user.svg", FnMapPanel.class);
        Icon ListIcon = IconLoader.getIcon("/icons/list.svg", FnMapPanel.class);

        JButton UserButton = new JButton(UserIcon);
        UserButton.setToolTipText("用户面板");
        JButton ListButton = new JButton(ListIcon);
        ListButton.setToolTipText("功能列表");

        buttonPanel.add(UserButton);
        buttonPanel.add(ListButton);
        add(buttonPanel, BorderLayout.NORTH);

        // 下拉菜单
        String[] options = {"位置", "名称", "类型"};
        JComboBox<String> dropdown = new JComboBox<>(options);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(dropdown);

        // 搜索框
        JTextField searchField = new JTextField(15);
        searchField.setText("搜索...");

        Icon searchIcon = IconLoader.getIcon("/icons/search.svg", FnMapPanel.class);
        JLabel searchIconLabel = new JLabel(searchIcon);

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchIconLabel, BorderLayout.EAST);

        controlPanel.add(searchPanel);

        // 导航栏
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new GridLayout(1, 3, 5, 0));

        JButton symbolButton = new JButton("符号");
        JButton currentButton = new JButton("当前");
        JButton allButton = new JButton("所有");

        Dimension buttonSize = new Dimension(100, 30);
        symbolButton.setPreferredSize(buttonSize);
        currentButton.setPreferredSize(buttonSize);
        allButton.setPreferredSize(buttonSize);

        symbolButton.setHorizontalAlignment(SwingConstants.CENTER);
        currentButton.setHorizontalAlignment(SwingConstants.CENTER);
        allButton.setHorizontalAlignment(SwingConstants.CENTER);

        navPanel.add(symbolButton);
        navPanel.add(currentButton);
        navPanel.add(allButton);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // 创建符号内容面板
        JPanel symbolContent = createSymbolContent();

        // 为Current界面实现手风琴效果
        JPanel currentContent = createCurrentContent();

        // 为All界面实现手风琴效果
        JPanel allContent = createAllContent();

        cardPanel.add(symbolContent, "Symbol");
        cardPanel.add(currentContent, "Current");
        cardPanel.add(allContent, "All");

        JPanel mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BoxLayout(mainContentPanel, BoxLayout.Y_AXIS));
        mainContentPanel.add(controlPanel);
        mainContentPanel.add(navPanel);
        mainContentPanel.add(cardPanel);

        removeAll();
        add(buttonPanel, BorderLayout.NORTH);
        add(mainContentPanel, BorderLayout.CENTER);

        cardLayout.show(cardPanel, "Symbol");

        symbolButton.addActionListener(e -> cardLayout.show(cardPanel, "Symbol"));
        currentButton.addActionListener(e -> cardLayout.show(cardPanel, "Current"));
        allButton.addActionListener(e -> cardLayout.show(cardPanel, "All"));
    }

    /**
     * 创建符号内容面板
     */
    private JPanel createSymbolContent() {
        JPanel symbolContent = new JPanel();
        symbolContent.setLayout(new GridLayout(2, 1, 5, 5)); // 2行1列，间距5

        // 结构树区域1 - 当前文件结构
        structurePanel1 = new JPanel(new BorderLayout());
        structurePanel1.setBorder(BorderFactory.createTitledBorder("当前文件结构"));

        // 添加刷新按钮
        JPanel panel1Header = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton1 = new JButton("🔄");
        refreshButton1.setToolTipText("刷新结构");
        refreshButton1.addActionListener(e -> updateFileStructure());
        panel1Header.add(refreshButton1);

        structurePanel1.add(panel1Header, BorderLayout.NORTH);
        structurePanel1.add(new JLabel("加载中..."), BorderLayout.CENTER);
        symbolContent.add(structurePanel1);

        // 结构树区域2 - 项目概览或其他信息
        structurePanel2 = new JPanel(new BorderLayout());
        structurePanel2.setBorder(BorderFactory.createTitledBorder("项目信息"));

        JPanel projectInfoPanel = createProjectInfoPanel();
        structurePanel2.add(projectInfoPanel, BorderLayout.CENTER);
        symbolContent.add(structurePanel2);

        return symbolContent;
    }

    /**
     * 创建项目信息面板
     */
    private JPanel createProjectInfoPanel() {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        infoPanel.add(new JLabel("项目名称: " + project.getName()));
        infoPanel.add(new JLabel("项目路径: " + project.getBasePath()));

        // 添加一些项目统计信息
        JButton analyzeButton = new JButton("分析项目结构");
        analyzeButton.addActionListener(e -> analyzeProject());
        infoPanel.add(analyzeButton);

        return infoPanel;
    }

    /**
     * 分析项目结构（可以扩展更多功能）
     */
    private void analyzeProject() {
        JOptionPane.showMessageDialog(this,
                "项目分析功能正在开发中...\n" +
                        "未来将包括：\n" +
                        "- 类数量统计\n" +
                        "- 方法复杂度分析\n" +
                        "- 依赖关系图\n" +
                        "- 代码质量报告",
                "项目分析",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 创建当前内容面板 - 修改版本
     */
    private JPanel createCurrentContent() {
        JPanel currentContent = new JPanel();
        currentContent.setLayout(new BoxLayout(currentContent, BoxLayout.Y_AXIS));

        // 书签部分
        JButton bookmarkButton = new JButton("书签");
        bookmarkButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        bookmarkButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, bookmarkButton.getPreferredSize().height));

        // 创建 BookMarkManage 实例并添加右键菜单
        bookMarkManage.addRightClickPopupMenu(bookmarkButton, this::addBookmark);

        // 管理按钮
        JButton bookmarkManage = new JButton("管理");
        bookmarkManage.setAlignmentX(Component.LEFT_ALIGNMENT);
        bookmarkManage.setMaximumSize(new Dimension(Integer.MAX_VALUE, bookmarkManage.getPreferredSize().height));

        // 将书签按钮和管理按钮放在一个水平面板中
        JPanel buttonRowPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonRowPanel.add(bookmarkButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);
        buttonRowPanel.add(bookmarkManage, gbc);

        buttonRowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttonRowPanel.getPreferredSize().height));

        // 书签内容面板
        bookmarkPanel = new JPanel();
        bookmarkPanel.setLayout(new BoxLayout(bookmarkPanel, BoxLayout.Y_AXIS));
        bookmarkPanel.setVisible(false);
        bookmarkPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bookmarkPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, bookmarkPanel.getPreferredSize().height));

        currentContent.add(buttonRowPanel);
        currentContent.add(bookmarkPanel);

        // 置顶部分
        JButton pinnedButton = new JButton("置顶");
        pinnedButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        pinnedButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, pinnedButton.getPreferredSize().height));

        // 新增按钮
        JButton pinnedManage = new JButton("管理");
        pinnedManage.setAlignmentX(Component.LEFT_ALIGNMENT);
        pinnedManage.setMaximumSize(new Dimension(Integer.MAX_VALUE, pinnedManage.getPreferredSize().height));

        // 将置顶按钮和新按钮放在一个水平面板中
        JPanel buttonRowPanel2 = new JPanel(new GridBagLayout()); // 使用GridBagLayout
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(0, 0, 0, 5); // 按钮之间的间距

        // 配置置顶按钮
        gbc2.gridx = 0;
        gbc2.gridy = 0;
        gbc2.weightx = 1.0; // 允许水平拉伸
        gbc2.fill = GridBagConstraints.HORIZONTAL; // 水平填充
        buttonRowPanel2.add(pinnedButton, gbc2);

        // 配置新按钮
        gbc2.gridx = 1;
        gbc2.gridy = 0;
        gbc2.weightx = 0.0; // 不允许水平拉伸
        gbc2.fill = GridBagConstraints.NONE; // 不填充
        gbc2.insets = new Insets(0, 0, 0, 0); // 移除新按钮右侧的间距
        buttonRowPanel2.add(pinnedManage, gbc2);

        buttonRowPanel2.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRowPanel2.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttonRowPanel2.getPreferredSize().height));

        JPanel pinnedPanel = new JPanel();
        pinnedPanel.add(new JLabel("这是置顶界面内容"));
        pinnedPanel.setVisible(false);
        pinnedPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        pinnedPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, pinnedPanel.getPreferredSize().height));

        currentContent.add(buttonRowPanel2); // 添加包含两个按钮的面板
        currentContent.add(pinnedPanel);

        JButton todoButton = new JButton("待办");
        todoButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        todoButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, todoButton.getPreferredSize().height));

        // 为待办按钮添加右键菜单
        todoManage.addRightClickPopupMenu(todoButton, this::addTodo);

        // 管理按钮
        JButton todoManage = new JButton("管理");
        todoManage.setAlignmentX(Component.LEFT_ALIGNMENT);
        todoManage.setMaximumSize(new Dimension(Integer.MAX_VALUE, todoManage.getPreferredSize().height));

        // 将待办按钮和管理按钮放在一个水平面板中
        JPanel buttonRowPanel3 = new JPanel(new GridBagLayout());
        GridBagConstraints gbc3 = new GridBagConstraints();
        gbc3.insets = new Insets(0, 0, 0, 5);

        gbc3.gridx = 0;
        gbc3.gridy = 0;
        gbc3.weightx = 1.0;
        gbc3.fill = GridBagConstraints.HORIZONTAL;
        buttonRowPanel3.add(todoButton, gbc3);

        gbc3.gridx = 1;
        gbc3.gridy = 0;
        gbc3.weightx = 0.0;
        gbc3.fill = GridBagConstraints.NONE;
        gbc3.insets = new Insets(0, 0, 0, 0);
        buttonRowPanel3.add(todoManage, gbc3);

        buttonRowPanel3.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRowPanel3.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttonRowPanel3.getPreferredSize().height));

        // 待办内容面板
        todoPanel = new JPanel();
        todoPanel.setLayout(new BoxLayout(todoPanel, BoxLayout.Y_AXIS));
        todoPanel.setVisible(false);
        todoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        todoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, todoPanel.getPreferredSize().height));

        currentContent.add(buttonRowPanel3);
        currentContent.add(todoPanel);

        currentContent.add(Box.createVerticalGlue());

        // 添加事件监听器
        bookmarkButton.addActionListener(e -> togglePanel(bookmarkPanel, currentContent));
        bookmarkManage.addActionListener(e -> {
            // 为书签管理按钮添加事件处理逻辑
            new BookMarkManage().showBookmarkManageWindow();
        });
        pinnedButton.addActionListener(e -> togglePanel(pinnedPanel, currentContent));
        pinnedManage.addActionListener(e -> {
            // 为置顶管理按钮添加事件处理逻辑
            new PinnedManage().showPinnedManageWindow();
        });
        todoButton.addActionListener(e -> togglePanel(todoPanel, currentContent));
        todoManage.addActionListener(e -> {
            // 为待办管理按钮添加事件处理逻辑
            new TodoManage().showTodoManageWindow();
        });

        return currentContent;
    }

    /**
     * 创建所有内容面板 - 修改版本
     */
    private JPanel createAllContent() {
        JPanel allContent = new JPanel();
        allContent.setLayout(new BoxLayout(allContent, BoxLayout.Y_AXIS));

        // 书签部分
        JButton bookmarkButton = new JButton("书签");
        bookmarkButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        bookmarkButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, bookmarkButton.getPreferredSize().height));

        // 创建 BookMarkManage 实例并添加右键菜单
        bookMarkManage.addRightClickPopupMenu(bookmarkButton, this::addBookmark);

        // 管理按钮
        JButton bookmarkManage = new JButton("管理");
        bookmarkManage.setAlignmentX(Component.LEFT_ALIGNMENT);
        bookmarkManage.setMaximumSize(new Dimension(Integer.MAX_VALUE, bookmarkManage.getPreferredSize().height));

        // 将书签按钮和管理按钮放在一个水平面板中
        JPanel buttonRowPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonRowPanel.add(bookmarkButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);
        buttonRowPanel.add(bookmarkManage, gbc);

        buttonRowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttonRowPanel.getPreferredSize().height));

        // 书签内容面板
        bookmarkPanelAll = new JPanel();
        bookmarkPanelAll.setLayout(new BoxLayout(bookmarkPanelAll, BoxLayout.Y_AXIS));
        bookmarkPanelAll.setVisible(false);
        bookmarkPanelAll.setAlignmentX(Component.LEFT_ALIGNMENT);
        bookmarkPanelAll.setMaximumSize(new Dimension(Integer.MAX_VALUE, bookmarkPanelAll.getPreferredSize().height));

        allContent.add(buttonRowPanel);
        allContent.add(bookmarkPanelAll);

        // 置顶部分
        JButton pinnedButton = new JButton("置顶");
        pinnedButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        pinnedButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, pinnedButton.getPreferredSize().height));

        // 新增按钮
        JButton pinnedManage = new JButton("管理");
        pinnedManage.setAlignmentX(Component.LEFT_ALIGNMENT);
        pinnedManage.setMaximumSize(new Dimension(Integer.MAX_VALUE, pinnedManage.getPreferredSize().height));

        // 将置顶按钮和新按钮放在一个水平面板中
        JPanel buttonRowPanel2 = new JPanel(new GridBagLayout()); // 使用GridBagLayout
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(0, 0, 0, 5); // 按钮之间的间距

        // 配置置顶按钮
        gbc2.gridx = 0;
        gbc2.gridy = 0;
        gbc2.weightx = 1.0; // 允许水平拉伸
        gbc2.fill = GridBagConstraints.HORIZONTAL; // 水平填充
        buttonRowPanel2.add(pinnedButton, gbc2);

        // 配置新按钮
        gbc2.gridx = 1;
        gbc2.gridy = 0;
        gbc2.weightx = 0.0; // 不允许水平拉伸
        gbc2.fill = GridBagConstraints.NONE; // 不填充
        gbc2.insets = new Insets(0, 0, 0, 0); // 移除新按钮右侧的间距
        buttonRowPanel2.add(pinnedManage, gbc2);

        buttonRowPanel2.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRowPanel2.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttonRowPanel2.getPreferredSize().height));

        JPanel pinnedPanel = new JPanel();
        pinnedPanel.add(new JLabel("这是置顶界面内容"));
        pinnedPanel.setVisible(false);
        pinnedPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        pinnedPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, pinnedPanel.getPreferredSize().height));

        allContent.add(buttonRowPanel2); // 添加包含两个按钮的面板
        allContent.add(pinnedPanel);

        // 待办部分
        JButton todoButton = new JButton("待办");
        todoButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        todoButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, todoButton.getPreferredSize().height));

        // 为待办按钮添加右键菜单
        todoManage.addRightClickPopupMenu(todoButton, this::addTodo);

        // 管理按钮
        JButton todoManage = new JButton("管理");
        todoManage.setAlignmentX(Component.LEFT_ALIGNMENT);
        todoManage.setMaximumSize(new Dimension(Integer.MAX_VALUE, todoManage.getPreferredSize().height));

        // 将待办按钮和管理按钮放在一个水平面板中
        JPanel buttonRowPanel3 = new JPanel(new GridBagLayout());
        GridBagConstraints gbc3 = new GridBagConstraints();
        gbc3.insets = new Insets(0, 0, 0, 5);

        gbc3.gridx = 0;
        gbc3.gridy = 0;
        gbc3.weightx = 1.0;
        gbc3.fill = GridBagConstraints.HORIZONTAL;
        buttonRowPanel3.add(todoButton, gbc3);

        gbc3.gridx = 1;
        gbc3.gridy = 0;
        gbc3.weightx = 0.0;
        gbc3.fill = GridBagConstraints.NONE;
        gbc3.insets = new Insets(0, 0, 0, 0);
        buttonRowPanel3.add(todoManage, gbc3);

        buttonRowPanel3.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRowPanel3.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttonRowPanel3.getPreferredSize().height));

        // 待办内容面板
        todoPanelAll = new JPanel();
        todoPanelAll.setLayout(new BoxLayout(todoPanelAll, BoxLayout.Y_AXIS));
        todoPanelAll.setVisible(false);
        todoPanelAll.setAlignmentX(Component.LEFT_ALIGNMENT);
        todoPanelAll.setMaximumSize(new Dimension(Integer.MAX_VALUE, todoPanelAll.getPreferredSize().height));

        allContent.add(buttonRowPanel3);
        allContent.add(todoPanelAll);

        allContent.add(Box.createVerticalGlue());

        // 添加事件监听器
        bookmarkButton.addActionListener(e -> togglePanel(bookmarkPanelAll, allContent));
        bookmarkManage.addActionListener(e -> {
            // 为书签管理按钮添加事件处理逻辑
            new BookMarkManage().showBookmarkManageWindow();
        });
        pinnedButton.addActionListener(e -> togglePanel(pinnedPanel, allContent));
        pinnedManage.addActionListener(e -> {
            // 为置顶管理按钮添加事件处理逻辑
            new PinnedManage().showPinnedManageWindow();
        });
        todoButton.addActionListener(e -> togglePanel(todoPanelAll, allContent));
        todoManage.addActionListener(e -> {
            // 为待办管理按钮添加事件处理逻辑
            new TodoManage().showTodoManageWindow();
        });

        return allContent;
    }

    /**
     * 添加书签
     */
    private void addBookmark(BookMarkManage.BookmarkItem bookmark) {
        bookmarks.add(bookmark);
        updateBookmarkPanels();
    }

    /**
     * 更新书签面板显示
     */
    private void updateBookmarkPanels() {
        updateBookmarkPanel(bookmarkPanel);
        if (bookmarkPanelAll != null) {
            updateBookmarkPanel(bookmarkPanelAll);
        }
    }

    /**
     * 更新指定的书签面板
     */
    private void updateBookmarkPanel(JPanel panel) {
        if (panel == null) return;

        panel.removeAll();

        for (int i = 0; i < bookmarks.size(); i++) {
            BookMarkManage.BookmarkItem bookmark = bookmarks.get(i);
            JPanel bookmarkItemPanel = createBookmarkItemPanel(bookmark, i);
            panel.add(bookmarkItemPanel);
        }

        panel.revalidate();
        panel.repaint();
    }

    /**
     * 创建单个书签项面板
     */
    private JPanel createBookmarkItemPanel(BookMarkManage.BookmarkItem bookmark, int index) {
        JPanel itemPanel = new JPanel(new BorderLayout());
        itemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // 主要信息面板
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        // 注释标签
        JLabel commentLabel = new JLabel(bookmark.getComment());
        commentLabel.setFont(commentLabel.getFont().deriveFont(Font.BOLD));
        infoPanel.add(commentLabel);

        // 文件信息标签
        JLabel fileInfoLabel = new JLabel(bookmark.getFileName() + ":" + bookmark.getLineNumber());
        fileInfoLabel.setFont(fileInfoLabel.getFont().deriveFont(Font.PLAIN, 10f));
        fileInfoLabel.setForeground(Color.GRAY);
        infoPanel.add(fileInfoLabel);

        // 代码预览标签
        if (bookmark.getLinePreview() != null && !bookmark.getLinePreview().isEmpty()) {
            JLabel previewLabel = new JLabel(bookmark.getLinePreview());
            previewLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
            previewLabel.setForeground(Color.DARK_GRAY);
            infoPanel.add(previewLabel);
        }

        itemPanel.add(infoPanel, BorderLayout.CENTER);

        // 删除按钮
        JButton deleteButton = new JButton("×");
        deleteButton.setPreferredSize(new Dimension(20, 20));
        deleteButton.setFont(deleteButton.getFont().deriveFont(Font.BOLD));
        deleteButton.setForeground(Color.RED);
        deleteButton.addActionListener(e -> {
            bookmarks.remove(index);
            updateBookmarkPanels();
        });
        itemPanel.add(deleteButton, BorderLayout.EAST);

        // 添加点击事件 - 跳转到书签位置
        itemPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) { // 单击跳转
                    bookMarkManage.navigateToBookmark(bookmark);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                itemPanel.setBackground(new Color(230, 230, 250));
                itemPanel.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                itemPanel.setBackground(null);
                itemPanel.repaint();
            }
        });

        // 设置鼠标指针
        itemPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return itemPanel;
    }

    /**
     * 切换面板显示状态
     */
    private void togglePanel(JPanel panel, JPanel container) {
        panel.setVisible(!panel.isVisible());
        if (panel.isVisible()) {
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        }
        container.revalidate();
        container.repaint();
    }

    /**
     * 设置文件编辑器监听器
     */
    private void setupFileEditorListener() {
        connection = project.getMessageBus().connect();
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void selectionChanged(FileEditorManagerEvent event) {
                // 当文件切换时更新结构树
                SwingUtilities.invokeLater(() -> updateFileStructure());
            }
        });
    }

    /**
     * 更新文件结构显示
     */
    private void updateFileStructure() {
        if (structurePanel1 == null) return;

        // 在后台线程中生成结构树，避免阻塞UI
        SwingUtilities.invokeLater(() -> {
            try {
                // 移除之前的内容
                structurePanel1.removeAll();

                // 添加刷新按钮
                JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton refreshButton = new JButton("🔄");
                refreshButton.setToolTipText("刷新结构");
                refreshButton.addActionListener(e -> updateFileStructure());
                headerPanel.add(refreshButton);
                structurePanel1.add(headerPanel, BorderLayout.NORTH);

                // 获取当前文件结构树
                JTree tree = fileTree.getCurrentFileStructureTree();
                JScrollPane scrollPane = new JScrollPane(tree);
                scrollPane.setPreferredSize(new Dimension(250, 200));

                structurePanel1.add(scrollPane, BorderLayout.CENTER);

                // 刷新UI
                structurePanel1.revalidate();
                structurePanel1.repaint();

            } catch (Exception e) {
                // 错误处理
                structurePanel1.removeAll();
                JLabel errorLabel = new JLabel("加载结构时出错: " + e.getMessage());
                errorLabel.setForeground(Color.RED);
                structurePanel1.add(errorLabel, BorderLayout.CENTER);
                structurePanel1.revalidate();
                structurePanel1.repaint();
            }
        });
    }

    /**
     * 添加待办
     */
    private void addTodo(TodoManage.TodoItem todo) {
        todos.add(todo);
        updateTodoPanels();
    }

    /**
     * 更新待办面板显示
     */
    private void updateTodoPanels() {
        updateTodoPanel(todoPanel);
        if (todoPanelAll != null) {
            updateTodoPanel(todoPanelAll);
        }
    }

    /**
     * 更新指定的待办面板
     */
    private void updateTodoPanel(JPanel panel) {
        if (panel == null) return;

        panel.removeAll();

        for (int i = 0; i < todos.size(); i++) {
            TodoManage.TodoItem todo = todos.get(i);
            JPanel todoItemPanel = createTodoItemPanel(todo, i);
            panel.add(todoItemPanel);
        }

        panel.revalidate();
        panel.repaint();
    }

    /**
     * 创建单个待办项面板
     */
    private JPanel createTodoItemPanel(TodoManage.TodoItem todo, int index) {
        JPanel itemPanel = new JPanel(new BorderLayout());
        itemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        // 左侧复选框
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(todo.isCompleted());
        checkBox.addActionListener(e -> {
            todo.setCompleted(checkBox.isSelected());
            updateTodoPanels(); // 刷新显示
        });

        // 主要信息面板
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        // 待办内容标签
        JLabel contentLabel = new JLabel(todo.getContent());
        contentLabel.setFont(contentLabel.getFont().deriveFont(Font.BOLD));

        // 如果已完成，添加删除线效果
        if (todo.isCompleted()) {
            contentLabel.setText("<html><strike>" + todo.getContent() + "</strike></html>");
            contentLabel.setForeground(Color.GRAY);
        }

        infoPanel.add(contentLabel);

        // 优先级和文件信息面板
        JPanel metaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        // 优先级标签
        JLabel priorityLabel = new JLabel("[" + todo.getPriority().getDisplayName() + "]");
        priorityLabel.setFont(priorityLabel.getFont().deriveFont(Font.PLAIN, 10f));
        priorityLabel.setForeground(todo.getPriority().getColor());
        metaPanel.add(priorityLabel);

        // 文件信息标签
        JLabel fileInfoLabel = new JLabel(" " + todo.getFileName() + ":" + todo.getLineNumber());
        fileInfoLabel.setFont(fileInfoLabel.getFont().deriveFont(Font.PLAIN, 10f));
        fileInfoLabel.setForeground(Color.GRAY);
        metaPanel.add(fileInfoLabel);

        infoPanel.add(metaPanel);

        // 代码预览标签
        if (todo.getLinePreview() != null && !todo.getLinePreview().isEmpty()) {
            JLabel previewLabel = new JLabel(todo.getLinePreview());
            previewLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
            previewLabel.setForeground(Color.DARK_GRAY);
            infoPanel.add(previewLabel);
        }

        // 右侧按钮面板
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        // 编辑按钮
        JButton editButton = new JButton("✎");
        editButton.setPreferredSize(new Dimension(25, 20));
        editButton.setFont(editButton.getFont().deriveFont(Font.BOLD));
        editButton.setToolTipText("编辑待办");
        editButton.addActionListener(e -> editTodo(todo, index));

        // 删除按钮
        JButton deleteButton = new JButton("×");
        deleteButton.setPreferredSize(new Dimension(25, 20));
        deleteButton.setFont(deleteButton.getFont().deriveFont(Font.BOLD));
        deleteButton.setForeground(Color.RED);
        deleteButton.setToolTipText("删除待办");
        deleteButton.addActionListener(e -> {
            todos.remove(index);
            updateTodoPanels();
        });

        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        itemPanel.add(checkBox, BorderLayout.WEST);
        itemPanel.add(infoPanel, BorderLayout.CENTER);
        itemPanel.add(buttonPanel, BorderLayout.EAST);

        // 添加点击事件 - 跳转到待办位置
        itemPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) { // 单击跳转
                    todoManage.navigateToTodo(todo);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                itemPanel.setBackground(new Color(230, 250, 230));
                itemPanel.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                itemPanel.setBackground(null);
                itemPanel.repaint();
            }
        });

        // 设置鼠标指针
        itemPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return itemPanel;
    }

    /**
     * 编辑待办
     */
    private void editTodo(TodoManage.TodoItem todo, int index) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // 待办内容
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("待办内容:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JTextField contentField = new JTextField(todo.getContent(), 20);
        panel.add(contentField, gbc);

        // 优先级
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        panel.add(new JLabel("优先级:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        JComboBox<TodoManage.TodoItem.Priority> priorityCombo = new JComboBox<>(TodoManage.TodoItem.Priority.values());
        priorityCombo.setSelectedItem(todo.getPriority());
        panel.add(priorityCombo, gbc);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "编辑待办",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String newContent = contentField.getText().trim();
            if (!newContent.isEmpty()) {
                todo.setContent(newContent);
                todo.setPriority((TodoManage.TodoItem.Priority) priorityCombo.getSelectedItem());
                updateTodoPanels();
            }
        }
    }

    /**
     * 清理资源
     */
    public void dispose() {
        if (connection != null) {
            connection.disconnect();
        }
    }
}
