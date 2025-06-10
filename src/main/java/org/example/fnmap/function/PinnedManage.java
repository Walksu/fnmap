package org.example.fnmap.function;

import javax.swing.*;
import java.awt.*;

public class PinnedManage {
    public void showPinnedManageWindow() {
        JFrame frame = new JFrame("置顶管理");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null); // 居中显示

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("这里是置顶管理窗口的内容"), BorderLayout.CENTER);

        frame.add(panel);
        frame.setVisible(true);
    }
}
