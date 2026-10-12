package com.example.jobPortal;

import com.example.Job;
import java.io.IOException;
import java.net.SocketTimeoutException;
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
            int currentPage = 1;
            Elements emplois = portail.select("div[class='careerfy-joblisting-text']");
            int totalJobs = 0;
            while (emplois.size() > 0) {
                try {
                    portail = Jsoup.connect(siteLink + "?pg=" + currentPage++).get();

                } catch (Exception e) {
                    System.out.println(
                            "Couldn't fetch the next page, your internet connection is bad, non-existant or some other reason\nReturning...");
                }
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
                    try {
                        Element annonce = Jsoup.connect(link).get().selectFirst("div[id^='pageid']");
                        Element jobDetailContent = annonce.selectFirst("div[class='careerfy-jobdetail-content']");
                        Element jobDetailOptions = annonce.selectFirst("ul[class='careerfy-jobdetail-options']");

                        // Information de l'offre :
                        String Title = jobDetailContent.selectFirst("div[class='careerfy-content-title']").text()
                                .replace("\"", "\\\"").replace("'", "\\'");

                        String[] categorie_tmp = annonce.select(".careerfy-jobdetail-list > figcaption > span").text()
                                .split("Offres d'emploi");
                        String Category = (categorie_tmp.length > 0) ? categorie_tmp[categorie_tmp.length - 1].trim().replace("\"", "\\\"").replace("'", "\\'")
                                : "null";
                        if (Category.matches("null")) {
                            break;
                        }

                        String Description = jobDetailContent.select("div[class='careerfy-description']").text()
                                .replace("\"", "\\\"").replace("'", "\\'");
                        Element careerfyRow = jobDetailContent.selectFirst("ul[class='careerfy-row']");

                        ArrayList<String> Exigences = new ArrayList<>();

                        String niveau_education = careerfyRow
                                .select("li:nth-child(2) > div[class='careerfy-services-text'] > small").text().strip();
                        niveau_education = niveau_education.length() <= 0 || niveau_education.matches("Pas important")
                                ? "null"
                                : niveau_education;

                        String Skills = careerfyRow
                                .select("li:nth-child(4) > div[class='careerfy-services-text'] > small").text()
                                .replace('/', ',').strip();
                        Exigences.add((Skills.length() <= 0) ? "" : Skills);

                        String Emplacement = jobDetailOptions.select("li:nth-child(1)").text();
                        String DatePublication = jobDetailOptions.select("li:nth-child(2)").text().substring(21);// 'Date
                                                                                                                 // de
                                                                                                                 // publication:
                                                                                                                 // a
                                                                                                                 // une
                                                                                                                 // taille
                                                                                                                 // de
                                                                                                                 // 21
                                                                                                                 // characters

                        String TypeContrat = careerfyRow
                                .select("li:nth-child(5) > div[class='careerfy-services-text'] > small").text().strip();
                        niveau_education = niveau_education.length() <= 0 || niveau_education.matches("Pas important")
                                ? "null"
                                : niveau_education;

                        Job newJob = new Job(link, Title, Emplacement, DatePublication, Exigences, Description, "",
                                niveau_education, TypeContrat, Category);
                        emploiDispo.add(newJob);
                        if (watch) {
                            System.out.printf("\u001b[2J\u001b[HTotal jobs found : %d\n", totalJobs);
                            System.out.printf("Current page : %d\n", currentPage);
                            System.out.printf("\u001b[38;2;100;255;100m%c\u001b[0m Current job : %s\nCategorie : %s",
                                    animArray[(currentAnimIndex = (currentAnimIndex + 1) % 4)], Title, Category);
                        }
                    } catch (Exception e) {
                        System.out.println(
                                "Couldn't get the link, socket timeout either because your internet connection is slow or non-existant or some other reason\nStopping the fetching of job announcements");
                        return;
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