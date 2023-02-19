package org.example;

import com.opencsv.exceptions.CsvException;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class Main {
    private final static Logger LOG = Logger.getGlobal();

    public static void main(String[] args) throws IOException, CsvException {
        SchoolFinder schoolFinder = new SchoolFinder("comments.csv", "전국초중고대데이터.csv");
        Map<String, Integer> result = schoolFinder.findValidSchool();
        FileWriter fileWriter = new FileWriter("result.txt");

        for (String key : result.keySet()) {
            fileWriter.write(key + "\t" + result.get(key) + "\n");
        }

        fileWriter.close();
        LOG.info("로깅로깅");
    }

}