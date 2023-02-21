package org.example;

import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) throws IOException, CsvException {

        log.info("==========학교 검색 시작==========");
        SchoolFinder schoolFinder = new SchoolFinder("comments.csv",
                "전국초중고대데이터.csv",
                "전국행정구역데이터(시,군).csv");
        Map<String, Integer> schoolCountMap = schoolFinder.findValidSchool();
        FileWriter fileWriter = new FileWriter("result.txt");

        for (Map.Entry<String, Integer> entry : schoolCountMap.entrySet()) {
            fileWriter.write(entry.getKey() + "\t" + entry.getValue() + "\n");
        }

        fileWriter.close();
        log.info("==========학교 검색 종료==========\n\n");
    }
}