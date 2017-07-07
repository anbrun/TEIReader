package de.uniwue.mk.kall.formatconversion.teireader.struct;

public enum UIMAAttributeType {STRING("uima.cas.String"),STRING_ARRAY("");
	
	
	private String uimaCorrespondingType;
	
	private UIMAAttributeType(String uimaCorrespondingType) {
		this.uimaCorrespondingType = uimaCorrespondingType;
	}

	public String getUimaCorrespondingType() {
		return uimaCorrespondingType;
	}

	
	

}
