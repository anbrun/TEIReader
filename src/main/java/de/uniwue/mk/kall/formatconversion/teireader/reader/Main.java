package de.uniwue.mk.kall.formatconversion.teireader.reader;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

public class Main {
	
	
	public void teiReadingBatch(String inFolder, String outFolder) throws ResourceInitializationException, FileNotFoundException, SAXException {

		new TEIReader().batchConvertDocuments(new File(inFolder), new File(outFolder), false);
	}

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Falsche Anzahl von Parameter. Aufrufsyntax:\njava -jar TEIReader.jar <inputDir> <outputDir>");
		}
		else {
			Main mymain = new Main();
			try {
				System.out.println(args[0] + " --- " + args[1]);
				mymain.teiReadingBatch(args[0], args[1]);
				
			}catch (Exception e) {
				System.out.println("Programmfehler!");
				e.printStackTrace();
			}
		}
		
	}

}
