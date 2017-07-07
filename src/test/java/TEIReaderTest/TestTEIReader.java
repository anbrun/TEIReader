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

public class TestTEIReader {

	String inTestFile = "Ahlefeld,-Charlotte-von_Erna.xml";

	@Test
	public void testTEIReading() throws ResourceInitializationException, FileNotFoundException, SAXException {

		InputStream is = getClass().getClassLoader().getResourceAsStream(inTestFile);
		CAS cas = new TEIReader().readDocument(is,false);

		try {
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		XmiCasSerializer.serialize(cas, new FileOutputStream(inTestFile + ".xmi"));
	}


}
