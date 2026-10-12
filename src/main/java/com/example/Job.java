package com.example;

import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Locale.Category;

import org.json.JSONObject;

public class Job {
    private String OriginalLink;
    private String Title;
    private String Emplacement;
    private String DatePublication;
    private ArrayList<String> Exigences;
    private String NiveauEtude;
    private String Description;
    private String Publisher;
    private String TypeContrat;
    private String Categorie;
    private static int Current_Id = 0;
    private int Id = 0;
    private static Set<String> Categories = new LinkedHashSet<>();
    public Job(String OriginalLink, String Title, String Emplacement, String DatePublication,
            ArrayList<String> Exigences, String Description, String Publisher, String NiveauEtude, String TypeContrat, String Categorie) {
        this.OriginalLink = OriginalLink;
        this.Title = Title.replace("\"", "\\\"").replace("'", "\\'").trim();
        this.Emplacement = Emplacement;
        this.DatePublication = DatePublication;
        this.Description = Description.replace("'", "\\'").replace("'", "\\'").trim();
        this.Exigences = Exigences;
        this.Publisher = Publisher;
        this.NiveauEtude = NiveauEtude;
        this.TypeContrat = TypeContrat;
        this.Categorie = Categorie;
        this.Id = Current_Id++;
        Categories.add("'" + Categorie.trim().replace("'", "\\'") + "'");
    }

    public Job() {
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

    public static Set<String> cats(){
        return Categories;
    }

    public static String arffHeader(){
        return String.format("""
                @relation \"Job\"
                @attribute Titre string
                @attribute Categorie %s
                @attribute Description string
                @attribute Type_contrat string
                @attribute Emplacement string
                @attribute Niveau_education string

                @data

        """, cats().toString().replace("[", "{").replace("]", "}"));
    }
    public String arffData(){
        return String.format("'%s', '%s', '%s', '%s', '%s', '%s'\n", Title, Categorie, Description, TypeContrat, Emplacement, NiveauEtude);
    }
    public JSONObject getJson(){
        String jsonBody = String.format("""
        {
            "Title" : "%s",
            "Emplacement" : "%s",
            "Link" : "%s",
            "Description" : "%s",
            "Niveau d'education" : "%s",
            "Type de contrat" : "%s",
            "Categorie" : "%s",
            "Exigences" : "%s"
        }
        """
        , Title, Emplacement, OriginalLink, Description, NiveauEtude, TypeContrat, Categorie, Exigences);
        return new JSONObject(jsonBody);
    }

    public String getCSV(){
        String csvBody = String.format("""
            %s, %s, %s, %s, %s, %s
        """, Title, Emplacement, OriginalLink, Description, NiveauEtude, Exigences);
        return csvBody;
    }
    public static String csvHeader(){
        return "Title, Emplacement, Lien, Description, Exigences\n";
    }

}
