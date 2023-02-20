package org.example;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

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

        originSchoolList = stringArrayListToListOrderBy(originSchoolLines);    // 전국 학교 정식명칭 리스트
        filteredSchoolList = getFilteredSchoolList(originSchoolList);   // 전국 학교 약어 리스트 (ex: 강릉초등학교 -> 강릉초)
    }

    public Map<String, Integer> findValidSchool() throws IOException {

        log.info("소스파일 데이터 수: {}개", sourceFileLines.size());
        log.info("==========필터링 시작==========");

        Map<String, Integer> schoolCountMap = new HashMap<>();

        for (String[] line : sourceFileLines) {
            // 1차 필터링 (정식명칭)
            String filteredLine = String.join("", line)
                    .replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣]", ""); // 한글제외 모든 불필요 문자 제거

            List<String> findedSchoolList = findSchoolAtList(filteredLine, originSchoolList);
            if (!findedSchoolList.isEmpty()) {
                for (String school : findedSchoolList) {
                    schoolCountMap.merge(school, 1, Integer::sum);
                }
                continue;
            }

            log.warn("(WARNING)알수없는 라인\n\n\"{}\"\n", filteredLine);
        }


            // 2차 필터링 (약어)
//            filteredLine = filteredLine
//                    .replace("등학교", "")  // OO초, OO중, OO고, OO대 라고 입력한 데이터 또한 유효한 문자열로 인식하기 위한 작업
//                    .replace("중학교", "중")
//                    .replace("대학교", "대")
//                    .replace("대학", "대")
//                    .replace("사범대", "")
//                    .replace("여중", "여자중")
//                    .replace("체고", "체육고")
//                    .replace("여고", "여자고")
//                    .replace("예중", "예술중")
//                    .replace("예고", "예술고")
//                    .replace("여대", "여자대");
//
//            school = findSchoolAtList(filteredLine, filteredSchoolList);
//            if (!school.isBlank()) {
//                schoolCountMap.merge(school, 1, Integer::sum);
////                log.info("{} 발견 - 2차 필터(약어)", school);
//                secondFilteredCount++;
//                continue;


            // 정식명칭, 약어 두 리스트에서 모두 찾을 수 없을 경우
//            schoolCountMap.merge("알수없음", 1, Integer::sum);
//            log.warn("(WARNING)알수없는 라인\n\n\"{}\"\n", filteredLine);
//        }
//
//        log.info("==========모든 필터링 종료==========");
//        log.info("1차 필터(정식명칭) 발견 학교 수: {}개", firstFilteredCount);
//        log.info("2차 필터(약어) 발견 학교 수: {}개", secondFilteredCount);
//        log.info("발견된 학교 수: {}개, 알수없는 라인: {}개",
//                firstFilteredCount + secondFilteredCount,
//                schoolCountMap.get("알수없음"));

        return schoolCountMap;
    }

    private List<String> getFilteredSchoolList(List<String> originSchoolList) {
        // 전국 학교 데이터를 약어로 변경 (ex: 삼척고등학교 -> 삼척고)
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

    private List<String> stringArrayListToListOrderBy(List<String[]> stringArrayList) {

        List<String> list = new ArrayList<>();
        for (String[] stringArray : stringArrayList) {
            String str = String.join("", stringArray);
            list.add(str);
        }

        // 길이 순으로 내림차순 정렬 (중복되는 학교 이름이 많기 때문에(ex: 삼척중앙초등학교, 중앙초등학교) 긴 학교명 먼저 대입)
        Comparator<String> desc = (s1, s2) -> Integer.compare(s2.length(), s1.length());
        list.sort(desc);

        return list;
    }

    private List<String> findSchoolAtList(String line, List<String> schoolList) {
        List<String> findedList = new ArrayList<>();

        for (String school : schoolList) {
            if (line.contains(school)) {
                findedList.add(school);
                line = line.replace(school, "");
            }
        }

        return findedList;
    }
}