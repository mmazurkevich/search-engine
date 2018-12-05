package org.search.app;

//import org.search.engine.SearchEngine;
//import org.search.engine.reduximpl.SearchEngine;
import org.search.engine.SearchEngine;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.stream.Collectors;

public class SwingExample extends JFrame {

    private SearchEngine main = new SearchEngine();

    public SwingExample() {
        initUI();
    }

    private void initUI() {

        createMenuBar();
        JButton button = new JButton("Search");
        JTextField textField = new JTextField();
        JTextArea textField1 = new JTextArea();

        setTitle("Search Engine App");
        setSize(600, 400);
        JPanel panel = new JPanel();

        textField.setPreferredSize(new Dimension(480, 28));
        panel.add(textField);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchResult = main.search(textField.getText()).stream().collect(Collectors.joining("\n"));
                textField1.setText(searchResult);
            }
        });
        panel.add(button);

        textField1.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textField1, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(560, 300));
        panel.add(scrollPane);

        getContentPane().add(panel);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void createMenuBar() {
        JMenuBar menubar = new JMenuBar();

        JMenu fileMenu = new JMenu("Index");
        JMenuItem folderIndexMenuItem = new JMenuItem("Index folder");
        folderIndexMenuItem.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select folder for indexation");

            if (chooser.showDialog(this, "Index") == JFileChooser.APPROVE_OPTION) {
                System.out.println(chooser.getSelectedFile().getPath());
                main.indexFolder(chooser.getSelectedFile().getPath());
            }
        });

        JMenuItem fileIndexMenuItem = new JMenuItem("Index file");
        fileIndexMenuItem.addActionListener(event -> {
            JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            jfc.setDialogTitle("Select file for indexation");

            int returnValue = jfc.showDialog(this, "Index");
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                System.out.println(jfc.getSelectedFile().getPath());
                main.indexFile(jfc.getSelectedFile().getPath());
            }
        });

        fileMenu.add(fileIndexMenuItem);
        fileMenu.add(folderIndexMenuItem);
        menubar.add(fileMenu);

        setJMenuBar(menubar);
    }

    public static void main(String[] args) {
        SwingExample ex = new SwingExample();
        ex.setVisible(true);
    }
}
