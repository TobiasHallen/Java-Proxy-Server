//package gui;
//
//import java.awt.BorderLayout;
//import java.awt.EventQueue;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.WindowAdapter;
//import java.awt.event.WindowEvent;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.io.PrintStream;
//import java.util.List;
//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import javax.swing.JButton;
//import javax.swing.JFileChooser;
//import javax.swing.JFrame;
//import javax.swing.JMenu;
//import javax.swing.JMenuBar;
//import javax.swing.JMenuItem;
//import javax.swing.JOptionPane;
//import javax.swing.JScrollPane;
//import javax.swing.JTextArea;
//import javax.swing.JTextField;
//import javax.swing.ScrollPaneConstants;
//import javax.swing.UIManager;
//import javax.swing.UnsupportedLookAndFeelException;
//import javax.swing.text.DefaultCaret;
//
//import proxy.Proxy;
//
///**
// * This class deals with the main GUI.
// * 
// * No comments here as all it does is create the windwos and adds event
// * listeners
// * 
// * @author julius
// * 
// */
//public class ProxyGUI {
//
//    private JFrame frmWebProxy;
//    final JFileChooser fc = new JFileChooser();
//    private JTextField txtAbout;
//    private JTextField infoField;
//    private static JTextArea txtInfoArea;
//    private Pattern patternIp;
//    private Matcher matcherIP;
//    private static JScrollPane txtInfoScrollPane;
//    static Proxy proxyMainThread;
//    public static OutputStream outputStream;
//    private static Lock l;
//    public static PrintStream originalSystemOut;
//
//    public static void main(String[] args) {
//	l = new ReentrantLock();
//	try {
//	    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//	} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
//		| UnsupportedLookAndFeelException e1) {
//	    e1.printStackTrace();
//	}
//	EventQueue.invokeLater(new Runnable() {
//	    public void run() {
//		try {
//		    ProxyGUI window = new ProxyGUI();
//		    window.frmWebProxy.setVisible(true);
//		} catch (Exception e) {
//		    e.printStackTrace();
//		}
//	    }
//	});
//	proxyMainThread = new Proxy(8085);
//	proxyMainThread.run();
//    }
//
//    public ProxyGUI() {
//	initialize();
//	setUpSystemOut();
//    }
//
//    private void setUpSystemOut() {
//	originalSystemOut = System.out;
//	System.setOut(new PrintStream(new OutputStream() {
//	    @Override
//	    public void write(int arg0) throws IOException {
//		addToInfoAread((char) arg0 + "", false);
//	    }
//	}));
//    }
//
//    private void initialize() {
//	patternIp = Pattern
//		.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
//	frmWebProxy = new JFrame();
//	frmWebProxy.addWindowListener(new WindowAdapter() {
//	    public void windowClosing(WindowEvent e) {
//		int confirmed = JOptionPane
//			.showConfirmDialog(
//				null,
//				"Are you sure you want to exit the program?\nThis will turn off the proxy.",
//				"Exit Program Message Box", JOptionPane.YES_NO_OPTION);
//
//		if (confirmed == JOptionPane.YES_OPTION) {
//		    System.setOut(originalSystemOut);
//		    frmWebProxy.dispose();
//		    proxyMainThread.closeServer();;
//		} else {
//		    frmWebProxy.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//		}
//	    }
//	});
//	frmWebProxy.setTitle("HTTP Proxy Manager");
//	frmWebProxy.setBounds(100, 100, 513, 400);
//	frmWebProxy.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//	frmWebProxy.setResizable(false);
//
//	JMenuBar menuBar = new JMenuBar();
//	frmWebProxy.setJMenuBar(menuBar);
//
//	JMenu mnFile = new JMenu("File");
//	menuBar.add(mnFile);
//
//	JMenuItem mntmOpenFile = new JMenuItem("Show Cache");
//	mntmOpenFile.addActionListener(new ActionListener() {
//	    public void actionPerformed(ActionEvent e) {
//		ProxyCacheManager manager = ProxyCacheManager.getInstance();
//		CacheInfoObject[] objs = manager.getAllCachedItems();
//		addToInfoAread(
//			"________________________CACHE_DUMP_START________________________________",
//			true);
//		for (int i = 0; i < objs.length; i++) {
//		    addToInfoAread("|" + objs[i].getDate().toString() + "|" + objs[i].getMaxAge()
//			    + "|" + objs[i].getKey(), true);
//		    addToInfoAread(
//			    "________________________________________________________________________________",
//			    true);
//		}
//		addToInfoAread(
//			"_________________________CACHE_DUMP_END_________________________________",
//			true);
//	    }
//	});
//	mnFile.add(mntmOpenFile);
//
//	JMenu mnEdit = new JMenu("Edit");
//	menuBar.add(mnEdit);
//
//	JMenuItem mnConfig = new JMenuItem("Config");
//	mnConfig.addActionListener(new ActionListener() {
//	    public void actionPerformed(ActionEvent e) {
//		ConfigEditGUI cui = new ConfigEditGUI();
//		cui.getMainFrame().setVisible(true);
//		cui.getMainFrame().setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
//	    }
//	});
//	mnEdit.add(mnConfig);
//
//	JMenu mnHelp = new JMenu("Help");
//	menuBar.add(mnHelp);
//
//	JMenuItem mntmAbout = new JMenuItem("About");
//	mntmAbout.addActionListener(new ActionListener() {
//	    public void actionPerformed(ActionEvent e) {
//		JFrame frmAbout = new JFrame("About HTTP Proxy");
//		int X = frmWebProxy.getWidth() / 2;
//		int Y = frmWebProxy.getHeight() / 2;
//		frmAbout.setBounds(X, Y, 300, 50);
//		frmAbout.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//		frmAbout.setResizable(false);
//		txtAbout = new JTextField();
//		txtAbout.setText("Created By Julius Mereckas for CS3031 -2015");
//		frmAbout.getContentPane().add(txtAbout, BorderLayout.CENTER);
//		txtAbout.setEditable(false);
//		txtAbout.setColumns(10);
//		frmAbout.setVisible(true);
//	    }
//	});
//	mnHelp.add(mntmAbout);
//	frmWebProxy.getContentPane().setLayout(null);
//
//	JButton btnBlockHost = new JButton("Block Host");
//	btnBlockHost.addActionListener(new ActionListener() {
//	    public void actionPerformed(ActionEvent e) {
//		String hostToBlock = infoField.getText();
//		if (!hostToBlock.isEmpty()) {
//		    infoField.setText("");
//		    int choice = JOptionPane.showConfirmDialog(null,
//			    "Are you sure you want block: " + hostToBlock, "Block Host",
//			    JOptionPane.YES_NO_OPTION);
//		    if (choice == JOptionPane.YES_OPTION) {
//			ProxyTrafficFilter.getInstance().addBlockedHost(hostToBlock);
//		    }
//		} else {
//		    txtInfoArea.append("You need to enter a host.\n");
//		}
//	    }
//	});
//	btnBlockHost.setBounds(12, 12, 155, 25);
//	frmWebProxy.getContentPane().add(btnBlockHost);
//
//	JButton btnBlockip = new JButton("Block IP");
//	btnBlockip.addActionListener(new ActionListener() {
//	    public void actionPerformed(ActionEvent e) {
//		String ipToBlock = infoField.getText();
//		if (!ipToBlock.isEmpty()) {
//		    infoField.setText("");
//		    int choice = JOptionPane.showConfirmDialog(null,
//			    "Are you sure you want block: " + ipToBlock, "Block IP",
//			    JOptionPane.YES_NO_OPTION);
//		    if (choice == JOptionPane.YES_OPTION) {
//			matcherIP = patternIp.matcher(ipToBlock);
//			if (matcherIP.matches()) {
//			    txtInfoArea.append("\n");
//			    ProxyTrafficFilter.getInstance().addBlockedIP(ipToBlock);
//			} else {
//			    txtInfoArea.append("Not valid ip\n");
//			}
//		    }
//		} else {
//		    txtInfoArea.append("You need to enter an IP.\n");
//		}
//	    }
//	});
//	btnBlockip.setBounds(12, 49, 155, 25);
//	frmWebProxy.getContentPane().add(btnBlockip);
//
//	JButton btnBlockPhrase = new JButton("Block phrase");
//	btnBlockPhrase.addActionListener(new ActionListener() {
//	    public void actionPerformed(ActionEvent e) {
//		String phraseToBlock = infoField.getText();
//		if (!phraseToBlock.isEmpty()) {
//		    phraseToBlock.trim();
//		    infoField.setText("");
//		    int choice = JOptionPane.showConfirmDialog(null,
//			    "Are you sure you want block: " + phraseToBlock, "Block phrase",
//			    JOptionPane.YES_NO_OPTION);
//		    if (choice == JOptionPane.YES_OPTION) {
//			ProxyTrafficFilter.getInstance().addBlockedPhrase(phraseToBlock);
//		    }
//		} else {
//		    txtInfoArea.append("You need to enter a phrase.\n");
//		}
//	    }
//	});
//	btnBlockPhrase.setBounds(12, 86, 155, 25);
//	frmWebProxy.getContentPane().add(btnBlockPhrase);
//
//	JButton btnUnblockHost = new JButton("Unblock Host");
//	btnUnblockHost.addActionListener(new ActionListener() {
//	    public void actionPerformed(ActionEvent e) {
//		String hostToUnblock = infoField.getText();
//		infoField.setText("");
//		int choice = JOptionPane.showConfirmDialog(null, "Are you sure you want unblock: "
//			+ hostToUnblock, "Unlock Host", JOptionPane.YES_NO_OPTION);
//		if (choice == JOptionPane.YES_OPTION) {
//		    if (ProxyTrafficFilter.getInstance().removeBlockedHost(hostToUnblock)) {
//			txtInfoArea.append("Unblocked: " + hostToUnblock + "\n");
//		    } else {
//			txtInfoArea.append("Unable to unblock: " + hostToUnblock + "\n");
//		    }
//		}
//	    }
//	});
//	btnUnblockHost.setBounds(179, 12, 155, 25);
//	frmWebProxy.getContentPane().add(btnUnblockHost);
//
//	JButton btnUnblockIp = new JButton("Unblock Ip");
//	btnUnblockIp.addActionListener(new ActionListener() {
//	    public void actionPerformed(ActionEvent e) {
//		String ipToUnblock = infoField.getText();
//		infoField.setText("");
//		int choice = JOptionPane.showConfirmDialog(null, "Are you sure you want unblock: "
//			+ ipToUnblock, "Unlock IP", JOptionPane.YES_NO_OPTION);
//		if (choice == JOptionPane.YES_OPTION) {
//		    if (ProxyTrafficFilter.getInstance().removeBlockedIP(ipToUnblock)) {
//			txtInfoArea.append("Unblocked: " + ipToUnblock + "\n");
//		    } else {
//			txtInfoArea.append("Unable to unblock: " + ipToUnblock + "\n");
//		    }
//		}
//	    }
//	});
//	btnUnblockIp.setBounds(179, 49, 155, 25);
//	frmWebProxy.getContentPane().add(btnUnblockIp);
//
//	JButton btnUnblockPhrase = new JButton("Unblock phrase");
//	btnUnblockPhrase.addActionListener(new ActionListener() {
//	    public void actionPerformed(ActionEvent e) {
//		String phraseToUnblock = infoField.getText();
//		phraseToUnblock.trim();
//		infoField.setText("");
//		int choice = JOptionPane.showConfirmDialog(null, "Are you sure you want unblock: "
//			+ phraseToUnblock, "Unlock phrase", JOptionPane.YES_NO_OPTION);
//		if (choice == JOptionPane.YES_OPTION) {
//		    if (ProxyTrafficFilter.getInstance().removeBlockedPhrase(phraseToUnblock)) {
//			txtInfoArea.append("Unblocked: " + phraseToUnblock + "\n");
//		    } else {
//			txtInfoArea.append("Unable to unblock: " + phraseToUnblock + "\n");
//		    }
//		}
//	    }
//	});
//	btnUnblockPhrase.setBounds(179, 86, 155, 25);
//	frmWebProxy.getContentPane().add(btnUnblockPhrase);
//
//	JButton btnListHost = new JButton("List");
//	btnListHost.addActionListener(new ActionListener() {
//	    public void actionPerformed(ActionEvent e) {
//		String s = "";
//		List<String> list = ProxyTrafficFilter.getInstance().getBlockedHostList();
//		for (int i = 0; i < list.size(); i++) {
//		    if (!list.get(i).isEmpty()) {
//			s += "[" + list.get(i) + "]";
//		    }
//		}
//		if (!s.isEmpty()) {
//		    txtInfoArea.append("Blocked host: " + s + "\n");
//		} else {
//		    txtInfoArea.append("Blocked host: " + "[]\n");
//		}
//	    }
//	});
//	btnListHost.setBounds(346, 12, 155, 25);
//	frmWebProxy.getContentPane().add(btnListHost);
//
//	JButton btnListIP = new JButton("List");
//	btnListIP.addActionListener(new ActionListener() {
//	    public void actionPerformed(ActionEvent e) {
//		String s = "";
//		List<String> list = ProxyTrafficFilter.getInstance().getBlockedIpList();
//		for (int i = 0; i < list.size(); i++) {
//		    if (!list.get(i).isEmpty()) {
//			s += "[" + list.get(i) + "]";
//		    }
//		}
//		if (!s.isEmpty()) {
//		    txtInfoArea.append("Blocked IP: " + s + "\n");
//		} else {
//		    txtInfoArea.append("Blocked IP: " + "[]\n");
//		}
//	    }
//	});
//	btnListIP.setBounds(346, 49, 155, 25);
//	frmWebProxy.getContentPane().add(btnListIP);
//
//	JButton btnListPhrase = new JButton("List");
//	btnListPhrase.addActionListener(new ActionListener() {
//	    public void actionPerformed(ActionEvent e) {
//		String s = "";
//		List<String> list = ProxyTrafficFilter.getInstance().getBlockedPhraseList();
//		for (int i = 0; i < list.size(); i++) {
//		    if (!list.get(i).isEmpty()) {
//			s += "[" + list.get(i) + "]";
//		    }
//		}
//		if (!s.isEmpty()) {
//		    txtInfoArea.append("Blocked phrase: " + s + "\n");
//		} else {
//		    txtInfoArea.append("Blocked phrase: " + "[]\n");
//		}
//	    }
//	});
//	btnListPhrase.setBounds(346, 86, 155, 25);
//	frmWebProxy.getContentPane().add(btnListPhrase);
//
//	txtInfoArea = new JTextArea(10, 40);
//	txtInfoArea.setLineWrap(true);
//	txtInfoScrollPane = new JScrollPane(txtInfoArea);
//	txtInfoScrollPane.setBounds(12, 162, 489, 150);
//	txtInfoScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
//	frmWebProxy.getContentPane().add(txtInfoScrollPane);
//	DefaultCaret caret = (DefaultCaret) txtInfoArea.getCaret();
//	caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
//
//	infoField = new JTextField();
//	infoField.setBounds(12, 123, 489, 32);
//	frmWebProxy.getContentPane().add(infoField);
//	infoField.setColumns(10);
//
//    }
//
//    public static void addToInfoAread(String s, boolean addNewLine) {
//	l.lock();
//	try {
//	    if (addNewLine) {
//		s += "\n";
//	    }
//	    txtInfoArea.append(s);
//	} finally {
//	    l.unlock();
//	}
//    }
//}