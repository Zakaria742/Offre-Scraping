package com.example.jobSquare;

import java.util.ArrayList;
import com.example.Job;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import java.util.Arrays;

public class jobSquare {
    private String siteOriginal = "https://www.jobsquare.ma/jobs";
    private static ArrayList<Job> availableJobs = new ArrayList<>();

    public jobSquare(boolean watch) {
        try {
            int currentPage = 0;
            Document jobSquare = Jsoup.connect(siteOriginal).get();
            Elements jobs = jobSquare.selectFirst("div[class='sj-jobs-list']").select("a[class='sj-card-btn']");
            int totalJobs = 0;

            while (jobs.size() > 0 && currentPage < 2) {
                String link = siteOriginal + "?page=" + ++currentPage;
                jobSquare = Jsoup.connect(link).get();
                jobs = jobSquare.selectFirst("div[class='sj-jobs-list']").select("a[class='sj-card-btn']");
                for (Element job : jobs) {
                    String jobLink = job.attr("href");
                    Document jobDocument = Jsoup.connect(jobLink).get();
                    String Title = jobDocument.select("h1[class='jd-title']").text();

                    String Publisher = jobDocument.select("div[class='jd-meta'] > a").text();
                    String Location = jobDocument.select(".job-card__tag--location").text().split("[, ]")[0];
                    Elements ContentSection = jobDocument.select("div[class='jd-content']");
                    String Description = ContentSection.text().split("Description de l'emploi")[1];

                    // #region
                    String[] tmp = Description.split("Exigences de l'emploi Profil recherché");
                    ArrayList<String> Exigences = null;
                    if (tmp.length > 1) {
                        Exigences = new ArrayList<>(
                                Arrays.asList(tmp[1].split("Informations clés")[0].split("[.]")));
                    }
                    // #endregion

                    // finding informations section
                    Element motCleInformations = jobDocument.selectFirst("div[class='jd-section']:nth-child(2)");
                    short i = 3;
                    while (motCleInformations != null) {

                        if (motCleInformations.select(".jd-section-title").text().matches("(.)+Informations(.)+")) {
                            break;
                        }
                        motCleInformations = jobDocument
                                .selectFirst(String.format("div[class='jd-section']:nth-child(%d)", i++));

                    }

                    String niveauEtud = "";
                    String typeContrat = "";
                    if (motCleInformations != null) {
                        for (Element e : motCleInformations.select("div[class='jd-info-box']")) {
                            String section = e.selectFirst(".jd-label").text();
                            switch (section) {
                                case "Type d'emploi désiré":
                                    typeContrat = e.selectFirst(".jd-value").text();
                                    break;
                                case "Niveau d'étude":
                                    niveauEtud = e.selectFirst(".jd-value").text();
                            }

                        }
                    }

                    // end of finding information section
                    String DateAfterPublishing = jobDocument.select(".job-card__tag--date").text();
                    Job jobObject = new Job(jobLink, Title, Location, DateAfterPublishing, Exigences, Description,
                            Publisher, niveauEtud, typeContrat, "");
                    availableJobs.add(jobObject);
                    if (watch) {
                        System.out.printf("\u001b[2J\u001b[HTotal jobs found : %d\n", ++totalJobs);
                        System.out.printf("Current page : %d\n", currentPage);
                        System.out.printf("Current job : %s\n", Title);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Job> jobs() {
        return availableJobs;
    }
}
