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
		try {
			exceptionWriter=new FileWriter("exception.txt",true);
		} catch (IOException e) {
			exceptionPW.write(new Date().toString()); // Adding the date
			exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
			e.printStackTrace(exceptionPW);
		}
        exceptionPW=new PrintWriter(exceptionWriter,true);
	}

	@Override
	public void run(){
		try {
			//read raw data from client to forward it to the server
			byte[] buffer = new byte[4096];
			int temp;
			do
			{
				temp = clientInput.read(buffer);
				if (temp > 0) {
					serverOutput.write(buffer, 0, temp);
					if (clientInput.available() < 1) 
					{
						serverOutput.flush();
					}
				}
			} while (temp >= 0);
		}
		catch (SocketTimeoutException e) 
		{
			exceptionPW.write("Client Listener Time Out Error\n");
			exceptionPW.write(new Date().toString()); // Adding the date
			exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
			e.printStackTrace(exceptionPW);
		}
		catch (IOException e1) {
			exceptionPW.write("Read/Write Error\n");
			exceptionPW.write(new Date().toString()); // Adding the date
			exceptionPW.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n"); // Formatted date
			e1.printStackTrace(exceptionPW);

		}
	}
}