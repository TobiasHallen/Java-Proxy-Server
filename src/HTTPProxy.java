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
import java.util.NoSuchElementException;
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




public class HTTPProxy{

	private static JFrame proxyGUI;

	//creates instance of HTTP proxy
	public static void main(String[] args) throws UnsupportedLookAndFeelException 
	{
		UIManager.setLookAndFeel(UIManager.getLookAndFeel());
		HTTPProxy proxy = new HTTPProxy(8080);
		EventQueue.invokeLater(new Runnable() 
		{
			public void run() 
			{
				try 
				{
					proxy.initialize();
					proxyGUI.setVisible(true);
				} catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
		});
		proxy.listen();	
	}

	final JFileChooser fc = new JFileChooser();
	private JTextArea txtAbout;
	private JTextField iOField;

	private static JTextArea connectionInfoArea;
	private static JTextArea userIOArea;

	private static JScrollPane connInfoScrollPane;
	private static JScrollPane userIOScrollPane;


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
				Thread t = new Thread(new HTTPProxyWorkerThread(s));				
				threadList.add(t);			
				t.start();	
			} catch (SocketException e) {
				System.out.println("Server Shut Down...");
			} catch (IOException e) {
				exceptionPW.write(new Date().toString()); // Adding the date
				exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
				e.printStackTrace(exceptionPW);
			} catch (NullPointerException e) {
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
			} catch (NoSuchElementException e) {
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

	private void initialize() {
		proxyGUI = new JFrame();
		proxyGUI.addWindowListener(new WindowAdapter() 
		{
			public void windowClosing(WindowEvent e) 
			{
				proxyGUI.dispose();
				shutDownProxy();
			}
		});
		proxyGUI.setTitle("Proxy");
		proxyGUI.setBounds(100, 100, 913, 400);
		proxyGUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		proxyGUI.setResizable(false);

		JMenuBar menuBar = new JMenuBar();
		proxyGUI.setJMenuBar(menuBar);

		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		JMenuItem mntmAbout = new JMenuItem("Usage");
		mntmAbout.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				JFrame frmAbout = new JFrame("How to Use");
				int X = proxyGUI.getWidth() / 2;
				int Y = proxyGUI.getHeight() / 2;
				frmAbout.setBounds(X, Y, 300, 150);
				frmAbout.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				frmAbout.setResizable(false);
				txtAbout = new JTextArea();
				txtAbout.setText("First box is input, second box is output.\nEnter a phrase and press corresponding \nbutton to add/remove from block list.");
				frmAbout.getContentPane().add(txtAbout, BorderLayout.CENTER);
				txtAbout.setEditable(false);
				frmAbout.pack();
				frmAbout.setVisible(true);
			}
		});
		mnHelp.add(mntmAbout);
		proxyGUI.getContentPane().setLayout(null);

		JButton blockPhrase = new JButton("Block Phrase");
		blockPhrase.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				String hostToBlock = iOField.getText();
				if (!hostToBlock.isEmpty()) 
				{
					iOField.setText("");
					int choice = JOptionPane.showConfirmDialog(null,"Are you sure you want block: " + hostToBlock, "Block Phrase",JOptionPane.YES_NO_OPTION);
					if (choice == JOptionPane.YES_OPTION) 
					{
						addBlocked(hostToBlock);
						addToInfoArea(hostToBlock + " has been blocked.");
					}
				} else addToInfoArea("You must enter a phrase to block.");
			}
		});
		blockPhrase.setBounds(12, 12, 119, 100);
		proxyGUI.getContentPane().add(blockPhrase);

		JButton unblockPhrase = new JButton("Unblock Phrase");
		unblockPhrase.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				String phraseToUnblock = iOField.getText();
				iOField.setText("");
				int choice = JOptionPane.showConfirmDialog(null, "Are you sure you want unblock: " + phraseToUnblock, "Unblock Phrase", JOptionPane.YES_NO_OPTION);
				if (choice == JOptionPane.YES_OPTION) 
				{
					if (blockList.remove(phraseToUnblock, phraseToUnblock)) addToInfoArea("Unblocked: " + phraseToUnblock );
					else addToInfoArea("Cannot unblock: " + phraseToUnblock + ", may not be present in list.");	
				}
			}
		});
		unblockPhrase.setBounds(139, 12, 119, 100);
		proxyGUI.getContentPane().add(unblockPhrase);

		JButton listBlocked = new JButton("List Blocked");
		listBlocked.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String temp = "";
				List<String> list = new ArrayList<String>(blockList.values());
				for (int i = 0; i < list.size(); i++) {
					if (!list.get(i).isEmpty()) {
						temp += "[" + list.get(i) + "]";
					}
				}
				if (!temp.isEmpty()) {
					addToInfoArea("Blocked host: " + temp);
				} else {
					addToInfoArea("Block List is Empty.");
				}
			}
		});
		listBlocked.setBounds(266, 12, 119, 100);
		proxyGUI.getContentPane().add(listBlocked);

		JButton showCacheButton = new JButton("Show Cache");
		showCacheButton.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				addToInfoArea("Cache Dump:\n");
				for (String key : cache.keySet()) 
				{
					addToInfoArea(""+key);
				}
				addToInfoArea("\n");
			}
		});
		showCacheButton.setBounds(393, 12, 119, 100);
		proxyGUI.getContentPane().add(showCacheButton);


		connectionInfoArea = new JTextArea(10, 40);
		userIOArea = new JTextArea(40,10);

		connectionInfoArea.setLineWrap(true);
		userIOArea.setLineWrap(true);

		connInfoScrollPane = new JScrollPane(connectionInfoArea);
		userIOScrollPane = new JScrollPane(userIOArea);

		connInfoScrollPane.setBounds(12, 122, 500, 190);
		userIOScrollPane.setBounds(513, 12, 380, 260);

		connInfoScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		userIOScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		proxyGUI.getContentPane().add(connInfoScrollPane);
		proxyGUI.getContentPane().add(userIOScrollPane);

		DefaultCaret connCaret = (DefaultCaret) connectionInfoArea.getCaret();
		connCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		DefaultCaret iOCaret = (DefaultCaret) userIOArea.getCaret();
		iOCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		iOField = new JTextField();

		iOField.setBounds(513, 280, 380, 32);

		proxyGUI.getContentPane().add(iOField);

		iOField.setColumns(10);

	}

	public static void addToConnArea(String s) {
		s += "\n";
		try {
			connectionInfoArea.append(s);
		} catch (NullPointerException e) {}	
	}

	public static void addToInfoArea(String s) { 
		s += "\n";
		userIOArea.append(s);	
	}
}