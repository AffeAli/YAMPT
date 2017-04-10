package affeali.curse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import affeali.curse.CurseProject.MinecraftVersions;

public class CustomModpack {
	
	public String name;
	public List<ModpackMod> files = new ArrayList<>();
	public MinecraftVersions mcVersion;
	public transient File FOLDER;
	
	public CustomModpack(String name, MinecraftVersions version) {
		mcVersion = version;
		FOLDER = new File(Main.multiMCInstances, name + "/minecraft/");
		FOLDER.mkdirs();
		new File(FOLDER, "mods").mkdir();
		try {
			if(!new File(FOLDER.getParentFile(), "instance.cfg").exists()) {
				BufferedWriter writer = new BufferedWriter(new FileWriter(new File(FOLDER.getParentFile(), "instance.cfg")));
				writer.write("InstanceType=OneSix\n"
						+ "IntendedVersion=" + mcVersion.name + "\n"
						+ "LogPrePostOutput=true\n"
						+ "OverrideCommands=false\n"
						+ "OverrideConsole=false\n"
						+ "OverrideJavaArgs=false\n"
						+ "OverrideJavaLocation=false\n"
						+ "OverrideMemory=false\n"
						+ "OverrideWindow=false\n"
						+ "iconKey=default\n"
						+ "lastLaunchTime=0\n"
						+ "name=" + name + "\n"
						+ "notes=Modpack generated with https://github.com/AffeAli/JavaCurseClient\n"
						+ "totalTimePlayed=0\n");
				writer.close();
				
				updateJson();
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		files.forEach(m -> m.modpack = this);
	}

	private void updateJson() {
		try {
			BufferedWriter writer2 = new BufferedWriter(new FileWriter(new File(FOLDER.getParentFile(), "modpack.json")));
			writer2.write(DownloadHelper.GSON.toJson(this));
			writer2.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addMod(ModpackMod mod) {
		mod.modpack = this;
		files.add(mod);
		updateJson();
		if(Main.realtimeApply) mod.saveFile();
	}
	
	public void rebuild(boolean destructive) {
		if(destructive) {
			DownloadHelper.deleteRecursively(FOLDER);
		}
		DownloadHelper.deleteRecursively(new File(FOLDER, "mods"));
		files.forEach(a -> a.saveFile());
	}
	
	public static class ModpackMod {
		public CurseProject project;
		public int fileID;
		public transient CustomModpack modpack;
		
		public ModpackMod(int projId, int fileId) {
			project = new CurseProject(projId);
			this.fileID = fileId;
		}

		public void saveFile() {
			try {
				DownloadHelper.saveFile(project.getDownloadURL(fileID), modpack.FOLDER + "/mods/");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
