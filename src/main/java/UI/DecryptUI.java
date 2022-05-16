package UI;

import Constants.Constants;
import JarHelper.JarResReader;
import Security.Aes;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DecryptUI extends JFrame {

    private JPanel progressBarPanel;
    private JTextField outputPathText;
    private JPanel outputPathPanel;
    private JScrollPane fileScrollPane;
    private JProgressBar progressBar;
    private JButton fileChooserButton;
    private JFileChooser outputPathChooser;

    private JPanel passwordPanel;
    private JPanel buttonPanel;
    private JPanel menuPanel;
    private JButton startButton;
    private JButton stopButton;
    private JPasswordField passwordField;
    private JLabel decodeTimeLimitDisplayTile;

    private JTable fileTable;
    private String[] column;
    private String outputDir = "";
    private String[][] data;
    private ArrayList<ArrayList<String>> dataList;
    private boolean TERMINATE_FLAG = false;

    JPanel titlePanel;
    private final JarResReader jarResReader = new JarResReader();

    public DecryptUI() throws URISyntaxException, IOException {
        loadContent();
        initSwingObject();
        initButtonListener();
        finalizeSwingObject();
    }

    private boolean verifyProfileJarMode() throws IOException, InvalidAlgorithmParameterException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        JarResReader jarResReader = new JarResReader();
        try {
            List<Path> result = jarResReader.getPathsFromResourceJAR(Constants.PROFILE_KEYWORD);
            for (Path path : result) {
                if(Constants.DEBUG_MODE) System.out.println("Path : " + path);

                String filePathInJAR = path.toString();
                if (filePathInJAR.startsWith("/")) {
                    filePathInJAR = filePathInJAR.substring(1);
                }

                if(Constants.DEBUG_MODE) System.out.println("filePathInJAR : " + filePathInJAR);
                StringBuilder sb = new StringBuilder();
                InputStream is = jarResReader.getFileFromResourceAsStream(filePathInJAR);
                try (InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
                     BufferedReader reader = new BufferedReader(streamReader)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                } catch (IOException e) {
                    is.close();
                    e.printStackTrace();
                }
                is.close();
                String jsonString = sb.toString();
                String expireDate = handleJson(jsonString.getBytes(StandardCharsets.UTF_8));
                if(Constants.DEBUG_MODE) System.out.println("now: " + LocalDateTime.now() + " exp: " + expireDate);
                if (LocalDateTime.now().isBefore(LocalDateTime.parse(expireDate))) {
                    return true;
                } else {
                    JOptionPane.showMessageDialog(menuPanel, "软件过期");
                    return false;
                }

            }
        }
        catch (URISyntaxException | IOException | ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String handleJson(byte[] bytes) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, ParseException {
        String decryptedJsonString = new String((Aes.decrypt(bytes, String.valueOf(passwordField.getPassword()))), StandardCharsets.UTF_8);
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(decryptedJsonString);
        return jsonObject.get("expireDate") + "T23:59:59.999";
    }

    private void initSwingObject() {
        setTitle("刻录文件解密工具");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        menuPanel = new JPanel();
        titlePanel = new JPanel();
        JLabel fileDisplayTitle = new JLabel("需要解密的文件:", SwingConstants.LEFT);
        JLabel passwordDisplayTitle = new JLabel("解密密码");
        startButton = new JButton("开始");
        stopButton = new JButton("停止");
        buttonPanel = new JPanel();
        passwordField = new JPasswordField();
        passwordPanel = new JPanel();
        JLabel outputPathDisplayTile = new JLabel("解密后保存到本地路径");
        progressBar = new JProgressBar();
        progressBarPanel = new JPanel();
        decodeTimeLimitDisplayTile = new JLabel("剩余解密次数:"+"无限" );
        outputPathChooser = new JFileChooser();
        fileChooserButton = new JButton("浏览");
        JLabel progressBarDisplayTile = new JLabel("解密进度");
        outputPathText = new JTextField();
        outputPathPanel = new JPanel();
        fileTable = new JTable(data, column);
        fileScrollPane = new JScrollPane(fileTable);
        fileTable.setShowGrid(true);
        fileTable.setBounds(30,40,500,300);
        fileTable.getColumn("完成状态").setPreferredWidth(60);
        fileTable.getColumn("文件路径").setPreferredWidth(440);
        fileTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

        //resizeTable();

        menuPanel.setPreferredSize(new Dimension(540, 490));
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.PAGE_AXIS));


        titlePanel.setPreferredSize(new Dimension(540, 20));
        titlePanel.setAlignmentX(SwingConstants.LEFT);
        titlePanel.add(fileDisplayTitle);

        startButton.setPreferredSize(new Dimension(80, 30));
        stopButton.setPreferredSize(new Dimension(80, 30));
        stopButton.setEnabled(false);
        startButton.setEnabled(true);

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        passwordField.setPreferredSize(new Dimension(200, 20));

        passwordPanel.add(passwordDisplayTitle);
        passwordPanel.add(passwordField);

        outputPathText.setPreferredSize(new Dimension(300,20));
        outputPathText.setEnabled(false);

        outputPathPanel.add(outputPathDisplayTile);
        outputPathPanel.add(outputPathText);
        outputPathPanel.add(fileChooserButton);

        progressBar.setBounds(40,40,160,30);
        progressBar.setValue(0);
        progressBar.setMaximum(fileTable.getRowCount());
        progressBar.setStringPainted(true);

        progressBarPanel.add(progressBarDisplayTile);
        progressBarPanel.add(progressBar);



    }

    private void finalizeSwingObject() {
        add(menuPanel);
        menuPanel.add(titlePanel);
        menuPanel.add(fileScrollPane);
        menuPanel.add(passwordPanel);
        menuPanel.add(outputPathPanel);
        menuPanel.add(progressBarPanel);
        menuPanel.add(decodeTimeLimitDisplayTile);
        menuPanel.add(buttonPanel);
        pack();
        setVisible(true);

    }

    private void initButtonListener() {
        startButton.addActionListener(e -> {
            boolean verified = false;
            try {
                verified = verifyProfileJarMode();
            }
            catch (BadPaddingException | IOException | InvalidAlgorithmParameterException | IllegalBlockSizeException | NoSuchPaddingException  | NoSuchAlgorithmException | InvalidKeyException ex) {
                JOptionPane.showMessageDialog(menuPanel, "密码错误",null,JOptionPane.WARNING_MESSAGE,null);
            }
            TERMINATE_FLAG = false;
            if (!Objects.equals(outputDir, "")) {
                if (verified) {
                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                    try {
                        decryptJarMode();
                    }
                    catch (BadPaddingException |IOException | InvalidAlgorithmParameterException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException ex) {
                        JOptionPane.showMessageDialog(menuPanel, "密码错误",null,JOptionPane.WARNING_MESSAGE,null);
                        deleteDirectory(new File(outputDir));
                    }
                }
            }
            else
            {
                JOptionPane.showMessageDialog(menuPanel, "请选择保存路径");
            }
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        });

        stopButton.addActionListener(e ->
        {
            TERMINATE_FLAG = true;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            //todo terminate all decrypt process and delete generated files
        });

        fileChooserButton.addActionListener(e -> {
            outputPathChooser.setCurrentDirectory(new File("."));
            outputPathChooser.setDialogTitle("选择输出路径");
            outputPathChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            outputPathChooser.setAcceptAllFileFilterUsed(false);

            if (JFileChooser.APPROVE_OPTION == outputPathChooser.showOpenDialog(outputPathChooser)) {
                if(Constants.DEBUG_MODE) System.out.println("getCurrentDirectory(): "
                        +  outputPathChooser.getCurrentDirectory());
                if(Constants.DEBUG_MODE) System.out.println("getSelectedFile() : "
                        +  outputPathChooser.getSelectedFile());
                outputPathText.setText(String.valueOf(outputPathChooser.getSelectedFile()));
                outputDir = String.valueOf(outputPathChooser.getSelectedFile());
            }
            else {
                if(Constants.DEBUG_MODE) System.out.println("No Selection ");
            }
        });

    }

    public void readFilesFromFolderJarMode() throws URISyntaxException, IOException {
        List<Path> paths = jarResReader.getPathsFromResourceJAR(Constants.DIR_KEYWORD);

        for (Path path : paths) {
            if(Constants.DEBUG_MODE) System.out.println("Path : " + path);

            String filePathInJAR = path.toString();

            if (filePathInJAR.startsWith("/")) {
                filePathInJAR = filePathInJAR.substring(1);
            }
            if(Constants.DEBUG_MODE) System.out.println("filePathInJAR : " + filePathInJAR);
            ArrayList<String> temp = new ArrayList<>();
            temp.add(filePathInJAR);
            temp.add("等待");
            dataList.add(temp);
        }

    }

    private void loadContent() throws URISyntaxException, IOException {
        dataList = new ArrayList<>();
        readFilesFromFolderJarMode();
        column = new String[]{"文件路径", "完成状态"};
        data = dataList.stream()
                .map(l -> l.toArray(new String[0]))
                .toArray(String[][]::new);
    }

    private void decryptJarMode() throws IOException, InvalidAlgorithmParameterException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, InterruptedException {
        int progressVal= 0;
        for (ArrayList<String> subArray : dataList) {
            byte[] decryptedContent = null;

            try {
                InputStream is = jarResReader.getFileFromResourceAsStream(subArray.get(0));
                byte[] data = IOUtils.toByteArray(is);
                is.close();
                decryptedContent = Aes.decrypt(data, String.valueOf(passwordField.getPassword()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            String outputFileName = outputDir + File.separator + subArray.get(0);
            Path dir = Paths.get(outputFileName.substring(0,outputFileName.lastIndexOf("/")));
            if(Constants.DEBUG_MODE) {
                System.out.println("output Dir: " + dir);
                System.out.println("output file name: " + outputFileName);
            }
            Files.createDirectories(dir);
            assert decryptedContent != null;
            Files.write(Paths.get(outputFileName), decryptedContent);
            if(Constants.DEBUG_MODE) System.out.println("RESULT written as "+ outputFileName);

            data[progressVal][1]= "完成";
            progressVal += 1;
            progressBar.setValue(progressVal);
            progressBar.repaint();
            fileScrollPane.repaint();
            fileTable.repaint();
            }

            if(TERMINATE_FLAG)
            {
                if(Constants.DEBUG_MODE) System.err.println("TERMINATED");
                deleteDirectory(new File(outputDir));
            }

        }

    public void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                if(file.toString().contains(Constants.DIR_KEYWORD))
                {
                    deleteDirectory(file);
                }

            }
        }
        directoryToBeDeleted.delete();
    }



}
