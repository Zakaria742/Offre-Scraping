package com.example;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import com.example.jobSquare.jobSquare;
import com.example.jobPortal.jobPortal;
import org.json.JSONArray;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        //jobSquare js = new jobSquare(false);
        jobPortal jp = new jobPortal(true);
        /*
        
        ArrayList<Job> availableJobs = js.jobs();
        availableJobs.addAll(jp.jobs());

        Path filePath = Path.of("./listings.json");
        JSONArray jsonA = new JSONArray();
        for(Job job : availableJobs){
            try{
                jsonA.put(job.getJson());
            }catch(Exception e){
                System.out.println("\u001b[38;2;255;0;0m\u001b[1mYou got an error right there! \u001b[0m");
                System.out.println("\u001b[38;2;255;255;0m\u001b[1mLast job : \u001b[0m");
                System.out.println("\"" + job + "\"");
                e.printStackTrace();
                break;
            }
        }
        Files.write(filePath, jsonA.toString().getBytes());
        */
    }
}
