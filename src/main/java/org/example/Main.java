package org.example;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, CsvException {
        CSVReader csvReader = new CSVReader(new FileReader("comments.csv"));
        List<String[]> lines = csvReader.readAll();
        lines.forEach(line -> System.out.println(String.join(",", line)));
    }
}