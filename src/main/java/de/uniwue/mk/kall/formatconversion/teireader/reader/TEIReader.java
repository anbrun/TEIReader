package de.uniwue.mk.kall.formatconversion.teireader.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.MalformedParametersException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.io.IOUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.util.CasCreationUtils;
import org.xml.sax.SAXException;

import de.uniwue.mk.kall.formatconversion.teireader.struct.XMLAttribute;
import de.uniwue.mk.kall.formatconversion.teireader.struct.XMLDocument;
import de.uniwue.mk.kall.formatconversion.teireader.struct.XMLElement;

public class TEIReader {

	// default encoding
	private String encoding = "UTF-8";

	private TypeSystemDescription currentTS = null;

	public TEIReader() {
		// use the own typesystem as start
		currentTS = TEIReaderUtil.createStandardTypesystem();
	}

	public void batchConvertDocuments(File inFolder, File outFolder, boolean inferTypesystem)
			throws ResourceInitializationException, FileNotFoundException, SAXException {

		if (!inFolder.isDirectory() || !outFolder.isDirectory()) {
			throw new IllegalArgumentException("Please provide 2 folder!!");
		}

		for (File f : inFolder.listFiles()) {
			if (!f.getName().endsWith(".xml"))
				continue;

			System.out.println(f);
			CAS cas = readDocument(f, inferTypesystem);
			XmiCasSerializer.serialize(cas, new FileOutputStream(new File(outFolder + "/" + f.getName() + ".xmi")));
		}

		// also write the typesystem
		try {
			currentTS.toXML(
					new FileOutputStream(new File(outFolder.getAbsolutePath() + "/" + "generatedTypesystem.xml")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public CAS readDocument(File inFile, boolean inferTypesystem) throws ResourceInitializationException {

		FileInputStream is = null;
		try {
			is = new FileInputStream(inFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			return readDocument(is, inferTypesystem);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public CAS readDocument(InputStream isXmlFile, boolean inferTypesystem) throws ResourceInitializationException {

		// step 1 is to read the text and the annotations on the text
		XMLDocument xmlDoc = null;
		try {
			xmlDoc = readTextAndAnnotations(isXmlFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (inferTypesystem) {
			TypeSystemDescription ts = inferTypesystem(xmlDoc);

			// merge into the current typesystem
			currentTS = CasCreationUtils.mergeTypeSystems(Arrays.asList(new TypeSystemDescription[] { ts, currentTS }));
		}
		// step 2 is to convert it to a UIMA Cas
		CAS cas = createCASFromXML(xmlDoc, currentTS);

		return cas;
	}

	// this reads the entire document, and infers all types
	private TypeSystemDescription inferTypesystem(XMLDocument xmlDoc) {

		// now create the typesystem

		// at first an empty ts
		TypeSystemDescription tsd = new TypeSystemDescription_impl();

		// now add the types
		Map<String, Set<XMLAttribute>> types = new HashMap<>();
		for (XMLElement el : xmlDoc.getElements()) {

			if (types.containsKey(el.getName())) {
				// update the features
				types.get(el.getName()).addAll(el.getAttributes());
			} else {
				types.put(el.getName(), new HashSet<>(el.getAttributes()));
			}
		}

		for (String type : types.keySet()) {
			Set<XMLAttribute> featureSet = types.get(type);
			String typeName = TEiReaderConstants.TEI_TYPES_PREFIX + type;
			// add the type to the typesystem
			TypeDescription addType = tsd.addType(typeName, "", "uima.tcas.Annotation");
			// add all features
			for (XMLAttribute feature : featureSet) {
				addType.addFeature(validateFeatureName(feature.getName()), "",
						feature.getType().getUimaCorrespondingType());
			}

		}

		return tsd;
	}

	private String validateFeatureName(String name) {

		if (name.matches("[0-9a-zA-Z_]*"))
			return name;
		else {

			// TODO this is probably not considering all cases
			name = name.replaceAll(":", "_");
		}
		return name;
	}

	private CAS createCASFromXML(XMLDocument xmlDoc, TypeSystemDescription ts) throws ResourceInitializationException {
		CAS cas = null;

		// create the CAS
		cas = CasCreationUtils.createCas(ts, null, null);

		// set the text
		cas.setDocumentText(xmlDoc.getDocText());

		// and add the annotations
		for (XMLElement el : xmlDoc.getElements()) {

			// create the defautl annotations
			Type annoType = cas.getTypeSystem().getType(TEiReaderConstants.DEFAULT_TYPESYSTEM_XML_TYPE);
			AnnotationFS xmlAnno = cas.createAnnotation(annoType, el.getBegin(), el.getEnd());
			setDefaultFeatures(xmlAnno, el);

			// add to cas
			cas.addFsToIndexes(xmlAnno);

			// also try to create specialized annos
			String specName = TEiReaderConstants.TEI_TYPES_PREFIX + el.getName();

			Type type = cas.getTypeSystem().getType(specName);
			if (type != null) {
				AnnotationFS specAnno = cas.createAnnotation(type, el.getBegin(), el.getEnd());
				//also add the features TODO
				for(XMLAttribute att : el.getAttributes()){
					
					Feature feat = type.getFeatureByBaseName(validateFeatureName(att.getName()));
					
					specAnno.setFeatureValueFromString(feat, att.getValue());
				}
				cas.addFsToIndexes(specAnno);

			}
		}
		return cas;
	}

	private void setDefaultFeatures(AnnotationFS xmlAnno, XMLElement el) {

		Feature elName = xmlAnno.getType()
				.getFeatureByBaseName(TEiReaderConstants.DEFAULT_TYPESYSTEM_XML_TAGNAME_FEATURE);
		Feature atts = xmlAnno.getType()
				.getFeatureByBaseName(TEiReaderConstants.DEFAULT_TYPESYSTEM_XML_ATTRIBUTES_FEATURE);

		xmlAnno.setFeatureValueFromString(elName, el.getName());

		StringBuilder attsSb = new StringBuilder();

		for (XMLAttribute att : el.getAttributes()) {
			attsSb.append(att.getName() + "=" + att.getValue() + "##");
		}
		xmlAnno.setFeatureValueFromString(atts, attsSb.toString());

	}

	// Note: this does not close the stream!!
	private XMLDocument readTextAndAnnotations(InputStream isXMlFile) throws IOException {

		// stores the actual text without any xml content
		StringBuilder sbText = new StringBuilder();

		// the char index of the contained text inside the document
		int currentIndex = 0;

		// read the file to a string - would fail if not enoug ram is available!
		String docText = IOUtils.toString(isXMlFile, encoding);

		// indicates if the currentIndex is inside a xml element, since this
		// does not count to the amount of chars in the text
		boolean inXMlRange = false;
		// whether the current xml tag is a beginning or and ending xml tag
		boolean isXmlBeg = false;

		// stores everything stored in the current element, e.g for <p n="2"> it
		// would store p n="2" same for end xml elements
		StringBuilder sbElement = new StringBuilder();

		// stores all xml elements (the strings) that are currently opened
		Stack<XMLElement> currentlyOpenedElements = new Stack<>();

		// stores the created elements
		List<XMLElement> xmlElements = new ArrayList<>();

		// iterate char for char through the xmlDoc
		for (int i = 0; i < docText.length(); i++) {

			// if we found an xml indicator
			if (docText.charAt(i) == '<') {
				inXMlRange = true;
				// check if it is a beginning or an ending xml tag
				isXmlBeg = false;
				if (i + 1 < docText.length()) {
					if (docText.charAt(i + 1) != '/') {
						isXmlBeg = true;
					}
				}
			}

			// end xml indicator
			if (docText.charAt(i) == '>') {
				inXMlRange = false;

				// if we have a new beginning element we push the stack
				if (isXmlBeg) {

					// this can now either be a regular opening element or an
					// element that has no content <element x="y"/>

					if (sbElement.toString().endsWith("/")) {
						// empty element!! => create an element
						// delete the slash first
						sbElement.deleteCharAt(sbElement.length() - 1);
						xmlElements.add(new XMLElement(sbElement.toString(), currentIndex, currentIndex));

					} else {
						currentlyOpenedElements.push(new XMLElement(sbElement.toString(), currentIndex, currentIndex));
					}
				}
				// if we have a closing element, we stript the beginnign /
				else {
					sbElement.deleteCharAt(0);

					// and compare it to the top element on the stack
					XMLElement pop = currentlyOpenedElements.pop();

					// if those are the same xmlElements we can create an
					// annotation

					if (pop.getName().equals(sbElement.toString())) {

						// set the correct end
						pop.setEnd(currentIndex);
						// and add to the list of finished xml elements
						xmlElements.add(pop);
					}
					// else this is either a bug in this code or a malformed
					// document!!
					else {
						throw new MalformedParametersException(
								"XML document probably malformed, can not parse annotations!");
					}

				}
				// reset it
				sbElement = new StringBuilder();
			}

			if (inXMlRange && docText.charAt(i) != '<') {
				sbElement.append(docText.charAt(i));
			} else if (!inXMlRange && docText.charAt(i) != '>') {
				// not in a xml range => we can read text
				sbText.append(docText.charAt(i));
				// and update our textpointer
				currentIndex++;
			}

		}

		return new XMLDocument(xmlElements, sbText.toString());
	}

}
