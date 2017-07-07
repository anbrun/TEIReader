package de.uniwue.mk.kall.formatconversion.teireader.struct;

public class XMLAttribute {

	private UIMAAttributeType type;

	private String name;
	private String value;

	public XMLAttribute(String content) {

		if (content.contains("=")) {
			name = content.split("=")[0];
			value = content.split("=")[1].replaceAll("\"", "");
			
			
		}
		inferType();

	}



	//uses the value and tries to infer the type
	private void inferType() {
		// TODO this is clearly not the best that is possible
		this.type = UIMAAttributeType.STRING;
		
	}


	public UIMAAttributeType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		XMLAttribute other = (XMLAttribute) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	
	
	

}
