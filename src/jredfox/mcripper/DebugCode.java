package jredfox.mcripper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import jredfox.filededuper.util.IOUtils;

public class DebugCode {
	
	public static void main(String[] args)
	{
		System.out.println(IOUtils.getReader(new File("a")));
		
	}

}
