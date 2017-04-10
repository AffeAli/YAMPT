package affeali.curse;

import java.io.File;
import java.io.IOException;

import affeali.curse.DownloadHelper;
import net.lingala.zip4j.exception.ZipException;

public class Main {
	
	static boolean verbose;
	static boolean quiet;
	
	public static void main(String[] args) {
		if(args.length < 1) {
			wronguse();
		}
		else if(args[0].equals("download")) {
			if(args.length < 2) wronguse();
			parseCLIOptions(args, 2);
			downloadPack(new File(args[1]));
		}
		else if(args[0].equals("help") || args[0].equals("--help")) printHelp();
	}

	private static void parseCLIOptions(String[] args, int ignore) {
		for(int i = ignore; i < args.length; i++) {
			if(args[i].equals("-q")) quiet = true;
			else if(args[i].equals("-v")) verbose = true;
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
