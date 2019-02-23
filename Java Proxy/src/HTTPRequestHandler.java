import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

public class HTTPRequestHandler implements Runnable {

	FileWriter exceptionWriter;
	PrintWriter exceptionPW;

	/**
	 * Socket connected to client passed by Proxy server
	 * READ
	 */
	Socket client;

	/**
	 * Read data client sends to proxy
	 * READ
	 */
	BufferedReader clientReader;

	/**
	 * Send data from proxy to client
	 * READ
	 */
	BufferedWriter clientWriter;


	/**
	 * Thread that is used to transmit data read from client to server when using HTTPS
	 * Reference to this is required so it can be closed once completed.
	 * READ
	 */
	private Thread httpsThread;


	/**
	 * Creates a ReuqestHandler object capable of servicing HTTP(S) GET requests
	 * @param clientSocket socket connected to the client
	 * READ
	 */
	public HTTPRequestHandler(Socket clientSocket){
		this.client = clientSocket;
		try{
			this.client.setSoTimeout(10000);
			clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			clientWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			exceptionWriter=new FileWriter("exception.txt",true);
			exceptionPW=new PrintWriter(exceptionWriter,true);
		} 
		catch (IOException e) {
			exceptionPW.write("Read/Write Error\n");
			exceptionPW.write(new Date().toString()); // Adding the date
			exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
			e.printStackTrace(exceptionPW);
		}
	}



	/**
	 * Reads and examines the requestString and calls the appropriate method based 
	 * on the request type. 
	 */
	@Override
	public void run() {

		//attempt to receive client request
		String request;
		try{
			request = clientReader.readLine();
		} catch (IOException e) {
			exceptionPW.write("Read/Write Error\n");
			exceptionPW.write(new Date().toString()); // Adding the date
			exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
			e.printStackTrace(exceptionPW);
			return;
		}
		//format request into two strings containing the request and the url respectively
		String[] formattedArray = formatRequest(request);

		// check if requested site is in block list
		if(HTTPProxy.blockedPhrase(formattedArray[1])){
			System.out.println(formattedArray[1]+" has been blocked on this proxy." );
			siteIsBlocked();
			return;
		}

		// Check if received request was for HTTPS
		if(formattedArray[0].equals("CONNECT")){
			System.out.println("HTTPS Request Received: " + formattedArray[1] + "\n");
			httpsHandler(formattedArray[1]);
		} 

		// Check if request is stashed in cache
		else{
			File f;
			if((f = HTTPProxy.searchCache(formattedArray[1])) != null){
				System.out.println("Found cached copy of: " + formattedArray[1] + "\n");
				getFromCache(f);
			} else {
				System.out.println("No cached copy found, GET-ting: " + formattedArray[1] + "\n");
				sendToClient(formattedArray[1]);
			}
		}
	} 

	/**
	 * Simple Class to format incoming requests and return an array containing the URL and the request type
	 * @param request The incoming request
	 */
	String[] formatRequest(String request)
	{
		String[]a = {"",""};
		if(request!=null)
		{
			System.out.println("Received Request: " + request);
			String actualRequest = request.substring(0,request.indexOf(' '));
			String url = request.substring(request.indexOf(' ')+1);
			url = url.substring(0, url.indexOf(' '));
			if(!url.substring(0,4).equals("http")){
				url = "http://" + url;
			}
			a[0]=actualRequest; a[1]= url;
		}
		return a; 
	}


	/**
	 * Gets Specified File from Cache
	 * @param file The file to be retrieved from the cache
	 */
	private void getFromCache(File file){
		try{
			//gets the file extension to identify its type
			String fileExtension = file.getName().substring(file.getName().lastIndexOf('.'));

			String response;
			//check if file is in a typical image format
			if((fileExtension.contains(".gif")) || fileExtension.contains(".jpeg") ||
					fileExtension.contains(".jpg") || fileExtension.contains(".png")){
				//if file is image use bufferedimage
				BufferedImage imageBuffer = ImageIO.read(file);

				if(imageBuffer == null ){
					//check if image is null, if yes response negative
					System.out.println("Requested image was null: "+file.getName());
					response = "HTTP/1.0 404 NOT FOUND \n" + "Proxy-agent: HTTPProxyServer/1.0\n" + "\r\n";
					clientWriter.write(response);
					clientWriter.flush();
				} else {
					//check if image is null, if not response positive
					response = "HTTP/1.0 200 OK\n" + "Proxy-agent: HTTPProxyServer/1.0\n" + "\r\n";
					clientWriter.write(response);
					clientWriter.flush();
					ImageIO.write(imageBuffer, fileExtension.substring(1), client.getOutputStream());
				}
			} 

			//should be text file otherwise
			else {
				BufferedReader textBuffer = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				response = "HTTP/1.0 200 OK\n" +
						"Proxy-agent: HTTPProxyServer/1.0\n" +
						"\r\n";
				clientWriter.write(response);
				clientWriter.flush();

				String temp;
				//keep writing out lines until textBuffer is empty
				while((temp = textBuffer.readLine()) != null){
					clientWriter.write(temp);
				}
				clientWriter.flush();

				//close textBuffer
				if(textBuffer != null){
					textBuffer.close();
				}	
			}


			//close clientWriter
			if(clientWriter != null){
				clientWriter.close();
			}

		} catch (IOException e) {
			exceptionPW.write("Read/Write Error\n");
			exceptionPW.write(new Date().toString()); // Adding the date
			exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
			e.printStackTrace(exceptionPW);
		}
	}


	/**
	 * Forwards file to client 
	 * @param url URL of requested file
	 */
	private void sendToClient(String url){

		try{	
			//transform URL into OS compatible filename while still being identifiable
			int fileExtensionIndex = url.lastIndexOf(".");
			String extension;

			//get the extension of the file
			extension = url.substring(fileExtensionIndex, url.length());

			//get everything but the extension of the file
			String fileName = url.substring(0,fileExtensionIndex);


			//get rid of sub domain
			fileName = fileName.substring(fileName.indexOf('.')+1);

			//replace any other illegal characters
			fileName = fileName.replace(".", "dot");
			fileName = fileName.replace("/","slash");

			//remove any illegal characters in extension and add ".html" to complete file name

			if(extension.contains("/"))
			{
				extension = extension.replace(".", "dot");
				extension = extension.replace("/","slash");
				extension += ".html";
			}
			if(extension.contains(".png"))
			{
				extension = extension.replace("?", "questionmark");
				extension = extension.replace(".png","");
				extension += ".png";
			}

			//			extension = extension.replace("\\","backslash");
			//			extension = extension.replace("%","percent");
			//			extension = extension.replace("*","asterisk");
			//			extension = extension.replace(":","colon");
			//			extension = extension.replace("|","pipe");
			//			extension = extension.replace("<","lessthan");
			//			extension = extension.replace(">","greaterthan");



			fileName = fileName + extension;
			if(extension.contains(".png"))extension=".png";
			//try to cache file
			boolean caching = true;
			File cacheFile = null;
			BufferedWriter writeToCache = null;

			try{
				cacheFile = new File("Cache/" + fileName);
				//create file if not already existing
				if(!cacheFile.exists())cacheFile.createNewFile();
				writeToCache = new BufferedWriter(new FileWriter(cacheFile));
			}
			catch (IOException e){
				caching = false;
				exceptionPW.write("Read/Write Error\n");
				exceptionPW.write(new Date().toString()); // Adding the date
				exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
				e.printStackTrace(exceptionPW);
			} catch (NullPointerException e) {
				exceptionPW.write("Null Pointer Exception when trying to open File\n");
			}





			//check if file is in a typical image format
			if((extension.contains(".png")) || extension.contains(".jpg") ||
					extension.contains(".jpeg") || extension.contains(".gif")){
				//get new BufferedImage from URL
				URL remoteURL = new URL(url);
				BufferedImage imageBuffer = ImageIO.read(remoteURL);

				//check that an image was received
				if(imageBuffer != null) {
					//if yes, write it to disk
					ImageIO.write(imageBuffer, extension.substring(1), cacheFile);

					//respond positively
					String temp = "HTTP/1.0 200 OK\n" + "Proxy-agent: HTTPProxyServer/1.0\n" + "\r\n";
					clientWriter.write(temp);
					clientWriter.flush();

					//forward image file to client
					ImageIO.write(imageBuffer, extension.substring(1), client.getOutputStream());

					//if nothing was received
				} else {
					System.out.println("404, image file not received."
							+ fileName);
					String error = "HTTP/1.0 404 NOT FOUND\n" + "Proxy-agent: HTTPProxyServer/1.0\n" + "\r\n";
					clientWriter.write(error);
					clientWriter.flush();
					return;
				}
			} 

			//should be text file otherwise
			else {					
				URL serverURL = new URL(url);
				//connect to the server
				HttpURLConnection connectionToServer = (HttpURLConnection)serverURL.openConnection();
				connectionToServer.setRequestProperty("Content-Type", 
						"application/x-www-form-urlencoded");
				connectionToServer.setRequestProperty("Content-Language", "en-US");  
				connectionToServer.setUseCaches(false);
				connectionToServer.setDoOutput(true);

				BufferedReader serverReader = new BufferedReader(new InputStreamReader(connectionToServer.getInputStream()));


				//respond positively to client
				String temp = "HTTP/1.0 200 OK\n" + "Proxy-agent: HTTPProxyServer/1.0\n" + "\r\n";
				clientWriter.write(temp);


				//keep reading lines until serverReader is empty
				while((temp = serverReader.readLine()) != null){
					//forward lines to the client
					clientWriter.write(temp);
					//also write to file for cache, assuming file initialisation was successful
					if(caching){
						writeToCache.write(temp);
					}
				}
				clientWriter.flush();

				//close serverReader
				if(serverReader != null){
					serverReader.close();
				}
			}


			if(caching){
				//write data to cache, add to cachemap
				writeToCache.flush();
				HTTPProxy.addToCache(url, cacheFile);
			}

			//close writeToCache
			if(writeToCache != null){
				writeToCache.close();
			}

			//close clientWriter
			if(clientWriter != null){
				clientWriter.close();
			}
		} 
		catch (Exception e){
			exceptionPW.write(new Date().toString()); // Adding the date
			exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
			e.printStackTrace(exceptionPW);
		}
	}


	/**
	 * Handles HTTPS requests between client and remote server
	 * @param url desired file to be transmitted over HTTPS
	 */
	private void httpsHandler(String url){
		//get server URL and port from request 
		String urlString = url.substring(7);
		String split[] = urlString.split(":");
		urlString = split[0];
		int port  = Integer.valueOf(split[1]);

		try
		{
			//read in the rest of the HTTPS request
			for(int i=0;i<5;i++){
				clientReader.readLine();
			}

			//use DNS to get IP name from URL
			InetAddress ip = InetAddress.getByName(urlString);

			//create socket for server
			Socket serverSocket = new Socket(ip, port);
			serverSocket.setSoTimeout(5000);

			//respond positively to client
			String line = "HTTP/1.0 200 Connection established\r\n" + "Proxy-Agent: HTTPProxyServer/1.0\r\n" + "\r\n";
			clientWriter.write(line);
			clientWriter.flush();

			//Connection now established, proxy receiving data from both parties

			BufferedWriter writeToServer = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
			BufferedReader readFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

			//listener thread for client
			ClientServerThread clientServerThread = new ClientServerThread(client.getInputStream(), serverSocket.getOutputStream());

			httpsThread = new Thread(clientServerThread);
			httpsThread.start();

			//Server Listener, forwards data to client
			try {
				byte[] buffer = new byte[4096];
				int temp;
				do
				{
					temp = serverSocket.getInputStream().read(buffer);
					if (temp > 0) 
					{
						client.getOutputStream().write(buffer, 0, temp);
						if (serverSocket.getInputStream().available() < 1) 
						{
							client.getOutputStream().flush();
						}
					}
				} while(temp>=0);
			}
			catch (SocketTimeoutException e) 
			{
				exceptionPW.write("HTTPS Time Out Error\n");
				exceptionPW.write(new Date().toString()); // Adding the date
				exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
				e.printStackTrace(exceptionPW);
			}
			catch (IOException e) {
				exceptionPW.write("Read/Write Error\n");
				exceptionPW.write(new Date().toString()); // Adding the date
				exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
				e.printStackTrace(exceptionPW);
			}


			//close serverSocket
			if(serverSocket != null){
				serverSocket.close();
			}

			//close readFromServer
			if(readFromServer != null){
				readFromServer.close();
			}

			//close writeToServer
			if(writeToServer != null){
				writeToServer.close();
			}

			//close clientWriter
			if(clientWriter != null){
				clientWriter.close();
			}


		} 
		catch (SocketTimeoutException e) 
		{
			String line = "HTTP/1.0 504 Timeout after 10s\n" + "User-Agent: HTTPProxyServer/1.0\n" + "\r\n";
			try
			{
				clientWriter.write(line);
				clientWriter.flush();
			} 
			catch (IOException e1) 
			{
				exceptionPW.write("Read/Write Error\n");
				e1.printStackTrace(exceptionPW);
			}
		} 
		catch (Exception e)
		{
			exceptionPW.write("Error processing HTTPS request: " + url +"\n");
			exceptionPW.write(new Date().toString()); // Adding the date
			exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
			e.printStackTrace(exceptionPW);
		}
	}


	/**
	 * Sends a block message for when a site is called which is blocked
	 */
	private void siteIsBlocked(){
		try {
			BufferedWriter temp = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
			String line = "HTTP/1.0 403 Access Forbidden \n" + "User-Agent: HTTPProxyServer/1.0\n" +"\r\n";
			temp.write(line);
			temp.flush();
		} catch (IOException e) {
			exceptionPW.write("Read/Write Error\n");
			exceptionPW.write(new Date().toString()); // Adding the date
			exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
			e.printStackTrace(exceptionPW);
		}
	}
}



