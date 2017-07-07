package TEIReaderTest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.uniwue.mk.kall.formatconversion.teireader.reader.TEIReader;

public class TestTEIReaderBatch {

	//String inFolder = "D:\\WORK\\RW-Projekt\\rohdaten\\grenzbotenTest";
	//String inFolder = "D:\\WORK\\RW-Projekt\\rohdaten\\corpus\\erz\\1840";
	String inFolder = "D:\\WORK\\RW-Projekt\\rohdaten\\teiReaderTest\\input";
	//String outFolder = "D:\\WORK\\RW-Projekt\\rohdaten\\grenzbotenTest_xmi";
	//String outFolder = "D:\\WORK\\RW-Projekt\\rohdaten\\corpus\\erz\\1840_xmi";
	String outFolder = "D:\\WORK\\RW-Projekt\\rohdaten\\teiReaderTest\\output";
		
	@Test
	public void testTEIReadingBatch() throws ResourceInitializationException, FileNotFoundException, SAXException {

		new TEIReader().batchConvertDocuments(new File(inFolder), new File(outFolder), true);
	}

}
