package com.example;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import com.example.jobSquare.jobSquare;
import com.example.jobPortal.jobPortal;
import org.json.JSONArray;

public class Main {
    public static void main(String[] args) throws Exception {
        // jobSquare js = new jobSquare(true);
        jobPortal jp = new jobPortal(true);

        ArrayList<Job> availableJobs = jp.jobs();
        // //availableJobs.addAll(js.jobs());

        File arffFile = new File("jobPortal.arff");
        String arffData = "";
        for(Job j: availableJobs){
            try{
                arffData += j.arffData();
            }catch(Exception e){
                continue;
            }
        }
        Files.write(arffFile.toPath(), (Job.arffHeader() + arffData).getBytes());
        ML ml = new ML("jobPortal.arff");

    }
}
