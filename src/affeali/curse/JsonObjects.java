package affeali.curse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import affeali.curse.CurseProject.MinecraftVersions;

public class JsonObjects {
	
	public static class Manifest {
		public Minecraft minecraft;
		public String manifestType;
		public int manifestVersion;
		public String name;
		public String version;
		public String author;
		public List<CurseFile> files;
		public String overrides;
	}
	
	public static class CurseFile {
		public int projectID;
		public int fileID;
		public boolean required;
		
		public String fileName; //Not in manifest.json!
		public MinecraftVersions mcVersion; //Not in manifest.json!
		
		public CurseFile() {
			
		}
	}
	
	public static class Minecraft {
		public String version;
		public ModLoader[] modLoaders;
	}
	
	public static class ModLoader {
		public String id;
		public boolean primary;
	}
	
	public static Manifest parseManifest(File manifestFile) {
		JsonObjects.Manifest man;
		try {//TODO Better file reading
			java.util.List<String> s = Files.readAllLines(manifestFile.toPath());
			String a = "";
			for (String s1 : s) {
				a = a + s1;
			}
			man = DownloadHelper.GSON.fromJson(a, JsonObjects.Manifest.class);
			man.files.forEach(element -> element.mcVersion = MinecraftVersions.getByName(man.minecraft.version));
			return man;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
