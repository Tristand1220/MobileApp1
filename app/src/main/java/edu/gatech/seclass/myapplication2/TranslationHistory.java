package edu.gatech.seclass.myapplication2;
import com.google.firebase.Timestamp;
import java.util.Date;

public class TranslationHistory {
    private String originalText;
    private String translatedText;
    private String sourceLang;
    private String targetLang;
    private Timestamp timestamp;
    private String documentID;

    public TranslationHistory(){}

    public TranslationHistory(String originalText, String translatedText,String sourceLang, String targetLang){
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.sourceLang = sourceLang;
        this.targetLang = targetLang;
        this.timestamp = Timestamp.now();
    }
    //Getters and Setters

    public String getOriginalText(){
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getTranslatedText(){
        return translatedText;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }

    public String getSourceLang() {
        return sourceLang;
    }

    public void setSourceLang(String sourceLang) {
        this.sourceLang = sourceLang;
    }

    public String getTargetLang() {
        return targetLang;
    }

    public void setTargetLang(String targetLang) {
        this.targetLang = targetLang;
    }

    public Timestamp getTimeStamp(){
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp){
        this.timestamp=timestamp;

    }

    public String getDocumentID() {
        return documentID;
    }

    public void setDocumentID(String documentID) {
        this.documentID = documentID;
    }

    // Helper to retrieve target language
    public String getTargetLangDisplay(){
        switch (targetLang){
            case "es": return "Spanish";
            case "zh-CN": return "Chinese";
            case "ja": return "Japanese";
            default: return targetLang;
        }
    }
}
