import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.text.DefaultCaret;




public class HTTPProxy implements Runnable{

	private static JFrame proxyGUI;

	//creates instance of HTTP proxy
	public static void main(String[] args) throws UnsupportedLookAndFeelException 
	{
		UIManager.setLookAndFeel(UIManager.getLookAndFeel());
		HTTPProxy proxy = new HTTPProxy(8080);
		EventQueue.invokeLater(new Runnable() {
		    public void run() {
			try {
				proxy.initialize();
				proxyGUI.setVisible(true);
			} catch (Exception e) {
			    e.printStackTrace();
			}
		    }
		});
		proxy.listen();	
	}

	final JFileChooser fc = new JFileChooser();
	private JTextField txtAbout;
	private JTextField infoField;
	private static JTextArea txtInfoArea;
	private Pattern patternIp;
	private Matcher matcherIP;
	private static JScrollPane txtInfoScrollPane;


	private ServerSocket serverSocket;	 
	private volatile boolean running = true;	 
	static HashMap<String, File> cache;
	static HashMap<String, String> blockList; 
	static ArrayList<Thread> threadList;

	FileWriter exceptionWriter;
	PrintWriter exceptionPW;


	@SuppressWarnings("unchecked")
	public HTTPProxy(int port) {
		cache = new HashMap<>();
		blockList = new HashMap<>();
		threadList = new ArrayList<>();
		System.setOut(new MyPrintStream(System.out));
		new Thread(this).start();	

		try
		{
			RandomAccessFile raf = new RandomAccessFile("exception.txt", "rw");
			raf.setLength(0);
			raf.close();
			exceptionWriter=new FileWriter("exception.txt",true);
			exceptionPW=new PrintWriter(exceptionWriter,true);
			File cacheFile = new File("cacheList.txt");
			if(!cacheFile.exists())
			{
				cacheFile.createNewFile();
			} 
			else 
			{
				FileInputStream fileStream = new FileInputStream(cacheFile);
				ObjectInputStream objectStream = new ObjectInputStream(fileStream);
				cache = (HashMap<String,File>)objectStream.readObject();
				fileStream.close();
				objectStream.close();
			}
			File blockFile = new File("blockedList.txt");
			if(!blockFile.exists())
			{
				blockFile.createNewFile();
			} 
			else 
			{
				FileInputStream fileStream = new FileInputStream(blockFile);
				ObjectInputStream objectStream = new ObjectInputStream(fileStream);
				blockList = (HashMap<String, String>)objectStream.readObject();
				fileStream.close();
				objectStream.close();
			}
		} 
		catch (IOException e) {
			exceptionPW.write("Error loading previously cached sites file");
			exceptionPW.write(new Date().toString()); // Adding the date
			exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
			e.printStackTrace(exceptionPW);
		} 
		catch (ClassNotFoundException e) {
			exceptionPW.write("Class not found loading in preivously cached sites file");
			exceptionPW.write(new Date().toString()); // Adding the date
			exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
			e.printStackTrace(exceptionPW);
		}

		try 
		{
			serverSocket = new ServerSocket(port);
			System.out.println("Proxy Started using port " + serverSocket.getLocalPort() + ".");
			running = true;
		} 
		catch (SocketException e) {
			exceptionPW.write("Socket Error");
			exceptionPW.write(new Date().toString()); // Adding the date
			exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
			e.printStackTrace(exceptionPW);
		}
		catch (SocketTimeoutException e) {
			exceptionPW.write("Timeout Error\n");
			exceptionPW.write(new Date().toString()); // Adding the date
			exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
			e.printStackTrace(exceptionPW);
		} 
		catch (IOException e) {
			System.out.println("Read/Write Error");
			exceptionPW.write(new Date().toString()); // Adding the date
			exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
			e.printStackTrace(exceptionPW);
		}
	}

	public void listen(){
		while(running){
			try {
				Socket s = serverSocket.accept();
				Thread t = new Thread(new HTTPRequestHandler(s));				
				threadList.add(t);			
				t.start();	
			} catch (SocketException e) {
				System.out.println("Server Shut Down...");
			} catch (IOException e) {
				exceptionPW.write(new Date().toString()); // Adding the date
				exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
				e.printStackTrace(exceptionPW);
			}
		}
	}

	public void shutDownProxy(){
		System.out.println("Shutting down Proxy..");
		running = false;
		try{
			FileOutputStream fileStream = new FileOutputStream("cacheList.txt");
			ObjectOutputStream objectStream = new ObjectOutputStream(fileStream);

			objectStream.writeObject(cache);
			System.out.println("Cashed Sites Saved");

			fileStream = new FileOutputStream("blockedList.txt");
			objectStream = new ObjectOutputStream(fileStream);
			objectStream.writeObject(blockList);
			objectStream.close();
			fileStream.close();
			System.out.println("Blocked sites saved");
			try
			{
				//go through and close all the threads
				Iterator<Thread> i=threadList.iterator();
				while(i.hasNext())
				{
					if(i.next().isAlive())
					{
						i.next().join();
					}
				}
			} catch (InterruptedException e) {
				exceptionPW.write(new Date().toString()); // Adding the date
				exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
				e.printStackTrace(exceptionPW);
			} catch (ConcurrentModificationException e) {
				exceptionPW.write(new Date().toString()); // Adding the date
				exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
				e.printStackTrace(exceptionPW);
			}

		} catch (IOException e) {
			exceptionPW.write("File read/write error");
			exceptionPW.write(new Date().toString()); // Adding the date
			exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
			e.printStackTrace(exceptionPW);
		}
		try
		{
			serverSocket.close();
		} catch (Exception e) {
			exceptionPW.write("Error closing socket");
			exceptionPW.write(new Date().toString()); // Adding the date
			exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
			e.printStackTrace(exceptionPW);
		}
	}

	public static File searchCache(String url)
	{
		return cache.get(url);
	}

	public static void addToCache(String url, File file)
	{
		cache.put(url, file);
	}
	
	public static void addBlocked(String url)
	{
		blockList.put(url, url);
	}

	public static boolean blocked (String url)
	{
		if(blockList.containsKey(url)) return true;
		else return false;
	}
	
	public static boolean blockedPhrase (String url)
	{
		for(String s : blockList.values())
		{
			if(url.contains(s))return true;
		}	
		return false;
	}

	@Override
	public void run() {
		Scanner scanner = new Scanner(System.in);
		String command;
		while(running){
			System.out.println("Enter new site to block, or type \"blocked\" to see blocked sites, \"cached\" to see cached sites, or \"close\" to close server.");
			command = scanner.nextLine();
			if(command.toLowerCase().equals("blocked")){
				System.out.println("\nCurrently Blocked Sites");
				for(String key : blockList.keySet()){
					System.out.println(key);
				}
				System.out.println();
			} 
			else if(command.toLowerCase().equals("cached")){
				System.out.println("\nCurrently Cached Sites");
				for(String key : cache.keySet()){
					System.out.println(key);
				}
				System.out.println();
			}
			else if(command.equals("close")){
				running = false;
				shutDownProxy();
			}
			else {
				blockList.put(command, command);
				System.out.println("\n" + command + " blocked successfully \n");
			}
		}
		scanner.close();
	} 

	private void initialize() {
		patternIp = Pattern
				.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
		proxyGUI = new JFrame();
		proxyGUI.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				int confirmed = JOptionPane
						.showConfirmDialog(
								null,
								"Are you sure you want to exit the program?\nThis will turn off the proxy.",
								"Exit Program Message Box", JOptionPane.YES_NO_OPTION);

				if (confirmed == JOptionPane.YES_OPTION) {
					System.setOut(System.out);
					proxyGUI.dispose();
					shutDownProxy();
				} else {
					proxyGUI.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				}
			}
		});
		proxyGUI.setTitle("HTTP Proxy Manager");
		proxyGUI.setBounds(100, 100, 513, 400);
		proxyGUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		proxyGUI.setResizable(false);

		JMenuBar menuBar = new JMenuBar();
		proxyGUI.setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmOpenFile = new JMenuItem("Show Cache");
		mntmOpenFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addToInfoAread(
						"________________________CACHE_DUMP_START________________________________",
						true);
				for (String key : cache.keySet()) 
				{
					addToInfoAread(key+"", true);
				}
				addToInfoAread(
						"_________________________CACHE_DUMP_END_________________________________",
						true);
			}
		});
		mnFile.add(mntmOpenFile);

		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);

//		JMenuItem mnConfig = new JMenuItem("Config");
//		mnConfig.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				ConfigEditGUI cui = new ConfigEditGUI();
//				cui.getMainFrame().setVisible(true);
//				cui.getMainFrame().setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
//			}
//		});
//		mnEdit.add(mnConfig);

		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFrame frmAbout = new JFrame("About HTTP Proxy");
				int X = proxyGUI.getWidth() / 2;
				int Y = proxyGUI.getHeight() / 2;
				frmAbout.setBounds(X, Y, 300, 50);
				frmAbout.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				frmAbout.setResizable(false);
				txtAbout = new JTextField();
				txtAbout.setText("Author - Tobias Hallen");
				frmAbout.getContentPane().add(txtAbout, BorderLayout.CENTER);
				txtAbout.setEditable(false);
				txtAbout.setColumns(10);
				frmAbout.setVisible(true);
			}
		});
		mnHelp.add(mntmAbout);
		proxyGUI.getContentPane().setLayout(null);

		JButton btnBlockHost = new JButton("Block URL");
		btnBlockHost.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String hostToBlock = infoField.getText();
				if (!hostToBlock.isEmpty()) {
					infoField.setText("");
					int choice = JOptionPane.showConfirmDialog(null,
							"Are you sure you want block: " + hostToBlock, "Block URL",
							JOptionPane.YES_NO_OPTION);
					if (choice == JOptionPane.YES_OPTION) {
						addBlocked(hostToBlock);
						txtInfoArea.append(hostToBlock + " has been blocked.\n");
					}
				} else {
					txtInfoArea.append("You need to enter a host.\n");
				}
			}
		});
		btnBlockHost.setBounds(12, 12, 155, 100);
		proxyGUI.getContentPane().add(btnBlockHost);

		JButton btnUnblockHost = new JButton("Unblock URL");
		btnUnblockHost.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String hostToUnblock = infoField.getText();
				infoField.setText("");
				int choice = JOptionPane.showConfirmDialog(null, "Are you sure you want unblock: "
						+ hostToUnblock, "Unlock URL", JOptionPane.YES_NO_OPTION);
				if (choice == JOptionPane.YES_OPTION) {
					if (blockList.remove(hostToUnblock, hostToUnblock)) {
						txtInfoArea.append("Unblocked: " + hostToUnblock + "\n");
					} else {
						txtInfoArea.append("Unable to unblock: " + hostToUnblock + "\n");
					}
				}
			}
		});
		btnUnblockHost.setBounds(179, 12, 155, 100);
		proxyGUI.getContentPane().add(btnUnblockHost);

		JButton btnListHost = new JButton("List Blocked URL's");
		btnListHost.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String s = "";
				List<String> list = new ArrayList<String>(blockList.values());
				for (int i = 0; i < list.size(); i++) {
					if (!list.get(i).isEmpty()) {
						s += "[" + list.get(i) + "]";
					}
				}
				if (!s.isEmpty()) {
					txtInfoArea.append("Blocked host: " + s + "\n");
				} else {
					txtInfoArea.append("Blocked host: " + "[]\n");
				}
			}
		});
		btnListHost.setBounds(346, 12, 155, 100);
		proxyGUI.getContentPane().add(btnListHost);

		txtInfoArea = new JTextArea(10, 40);
		txtInfoArea.setLineWrap(true);
		txtInfoScrollPane = new JScrollPane(txtInfoArea);
		txtInfoScrollPane.setBounds(12, 162, 489, 150);
		txtInfoScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		proxyGUI.getContentPane().add(txtInfoScrollPane);
		DefaultCaret caret = (DefaultCaret) txtInfoArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		infoField = new JTextField();
		infoField.setBounds(12, 123, 489, 32);
		proxyGUI.getContentPane().add(infoField);
		infoField.setColumns(10);

	}
	
    public static void addToInfoAread(String s, boolean addNewLine) {
	
	    if (addNewLine) {
		s += "\n";
	    }
	    txtInfoArea.append(s);
	
    }


}