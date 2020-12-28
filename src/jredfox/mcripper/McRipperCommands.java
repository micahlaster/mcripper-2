package jredfox.mcripper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.jml.evilnotch.lib.json.JSONObject;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.ParamList;
import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.filededuper.util.JarUtil;

public class McRipperCommands {
	
	public static RunableCommand checkDisk = new RunableCommand(new String[]{"--mcDir=value", "--skipSnaps", "--skipOldMajors", "--forceDlCheck"}, "checkDisk")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			try 
			{
				McRipper.mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McRipper.mcDir;
				McRipper.checkDisk(params.hasFlag("skipSnaps"), params.hasFlag("skipOldMajors"), params.hasFlag("forceDlCheck"));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			McRipper.checkJsons.clear();
			McRipper.mcDir = McRipper.mcDefaultDir;
		}
	};
	
	public static RunableCommand checkMojang = new RunableCommand(new String[]{"--mcDir=value", "--skipSnaps"}, "checkMojang")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			try 
			{
				McRipper.mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McRipper.mcDir;
				McRipper.checkMojang(params.hasFlag("skipSnaps"));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			} 
			McRipper.checkJsons.clear();
			McRipper.mcDir = McRipper.mcDefaultDir;
		}
	};
	
	public static RunableCommand checkOmni = new RunableCommand("checkOmni")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			McRipper.checkOmni();
		}
	};
	
	public static RunableCommand checkOld = new RunableCommand(new String[]{"--forceDlCheck","--mcDir=value"}, "checkOld")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			McRipper.mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McRipper.mcDir;
			McRipper.checkOldMc(params.hasFlag("forceDlCheck"));
			McRipper.mcDir = McRipper.mcDefaultDir;
		}
	};
	
	public static Command<File> rip = new Command<File>(new String[]{"--mcDir=value"}, "rip")
	{
		@Override
		public String[] displayArgs() 
		{
			return new String[]{"version.json/assetsIndex.json", "version.json/assetsIndex.json & outputDir", "assetsIndex.json & minecraft.jar & outputDir"};
		}

		@Override
		public File[] parse(String... inputs)
		{
			if(this.hasScanner(inputs))
			{
				File jsonFile = this.nextFile("input the version.json/assetsIndex.json:");
				File jarFile = !RippedUtils.getJSON(jsonFile).containsKey("assetIndex") ? this.nextFile("input the client jar:") : null;
				File outDir = this.nextFile("input the directory of the output:");
				return new File[]{jsonFile, jarFile, outDir};
			}
			boolean hasJar = inputs.length == 3;
			return new File[]{new File(inputs[0]), hasJar ? new File(inputs[1]) : null, new File(inputs[hasJar ? 2 : 1])};
		}

		@Override
		public void run(ParamList<File> params) 
		{
			long ms = System.currentTimeMillis();
			File jsonFile = params.get(0);
			File jarFile = params.get(1);
			File out = params.get(2);
			boolean isAssets = jarFile != null;
			File mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McRipper.mcDir;
			
			JSONObject json = RippedUtils.getJSON(jsonFile);
			File outDir = isAssets ? new File(out, DeDuperUtil.getTrueName(jsonFile)) : new File(out, json.getString("assets"));
			try
			{
				if(isAssets)
					this.ripAssetsIndex(jarFile, json, mcDir, outDir);
				else
					this.ripMinor(json, mcDir, outDir);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			System.out.println("completed ripping assets in:" + (System.currentTimeMillis() - ms) / 1000D + " seconds");
		}

		public void ripMinor(JSONObject json, File mcDir, File outDir) throws FileNotFoundException, IOException 
		{
			//fetch the assetsIndex json file
			JSONObject assetsLoc = json.getJSONObject("assetIndex");
			String idAssets = assetsLoc.getString("id");
			String sha1Assets = assetsLoc.getString("8b054e43cf4edb69f78b1c96472a37c0b513d4d3");
			String urlAssets = assetsLoc.getString("url");
			File assetsIndexFile = McRipper.dlFromMc(McRipper.mojang, mcDir, urlAssets, "assets/indexes/" + idAssets + ".json", new File(McRipper.tmp, "jsons/assets/" + idAssets + ".json"), sha1Assets);
			JSONObject assetsIndex = RippedUtils.getJSON(assetsIndexFile);
			
			//fetch the jar
			JSONObject downloads = json.getJSONObject("downloads");
			JSONObject client = downloads.getJSONObject("client");
			String idClient = json.getString("id");
			String sha1Client = client.getString("sha1");
			String urlClient = client.getString("url");
			File jar = McRipper.dlFromMc(McRipper.mojang, mcDir, urlClient, "versions/" + idClient + ".jar", new File(McRipper.tmp, "versions/" + idClient + ".jar"), sha1Client);
			this.ripAssetsIndex(jar, assetsIndex, mcDir, outDir);
		}

		public void ripAssetsIndex(File jar, JSONObject json, File mcDir, File outDir) throws ZipException, IOException 
		{
			System.out.println("ripping assetsIndex");
			JSONObject objects = json.getJSONObject("objects");
			String pathBase = "assets/objects/";
			for(String key : objects.keySet())
			{
				JSONObject assetJson = objects.getJSONObject(key);
				String assetSha1 = assetJson.getString("hash");
				String assetSha1Char = assetSha1.substring(0, 2);
				String hpath = assetSha1Char + "/" + assetSha1;
				String assetUrl = "https://resources.download.minecraft.net/" + hpath;
				try
				{
					McRipper.dlFromMc(McRipper.mojang, mcDir, assetUrl, pathBase + hpath, new File(outDir, (isAssetRoot(key) ? "" : "assets/") + key).getAbsoluteFile(), assetSha1);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			
			System.out.println("extracting missing files");
			ZipFile zip = new ZipFile(jar);
			List<ZipEntry> mcmetas = JarUtil.getEntriesFromDir(zip, "assets/", "mcmeta");
			for(ZipEntry mcmeta : mcmetas)
			{
				String pathMeta = mcmeta.getName();
				File file = new File(outDir, pathMeta);
				if(!file.exists())
				{
					JarUtil.unzip(zip, mcmeta, file);
					System.out.println("extracted:" + pathMeta + " to:" + file);
				}
			}
		}
	};
	
	public static boolean isAssetRoot(String key) 
	{
		return key.equals("pack.mcmeta") || key.equals("pack.png");
	}
	
	public static RunableCommand recomputeHashes = new RunableCommand("recomputeHashes")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			try 
			{
				if(McRipper.hashWriter != null)
					McRipper.hashWriter.close();
				if(McRipper.hashFile.exists())
					McRipper.hashFile.delete();
				McRipper.parseHashes();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	};
	
	public static RunableCommand verify = new RunableCommand(new String[]{"--info"}, "verify")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			try 
			{
				boolean delete = !params.hasFlag("info");
				boolean shouldSave = false;
				Iterator<Map.Entry<String, String>> it = McRipper.hashes.entrySet().iterator();
				while(it.hasNext())
				{
					Map.Entry<String, String> p = it.next();
					String h = p.getKey();
					String path = p.getValue();
					File f = new File(McRipper.root, path);
					if(!h.equals(RippedUtils.getSHA1(f)))
					{
						System.err.println("file has been modified removing:" + path);
						if(delete)
						{
							it.remove();
							f.delete();
							shouldSave = true;
						}
					}
				}
				if(shouldSave)
					RippedUtils.saveFileLines(McRipper.hashes, McRipper.hashFile, true);
				else
					System.out.println("All files have been verified with no errors");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	};

	public static void load() {}

}
