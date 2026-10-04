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
            int currentAnimIndex = 0;
            char[] animArray = {'/', '-', '\\', '|'};
            int totalJobs = 0;
            while (jobs.size() > 0) {
                String link = siteOriginal + "?page=" + ++currentPage;
                jobSquare = Jsoup.connect(link).get();
                jobs = jobSquare.selectFirst("div[class='sj-jobs-list']").select("a[class='sj-card-btn']");
                for (Element job : jobs) {
                    String jobLink = job.attr("href");
                    Document jobDocument = Jsoup.connect(jobLink).get();
                    String Title = jobDocument.select("h1[class='jd-title']").text();
                    if(watch){
                        System.out.printf("\u001b[2J\u001b[H Total jobs found : %d\n", ++totalJobs);
                        System.out.printf("\u001b[38;2;100;255;100m%c\u001b[0m current job : %s\n", animArray[ ( currentAnimIndex = (currentAnimIndex + 1) % 4 ) ], Title);
                    }

                    String Publisher = jobDocument.select("div[class='jd-meta'] > a").text();
                    String Location = jobDocument.select(".job-card__tag--location").text();
                    String ContentSection = jobDocument.select("div[class='jd-content']").text().replace("\"", "");
                    String Description = ContentSection.split("Description de l'emploi")[1];

                    //#region
                    String[] tmp = Description.split("Exigences de l'emploi Profil recherché");
                    ArrayList<String> Exigences = null;
                    if(tmp.length > 1){
                        Exigences = new ArrayList<>(
                            Arrays.asList(tmp[1].split("Informations clés")[0].split("[.]"))
                        );
                    }
                        
                    //#endregion
                    String DateAfterPublishing = jobDocument.select(".job-card__tag--date").text();
                    Job jobObject = new Job(jobLink, Title, Location, DateAfterPublishing, Exigences, Description, Publisher);
                    availableJobs.add(jobObject);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Job> jobs(){
        return availableJobs;
    }
}
