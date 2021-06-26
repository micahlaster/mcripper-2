package jredfox.mcripper;

import java.io.File;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.Commands;
import jredfox.filededuper.config.simple.MapConfig;
import jredfox.mcripper.command.McRipperCommands;
import jredfox.mcripper.command.RipperCommand;
import jredfox.mcripper.utils.McChecker;
import jredfox.selfcmd.SelfCommandPrompt;
import jredfox.selfcmd.util.OSUtil;

public class McRipper {
	
	static
	{
		Command.get("");
		Command.cmds.clear();
		Command.cmds.put("help", Commands.help);
		McRipperCommands.load();
	}
	
	public static final String appId = "Mcripper";
	public static final String version = "rc.2.2.0";
	public static final String appName = "MC Ripper 2 Build: " + version;
	
	public static void main(String[] args) throws Exception
	{
		args = SelfCommandPrompt.wrapWithCMD("input a command: ", appId, appName, args, true, true);
		loadCfg();
		args = args.length == 0 ? new String[]{"rip"} : args;
		Command<?> cmd = Command.fromArgs(args);
		if(shouldParseHashes(cmd))
			McChecker.parseHashes();
		cmd.run();
		McChecker.closePrinters();
	}
	
	public static boolean shouldParseHashes(Command<?> cmd)
	{
		return cmd instanceof RipperCommand && cmd != McRipperCommands.recomputeHashes && cmd != McRipperCommands.rip && !McChecker.loaded;
	}
	
	public static void loadCfg() throws Exception
	{
		File appdir = new File(OSUtil.getAppData(), McRipper.appId);
		MapConfig cfg = new MapConfig(new File(System.getProperty("user.dir"), McRipper.appId + ".cfg"));
		cfg.load();
		appdir = new File(cfg.get(McRipper.appId + "Dir", appdir.getPath())).getAbsoluteFile();
		cfg.save();
		if(!appdir.exists() && !appdir.mkdirs())
		{
			throw new RuntimeException("appdata \"" + appdir + "\" doesn't exist and cannot be created. Please reconfigure Mc Ripper 2 to a valid path!");
		}
		McChecker.setRoot(appdir);
		System.out.println("starting:" + appName);
	}

}
