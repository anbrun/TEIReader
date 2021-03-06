package de.idsma.rw.correctmetadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.WordUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

public class CorrectMetadata {
    public static final String[] SET_VALUES = new String[]{"id", "filename", "year", "decade", "corpuspart", "title",
            "author", "fictional", "textlength", "sample_id", "narrative", "text_type", "periodical"};
    public static final Set<String> METADATA_SET = new HashSet<>(Arrays.asList(SET_VALUES));


    /**
     * Run this pipe for creating all necessary annotations if an XML document is transformed
     *
     * @param mainCas
     * @return
     */
    public CAS createAllRWAnnos(CAS mainCas) {
        // correct the fvals and get Metadata anno from TEI metadata
        mainCas = this.getMetadataFromTEI(mainCas);
        // correct the Metadata and insert defaults
        mainCas = this.correctMetadata(mainCas, true);
        // create CabTokens
        mainCas = this.createCabTokens(mainCas);
        // create Sentence and Text annos
        mainCas = this.createSentAndText(mainCas);
        // create STWR annos (if the input xml contained any)
        mainCas = this.createSTWRAnnos(mainCas);
        return mainCas;
    }

    /**
     * If the XML input contained <stwr>-Tags, transform those to STWR annotations
     * @param mainCas
     * @return
     */
    public CAS createSTWRAnnos(CAS mainCas) {
        // get the STWR annotation
        Type stwrType = mainCas.getTypeSystem().getType("de.idsma.rw.Stwr");
        Feature stwr_rtype = stwrType.getFeatureByBaseName("RType");
        Feature stwr_medium = stwrType.getFeatureByBaseName("Medium");
        Feature stwr_surf = stwrType.getFeatureByBaseName("Stwr");
        Feature stwr_id = stwrType.getFeatureByBaseName("StwrID");

        // get the TeiType annotation
        String typeName = "de.uniwue.kalimachos.coref.type.TeiType";
        Type teiType = mainCas.getTypeSystem().getType(typeName);
        AnnotationIndex<AnnotationFS> teiTypeIndex = mainCas.getAnnotationIndex(teiType);
        Feature tagName = teiType.getFeatureByBaseName("TagName");
        Feature attributes = teiType.getFeatureByBaseName("Attributes");

        // collect the <f> tag annotations
        // and start and end pos of fs anno
        int fs_start = 0;
        int fs_end = 0;
        Set<AnnotationFS> fAnnos = new HashSet<>();
        Integer idCount = 1;
        for (AnnotationFS teiTypeAnno : teiTypeIndex) {
            String value = teiTypeAnno.getFeatureValueAsString(tagName);
            if (value.equals("stwr")) {
                AnnotationFS stwrAnno = mainCas.createAnnotation(stwrType, teiTypeAnno.getBegin(), teiTypeAnno.getEnd());
                stwrAnno.setFeatureValueFromString(stwr_rtype, this.getFeatureValFromTeiType(teiTypeAnno, "type", mainCas));
                stwrAnno.setFeatureValueFromString(stwr_medium, this.getFeatureValFromTeiType(teiTypeAnno, "medium", mainCas));
                stwrAnno.setFeatureValueFromString(stwr_id, idCount.toString());
                stwrAnno.setFeatureValueFromString(stwr_surf, teiTypeAnno.getCoveredText());

                mainCas.addFsToIndexes(stwrAnno);
                idCount++;
            }
        }
        return mainCas;
    }


    /**
     * Creates a Metadata annotation from the TEI metadata
     *
     * @param mainCas
     * @return
     */
    public CAS getMetadataFromTEI(CAS mainCas) {

        // get the Metadata annotation
        Type metaType = mainCas.getTypeSystem().getType("de.idsma.rw.Metadata");
        AnnotationIndex<AnnotationFS> metatypeIndex = mainCas.getAnnotationIndex(metaType);
        AnnotationFS metaDataAnno;
        // try to get the Metadata and add the annotation if is does not exist
        if (metatypeIndex.size() == 0) {
            metaDataAnno = mainCas.createAnnotation(metaType, 0, 0);
            mainCas.addFsToIndexes(metaDataAnno);
        } else {
            // there is only one metaData Annotation per text
            metaDataAnno = metatypeIndex.iterator().next();
        }

        // get the TeiType annotation
        String typeName = "de.uniwue.kalimachos.coref.type.TeiType";
        Type teiType = mainCas.getTypeSystem().getType(typeName);
        AnnotationIndex<AnnotationFS> teiTypeIndex = mainCas.getAnnotationIndex(teiType);
        Feature tagName = teiType.getFeatureByBaseName("TagName");
        Feature attributes = teiType.getFeatureByBaseName("Attributes");

        // collect the <f> tag annotations
        // and start and end pos of fs anno
        int fs_start = 0;
        int fs_end = 0;
        Set<AnnotationFS> fAnnos = new HashSet<>();
        for (AnnotationFS teiTypeAnno : teiTypeIndex) {
            String value = teiTypeAnno.getFeatureValueAsString(tagName);
            if (value.equals("f")) {
                fAnnos.add(teiTypeAnno);
            } else if (value.equals("fs")) {
                fs_start = teiTypeAnno.getBegin();
                fs_end = teiTypeAnno.getEnd();
            }
        }

        // check for each expected Metadatum:
        for (String meta : METADATA_SET) {
            AnnotationFS matchingF = null;
            String value = "";
            for (AnnotationFS fAnno : fAnnos) {
                String attrs = fAnno.getFeatureValueAsString(attributes);
                if (attrs.matches(".*name=" + meta + "##.*")) {
                    matchingF = fAnno;
                    break;
                }
            }
            if (matchingF != null) {
                // check if fVal is present and add it if not
                String attrs = matchingF.getFeatureValueAsString(attributes);
                Pattern p = Pattern.compile(".*fVal=([^#]+)##.*");
                Matcher m = p.matcher(attrs);
                boolean b = m.matches();

                if (m.matches()) {
                    //System.out.println(meta + ": fVal found");
                    // store the value for later
                    value = m.group(1);
                    //System.out.println("value: " + value);
                } else {
                    //System.out.println(meta + ": fVal missing");
                    // retrieve value from covered text:
                    value = matchingF.getCoveredText().trim();
                    matchingF.setFeatureValueFromString(attributes,
                            "name=" + meta + "##fVal=" + value + "##");
                    //System.out.println("value from covered text: " + value);
                }
            } else {
                System.out.println(meta + " missing!");
                // create a new annotation for the missing meta datum with empty value
                // insert it at the start position of the <fs> tag with zero
                // length
                value = "";
                AnnotationFS newAnno = mainCas.createAnnotation(teiType, fs_start, fs_start);
                newAnno.setFeatureValueFromString(tagName, "f");
                newAnno.setFeatureValueFromString(attributes, "name=" + meta + "##fVal=##");
                mainCas.addFsToIndexes(newAnno);
            }


            // additionally, add a feature for the Metadata annotation
            String metaFeatName = "";
            switch (meta) {
                case "id":
                    metaFeatName = "Id";
                    break;
                case "filename":
                    metaFeatName = "OrigFile";
                    break;
                case "year":
                    metaFeatName = "Year";
                    break;
                case "decade":
                    metaFeatName = "Decade";
                    break;
                case "corpuspart":
                    metaFeatName = "Corpuspart";
                    break;
                case "title":
                    metaFeatName = "Title";
                    break;
                case "author":
                    metaFeatName = "Author";
                    break;
                case "fictional":
                    metaFeatName = "Fictional";
                    break;
                case "textlength":
                    metaFeatName = "Textlength";
                    break;
                case "sample_id":
                    metaFeatName = "SampleID";
                    break;
                case "narrative":
                    metaFeatName = "Narrative";
                    break;
                case "text_type":
                    metaFeatName = "TextType";
                    break;
                case "periodical":
                    metaFeatName = "Periodical";
                    break;
                default:
                    metaFeatName = "";

            }

            // print warning if any feature is empty (except for TextType, Narrative and Periodical)
            if (!metaFeatName.equals("")) {
                if (value.equals("") &&
                        !(metaFeatName.equals("TextType") || metaFeatName.equals("Narrative") || metaFeatName.equals("Periodical"))
                        ) {
                    System.out.println("WARN: value empty for " + metaFeatName);
                }
                //System.out.println(metaFeatName + ": " + value);
                Feature metaFeat = metaType.getFeatureByBaseName(metaFeatName);
                metaDataAnno.setFeatureValueFromString(metaFeat, value);
            }

            // in addition, if the MetaData attributes Name, Timestamp, Version
            // are missing, add them with empty String ""
            String[] autoAttributes = {"Name", "Timestamp", "Version"};
            for (String feat : autoAttributes) {
                Feature metaFeat = metaType.getFeatureByBaseName(feat);
                if (metaDataAnno.getFeatureValueAsString(metaFeat) == null) {
                    metaDataAnno.setFeatureValueFromString(metaFeat, "");
                }
            }
        }
        return (mainCas);
    }

    /**
     * Reads changefile and changes autor and title values accordingly
     * Structure of changefile: csv-file with columns
     * file	title	title_korr	author	author_korr
     * TODO: Noch implementieren
     * @param mainCas
     * @param changefile
     * @return
     */
    public CAS correct_autor_title_with_list(CAS mainCas, String changefile){
        // get the Metadata annotation
        Type metaType = mainCas.getTypeSystem().getType("de.idsma.rw.Metadata");
        AnnotationIndex<AnnotationFS> metatypeIndex = mainCas.getAnnotationIndex(metaType);
        AnnotationFS metaDataAnno;
        if (metatypeIndex.size() == 0) {
            metaDataAnno = mainCas.createAnnotation(metaType, 0, 0);
            mainCas.addFsToIndexes(metaDataAnno);
        } else {
            // there is only one metaData Annotation per text
            metaDataAnno = metatypeIndex.iterator().next();
        }

        Feature author = metaType.getFeatureByBaseName("Author");
        String authorVal = metaDataAnno.getFeatureValueAsString(author);
        Feature title = metaType.getFeatureByBaseName("Title");
        String titleVal = metaDataAnno.getFeatureValueAsString(title);

        // read the changefile

        return(mainCas);
    }


    /**
     * Assumes that there is a Metadata annotation;
     * adds missing values and corrects mistakes in that annotation
     * does NOT make any changes to the TEI metadata values!!!
     * If WithDefaults is set to true, the following defaults will be set:
     * (for EMPTY entries only!):
     * "fictional" --> "yes" for erz; "no" or famz/zeit
     * "narrative" --> "yes" for erz; "no" or famz/zeit
     * This may lead to incorrect values!!!
     *
     * @param mainCas
     * @param withDefaults
     * @return
     */
    public CAS correctMetadata(CAS mainCas, boolean withDefaults) {
        // get the Metadata annotation
        Type metaType = mainCas.getTypeSystem().getType("de.idsma.rw.Metadata");
        AnnotationIndex<AnnotationFS> metatypeIndex = mainCas.getAnnotationIndex(metaType);
        AnnotationFS metaDataAnno;
        if (metatypeIndex.size() == 0) {
            metaDataAnno = mainCas.createAnnotation(metaType, 0, 0);
            mainCas.addFsToIndexes(metaDataAnno);
        } else {
            // there is only one metaData Annotation per text
            metaDataAnno = metatypeIndex.iterator().next();
        }

        Feature texttype = metaType.getFeatureByBaseName("TextType");
        String texttypeVal = metaDataAnno.getFeatureValueAsString(texttype);
        Feature corpuspart = metaType.getFeatureByBaseName("Corpuspart");
        String corpuspartVal = metaDataAnno.getFeatureValueAsString(corpuspart);
        Feature origfile = metaType.getFeatureByBaseName("OrigFile");
        String origfileVal = metaDataAnno.getFeatureValueAsString(origfile);
        Feature narrative = metaType.getFeatureByBaseName("Narrative");
        String narrativeVal = metaDataAnno.getFeatureValueAsString(narrative);
        Feature fictional = metaType.getFeatureByBaseName("Fictional");
        String fictionalVal = metaDataAnno.getFeatureValueAsString(fictional);
        Feature periodical = metaType.getFeatureByBaseName("Periodical");
        String periodicalVal = metaDataAnno.getFeatureValueAsString(periodical);
        Feature author = metaType.getFeatureByBaseName("Author");
        String authorVal = metaDataAnno.getFeatureValueAsString(author);
        Feature title = metaType.getFeatureByBaseName("Title");
        String titleVal = metaDataAnno.getFeatureValueAsString(title);


        // if corpuspart is missing, restore it from id
        //System.out.println("corpuspartVal: " + corpuspartVal);
        if (corpuspartVal.equals("undefined")){
            Feature idtype = metaType.getFeatureByBaseName("Id");
            String idtypeVal = metaDataAnno.getFeatureValueAsString(idtype);
            //System.out.println(idtypeVal);
            corpuspartVal = idtypeVal.replaceAll("_(.*)", "");
            //System.out.println("corpuspartVal: " + corpuspartVal);
        }

        // rename the corpusparts
        String corpuspartValNew = "";
        switch (corpuspartVal) {
            case "erz":
                corpuspartValNew = "digbib";
                break;
            case "zeit":
                corpuspartValNew = "mkhz";
                break;
            case "famz":
                corpuspartValNew = "grenz";
                break;
                default:
                    corpuspartValNew = corpuspartVal;
        }

        metaDataAnno.setFeatureValueFromString(corpuspart, corpuspartValNew);


        // set periodical val to "Undefined", if the corpuspartValNew is digbib
        if (corpuspartValNew.equals("digbib")) {
            periodicalVal = "Undefined";
        }

        // fix the new periodical names
        String periodicalValNew = periodicalVal;
        switch (periodicalVal) {
            case "BZ":
                periodicalValNew = "badener";
                break;
            case "CAZ":
                periodicalValNew = "czernowitzer";
                break;
            case "MT":
                periodicalValNew = "maehrisches";
                break;
            case "MZ":
                periodicalValNew = "marburger";
                break;
            case "RP":
                periodicalValNew = "reichspost";
                break;
            case "SGV":
                periodicalValNew = "stgaller";
                break;
            default:
                periodicalValNew = periodicalVal.replaceAll("nn_", "");
                break;
        }
        metaDataAnno.setFeatureValueFromString(periodical, periodicalValNew);


        // add metadata source
        Feature source = metaType.getFeatureByBaseName("Source");
        String sourceVal = corpuspartValNew;
        if (corpuspartValNew.equals("mkhz")) {
                sourceVal = corpuspartValNew + "." + periodicalValNew;
        }
        metaDataAnno.setFeatureValueFromString(source, sourceVal);

        // fix texttype
        String texttypeValNew = texttypeVal;
        if (texttypeValNew.equals("") || texttypeValNew.equals("None")) {
            if (corpuspartVal.equals("erz")) {
                texttypeValNew = "Erzähltext";
            } else {
                texttypeValNew = "Undefined";
            }
        }
        // correct wrong values here
        if (texttypeValNew.equals("Reisebericht")) {
            texttypeValNew = "Reisebericht/Brief";
        }
        else if (texttypeValNew.equals("Brief")) {
            texttypeValNew = "Reisebericht/Brief";
        }
        else if (texttypeValNew.equals("erzaehlung")
                || texttypeValNew.equals("Erzähltexte")
                || texttypeValNew.equals("Erzählung")){
            texttypeValNew = "Erzähltext";
            System.out.println("Korrekt Erzähltext");
        }
        texttypeValNew = WordUtils.capitalize(texttypeValNew);
        metaDataAnno.setFeatureValueFromString(texttype, texttypeValNew);

        //System.out.println("Texttype: " + texttypeVal + " --> " + texttypeValNew);

        // fix narrative --> if "WithDefaults" is set to true, add defaults here
        if (narrativeVal.equals("") ||  narrativeVal.equals("None")) {
            if (withDefaults) {
                if (corpuspartVal.equals("erz")) {
                    metaDataAnno.setFeatureValueFromString(narrative, "yes");
                } else {
                    metaDataAnno.setFeatureValueFromString(narrative, "no");
                }
            } else {
                metaDataAnno.setFeatureValueFromString(narrative, "Undefined");
            }
        }
        // fix fictional --> if "WithDefaults" is set to true, add defaults here
        if (fictionalVal.equals("") | fictionalVal.equals("None")) {
            if (withDefaults) {
                if (corpuspartVal == "erz") {
                    metaDataAnno.setFeatureValueFromString(fictional, "yes");
                } else {
                    metaDataAnno.setFeatureValueFromString(fictional, "no");
                }
            } else {
                metaDataAnno.setFeatureValueFromString(fictional, "Undefined");
            }
        }
        // fix periodical
        if (periodicalVal.equals("") || periodicalVal.equals("None")) {
            if (corpuspartVal.equals("zeit")) {
                String val = origfileVal.replaceAll("(^[^\\d]+)\\d.*$", "$1");
                metaDataAnno.setFeatureValueFromString(periodical, val);
            } else if (corpuspartVal.equals("famz")) {
                metaDataAnno.setFeatureValueFromString(periodical, "grenzboten");
            } else {
                metaDataAnno.setFeatureValueFromString(periodical, "Undefined");
            }
        }

        String authorValOld = authorVal;
        // fix author (add Undefined for missing author)
        if (authorVal.equals("") || authorVal.equals("None")) {
            metaDataAnno.setFeatureValueFromString(author, "Undefined");
        }
        // try to fix Author spacing
        else {
            if (Pattern.matches(".*,[^\\s].*", authorVal)) {
                authorVal = authorVal.replaceAll(",([^\\s])", ", $1");
            }
            // add a space before a "von" (if missing)
            if (Pattern.matches(".*[^\\s]von$", authorVal)) {
                authorVal = authorVal.replaceAll("([^\\s])von$", "$1 von");
            }
            // add a space before any capitalized Letter in the word
            authorVal = authorVal.replaceAll("(\\p{Lower})(\\p{Upper})", "$1 $2");
            metaDataAnno.setFeatureValueFromString(author, authorVal);
        }
        //System.out.println(authorValOld + " --- " + authorVal);

        // fix author (add Undefined for missing titel)
        if (titleVal.equals("") || titleVal.equals("None")) {
            metaDataAnno.setFeatureValueFromString(title, "Undefined");
        }
        // try to fix Title spacing (Space before upper case character)
        else {
            titleVal = titleVal.replaceAll("(\\p{Lower})(\\p{Upper})", "$1 $2");
            metaDataAnno.setFeatureValueFromString(title, titleVal);
        }

        return (mainCas);
    }


    public CAS removeAllAnnotationsOfType(CAS mainCas, String typeName) {
        Type myType = mainCas.getTypeSystem().getType(typeName);
        // remove sent and text annotations if they exist already
        // (must create a list first, because uima does not allow removing annos
        // while iterating over the annotation index)
        AnnotationIndex<AnnotationFS> myTypeIndex = mainCas.getAnnotationIndex(myType);
        List<AnnotationFS> myList = new ArrayList<>();
        for (AnnotationFS anno : myTypeIndex) {
            myList.add(anno);
        }
        for (AnnotationFS anno : myList) {
            mainCas.removeFsFromIndexes(anno);
        }
        return mainCas;
    }

    public CAS createSentAndText(CAS mainCas) {
        // first remove all sent and text annos;
        mainCas = this.removeAllAnnotationsOfType(mainCas, "de.idsma.rw.Sentence");
        mainCas = this.removeAllAnnotationsOfType(mainCas, "de.idsma.rw.Text");

        Type sentType = mainCas.getTypeSystem().getType("de.idsma.rw.Sentence");
        Type textType = mainCas.getTypeSystem().getType("de.idsma.rw.Text");

        String typeName = "de.uniwue.kalimachos.coref.type.TeiType";
        Type teiType = mainCas.getTypeSystem().getType(typeName);
        AnnotationIndex<AnnotationFS> teiTypeIndex = mainCas.getAnnotationIndex(teiType);
        Feature tagName = teiType.getFeatureByBaseName("TagName");
        boolean in_body = false;

        for (AnnotationFS teiTypeAnno : teiTypeIndex) {
            String value = teiTypeAnno.getFeatureValueAsString(tagName);
            if (value.equals("body")) {
                in_body = true;
                AnnotationFS newAnno = mainCas.createAnnotation(textType, teiTypeAnno.getBegin(), teiTypeAnno.getEnd());
                mainCas.addFsToIndexes(newAnno);
            } else if (in_body && value.equals("s")) {
                if (this.getFeatureValFromTeiType(teiTypeAnno, "id", mainCas) == null) {
                    System.out.println("Sentence with id=null was skipped");
                }
                else {
                    AnnotationFS newAnno = mainCas.createAnnotation(sentType, teiTypeAnno.getBegin(), teiTypeAnno.getEnd());
                    newAnno.setFeatureValueFromString(sentType.getFeatureByBaseName("Id"),
                            this.getFeatureValFromTeiType(teiTypeAnno, "id", mainCas));
                    newAnno.setFeatureValueFromString(sentType.getFeatureByBaseName("Prev"),
                            this.getFeatureValFromTeiType(teiTypeAnno, "prev", mainCas));
                    newAnno.setFeatureValueFromString(sentType.getFeatureByBaseName("Next"),
                            this.getFeatureValFromTeiType(teiTypeAnno, "next", mainCas));
                    mainCas.addFsToIndexes(newAnno);
                }
            }
        }

        return (mainCas);

    }

    /**
     * Creates CabToken annotations from the TEI w annotations
     * WARNING: The attributes of the CabToken can be empty Strings (if the CAB has not generated xlit and moot tags
     * for a specific word)
     *
     * @param mainCas
     * @return
     */
    public CAS createCabTokens(CAS mainCas) {
        // remove all existing CabTokens
        mainCas = this.removeAllAnnotationsOfType(mainCas, "de.idsma.rw.CabToken");

        // create CabTokens from the wAnnos
        String typeName2 = "de.idsma.rw.CabToken";
        Type cabType = mainCas.getTypeSystem().getType(typeName2);

        String typeName = "de.uniwue.kalimachos.coref.type.TeiType";
        Type teiType = mainCas.getTypeSystem().getType(typeName);
        AnnotationIndex<AnnotationFS> teiTypeIndex = mainCas.getAnnotationIndex(teiType);
        Feature tagName = teiType.getFeatureByBaseName("TagName");

        Set<AnnotationFS> wAnnos = new HashSet<>();
        Set<AnnotationFS> mootAnnos = new HashSet<>();
        Set<AnnotationFS> xlitAnnos = new HashSet<>();
        for (AnnotationFS teiTypeAnno : teiTypeIndex) {
            String value = teiTypeAnno.getFeatureValueAsString(tagName);
            if (value.equals("w")) {
                wAnnos.add(teiTypeAnno);
            } else if (value.equals("moot")) {
                mootAnnos.add(teiTypeAnno);
            } else if (value.equals("xlit")) {
                xlitAnnos.add(teiTypeAnno);
            }
        }

        //System.out.println("w: " + wAnnos.size() + " moot: " + mootAnnos.size() + " xlit: " + xlitAnnos.size());
        if (wAnnos.isEmpty()) {
            System.out.println("WARN: Contains no w-Annos");
        }
        for (AnnotationFS wAnno : wAnnos) {
            AnnotationFS matchingMoot = this.getCovered(wAnno, mootAnnos);
            AnnotationFS matchingXlit = this.getCovered(wAnno, xlitAnnos);

            AnnotationFS newAnno = mainCas.createAnnotation(cabType, wAnno.getBegin(), wAnno.getEnd());
            // System.out.println(cabType.getFeatures());
            newAnno.setFeatureValueFromString(cabType.getFeatureByBaseName("Id"),
                    this.getFeatureValFromTeiType(wAnno, "id", mainCas));
            if (matchingXlit != null) {
                newAnno.setFeatureValueFromString(cabType.getFeatureByBaseName("Xlit"),
                        this.getFeatureValFromTeiType(matchingXlit, "latin1Text", mainCas));
            } else {
                System.out.println("WARN: Missing xlit for: " + wAnno.getCoveredText());
            }
            if (matchingMoot != null) {
                newAnno.setFeatureValueFromString(cabType.getFeatureByBaseName("Canon"),
                        this.getFeatureValFromTeiType(matchingMoot, "word", mainCas));
                newAnno.setFeatureValueFromString(cabType.getFeatureByBaseName("Lemma"),
                        this.getFeatureValFromTeiType(matchingMoot, "lemma", mainCas));
                newAnno.setFeatureValueFromString(cabType.getFeatureByBaseName("Pos"),
                        this.getFeatureValFromTeiType(matchingMoot, "tag", mainCas));
                mainCas.addFsToIndexes(newAnno);
            } else {
                newAnno.setFeatureValueFromString(cabType.getFeatureByBaseName("Canon"),
                        "");
                newAnno.setFeatureValueFromString(cabType.getFeatureByBaseName("Lemma"),
                        "");
                newAnno.setFeatureValueFromString(cabType.getFeatureByBaseName("Pos"),
                        "");
                System.out.println("WARN: Missing moot for : " + wAnno.getCoveredText());
            }
            mainCas.addFsToIndexes(newAnno);
            //System.out.println("New Anno: " + newAnno);
        }

        return mainCas;
    }

    /**
     * get the value for a featName from the TEIType Annotation returns null if
     * there is no value with this featName
     *
     * @param anno
     * @param featName
     * @param mainCas
     * @return
     */
    private String getFeatureValFromTeiType(AnnotationFS anno, String featName, CAS mainCas) {
        String val = null;
        // get Types and Features
        String typeName = "de.uniwue.kalimachos.coref.type.TeiType";
        Type teiType = mainCas.getTypeSystem().getType(typeName);
        Feature attributes = teiType.getFeatureByBaseName("Attributes");
        //System.out.println("Curr Anno: "+ anno);
        //System.out.println("Attributes: "+ anno.getFeatureValueAsString(attributes));
        String attrString = anno.getFeatureValueAsString(attributes);

        Pattern p = Pattern.compile(".*" + featName + "=([^##]*)##.*");
        Matcher m = p.matcher(attrString);
        if (m.matches()) {
            val = m.group(1);
        }
        return val;
    }

    /**
     * Return the first AnnotationFS from the candidate set that is covered by
     * mainAnno NOTE: If there are multiple covered AnnotationFS, only one is
     * returned! Returns null if there is no covered AnnotationFS
     *
     * @param mainAnno
     * @param candidates
     * @return
     */
    private AnnotationFS getCovered(AnnotationFS mainAnno, Set<AnnotationFS> candidates) {
        int mainBegin = mainAnno.getBegin();
        int mainEnd = mainAnno.getEnd();
        AnnotationFS result = null;
        for (AnnotationFS candidate : candidates) {
            if (mainBegin <= candidate.getBegin() && candidate.getEnd() <= mainEnd) {
                result = candidate;
                break;
            }
        }
        return result;
    }


    public static void main(String[] args) {

        try {
            CorrectMetadata myMain = new CorrectMetadata();

            String inputPath = "E:\\Git_RW\\myrepo\\7_final\\FINAL-release\\rwk_mkhz_10015-1.xmi";
            String outputPath = "E:\\Git_RW\\myrepo\\7_final\\FINAL-release_metacorrect\\rwk_mkhz_10015-1.xmi";

            String typeSystem = "E:/Github/pycas_rw/pycas_rw_core/redeWiedergabeTypesystem_compare_tei_cab_fI.xml";
            TypeSystemDescription tsd = TypeSystemDescriptionFactory
                    .createTypeSystemDescriptionFromPath(new File(typeSystem).toURI().toURL().toString());
            CAS mainCas = CasCreationUtils.createCas(tsd, null, null);
            XmiCasDeserializer.deserialize(new FileInputStream(inputPath), mainCas);

            myMain.correctMetadata(mainCas, true);

            FileOutputStream outStream = new FileOutputStream(outputPath);
            XmiCasSerializer.serialize(mainCas, outStream);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
