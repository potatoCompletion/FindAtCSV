package org.example;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.*;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchoolFinder {
    private final List<String[]> originSchoolLines;
    private final List<String[]> sourceFileLines;
    private final List<String> originSchoolList;
    private final List<String> filteredSchoolList;

    public SchoolFinder(String sourceFilePath, String schoolDataFilePath) throws IOException, CsvException {

        CSVReader schoolsCsvReader = new CSVReader(new FileReader(schoolDataFilePath));
        CSVReader sourceFileCsvReader = new CSVReader(new FileReader(sourceFilePath));

        originSchoolLines = schoolsCsvReader.readAll();
        sourceFileLines = sourceFileCsvReader.readAll();

        originSchoolList = stringArrayListToList(originSchoolLines);
        filteredSchoolList = getFilteredSchoolList(originSchoolList);
    }

    public static class SchoolFinderBuilder {};

    public Map<String, Integer> findValidSchool() throws IOException {
        Map<String, Integer> result = new HashMap<>();

        for (String[] line : sourceFileLines) {
            String filteredLine = String.join("", line)
                    .replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣]", ""); // 한글제외 모든 불필요 문자 제거

            String school = findAtSchoolList(filteredLine, originSchoolList);
            if (!school.isBlank()) {
                result.merge(school, 1, Integer::sum);
                continue;
            }

            filteredLine = filteredLine
                    .replace("등학교", "")  // OO초, OO중, OO고, OO대 라고 입력한 데이터 또한 유효한 문자열로 인식하기 위한 작업
                    .replace("중학교", "중")
                    .replace("대학교", "대")
                    .replace("대학", "대")
                    .replace("사범대", "")
                    .replace("여중", "여자중")
                    .replace("체고", "체육고")
                    .replace("여고", "여자고")
                    .replace("예고", "예술고")
                    .replace("여대", "여자대");

            school = findAtSchoolList(filteredLine, filteredSchoolList);
            if (!school.isBlank()) {
                result.merge(school, 1, Integer::sum);
                continue;
            }

            result.merge("기타", 1, Integer::sum);
        }

        return result;
    }

    private List<String> getFilteredSchoolList(List<String> originSchoolList) {
        List<String> filteredList = new ArrayList<>();
        for (String line : originSchoolList) {
            String filteredString = line
                    .replace(" ", "")
                    .replace("등학교", "")
                    .replace("중학교", "중")
                    .replace("대학교", "대")
                    .replace("대학", "대")
                    .replace("사범대", "");


            filteredList.add(filteredString);
        }

        return filteredList;
    }

    private List<String> stringArrayListToList(List<String[]> stringArrayList) {
        List<String> list = new ArrayList<>();
        for (String[] stringArray : stringArrayList) {
            String str = String.join("", stringArray);
            list.add(str);
        }

        return list;
    }

    private String findAtSchoolList(String comment, List<String> schoolList) {
        for (String school : schoolList) {

            if (comment.contains(school)) {
                int index = schoolList.indexOf(school);
                return String.join("", originSchoolLines.get(index));
            }
        }

        return "";
    }
}
