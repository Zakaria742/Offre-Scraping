package com.example;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import com.example.jobPortal.jobPortal;
import com.example.mAnnonces.mAnnonces;

import org.json.JSONArray;

public class Main {
    public static void main(String[] args) throws Exception {
        mAnnonces an = new mAnnonces(true);
        // // jobSquare js = new jobSquare(true);
        jobPortal jp = new jobPortal(true);

        ArrayList<Job> availableJobs = jp.jobs();
        availableJobs.addAll(an.jobs());

        File arffFile = new File("listings.arff");
        String arffData = "";
        for(Job j: availableJobs){
            try{
                arffData += j.arffData();
            }catch(Exception e){
                continue;
            }
        }
        Files.write(arffFile.toPath(), (Job.arffHeader() + arffData).getBytes());
        ML ml = new ML("listings.arff");

    }
}
