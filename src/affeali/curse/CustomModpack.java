package affeali.curse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import affeali.curse.CurseProject.MinecraftVersions;
import affeali.curse.JsonObjects.CurseFile;

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
			else Main.logE("Modpack already existing");
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		files.forEach(m -> m.modpack = this);
	}
	
	public File getFolder() {
		if(FOLDER != null) return FOLDER;
		else return FOLDER = new File(Main.multiMCInstances, name + "/minecraft/");
	}

	private void updateJson() {
		try {
			BufferedWriter writer2 = new BufferedWriter(new FileWriter(new File(getFolder().getParentFile(), "modpack.json")));
			writer2.write(DownloadHelper.GSON.toJson(this));
			writer2.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean addMod(ModpackMod mod) {
		mod.modpack = this;
		if(files.stream().filter(f -> f.project.id == mod.project.id).findFirst().orElse(null) != null) return false;
		files.add(mod);
		updateJson();
		if(Main.realtimeApply) mod.saveFile();
		Main.log("Added mod " + mod.project.name);
		return true;
	}
	
	public boolean updateMod(ModpackMod mod) {
		mod.modpack = this;
		ModpackMod updateMod = files.stream().filter(f -> f.project.id == mod.project.id).findFirst().orElse(null);
		if(updateMod == null) return false;
		if(!updateMod.updateFileID(mod.fileID)) return false;
		updateJson();
		if(Main.realtimeApply) mod.saveFile();
		Main.log("Updated mod " + updateMod.project.name + "to file " + updateMod.fileID);
		return true;
	}
	
	public boolean removeMod(ModpackMod mod) {
		mod.modpack = this;
		ModpackMod removeMod = files.stream().filter(f -> f.project.id == mod.project.id).findFirst().orElse(null);
		if(removeMod == null) return false;
		files.remove(removeMod);
		updateJson();
		if(Main.realtimeApply) new File(getFolder(), "/mods/" + removeMod.getFileName()).delete();
		Main.log("Removed mod " + removeMod.project.name);
		return true;
	}
	
	public void rebuild(boolean destructive) {
		if(destructive) {
			DownloadHelper.deleteRecursively(getFolder());
		}
		DownloadHelper.deleteRecursively(new File(getFolder(), "mods"));
		files.forEach(a -> a.saveFile());
	}
	
	public static CustomModpack parseJson(String name) {
		String str;
		try {
			Scanner s = new Scanner(new File(Main.multiMCInstances, name + "/modpack.json")).useDelimiter("\\A");
			str = s.next();
			CustomModpack modpack = DownloadHelper.GSON.fromJson(str, CustomModpack.class);
			if(modpack.name == null) modpack.name = name;
			modpack.files.forEach(f -> f.modpack = modpack);
			return modpack;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static class ModpackMod {
		public CurseProject project;
		public int fileID;
		public String fileName;
		public transient CustomModpack modpack;
		
		public ModpackMod(int projId, int fileId) {
			project = new CurseProject(projId);
			this.fileID = fileId;
		}
		
		public boolean updateFileID(int newID) {
			CurseFile newCF = project.getFilesForVersion(modpack.mcVersion).stream().filter(f -> f.fileID == newID).findFirst().orElse(null);
			if(newCF == null) return false;
			fileID = newID;
			return true;
		}

		public void saveFile() {
			try {
				if(new File(modpack.getFolder() + "/mods/" + getFileName()).exists()) {
					new File(modpack.getFolder() + "/mods/" + getFileName()).delete();
				}
				fileName = DownloadHelper.saveFile(project.getDownloadURL(fileID), modpack.getFolder() + "/mods/");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public String getFileName() {
			if(fileName == null)
				try {
					fileName = DownloadHelper.saveFile(project.getDownloadURL(fileID), modpack.getFolder() + "/mods/");
				} catch (IOException e) {
					e.printStackTrace();
				}
			return fileName;
		}
	}

}
