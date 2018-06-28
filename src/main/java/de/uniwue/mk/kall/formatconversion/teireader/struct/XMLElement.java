package de.uniwue.mk.kall.formatconversion.teireader.struct;

import java.util.ArrayList;
import java.util.List;

public class XMLElement {

	private String name;

	private List<XMLAttribute> attributes;

	// begin charoffset
	private int begin;

	// end charoffst exclusive
	private int end;

	public XMLElement(String content, int beg, int end) {

		this.begin = beg;
		this.end = end;
		attributes = new ArrayList<>();

		// parse the element

		int nrElements = 0;
		StringBuilder currentAttSb = new StringBuilder();
			for (char c : content.toCharArray()) {
				if (Character.isWhitespace(c)) {
				//if (c == ' ') {
					if (nrElements == 0) {
						// reste again
						this.name = currentAttSb.toString();
						currentAttSb = new StringBuilder();
						nrElements++;
					}
					// now its either an attribute or part of an attribute
					else {

						// this means it is a full attribute
						if (currentAttSb.toString().endsWith("\"")) {
							attributes.add(new XMLAttribute(currentAttSb.toString()));
							currentAttSb = new StringBuilder();
						}
					}
				}

				else {
					currentAttSb.append(c);
				}
			}
			
			//last one
			if(!currentAttSb.toString().isEmpty()){
				if(nrElements==0){
					this.name = currentAttSb.toString();
				}
				else{
					attributes.add(new XMLAttribute(currentAttSb.toString()));
				}
			}
	

	}

	public XMLElement(String name, List<XMLAttribute> attributes) {
		super();
		this.name = name;
		this.attributes = attributes;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<XMLAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<XMLAttribute> attributes) {
		this.attributes = attributes;
	}

	public int getBegin() {
		return begin;
	}

	public void setBegin(int begin) {
		this.begin = begin;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

}
