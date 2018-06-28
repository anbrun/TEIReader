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
			String help =
                    "TEIReader converts TEI XML files to XMI files while preserving their tags, creates XMI that matches the RW typesystem\n" +
                            "and generates and corrects Metadata\n" +
                            "\n" +
                            "Usage: \n" +
                            "java -jar TEIReader-0.0.1-SNAPSHOT-jar-with-dependencies.jar <input directory> <output directory> [--metadata or --metadataWithDefaults]\n" +
                            "\n" +
                            "There are two modes:\n\n" +
                            "XML mode (no option given):\n" +
                            "- Only files ending with .xml in input directory are converted (any other files or directories are ignored).\n" +
                            "- Resulting XMI documents match the RW typesystem, this means:\n" +
                            "   - XML tags are converted into the annotation TeiType\n" +
                            "   - CabToken and Sentence annotation is generated\n" +
                            "   - Metadata annotation is generated with default values for 'fictional' and 'narrative' (see metadata-mode)\n" +
                            "\n" +
                            "Metadata mode (option --metadata or --metadataWithDefaults):\n" +
                            "- Only files ending with .xmi in input directory are converted (any other files or directories are ignored).\n" +
                            "- Only the Metadata annotation in the xmi files is affected, no other changes (fvals in TeiType are NOT changed!)\n" +
                            "- Option --metadata: correct captitalization and typos, add 'Undefined' for empty values\n" +
                            "- Option --metadataWithDefault: also add default values for EMPTY fictional and narrative slots\n" +
                            "  fictional/narrative is 'yes' for corpuspart = erz, 'no' otherwise.";

			try {
			    if (args.length == 1) {
			        if (args[0].equals("--help")) {
                        System.out.println(help);
                    }
                    else {
                        System.out.println("Wrong number of parameters. Syntax:\njava -jar TEIReader.jar <inputDir> <outputDir> [--metadata or --metadataWithDefaults].\n" +
                                "Display help with parameter --help");
                    }
                }
			    else if (args.length == 3) {
			        if (args[2].equals("--metadata")) {
                        System.out.println("Mode: Correct Metadata");
                        mymain.corrMetadataBatch(args[0], args[1], false);
                    }
                    else if (args[2].equals("--metadataWithDefaults")) {
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
                    System.out.println("Wrong number of parameters. Syntax:\njava -jar TEIReader.jar <inputDir> <outputDir> [--metadata or --metadataWithDefaults].\n" +
                            "Display help with parameter --help");
                }
				
			} catch (Exception e) {
                System.out.println("ERROR!");
                e.printStackTrace();
            }
		
	}

}
