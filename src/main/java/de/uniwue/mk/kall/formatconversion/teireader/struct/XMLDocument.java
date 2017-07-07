package de.uniwue.mk.kall.formatconversion.teireader.struct;

import java.util.List;

public class XMLDocument {
	
	private List<XMLElement> elements;
	
	private String docText;

	public XMLDocument(List<XMLElement> elements, String docText) {
		super();
		this.elements = elements;
		this.docText = docText;
	}

	public List<XMLElement> getElements() {
		return elements;
	}

	public void setElements(List<XMLElement> elements) {
		this.elements = elements;
	}

	public String getDocText() {
		return docText;
	}

	public void setDocText(String docText) {
		this.docText = docText;
	}
	
	

}
