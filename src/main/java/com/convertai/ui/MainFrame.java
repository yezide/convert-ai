package com.convertai.ui;

import com.convertai.service.DirectoryConversionService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainFrame extends JFrame {
    private final DirectoryConversionService conversionService;
    private final JTextField directoryField = new JTextField();
    private final JTextArea logArea = new JTextArea();
    private final JButton selectButton = new JButton("选择目录");
    private final JButton startButton = new JButton("开始转换");
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public MainFrame(DirectoryConversionService conversionService) {
        super("Convert AI");
        this.conversionService = conversionService;
        initUi();
    }

    private void initUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        topPanel.add(new JLabel("剧集目录"), BorderLayout.WEST);
        directoryField.setEditable(false);
        topPanel.add(directoryField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.add(selectButton);
        buttonPanel.add(startButton);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        selectButton.addActionListener(event -> chooseDirectory());
        startButton.addActionListener(event -> startConvert());
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("选择需要转换的剧集目录");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            directoryField.setText(chooser.getSelectedFile().toPath().toString());
        }
    }

    private void startConvert() {
        if (directoryField.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "请先选择目录。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        setWorking(true);
        logArea.setText("");
        Path directory = Path.of(directoryField.getText());

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                conversionService.convert(directory, message -> publish(message));
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String chunk : chunks) {
                    appendLog(chunk);
                }
            }

            @Override
            protected void done() {
                setWorking(false);
                try {
                    get();
                    appendLog("任务完成。");
                    JOptionPane.showMessageDialog(MainFrame.this, "处理完成，结果已输出到 convert 子目录。", "完成", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    e.printStackTrace();
                    appendLog("任务失败: " + cause.getMessage());
                    JOptionPane.showMessageDialog(MainFrame.this, cause.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void setWorking(boolean working) {
        SwingUtilities.invokeLater(() -> {
            selectButton.setEnabled(!working);
            startButton.setEnabled(!working);
        });
    }

    private void appendLog(String message) {
        logArea.append("[" + LocalDateTime.now().format(formatter) + "] " + message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
