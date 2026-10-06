package com.example;

import java.util.ArrayList;
import org.json.JSONObject;

public class Job {
    private String OriginalLink;
    private String Title;
    private String Emplacement;
    private String DatePublication;
    private ArrayList<String> Exigences;
    private String Description;
    private String Publisher;

    public Job(String OriginalLink, String Title, String Emplacement, String DatePublication,
            ArrayList<String> Exigences, String Description, String Publisher) {
        this.OriginalLink = OriginalLink;
        this.Title = Title;
        this.Emplacement = Emplacement;
        this.DatePublication = DatePublication;
        this.Description = Description;
        this.Exigences = Exigences;
        this.Publisher = Publisher;
    }

    public Job() {
        this.Title = "\0";
        this.Emplacement = "\0";
        this.DatePublication = "\0";
        this.Description = "\0";
        this.Publisher = "\0";
        this.Exigences = new ArrayList<>();
    }

    public String toString() {
        return String.format(
                "Original Link : %s\nTitle : %s\nEmplacement : %s\nDate de Publication : %s\nExigences : %s\nDescription : %s\nPublisher : %s\n",
                OriginalLink, Title, Emplacement, DatePublication, Exigences, Description, (Publisher.length() <= 0) ? "Not found" : Publisher);
    }

    public String getTitle() {
        return Title;
    }

    public String getEmplacement() {
        return Emplacement;
    }

    public String getOriginalLink() {
        return OriginalLink;
    }

    public String getDescription() {
        return OriginalLink;
    }


    public ArrayList<String> getExigences() {
        return Exigences;
    }

    public JSONObject getJson(){
        String jsonBody = String.format("""
        {
            "Title" : "%s",
            "Emplacement" : "%s",
            "Link" : "%s",
            "Description" : "%s",
            "Exigences" : "%s"
        }
        """
        , Title, Emplacement, OriginalLink, Description, Exigences);
        return new JSONObject(jsonBody);
    }

    public String getCSV(){
        String csvBody = String.format("""
            %s, %s, %s, %s, %s
        """, Title, Emplacement, OriginalLink, Description, Exigences);
        return csvBody;
    }
    public static String csvHeader(){
        return "Title, Emplacement, Lien, Description, Exigences\n";
    }

}
