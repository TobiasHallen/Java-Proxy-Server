//package gui;
//
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.io.File;
//import java.text.Format;
//
//import javax.swing.JButton;
//import javax.swing.JFileChooser;
//import javax.swing.JFormattedTextField;
//import javax.swing.JFrame;
//import javax.swing.JLabel;
//import javax.swing.JRadioButton;
//import javax.swing.JTextField;
//import javax.swing.UIManager;
//import javax.swing.UnsupportedLookAndFeelException;
//
//
//public class ConfigEditGUI {
//
//    private JFrame frame;
//    private JTextField txtError;
//    private JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
//    private JFormattedTextField maxBufferField;
//
//    public JFrame getMainFrame() {
//	return frame;
//    }
//
//    public ConfigEditGUI() {
//	try {
//	    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//	} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
//		| UnsupportedLookAndFeelException e) {
//	    e.printStackTrace();
//	}
//	initialize();
//    }
//
//    private void initialize() {
//	fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
//	fc.setFileHidingEnabled(false);
//
//	frame = new JFrame();
//	frame.setBounds(100, 100, 450, 300);
//	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//	frame.getContentPane().setLayout(null);
//	frame.setResizable(false);
//
//	JLabel lblPort = new JLabel("Port");
//	lblPort.setBounds(12, 12, 142, 15);
//	frame.getContentPane().add(lblPort);
//
//	JLabel lblLogging = new JLabel("Logging Enabled");
//	lblLogging.setBounds(12, 61, 142, 15);
//	frame.getContentPane().add(lblLogging);
//
//	JLabel lblFiltering = new JLabel("Filtering");
//	lblFiltering.setBounds(12, 88, 142, 15);
//	frame.getContentPane().add(lblFiltering);
//
//	JButton btnLogFie = new JButton("Select");
//	btnLogFie.addActionListener(new ActionListener() {
//	    public void actionPerformed(ActionEvent e) {
//		int returnVal = fc.showOpenDialog(null);
//
//		if (returnVal == JFileChooser.APPROVE_OPTION) {
//		    File file = fc.getSelectedFile();
//		    String oldPath = ProxySettings.getInstance().getPathToLog();
//		    ProxySettings.getInstance().setLogFilePath(file.getAbsolutePath());
//		    ProxyLogger.getInstance().log(ProxyLogLevel.INFO,
//			    "Changed log path from: " + oldPath + " to: " + file.getPath());
//		} else {
//		    fc.setEnabled(false);
//		}
//	    }
//	});
//	btnLogFie.setBounds(165, 110, 117, 25);
//	frame.getContentPane().add(btnLogFie);
//
//	JButton btnBlockedHost = new JButton("Select");
//	btnBlockedHost.addActionListener(new ActionListener() {
//	    public void actionPerformed(ActionEvent e) {
//	    }
//	});
//	btnBlockedHost.setBounds(165, 138, 117, 25);
//	frame.getContentPane().add(btnBlockedHost);
//
//	final JFormattedTextField proxyPortField = new JFormattedTextField((Format) null);
//	proxyPortField.setBounds(165, 12, 123, 19);
//	frame.getContentPane().add(proxyPortField);
//	proxyPortField.setText(new String(ProxySettings.getInstance().getProxyPort() + ""));
//
//	final JRadioButton rbtnLoggingEnabled = new JRadioButton("");
//	rbtnLoggingEnabled.setSelected(ProxySettings.getInstance().isLoggingEnabled());
//	rbtnLoggingEnabled.setBounds(165, 57, 149, 23);
//	frame.getContentPane().add(rbtnLoggingEnabled);
//
//	final JRadioButton rbtnFilteringEnabled = new JRadioButton("");
//	rbtnFilteringEnabled.setSelected(ProxySettings.getInstance().isFilteringEnabled());
//	rbtnFilteringEnabled.setBounds(165, 80, 149, 23);
//	frame.getContentPane().add(rbtnFilteringEnabled);
//
//	JLabel lblLogFile = new JLabel("Log file");
//	lblLogFile.setBounds(12, 115, 142, 15);
//	frame.getContentPane().add(lblLogFile);
//
//	JLabel lblBlockedHostFile = new JLabel("Blocked host file");
//	lblBlockedHostFile.setBounds(12, 143, 142, 15);
//	frame.getContentPane().add(lblBlockedHostFile);
//
//	JLabel lblFilterFile = new JLabel("Filter file");
//	lblFilterFile.setBounds(12, 170, 142, 15);
//	frame.getContentPane().add(lblFilterFile);
//
//	JButton btnFilterFile = new JButton("Select");
//	btnFilterFile.setBounds(165, 165, 117, 25);
//	frame.getContentPane().add(btnFilterFile);
//
//	JLabel lblBufferSize = new JLabel("Buffer size");
//	lblBufferSize.setBounds(12, 39, 70, 15);
//	frame.getContentPane().add(lblBufferSize);
//
//	maxBufferField = new JFormattedTextField((Format) null);
//	maxBufferField.setText(new String(ProxySettings.getInstance().getMaxBuffer() + ""));
//	maxBufferField.setBounds(165, 37, 123, 19);
//	frame.getContentPane().add(maxBufferField);
//
//	JLabel lblLogPath = new JLabel("");
//	lblLogPath.setBounds(300, 120, 136, 15);
//	frame.getContentPane().add(lblLogPath);
//	lblLogPath.setText(ProxySettings.getInstance().getPathToLog());
//
//	JLabel blbBlockedFilePath = new JLabel("");
//	blbBlockedFilePath.setBounds(300, 143, 136, 15);
//	frame.getContentPane().add(blbBlockedFilePath);
//	blbBlockedFilePath.setText(ProxySettings.getInstance().getPathToBlocked());
//
//	JLabel lblFilterFilePath = new JLabel("");
//	lblFilterFilePath.setBounds(300, 170, 136, 15);
//	frame.getContentPane().add(lblFilterFilePath);
//	lblFilterFilePath.setText(ProxySettings.getInstance().getPathToFilter());
//
//	JButton btnSave = new JButton("Save");
//	btnSave.addActionListener(new ActionListener() {
//	    public void actionPerformed(ActionEvent e) {
//		try {
//		    int port = Integer.parseInt(proxyPortField.getText());
//		    int bufSize = Integer.parseInt(maxBufferField.getText());
//		    boolean log = rbtnLoggingEnabled.isSelected();
//		    boolean filter = rbtnFilteringEnabled.isSelected();
//		    ProxySettings.getInstance().setProxyPort(port);
//		    ProxySettings.getInstance().setMaxBuffer(bufSize);
//		    ProxySettings.getInstance().setLoggingEnabled(log);
//		    ProxySettings.getInstance().setFilteringEnabled(filter);
//		    ProxySettings.getInstance().writeConfigHostIpPhraseDataToFile();
//		} catch (NumberFormatException error) {
//		    txtError.setText("Error parsting port or buffer size"
//			    + error.getLocalizedMessage());
//		}
//	    }
//	});
//	btnSave.setBounds(165, 236, 117, 25);
//	frame.getContentPane().add(btnSave);
//
//	txtError = new JTextField();
//	txtError.setEditable(false);
//	txtError.setBounds(12, 197, 424, 30);
//	frame.getContentPane().add(txtError);
//	txtError.setColumns(10);
//    }
//}