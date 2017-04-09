import java.io.File;
import java.io.IOException;

import affeali.curse.DownloadHelper;
import net.lingala.zip4j.exception.ZipException;

public class Main {
	
	public static void main(String[] args) {
		if(args.length < 1) {
			wronguse();
		}
		else if(args[0].equals("download")) {
			if(args.length != 2) wronguse();
			downloadPack(new File(args[1]));
		}
		else if(args[0].equals("help") || args[0].equals("--help")) printHelp();
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
			DownloadHelper.setupExportEnv(inputFile);
			DownloadHelper.toMultiMC();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ZipException e) {
			e.printStackTrace();
		}
	}

}
