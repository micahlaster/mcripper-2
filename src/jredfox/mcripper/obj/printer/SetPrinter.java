package jredfox.mcripper.obj.printer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import jredfox.filededuper.util.IOUtils;

public class SetPrinter extends Printer{

	public Set<String> set;
	public SetPrinter(File root, File log, int capacity) throws IOException 
	{
		super(root, log);
		this.set = new LinkedHashSet<>(capacity);
	}

	@Override
	protected void parse(String line)
	{
		this.set.add(line.trim());
	}

	@Override
	public void save(BufferedWriter writer)
	{
		try
		{
			for(String s : this.set)
				writer.write(s + System.lineSeparator());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			IOUtils.close(writer);
		}
	}

}
