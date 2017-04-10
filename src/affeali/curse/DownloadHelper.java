package affeali.curse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;

import affeali.curse.JsonObjects.CurseFile;
import affeali.curse.JsonObjects.Manifest;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class DownloadHelper {
	
	public static final String curseforge = "https://minecraft.curseforge.com";
	public static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir") + "/curseDownloader/minecraft");
	public static final File DECOMP_DIR = new File(System.getProperty("java.io.tmpdir") + "/curseDecomp");
	public static Gson GSON = new Gson();
	
	static {
		if(!TMP_DIR.exists()) TMP_DIR.mkdirs();
		if(!new File(TMP_DIR, "mods").exists()) new File(TMP_DIR, "mods").mkdir();
		if(!DECOMP_DIR.exists()) DECOMP_DIR.mkdir();
	}
	
	public static void setupExportEnv(File zipFile) throws IOException, ZipException {
		ZipFile zip = new ZipFile(zipFile);
		zip.extractAll(DECOMP_DIR.getAbsolutePath());
		
		Manifest manifest = JsonObjects.parseManifest(new File(DECOMP_DIR, "manifest.json"));
		cloneManifestFiles(manifest.files);
		File overridesDir = new File(DECOMP_DIR, manifest.overrides);

		Main.log("Copying overrides...");
		Files.walk(overridesDir.toPath()).forEach(path -> {
			try {
				Files.copy(path, Paths.get(path.toString().replace(overridesDir.toString(), TMP_DIR.toString())));
			} catch(IOException e) { }
		});
	}
	
	public static void cloneManifestFiles(Collection<CurseFile> files) throws IOException {
		int i = 1;
		for(JsonObjects.CurseFile file : files) {
			Main.log("Downloading file " + i + "/" + files.size());
			new CurseProject(file.projectID).saveFile(file.fileID);
			i++;
		}
	}
	public static void toMultiMC() throws IOException {
		Manifest manifest = JsonObjects.parseManifest(new File(DECOMP_DIR, "manifest.json"));
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(TMP_DIR.getParentFile(), "instance.cfg")));
		writer.write("InstanceType=OneSix\n"
				+ "IntendedVersion=" + manifest.minecraft.version + "\n"
				+ "LogPrePostOutput=true\n"
				+ "OverrideCommands=false\n"
				+ "OverrideConsole=false\n"
				+ "OverrideJavaArgs=false\n"
				+ "OverrideJavaLocation=false\n"
				+ "OverrideMemory=false\n"
				+ "OverrideWindow=false\n"
				+ "iconKey=default\n"
				+ "lastLaunchTime=0\n"
				+ "name=" + manifest.name + " " + manifest.version + "\n"
				+ "notes=Modpack by " + manifest.author + ". Using Forge " + manifest.minecraft.modLoaders[0].id + ".\n"
				+ "totalTimePlayed=0\n");
		writer.close();
		toZip(TMP_DIR.getParent(), manifest.name + "(MultiMC).zip");
	}
	
	public static void saveFile(String url, String filePath) throws IOException {
		FileInput fi = downloadFileFollowRedirects(url);
		if(!new File(filePath + fi.name).exists()) Files.copy(fi.stream, new File(filePath + fi.name).toPath());
		else Main.log("Skipping download of " + fi.name);
	}
	
	public static FileInput downloadFileFollowRedirects(String url) throws MalformedURLException, IOException {
		URLConnection connection = new URL(url).openConnection();
		String loc = connection.getHeaderField("Location");
		if(loc != null) return downloadFileFollowRedirects(loc);
		else {
			String fileUrl = connection.getURL().toString();
			return new FileInput(connection.getInputStream(), fileUrl.substring(fileUrl.lastIndexOf("/") + 1));
		}
	}
	
	public static void toZip(String sourceDirPath, String zipFilePath) throws IOException {
	    Path p = Files.createFile(Paths.get(zipFilePath));
	    try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
	        Path pp = Paths.get(sourceDirPath);
	        Files.walk(pp)
	          .filter(path -> !Files.isDirectory(path))
	          .forEach(path -> {
	              ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
	              try {
	                  zs.putNextEntry(zipEntry);
	                  zs.write(Files.readAllBytes(path));
	                  zs.closeEntry();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	          });
	    }
	}
	
	public static class FileInput {
		public FileInput(InputStream inputStream, String headerField) {
			stream = inputStream;
			name = headerField;
		}
		public InputStream stream;
		public String name;
	}

	public static void cleanCache() {
		
	}

}
