package affeali.curse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import affeali.curse.JsonObjects.CurseFile;

public class CurseProject {
	
	int id;
	String name;
	transient HashMap<MinecraftVersions, List<CurseFile>> fileCache = new HashMap<>();
	
	public CurseProject(int id) {
		this.id = id;
		if(name == null) name = resolveName();
	}
	
	private String resolveName() {
		String url;
		try {
			URLConnection conn = new URL(DownloadHelper.curseforge + "/mc-mods/" + id).openConnection();
			conn.setRequestProperty("User-Agent", "Linux Client?");
			InputStream is = conn.getInputStream();
			url = conn.getURL().toString();
			url = url.replace(DownloadHelper.curseforge + "/projects/", "");
			url = url.replace("?cookieTest=1", "");
			is.close();
			Main.logV("Found name for project " + id + ":" + url);
			return url;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public CurseProject(String name) {
		this.name = name;
		if(id == 0) resolveID();
	}
	
	private void resolveID() {
		try {
			URLConnection conn = new URL(getURL() + "/files").openConnection();
			Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
			String result = s.hasNext() ? s.next() : "";
			
			int pos = result.indexOf(".projectID = \"") + 14;
			this.id = Integer.parseInt(result.substring(pos, result.indexOf("\";", pos)));
			Main.logV("Found id for project " + name + ":" + id);
		}
		catch(FileNotFoundException e) {
			Main.logE("Unable to find project " + name);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	public String getDownloadURL(int fileID) {
		return DownloadHelper.curseforge + "/projects/" + name + "/files/" + fileID + "/download";
	}
	
	public String getURL() {
		return DownloadHelper.curseforge + "/projects/" + name;
	}
	
	public void saveFile(int fileID) throws IOException {
		DownloadHelper.saveFile(getDownloadURL(fileID), DownloadHelper.TMP_DIR.toString() + "/mods/");
	}
	
	public boolean updatesAvailable(CurseFile file) {
		for(CurseFile cf : getFilesForVersion(file.mcVersion)) {
			if(cf.fileID > file.fileID) return true;
		}
		return false;
	}
	
	public List<CurseFile> getFilesForVersion(MinecraftVersions version) {
		if(fileCache == null) fileCache = new HashMap<>();
		if(fileCache.containsKey(version)) return fileCache.get(version);
		ArrayList<CurseFile> list = new ArrayList<>();
		try {
			URLConnection conn = new URL(getURL() + "/files?filter-game-version=" + version.filterId).openConnection();
			Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
			String result = s.hasNext() ? s.next() : "";
			
			int searchIndex = 0;
			ArrayList<String> strings = new ArrayList<>();
			for( ; ; ) {
				int link = result.indexOf("<a class=\"overflow-tip\"", searchIndex);
				if(link == -1) break;
				int linkEnd = result.indexOf("</a>", link);
				strings.add(result.substring(link, linkEnd));
				searchIndex = linkEnd;
			}
			
			for(String str : strings) {
				str = str.replace("<a class=\"overflow-tip\" href=\"", "");
				CurseFile cf = new CurseFile();
				cf.projectID = this.id;
				cf.fileID = Integer.parseInt(str.substring(str.indexOf("/files/") + 7, str.indexOf("\">")));
				cf.fileName = str.substring(str.indexOf("\">") + 2);
				list.add(cf);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		fileCache.put(version, list);
		
		return list;
	}
	
	public enum MinecraftVersions {
		M1112("1.11.2", "2020709689%3A6452"),
		M1102("1.10.2", "2020709689%3A6170"),
		M1710("1.7.10", "2020709689%3A4449"),
		M164("1.6.4", "2020709689%3A326");
		
		String name;
		String filterId;
		
		MinecraftVersions(String name, String filterId) {
			this.name = name;
			this.filterId = filterId;
		}
		public static MinecraftVersions getByName(String n) {
			for(MinecraftVersions v : values()) {
				if(v.name.equals(n)) return v;
			}
			return null;
		}
	}

}
