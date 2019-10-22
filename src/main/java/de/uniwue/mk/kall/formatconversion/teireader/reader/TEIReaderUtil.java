package de.uniwue.mk.kall.formatconversion.teireader.reader;

import java.net.URL;

import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;

public class TEIReaderUtil {
	
	
	public static TypeSystemDescription createStandardTypesystem(){
		
		//URL urlTS = ClassLoader.getSystemClassLoader().getResource("MiKalliTypesystem.xml");
		URL urlTS = ClassLoader.getSystemClassLoader().getResource("redeWiedergabeTypesystem_compare_tei_cab_fI.xml");
		TypeSystemDescription tsds = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(urlTS.toString());
		
		return tsds;
	}

}
