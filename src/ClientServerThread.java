import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;

class ClientServerThread implements Runnable
{
	InputStream clientInput;
	OutputStream serverOutput;
	FileWriter exceptionWriter;
	PrintWriter exceptionPW;


	public ClientServerThread(InputStream clientInput, OutputStream serverOutput) 
	{
		this.clientInput = clientInput;
		this.serverOutput = serverOutput;
		try 
		{
			exceptionWriter=new FileWriter("exception.txt",true);
		} 
		catch (IOException e) 
		{
			exceptionPW.write("\n"+new Date().toString()+"\n"); // Adding the date

			e.printStackTrace(exceptionPW);
		}
		exceptionPW=new PrintWriter(exceptionWriter,true);
	}

	@Override
	public void run()
	{
		try 
		{
			//read raw data from client to forward it to the server
			byte[] buffer = new byte[4096];
			int readTemp = clientInput.read(buffer);
			while (readTemp >= 0)
			{
				if (readTemp > 0) 
				{
					serverOutput.write(buffer, 0, readTemp);
					if (clientInput.available() < 1) 
					{
						serverOutput.flush();
					}
				}
				readTemp = clientInput.read(buffer);
			} 
		}
		catch (SocketTimeoutException e) 
		{
			exceptionPW.write("Client HTTPS Timeout\n");
			exceptionPW.write("\n"+new Date().toString()+"\n"); // Adding the date

			e.printStackTrace(exceptionPW);
		}
		catch (IOException e1) 
		{
			exceptionPW.write("Read/Write Error\n");
			exceptionPW.write("\n"+new Date().toString()+"\n"); // Adding the date

			e1.printStackTrace(exceptionPW);

		}
	}
}