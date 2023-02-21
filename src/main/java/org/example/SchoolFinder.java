package org.example;

import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBeanBuilder;
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
    private final List<School> schools;
    private final List<School> filteredSchools;
    List<String> addressList;
    private final List<String> originSchoolList;
    private final List<String> filteredSchoolList;
    private int howMany = 0;

    public SchoolFinder(String sourceFilePath, String schoolDataFilePath) throws IOException, CsvException {

        CSVReader schoolsCsvReader = new CSVReader(new FileReader(schoolDataFilePath));
        CSVReader sourceFileCsvReader = new CSVReader(new FileReader(sourceFilePath));

        originSchoolLines = schoolsCsvReader.readAll();
        sourceFileLines = sourceFileCsvReader.readAll();

        originSchoolList = stringArrayListToListOrderBy(originSchoolLines);    // 전국 학교 정식명칭 리스트
        filteredSchoolList = getFilteredSchoolList(originSchoolList);   // 전국 학교 약어 리스트 (ex: 강릉초등학교 -> 강릉초)

        schools = new CsvToBeanBuilder<School>(new FileReader(schoolDataFilePath))
                .withType(School.class)
                .build()
                .parse();
        Comparator<School> desc = (s1, s2) -> Integer.compare(s2.getName().length(), s1.getName().length());
        schools.sort(desc);
        filteredSchools = getFilteredSchools(schools);

        CSVReader addressDataCsvReader = new CSVReader(new FileReader("전국행정구역데이터(도,시,군).csv"));
        List<String[]> addressData = addressDataCsvReader.readAll();
        addressList = stringArrayListToListOrderBy(addressData);
    }

    public Map<String, Integer> findValidSchool() {

        log.info("소스파일 데이터 수: {}개", sourceFileLines.size());
        log.info("==========필터링 시작==========");

        int firstFilteredCount = 0;
        int secondFilteredCount = 0;
        int thirdFilteredCount = 0;
        int cannotFoundCount = 0;
        Map<String, Integer> schoolCountMap = new HashMap<>();

        for (String[] line : sourceFileLines) {
            String filteredLine;
            List<String> findedSchoolList;

            // 1차 필터링 (공백제거)
//            filteredLine = String.join("", line)
//                    .replace(" ", "");
//            if (filteredLine.contains("동국대학교")) {
//                var a = 1;
//            }
//            findedSchoolList = filteringSchool(filteredLine, schools);
//            if (!findedSchoolList.isEmpty()) {
//                for (String school : findedSchoolList) {
//                    schoolCountMap.merge(school, 1, Integer::sum);
//                }
//
//                firstFilteredCount += findedSchoolList.size();
//                continue;
//            }

            // 2차 필터링 (한글 제외한 모든 문자제거)
            filteredLine = String.join("", line)
                    .replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣]", "");
            findedSchoolList = filteringSchool(filteredLine, schools);
            if (!findedSchoolList.isEmpty()) {
                for (String school : findedSchoolList) {
                    schoolCountMap.merge(school, 1, Integer::sum);
                }

                secondFilteredCount += findedSchoolList.size();
                continue;
            }

            // 3차 필터링 (OO초, OO중, OO고, OO대와 같은 약어 표현)
            filteredLine = String.join("", line)
                    .replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣]", "")
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
                    .replace("사대부중", "부속중")
                    .replace("사대부고", "부속고")
                    .replace("여대", "여자대");

            findedSchoolList = filteringSchool(filteredLine, filteredSchools);
            if (!findedSchoolList.isEmpty()) {
                for (String school : findedSchoolList) {
                    schoolCountMap.merge(school, 1, Integer::sum);
                }

                thirdFilteredCount += findedSchoolList.size();
                continue;
            }

            log.warn("(WARNING)알수없는 라인\n\n{}\n", String.join("", line));
            cannotFoundCount++;
        }

        log.info("==========모든 필터링 종료==========");
        log.info("1차 필터(공백제거) 발견 학교 수: {}개", firstFilteredCount);
        log.info("2차 필터(한글제외 모든 문자제거) 발견 학교 수: {}개", secondFilteredCount);
        log.info("3차 필터(약어) 발견 학교 수: {}개", thirdFilteredCount);
        log.info("발견된 학교 수: {}개, 알수없는 라인: {}개",
                firstFilteredCount + secondFilteredCount + thirdFilteredCount,
                cannotFoundCount);
        log.info("총 연산 수: {}번", howMany);

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

    private List<School> getFilteredSchools(List<School> originSchools) {
        List<School> filteredSchools = new ArrayList<>();
        String filteredName;

        for (School school : originSchools) {
            filteredName = school.getName()
                    .replace(" ", "")
                    .replace("등학교", "")
                    .replace("중학교", "중")
                    .replace("대학교", "대")
                    .replace("대학", "대")
                    .replace("사범대", "");
            School filteredSchool = new School(filteredName, school.getAddress());
            filteredSchools.add(filteredSchool);
        }

        return filteredSchools;
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

    private List<String> filteringSchool(String line, List<School> schoolList) {
        List<String> findedList = new ArrayList<>();
        List<String> findedAddressList = new ArrayList<>();
        List<School> validList;
        Comparator<School> desc = (s1, s2) -> Integer.compare(s2.getName().length(), s1.getName().length());

        if (line.contains("경기국제통상고등학교")) {
            var a = 1;
        }

        for (String address : addressList) {
            if (line.contains(address)) {
                findedAddressList.add(address);
            }
        }

        for (String address : findedAddressList) {
            validList = schoolList.stream().filter(s -> s.getAddress().contains(address)).sorted(desc).toList();
            for (School school : validList) {
                if (line.contains(school.getName())) {
                    int index = schoolList.indexOf(school);
                    findedList.add(schools.get(index).getName());
                    line = line.replace(school.getName(), "");
                }
//                schools.remove(school); // 한번 확인한 데이터는 삭제
                howMany++;
            }
            if(!findedList.isEmpty()) {
                break;
            }
        }

        if (findedList.isEmpty()) {
            for (School school : schoolList) {
                if (line.contains(school.getName())) {
                    int index = schoolList.indexOf(school);
                    findedList.add(schools.get(index).getName());
                    line = line.replace(school.getName(), "");
                }
                howMany++;
            }
        }


        if (findedList.size() > 1) {
            if (!findedList.get(0).contains(findedList.get(1))) {
                log.info("판단불가 데이터 찾음!!!!!!!!!!!!!\n");

                for (String finded : findedList) {
                    log.info("{}. {}\n", findedList.indexOf(finded), finded);
                }
            }


            // 판단기준
        }

        return findedList;
    }
}