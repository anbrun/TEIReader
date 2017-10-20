README

TEIReader converts TEI XML files to xmi files while preserving their tags and also automatically generates a typesystem for them.

Usage: 
java -jar TEIReader-0.0.1-SNAPSHOT-jar-with-dependencies.jar <input directory> <output directory>

Notes:
- Only files ending with .xml in input directory are converted (any other files or directories are ignored).
- The typesystem file is written to the output directory and is always named "generatedTypesystem.xml". This typesystem contains type declarations for all Tags that were present in the input files.
- XML annotations will appear twice in the output xmi files: 
	1) annotation type "TeiType" contains all XML tags; original tag name is stored in the attribute "TagName", original attributes are stored in attribute "Attributes" (separated by ##)
	2) for each XML annotation, an annotation type is generated that has the same structure a the original XML annotation: Name = Tag name; names of each attribute = names of the XML attributes 
