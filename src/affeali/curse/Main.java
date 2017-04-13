package affeali.curse;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import affeali.curse.CurseProject.MinecraftVersions;
import affeali.curse.CustomModpack.ModDependency;
import affeali.curse.CustomModpack.ModpackMod;
import affeali.curse.DownloadHelper;
import affeali.curse.JsonObjects.CurseFile;
import net.lingala.zip4j.exception.ZipException;

public class Main {
	
	static boolean verbose;
	static boolean quiet;
	public static File multiMCInstances = new File("instances/");
	public static boolean realtimeApply = true;
	
	public static void main(String[] args) {
		if(args.length < 1) {
			wronguse();
		}
		else if(args[0].equals("download")) {
			if(args.length < 2) wronguse();
			parseCLIOptions(args, 2);
			if(args[1].endsWith(".zip")) downloadPack(new File(args[1]));
			else {
				String tmpfile = "";
				try {
					tmpfile = DownloadHelper.saveFile(DownloadHelper.curseforge + "/projects/" + args[1] + "/files/latest", tmpfile);
				}
				catch (FileNotFoundException e) {
					logE("invalid modpack name");
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				
				downloadPack(new File(tmpfile));
			}
		}
		else if(args[0].equals("modpack")) {
			parseCLIOptions(args, 1);
			if(!new File(multiMCInstances.getParentFile(), "accounts.json").exists()) logE("Please use -M to set the MultiMC directory.");
			interactiveModpack();
		}
		else if(args[0].equals("help") || args[0].equals("--help")) printHelp();
	}

	private static void interactiveModpack() {
		Console console = System.console();
		CustomModpack modpack;
		String modpackName = console.readLine("Choose modpack:");
		if((modpack = CustomModpack.parseJson(modpackName)) == null) {
			String ans = console.readLine("Modpack " + modpackName + " does not exist. Do you want to create it? (Y/n)");
			if(ans.equalsIgnoreCase("y") || ans.equals("")) {
				MinecraftVersions modpackVersion = MinecraftVersions.getByName(console.readLine("Choose Minecraft version:"));
				if(modpackVersion == null) {
					logE("Unable to use selected Minecraft version");
					return;
				}
				modpack = new CustomModpack(modpackName, modpackVersion);
			}
			else return;
		}
		for( ; ; ) {
			String command = console.readLine(":");
			if(command.startsWith("add")) {
				ModpackMod mod = getModConsole(modpack, command);
				for(ModpackMod m : ModDependency.getDepsToInstall(mod, modpack)) {
					log("Adding required dependency " + m.project.name);
					modpack.addMod(m);
				}
				ModDependency.getOptionalDeps(mod, modpack).forEach(d -> log(d.project.name + " is an optional dependency"));
				modpack.addMod(mod);
			}
			else if(command.startsWith("remove")) {
				ModpackMod mod = getModConsole(modpack, command);
				modpack.removeMod(mod);
			}
			else if(command.equals("updateall")) {
				modpack.files.forEach(f -> {
					CurseFile cf = new CurseFile();
					cf.fileID = f.fileID;
					cf.projectID = f.project.id;
					cf.mcVersion = f.modpack.mcVersion;
					if(f.project.updatesAvailable(cf)) log("Update avalible for " + f.project.name);
				});
			}
			else if(command.startsWith("update")) {
				ModpackMod mod = getModConsole(modpack, command);
				mod.clearDepCache();
				modpack.updateMod(mod);
			}
			else if(command.equals("rebuild")) {
				modpack.rebuild(false);
			}
			else if(command.equals("list")) {
				modpack.files.forEach(m -> log(m));
			}
			else if(command.equals("exit")) return;
			else log("Avalible commands are : add, remove, update, rebuild, exit, list, updateall.");
		}
	}

	private static ModpackMod getModConsole(CustomModpack pack, String mode) {
		String[] array = mode.split(" ");
		String data;
		if(array.length > 1) data = array[1];
		else data = System.console().readLine("Enter Mod data:");
		
		String project, file = "-1";
		if(data.contains("/")) {
			project = data.substring(0, data.indexOf("/"));
			file = data.substring(data.indexOf("/") + 1);
		}
		else {
			project = data;
			log("No version specified, choosing latest");
		}
		return new ModpackMod(project, Integer.parseInt(file), pack);
	}

	private static void parseCLIOptions(String[] args, int ignore) {
		for(int i = ignore; i < args.length; i++) {
			if(args[i].equals("-q")) quiet = true;
			else if(args[i].equals("-v")) verbose = true;
			else if(args[i].equals("-M")) {
				multiMCInstances = new File(args[i + 1] + "/instances");
				i += 1;
			}
		}
	}

	private static void printHelp() {
		System.out.println(
				"Use the following options:\n" +
				"help - print this help" +
				"download [zip file] - convert zip file to MultiMC Import" +
				"");
		System.exit(0);
	}

	private static void wronguse() {
		System.out.println(
				"Wrong usage!\n" + 
				"Use --help option to get help."
				);
		System.exit(-1);
	}

	private static void downloadPack(File inputFile) {
		try {
			if(!inputFile.exists()) logE("File " + inputFile.toString() + " does not exist");
			DownloadHelper.setupExportEnv(inputFile);
			log("Exporting to MultiMC...");
			DownloadHelper.toMultiMC();
			DownloadHelper.cleanCache();
			log("Download successful!");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ZipException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean logV(Object o) {
		if(verbose) System.out.println(o);
		return false;
	}
	
	public static boolean log(Object o) {
		if(!quiet) System.out.println(o);
		return false;
	}
	
	public static void logE(Object o) {
		System.err.println(o);
		System.exit(-1);
	}

}
