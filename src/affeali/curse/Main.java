package affeali.curse;

import java.io.Console;
import java.io.File;
import java.io.IOException;

import affeali.curse.CurseProject.MinecraftVersions;
import affeali.curse.CustomModpack.ModpackMod;
import affeali.curse.DownloadHelper;
import net.lingala.zip4j.exception.ZipException;

public class Main {
	
	static boolean verbose;
	static boolean quiet;
	public static File multiMCInstances = new File(".");
	public static boolean realtimeApply = true;
	
	public static void main(String[] args) {
		if(args.length < 1) {
			wronguse();
		}
		else if(args[0].equals("download")) {
			if(args.length < 2) wronguse();
			parseCLIOptions(args, 2);
			downloadPack(new File(args[1]));
		}
		else if(args[0].equals("modpack")) {
			parseCLIOptions(args, 1);
			interactiveModpack();
		}
		else if(args[0].equals("help") || args[0].equals("--help")) printHelp();
	}

	private static void interactiveModpack() {
		Console console = System.console();
		if(multiMCInstances.toString().equals(".")) multiMCInstances = new File(console.readLine("MultiMC folder:") + "instances");
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
			String command = console.readLine("Avalible commands are : add, remove, update, rebuild, export, exit   :");
			if(command.equals("add")) {
				ModpackMod mod = getModConsole(modpack);
				log(modpack.addMod(mod));
			}
			else if(command.equals("remove")) {
				ModpackMod mod = getModConsole(modpack);
				modpack.removeMod(mod);
			}
			else if(command.equals("update")) {
				ModpackMod mod = getModConsole(modpack);
				modpack.updateMod(mod);
			}
			else if(command.equals("rebuild")) {
				modpack.rebuild(false);
			}
			else if(command.equals("exit")) return;
		}
	}

	private static ModpackMod getModConsole(CustomModpack pack) {
		String data = System.console().readLine("Enter Mod data:");
		if(!data.contains("/")) logE("Wrong formatted curse project data");
		return new ModpackMod(data.substring(0, data.indexOf("/")), Integer.parseInt(data.substring(data.indexOf("/") + 1)), pack);
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
