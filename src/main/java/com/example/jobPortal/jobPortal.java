package com.example.jobPortal;

import com.example.Job;
import java.io.IOException;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//Class pour la website maroccain : https://www.jobportal.ma/

public class jobPortal {
    static ArrayList<Job> emploiDispo = new ArrayList<>();
    static String siteLink = "https://www.jobportal.ma/offres-emploi/";

    public jobPortal(boolean watch) {

        try {
            Document portail = Jsoup.connect(siteLink).get();
            int currentPage = 2;
            Elements emplois = portail.select("div[class='careerfy-joblisting-text']");
            int totalJobs = 0;
            while (emplois.size() > 0) {
                portail = Jsoup.connect(siteLink + "?pg=" + currentPage++).get();
                emplois = portail.select("div[class='careerfy-joblisting-text']");
                int i = 0;
                int currentAnimIndex = 0;
                char[] animArray = { '/', '-', '\\', '|' };
                ArrayList<String> listLinks = new ArrayList<>();
                for (Element emploi : emplois) {
                    listLinks.add(emploi.select("div[class='careerfy-list-option'] > * > a").attr("href"));
                    if (listLinks.get(i).strip().isEmpty()) {
                        continue;
                    }
                    totalJobs++;
                    String link = listLinks.get(i++);
                    Element annonce = Jsoup.connect(link).get().selectFirst("div[id^='pageid']");

                    Element jobDetailContent = annonce.selectFirst("div[class='careerfy-jobdetail-content']");
                    Element jobDetailOptions = annonce.selectFirst("ul[class='careerfy-jobdetail-options']");

                    // Information de l'offre :
                    String Title = jobDetailContent.select("div[class='careerfy-content-title']").text();

                    String Description = jobDetailContent.select("div[class='careerfy-description']").text().replace(
                            "\"",
                            "");
                    Element careerfyRow = jobDetailContent.selectFirst("ul[class='careerfy-row']");

                    ArrayList<String> Exigences = new ArrayList<>();
                    Exigences.add(
                            careerfyRow.select("li:nth-child(2) > div[class='careerfy-services-text'] > small").text());
                    Exigences.add(careerfyRow.select("li:nth-child(3) > div[class='careerfy-services-text'] > small")
                            .text()
                            .replace('/', ' ') + " "
                            + careerfyRow.select("li:nth-child(4) > div[class='careerfy-services-text'] > small").text()
                                    .replace('/', ' '));

                    String Emplacement = jobDetailOptions.select("li:nth-child(1)").text();
                    String DatePublication = jobDetailOptions.select("li:nth-child(2)").text().substring(21);// 'Date de
                    // publication:
                    // ' a une
                    // taille de 21
                    // characters

                    Job newJob = new Job(link, Title, Emplacement, DatePublication, Exigences, Description, "");
                    emploiDispo.add(newJob);
                    if (watch) {
                        System.out.printf("\u001b[2J\u001b[HTotal jobs found : %d\n", totalJobs);
                        System.out.printf("Current page : %d\n", currentPage - 2);
                        System.out.printf("\u001b[38;2;100;255;100m%c\u001b[0m Current job : %s\n",
                        animArray[(currentAnimIndex = (currentAnimIndex + 1) % 4)], newJob);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Job> jobs() {
        return emploiDispo;
    }

}