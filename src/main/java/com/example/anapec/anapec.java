package com.example.anapec;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Document;
import com.example.Job;

public class anapec {
    String site = "https://www.marocannonces.com/";
    String originalLink = "https://www.marocannonces.com/categorie/309/Emploi/Offres-emploi/";
    int totalPages = 100;
    private static ArrayList<Job> jobs = new ArrayList<>();

    public anapec(boolean watch) {
        try {
            int currentPage = 1;
            int totalJobs = 0;
            ArrayList<String> links = new ArrayList<>();
            while (currentPage < totalPages) {
                Document annoncementsSite = Jsoup.connect(originalLink + currentPage++ + ".html").get();
                Elements jobLinks = annoncementsSite.select("ul[class='cars-list'] > li > a");

                for (Element job : jobLinks) {
                    String link = job.attr("href");
                    if (links.contains(link)) {
                        continue;
                    }
                    try {

                        Document annoncement = Jsoup.connect(site + link).get();
                        links.add(link);

                        Elements Exigences = annoncement.selectFirst("ul[id='extraQuestionName']").children();

                        String Categorie = "";
                        String Contrat = "";
                        String NiveauEtude = "";
                        String Ville = "";
                        for (Element child : Exigences) {
                            String[] tmp = child.text().split(":");
                            switch (tmp[0].trim()) {
                                case "Domaine":
                                    Categorie = tmp[1].trim();
                                    break;
                                case "Contrat":
                                    Contrat = tmp[1].trim();
                                    break;
                                case "Niveau d'études":
                                    NiveauEtude = tmp[1].trim();
                                    break;
                                case "Ville":
                                    Ville = tmp[1].trim();
                            }
                        }
                        String Titre = annoncement.selectFirst("h1").text();
                        String Description = annoncement.select(".desccatemploi > .block").text();

                        if(watch){

                            System.out.println("\u001b[2J\u001b[H-----------------------------------------------------");
                            System.out.println(" \u001b[2mLink : " + (site + link) + "\u001b[0m");
                            System.out.println("Titre :" + Titre);
                            System.out.println("Categorie : " + Categorie);
                            System.out.println("Contrat : " + Contrat);
                            System.out.println("Niveau d'education : " + NiveauEtude);
                            System.out.println("Ville : " + Ville);
                            System.out.println("Description : " + Description);
                        }
                        Job newJob = new Job(site+link, Titre, Ville, "", null, Description, "", NiveauEtude, Contrat, Categorie);
                        jobs.add(newJob);
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public ArrayList<Job> jobs(){
        return jobs;
    }
}
