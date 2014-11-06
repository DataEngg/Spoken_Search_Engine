package org.galagosearch.core.tools;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class server {
	public static String speech;
	public static int i=10;
	public static void main(String args[]) throws IOException
	{
		ServerSocket s=null;
		Socket service=null;
		DataInputStream is=null;
		DataOutputStream os=null,os1=null;
		FileOutputStream fos=null;
		
		s=new ServerSocket(8888);
		//while(true){System.out.println(speech);
		
		try
		{  File file=new File("/home/kunal/speech.txt");
			if(!file.exists())
		      {
			   file.createNewFile();  
		      }
			
			
			System.out.println("listening");
			fos=new FileOutputStream(file);
			for(int j=0;j<20;j++)
			{
				
			System.out.println("connected");
			service=s.accept();
			is=new DataInputStream(service.getInputStream());
			os=new DataOutputStream(service.getOutputStream());
			os1=new DataOutputStream(fos);
			System.out.println("Reading the data");
			String ok;
			while(true){
			    ok=is.readLine();
			    if(ok!=null)
			    {
	             i++;
	             fos.write((new String()).getBytes());
	             fos.flush();
	             fos.close();
	             fos=new FileOutputStream(file);
				 fos.write(ok.getBytes());
				 fos.flush();
				 fos.close();
				 speech=ok;
				 //System.out.println(speech);
			    }
			    else
			    {
			    	break;
			    }
	            
	              
	           }
			

			  
			//service.close();	
			  
			}
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
		finally
		{
			 
			fos.close();
			  os1.close();
		os.close();is.close();
		}
		
		
		
	}

}