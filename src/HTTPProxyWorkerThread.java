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

public class HTTPProxyWorkerThread implements Runnable 
{

	FileWriter exceptionWriter;
	PrintWriter exceptionPW;
	private Thread httpsThread;
	Socket client;
	BufferedReader clientReader;
	BufferedWriter clientWriter;
	/**
	 * Constructor for client request handler
	 * @param socket socket connected to the client
	 * READ
	 */
	public HTTPProxyWorkerThread(Socket socket)
	{
		this.client = socket;
		try
		{
			this.client.setSoTimeout(3000);
			clientReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			clientWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			exceptionWriter=new FileWriter("exception.txt",true);
			exceptionPW=new PrintWriter(exceptionWriter,true);
		} 
		catch (IOException e) 
		{
			exceptionPW.write("Read/Write Error\n");
			exceptionPW.write("\n"+new Date().toString()+"\n"); // Adding the date

			e.printStackTrace(exceptionPW);
		}
	}



	/**
	 * Parses Request, initialises appropriate response based on request type
	 */
	@Override
	public void run() 
	{

		//attempt to receive client request
		String request;
		try
		{
			request = clientReader.readLine();
		} 
		catch (IOException e) 
		{
			exceptionPW.write("Read/Write Error\n");
			exceptionPW.write("\n"+new Date().toString()+"\n"); // Adding the date

			e.printStackTrace(exceptionPW);
			return;
		}
		//format request into two strings containing the request and the URL respectively
		String[] formattedArray = formatRequest(request);

		// check if requested site is in block list
		if(HTTPProxy.blockedPhrase(formattedArray[1]))
		{
			HTTPProxy.addToInfoArea(formattedArray[1]+" has been blocked on this proxy.");
			return;
		}

		// Check if received HTTPS request
		if(formattedArray[0].equals("CONNECT"))
		{
			HTTPProxy.addToConnArea("HTTPS Request Received: " + formattedArray[1] + "\n");
			httpsHandler(formattedArray[1]);
		} 

		// Check if request is stashed in cache
		else
		{
			File f;
			if((f = HTTPProxy.searchCache(formattedArray[1])) != null)
			{
				HTTPProxy.addToConnArea("Found cached copy of: " + formattedArray[1] + "\n");
				getFromCache(f);
			} 
			else 
			{
				HTTPProxy.addToConnArea("No cached copy found, GET-ting: " + formattedArray[1] + "\n");
				sendToClient(formattedArray[1]);
			}
		}
	} 

	/**
	 * Deals with HTTPS connections/requests
	 * @param url desired file to be transmitted over HTTPS
	 */
	private void httpsHandler(String url)
	{
		//cut out the "http://" from the url 
		String onlyURL = url.substring(7);

		//split the URL into actual URL and port
		String split[] = onlyURL.split(":");
		onlyURL = split[0];
		int port  = Integer.valueOf(split[1]);

		try
		{
			//use DNS to get IP from URL
			InetAddress ip = InetAddress.getByName(onlyURL);

			//read in the HTTPS request
			for(int i=0;i<5;i++)clientReader.readLine();

			//create socket for server
			Socket serverSocket = new Socket(ip, port);
			serverSocket.setSoTimeout(4000);

			//respond positively to client
			String s = "HTTP/1.0 200 Connection established\r\n\r\n";
			clientWriter.write(s);
			clientWriter.flush();


			//reader and writer to handle data forwarding between client and server
			BufferedWriter writeToServer = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
			BufferedReader readFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

			//open a new thread to handle data forwarding from client to server
			ClientServerThread clientServerThread = new ClientServerThread(client.getInputStream(), serverSocket.getOutputStream());

			httpsThread = new Thread(clientServerThread);
			httpsThread.start();

			try 
			{
				//read raw data from server to send it on to the client
				byte[] buffer = new byte[4096];
				int readTemp = serverSocket.getInputStream().read(buffer);
				while(readTemp>=0)
				{		
					if (readTemp > 0) 
					{
						client.getOutputStream().write(buffer, 0, readTemp);
						if (serverSocket.getInputStream().available() < 1) 
						{
							client.getOutputStream().flush();
						}
					}
					readTemp = serverSocket.getInputStream().read(buffer);
				}
			}
			catch (SocketTimeoutException e) 
			{
				exceptionPW.write("HTTPS Time Out Error\n");
				exceptionPW.write("\n"+new Date().toString()+"\n"); // Adding the date

				e.printStackTrace(exceptionPW);
			}
			catch (IOException e) 
			{
				exceptionPW.write("Read/Write Error\n");
				exceptionPW.write("\n"+new Date().toString()+"\n"); // Adding the date

				e.printStackTrace(exceptionPW);
			}

			try 
			{
				serverSocket.close();
				readFromServer.close();
				writeToServer.close();
				clientWriter.close();
			} catch (NullPointerException e) {}

		} 
		catch (SocketTimeoutException e) 
		{
			String line = "HTTP/1.0 504\n";
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
			exceptionPW.write("\n"+new Date().toString()+"\n"); // Adding the date

			e.printStackTrace(exceptionPW);
		}
	}



	/**
	 * Forwards file to client 
	 * @param url URL of requested file
	 */
	private void sendToClient(String url)
	{
		try
		{	
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
			if(extension.contains(".gif"))
			{
				extension = extension.replace("?", "questionmark");
				extension = extension.replace(".gif","");
				extension += ".gif";
			}
			if(extension.contains(".jpg"))
			{
				extension = extension.replace("?", "questionmark");
				extension = extension.replace(".jpg","");
				extension += ".jpg";
			}
			if(extension.contains(".jpeg"))
			{
				extension = extension.replace("?", "questionmark");
				extension = extension.replace(".jpeg","");
				extension += ".jpeg";
			}

			fileName = fileName + extension;
			if(extension.contains(".png"))extension=".png";
			if(extension.contains(".gif"))extension=".gif";
			if(extension.contains(".jpg"))extension=".jpg";
			if(extension.contains(".jpeg"))extension=".jpeg";

			//try to cache file
			boolean caching = true;
			File cacheFile = null;
			BufferedWriter writeToCache = null;

			try
			{
				cacheFile = new File("Cache/" + fileName);
				//create file if not already existing
				if(!cacheFile.exists())cacheFile.createNewFile();
				writeToCache = new BufferedWriter(new FileWriter(cacheFile));
			}
			catch (IOException e)
			{
				caching = false;
				exceptionPW.write("Read/Write Error\n");
				exceptionPW.write("\n"+new Date().toString()+"\n"); // Adding the date

				e.printStackTrace(exceptionPW);
			} 
			catch (NullPointerException e) 
			{
				exceptionPW.write("NullPointerException when trying to open File\n");
			}

			//check if file is in a conventional image format
			if((extension.contains(".gif")) || extension.contains(".jpeg") ||extension.contains(".jpg") || extension.contains(".png"))
			{
				//get new BufferedImage from URL
				URL remoteURL = new URL(url);
				BufferedImage imageBuffer = ImageIO.read(remoteURL);

				//check that an image was received
				if(imageBuffer != null) 
				{
					//if yes, write it to disk
					ImageIO.write(imageBuffer, extension.substring(1), cacheFile);

					//respond positively
					String temp = "HTTP/1.0 200 OK\n\r\n";
					clientWriter.write(temp);
					clientWriter.flush();

					//forward image file to client
					ImageIO.write(imageBuffer, extension.substring(1), client.getOutputStream());

				} 
				//otherwise nothing was received
				else 
				{
					HTTPProxy.addToConnArea("404, image not found." + fileName);
					String error = "HTTP/1.0 404 NOT FOUND\n"  + "\r\n";
					clientWriter.write(error);
					clientWriter.flush();
					return;
				}
			} 

			//should be text file otherwise
			else 
			{					
				URL serverURL = new URL(url);
				//connect to the server
				HttpURLConnection connectionToServer = (HttpURLConnection)serverURL.openConnection();
				connectionToServer.setDoOutput(true);
				connectionToServer.setUseCaches(false);
				connectionToServer.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				connectionToServer.setRequestProperty("charset", "utf-8");  


				BufferedReader serverReader = new BufferedReader(new InputStreamReader(connectionToServer.getInputStream()));


				//respond positively to client
				String temp = "HTTP/1.0 200 OK\n\r\n";
				clientWriter.write(temp);


				//keep reading lines until serverReader is empty
				while((temp = serverReader.readLine()) != null)
				{
					//write to file for cache, assuming file initialisation was successful
					if(caching)	writeToCache.write(temp);

					//forward lines to the client
					clientWriter.write(temp);
				}
				//close serverReader
				if(serverReader != null)serverReader.close();
				clientWriter.flush();
			}


			if(caching)
			{
				//add data to cache in main class
				writeToCache.flush();
				HTTPProxy.addToCache(url, cacheFile);
			}

			//close writeToCache
			if(writeToCache != null)writeToCache.close();

			//close clientWriter
			if(clientWriter != null)clientWriter.close();
		} 
		catch (Exception e)
		{
			exceptionPW.write("\n"+new Date().toString()+"\n"); // Adding the date

			e.printStackTrace(exceptionPW);
		}
	}



	/**
	 * Gets Specified File from Cache
	 * @param file The file to be retrieved from the cache
	 */
	private void getFromCache(File file){
		try
		{
			//gets the file extension to identify its type
			String fileExtension = file.getName().substring(file.getName().lastIndexOf('.'));

			String proxyResponse;
			//check if file is in a conventional image format
			if((fileExtension.contains(".gif")) || fileExtension.contains(".jpeg") || fileExtension.contains(".jpg") || fileExtension.contains(".png"))
			{
				BufferedImage imageBuffer = ImageIO.read(file);
				if(imageBuffer == null )
				{
					//check if image is null, if yes response negative
					HTTPProxy.addToConnArea("NullPointer getting: "+file.getName());
					proxyResponse = "HTTP/1.0 404 NOT FOUND \n"  + "\r\n";
					clientWriter.write(proxyResponse);
					clientWriter.flush();
				} 
				else 
				{
					//check if image is null, if not response positive
					proxyResponse = "HTTP/1.0 200 OK\n\r\n";
					clientWriter.write(proxyResponse);
					clientWriter.flush();
					ImageIO.write(imageBuffer, fileExtension.substring(1), client.getOutputStream());
				}
			} 

			//should be text file otherwise
			else 
			{
				BufferedReader textBuffer = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				proxyResponse = "HTTP/1.0 200 OK\n\r\n";
				clientWriter.write(proxyResponse);
				clientWriter.flush();

				String temp;
				//keep writing out lines until textBuffer is empty
				while((temp = textBuffer.readLine()) != null){clientWriter.write(temp);}
				clientWriter.flush();

				//close textBuffer
				if(textBuffer != null)textBuffer.close();
			}


			//close clientWriter
			if(clientWriter != null)clientWriter.close();

		} 
		catch (IOException e) 
		{
			exceptionPW.write("Read/Write Error\n");
			exceptionPW.write("\n"+new Date().toString()+"\n"); // Adding the date
			e.printStackTrace(exceptionPW);
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
			HTTPProxy.addToConnArea("Received Request: " + request);
			String actualRequest = request.substring(0,request.indexOf(' '));
			String url = request.substring(request.indexOf(' ')+1);
			url = url.substring(0, url.indexOf(' '));
			if(!url.substring(0,4).equals("http"))url = "http://" + url;	
			a[0]=actualRequest; a[1]= url;
		}
		return a; 
	}
}



