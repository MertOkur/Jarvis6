package com.example.jarvis6;

import android.os.Environment;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// is called to save request and answers to corresponding files
class MemorySaver {
    File publicDirectory;
    File appDirectory;
    Gson gson;

    public MemorySaver() {
        publicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        appDirectory = new File(publicDirectory, "Jarvis6");
        gson = new Gson();
    }

    /** WRITING */
    public void saveRequests(String input) {
        if (!appDirectory.exists()) {
            appDirectory.mkdirs();
        }

        var jsonlFile = new File(appDirectory, "my_requests.jsonl");
        try (var writer = new BufferedWriter(new FileWriter(jsonlFile, true))) {
            var jsonStr = gson.toJson(input);
            writer.write(jsonStr);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("following error occurred: " + e);
        }
    }

    public void saveAnswers(String input) {
        if (!appDirectory.exists()) {
            appDirectory.mkdirs();
        }

        var jsonFile = new File(appDirectory, "gpt_answers.jsonl");
        try (var writer = new BufferedWriter(new FileWriter(jsonFile, true))) {
            var jsonStr = gson.toJson(input);
            writer.write(jsonStr);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("following error occurred: " + e);
        }
    }

    /**READING */
    public List<String> readRequests() {
        var requestList = new ArrayList<String>();

        if (!appDirectory.exists())
            System.err.println("appDirectory does not exist");

        var jsonFile = new File(appDirectory, "my_requests.jsonl");
        try (var reader = new BufferedReader(new FileReader(jsonFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                var request = gson.fromJson(line, String.class);
                requestList.add(request);
            }
        } catch (IOException e) {
            System.err.println("following error occurred: " + e);
        }

        return requestList;
    }

    public List<String> readAnswers() {
        var answerList = new ArrayList<String>();

        if (!appDirectory.exists())
            System.err.println("appDirectory does not exist");

        var jsonFile = new File(appDirectory, "gpt_answers.jsonl");
        try (var reader = new BufferedReader(new FileReader(jsonFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                var answer = gson.fromJson(line, String.class);
                answerList.add(answer);
            }
        } catch (IOException e) {
            System.err.println("following error occurred: " + e);
        }

        return answerList;
    }
}
