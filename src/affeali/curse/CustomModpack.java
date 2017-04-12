package affeali.curse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import affeali.curse.CurseProject.MinecraftVersions;
import affeali.curse.CustomModpack.ModpackMod;
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
		if(files.stream().filter(f -> f.project.id == mod.project.id).findFirst().orElse(null) != null) {
			Main.log("Mod already in use! Please use update to change its version");
			return false;
		}
		files.add(mod);
		if(Main.realtimeApply) mod.saveFile();
		updateJson();
		Main.log("Added mod " + mod.project.name);
		return true;
	}
	
	public boolean updateMod(ModpackMod mod) {
		mod.modpack = this;
		ModpackMod updateMod = files.stream().filter(f -> f.project.id == mod.project.id).findFirst().orElse(null);
		if(updateMod == null) {
			Main.log("Mod not in use! Please use add to add it to the pack");
			return false;
		}
		if(!updateMod.updateFileID(mod.fileID)) {
			Main.log("Could not update fileId");
			return false;
		}
		if(Main.realtimeApply) {
			new File(getFolder(), "/mods/" + updateMod.getFileName()).delete();
			mod.saveFile();
		}
		updateJson();
		Main.log("Updated mod " + updateMod.project.name + " to file " + updateMod.fileID);
		return true;
	}
	
	public boolean removeMod(ModpackMod mod) {
		mod.modpack = this;
		ModpackMod removeMod = files.stream().filter(f -> f.project.id == mod.project.id).findFirst().orElse(null);
		if(removeMod == null) {
			Main.log("Unable to remove mod! Mod is not in this pack");
			return false;
		}
		files.remove(removeMod);
		if(Main.realtimeApply) new File(getFolder(), "/mods/" + removeMod.getFileName()).delete();
		updateJson();
		Main.log("Removed mod " + removeMod.project.name);
		return true;
	}
	
	public void rebuild(boolean destructive) {
		if(destructive) {
			DownloadHelper.deleteRecursively(getFolder());
		}
		DownloadHelper.deleteRecursively(new File(getFolder(), "mods"));
		Main.log("Downloading all files...");
		files.forEach(a -> a.saveFile());
		updateJson();
		Main.log("Rebuild complete!");
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
			return null;
		}
	}
	
	public static class ModpackMod {
		public CurseProject project;
		public int fileID;
		public String fileName;
		private List<ModDependency> dependencies;
		public boolean isDependency;
		public transient CustomModpack modpack;
		
		public ModpackMod(int projId, int fileId) {
			project = new CurseProject(projId);
			this.fileID = fileId;
		}
		
		public ModpackMod(String name, int fileId, CustomModpack pack) {
			modpack = pack;
			try {
				project = new CurseProject(Integer.parseInt(name));
			}
			catch(NumberFormatException e) {
				project = new CurseProject(name);
			}
			if(fileId != -1)updateFileID(fileId);
			else updateFileID(project.getFilesForVersion(pack.mcVersion).get(0).fileID);
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
			}
			catch (FileNotFoundException e) {
				Main.logE("Invalid fileId");
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public String getFileName() {
			if(fileName == null)
			try {
				fileName = DownloadHelper.saveFile(project.getDownloadURL(fileID), modpack.getFolder() + "/mods/");
			}
			catch (FileNotFoundException e) {
				Main.logE("Invalid fileId");
			}
			catch (IOException e) {
					e.printStackTrace();
				}
			return fileName;
		}
		
		@Override
		public String toString() {
			return project.name + "  " + project.id + "/" + fileID + "(" + getFileName() + ")";
		}
		
		public List<ModDependency> getDependencies() {
			if(dependencies != null) return dependencies;
			else {
				dependencies = new ArrayList<>();
				try {
					generateDeps(project.getURL() + "/relations/dependencies?filter-related-dependencies=3", false);
					
					generateDeps(project.getURL() + "/relations/dependencies?filter-related-dependencies=2", true);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			return dependencies;
		}

		private void generateDeps(String url, boolean b) throws IOException, MalformedURLException {
			URLConnection conn = new URL(url).openConnection();
			Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
			String result = s.hasNext() ? s.next() : "";
			int searchIndex = 0;
			for( ; ; ) {
				int link = result.indexOf("<a href=\"https://minecraft.curseforge.com/projects/", searchIndex);
				if (link == -1) break;
				int linkEnd = result.indexOf("\">", link);
				link += 51;
				String test = result.substring(link, linkEnd);
				dependencies.add(new ModDependency(result.substring(link, linkEnd), b));
				searchIndex += linkEnd;
			}
		}

		public void clearDepCache() {
			dependencies = null;
		}
	}
	
	public static class ModDependency {
		
		public CurseProject project;
		public boolean optional;
		
		public ModDependency(String name, boolean optional) {
			this.optional = optional;
			project = new CurseProject(name);
		}
		
		public static List<ModpackMod> getDepsToInstall(ModpackMod mod, CustomModpack pack) {
			ArrayList<ModpackMod> list = new ArrayList<>();
			if(mod.getDependencies().size() == 0) return list;
			mod.getDependencies().forEach(d -> {
				if(!d.optional) list.add(d.toMod(pack, true));
			});
			return list;
		}
		
		public static List<ModpackMod> getOptionalDeps(ModpackMod mod, CustomModpack pack) {
			ArrayList<ModpackMod> list = new ArrayList<>();
			if(mod.getDependencies().size() == 0) return list;
			mod.getDependencies().forEach(d -> {
				if(d.optional) list.add(d.toMod(pack, true));
			});
			return list;
		}

		private ModpackMod toMod(CustomModpack pack, boolean isDep) {
			ModpackMod mod = new ModpackMod(project.name, -1, pack);
			mod.isDependency = isDep;
			return mod;
		}

		public static List<ModpackMod> getUnneededDeps(CustomModpack modpack) {
			ArrayList<ModpackMod> list = new ArrayList<>();
			ArrayList<ModpackMod> allModsInstalledAsDeps = new ArrayList<>();
			for(ModpackMod m : modpack.files) {
				if(m.isDependency) allModsInstalledAsDeps.add(m);
			}
			ArrayList<ModpackMod> allDeps = new ArrayList<>();
			for(ModpackMod mm : allModsInstalledAsDeps) {
				int found = 0;
				for(ModpackMod mm2 : allDeps) {
					if(mm2.project.id == mm.project.id) found = 1;
				}
				if(found == 0) list.add(mm);
			}
			return list;
		}

		public static boolean isRequired(ModpackMod mod, CustomModpack modpack) {
			if(!mod.isDependency) return false;
			for(ModpackMod m : modpack.files) {
				for(ModDependency d : m.getDependencies()) {
					if(d.project.id == mod.project.id) return true;
				}
			}
			return false;
		}
		
	}

}
