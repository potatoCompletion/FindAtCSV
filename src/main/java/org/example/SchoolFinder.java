package org.example;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchoolFinder {
    private static final Logger log = LoggerFactory.getLogger(SchoolFinder.class);
    private final List<String[]> originSchoolLines;
    private final List<String[]> sourceFileLines;
    private final List<String> originSchoolList;
    private final List<String> filteredSchoolList;

    public SchoolFinder(String sourceFilePath, String schoolDataFilePath) throws IOException, CsvException {

        CSVReader schoolsCsvReader = new CSVReader(new FileReader(schoolDataFilePath));
        CSVReader sourceFileCsvReader = new CSVReader(new FileReader(sourceFilePath));

        originSchoolLines = schoolsCsvReader.readAll();
        sourceFileLines = sourceFileCsvReader.readAll();

        originSchoolList = stringArrayListToList(originSchoolLines);    // 전국 학교 정식명칭 리스트
        filteredSchoolList = getFilteredSchoolList(originSchoolList);   // 전국 학교 약어 리스트 (ex: 강릉초등학교 -> 강릉초)
    }

    public Map<String, Integer> findValidSchool() throws IOException {

        Map<String, Integer> schoolCountMap = new HashMap<>();

        for (String[] line : sourceFileLines) {
            // 1차 필터 (정식명칭)
            String filteredLine = String.join("", line)
                    .replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣]", ""); // 한글제외 모든 불필요 문자 제거

            String school = findAtSchoolList(filteredLine, originSchoolList);
            if (!school.isBlank()) {
                schoolCountMap.merge(school, 1, Integer::sum);
                log.info("{} 발견", school);
                continue;
            }

            // 2차 필터 (약어)
            filteredLine = filteredLine
                    .replace("등학교", "")  // OO초, OO중, OO고, OO대 라고 입력한 데이터 또한 유효한 문자열로 인식하기 위한 작업
                    .replace("중학교", "중")
                    .replace("대학교", "대")
                    .replace("대학", "대")
                    .replace("사범대", "")
                    .replace("여중", "여자중")
                    .replace("체고", "체육고")
                    .replace("여고", "여자고")
                    .replace("예중", "예술중")
                    .replace("예고", "예술고")
                    .replace("여대", "여자대");

            school = findAtSchoolList(filteredLine, filteredSchoolList);
            if (!school.isBlank()) {
                schoolCountMap.merge(school, 1, Integer::sum);
                log.info("{} 발견", school);
                continue;
            }

            // 정식명칭, 약어 두 리스트에서 모두 찾을 수 없을 경우
            schoolCountMap.merge("알수없음", 1, Integer::sum);
            log.warn("(WARNING)알수없는 라인\n\n\"{}\"\n\n", filteredLine);
        }

        return schoolCountMap;
    }

    private List<String> getFilteredSchoolList(List<String> originSchoolList) {

        List<String> filteredList = new ArrayList<>();

        for (String line : originSchoolList) {
            filteredList.add(line
                    .replace(" ", "")
                    .replace("등학교", "")
                    .replace("중학교", "중")
                    .replace("대학교", "대")
                    .replace("대학", "대")
                    .replace("사범대", ""));
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

    private String findAtSchoolList(String line, List<String> schoolList) {

        for (String school : schoolList) {
            if (line.contains(school)) {
                int index = schoolList.indexOf(school);
                return originSchoolList.get(index); // 학교의 정식명칭으로 통일화해서 Count
            }
        }

        return "";
    }

}
