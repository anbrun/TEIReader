package de.uniwue.mk.kall.formatconversion.teireader.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

public class Main {
	
	
	public void teiReadingBatch(String inFolder, String outFolder) throws ResourceInitializationException, FileNotFoundException, SAXException {

		new TEIReader().batchConvertDocuments(new File(inFolder), new File(outFolder), false);
	}

    public void corrMetadataBatch(String inFolder, String outFolder, boolean withDefaults) throws ResourceInitializationException, IOException, SAXException {
        new TEIReader().batchCorrectMetadata(new File(inFolder), new File(outFolder), withDefaults);
    }

	public static void main(String[] args) {
			Main mymain = new Main();
			try {
			    if (args.length == 3) {
			        if (args[2].equals("-metadata")) {
                        System.out.println("Mode: Correct Metadata");
                        mymain.corrMetadataBatch(args[0], args[1], false);
                    }
                    else if (args[2].equals("-metadataWithDefaults")) {
                        System.out.println("Mode: Correct Metadata (add Defaults for empty 'fictional' and 'narrative' values)");
                        mymain.corrMetadataBatch(args[0], args[1], true);
                    }
                    else {
                        System.out.println("Unknown option '" + args[2] + "'. Must be '-metadata' oder '-metadataWithDefaults' or be omitted.");
                    }
                }
                else if (args.length == 2) {
                    System.out.println("Mode: Convert TEI to XMI and then correct Metadata (with Defaults)");
                    mymain.teiReadingBatch(args[0], args[1]);
                }
                else {
                    System.out.println("Wrong number of parameters. Syntax:\njava -jar TEIReader.jar <inputDir> <outputDir> [-metadata or -metadataWithDefaults]");
                }
				
			} catch (Exception e) {
                System.out.println("ERROR!");
                e.printStackTrace();
            }
		
	}

}
