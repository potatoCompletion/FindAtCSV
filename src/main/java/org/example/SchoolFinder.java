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
    private final List<String[]> sourceFileLines;
    private final List<School> schools;
    private final List<School> filteredSchools;
    private final List<String> addressList;

    public SchoolFinder(String sourceFilePath, String schoolDataFilePath, String locationDataFilePath) throws IOException, CsvException {
        // 소스파일 세팅 (comments.csv)
        CSVReader sourceFileCsvReader = new CSVReader(new FileReader(sourceFilePath));
        sourceFileLines = sourceFileCsvReader.readAll();

        // 학교데이터 세팅 (전국초중고대데이터.csv)
        schools = new CsvToBeanBuilder<School>(new FileReader(schoolDataFilePath))
                .withType(School.class)
                .build()
                .parse();
        Comparator<School> desc = (s1, s2) -> Integer.compare(s2.getName().length(), s1.getName().length());
        schools.sort(desc);
        filteredSchools = getFilteredSchools(schools);  // 약어를 사용한 댓글들을 검출하기 위한 기준 학교 데이터

        // 시, 군 데이터 세팅 (전국행정구역데이터(시,군).csv)
        CSVReader addressDataCsvReader = new CSVReader(new FileReader(locationDataFilePath));
        List<String[]> addressData = addressDataCsvReader.readAll();
        addressList = stringArrayListToList(addressData);
    }

    public Map<String, Integer> findValidSchool() {

        log.info("소스파일 데이터 수: {}개", sourceFileLines.size());
        log.info("==========필터링 시작==========");

        int firstFilteredCount = 0;
        int secondFilteredCount = 0;
        int cannotFoundCount = 0;
        Map<String, Integer> schoolCountMap = new HashMap<>();

        for (String[] line : sourceFileLines) {
            String filteredLine;
            String foundSchool;

            // 1차 필터링 (한글 제외한 모든 문자제거)
            filteredLine = String.join("", line)
                    .replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣]", "");

            if (!filteredLine.contains("대학교")) {    // 사대부고, 사대부중 필터링을 위한 검증
                foundSchool = filteringSchool(filteredLine, schools);
                if (!foundSchool.isBlank()) {
                    foundSchool = validSchoolFilter(foundSchool); // 현재 이상이 있는 학교인지 검증 (학교명변경 등)
                }
                if (!foundSchool.isBlank()) {
                    schoolCountMap.merge(foundSchool, 1, Integer::sum);

                    firstFilteredCount++;
                    continue;
                }
            }


            // 2차 필터링 (OO초, OO중, OO고, OO대와 같은 약어)
            filteredLine = String.join("", line)
                    .replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣]", "")
                    .replace("등학교", "")
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

            foundSchool = filteringSchool(filteredLine, filteredSchools);
            if (!foundSchool.isBlank()) {
                foundSchool = validSchoolFilter(foundSchool); // 현재 이상이 있는 학교인지 검증 (학교명변경 등)
            }
            if (!foundSchool.isBlank()) {
                schoolCountMap.merge(foundSchool, 1, Integer::sum);

                log.info("약어에서 발견: {}\n\n{}", foundSchool, line);
                secondFilteredCount++;
                continue;
            }

            log.warn("(WARNING)알수없는 라인\n\n{}\n", String.join("", line));
            cannotFoundCount++;
        }

        log.info("==========모든 필터링 종료==========");
        log.info("1차 필터(한글제외 모든 문자제거) 발견 학교 수: {}개", firstFilteredCount);
        log.info("2차 필터(약어) 발견 학교 수: {}개", secondFilteredCount);
        log.info("발견된 학교 수: {}개, 알수없는 라인: {}개",
                firstFilteredCount + secondFilteredCount,
                cannotFoundCount);

        return schoolCountMap;
    }

    private List<School> getFilteredSchools(List<School> originSchools) {
        // 전국 학교 데이터를 약어로 변경 (ex: 삼척고등학교 -> 삼척고)
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

    private List<String> stringArrayListToList(List<String[]> stringArrayList) {

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

    private String filteringSchool(String line, List<School> schoolList) {
        Map<Integer, String> foundMap = new HashMap<>();    // 중복검출 시 더 먼저 적은 학교를 찾기위한 Map 구조
        List<String> findedAddressList = new ArrayList<>();
        List<School> validList;
        Comparator<School> desc = (s1, s2) -> Integer.compare(s2.getName().length(), s1.getName().length());

        // 댓글에 소재지에 대한 정보 있는지 검출
        for (String address : addressList) {
            if (line.contains(address)) {
                findedAddressList.add(address);
            }
        }

        // 소재지가 파악되었다면 해당 소재지에 존재하는 학교로 우선 탐색
        for (String address : findedAddressList) {
            validList = schoolList.stream().filter(s -> s.getAddress().contains(address)).sorted(desc).toList();
            for (School school : validList) {
                if (isDefaultName(address, school.getName())) { // 지명 + 기본이름으로 쓰는 학교 건너뛰기 (ex: 삼척중학교, 강릉고등학교)
                    continue;
                }
                if (line.contains(school.getName().replace(address, ""))) {
                    int index = schoolList.indexOf(school);
                    foundMap.put(line.indexOf(school.getName().replace(address, "")), schools.get(index).getName());
                    line = line.replace(school.getName().replace(address, ""), "");
                }
            }
            if (!foundMap.isEmpty()) {
                break;
            }
        }

        // 검출 안되었을 경우 전체 탐색
        if (foundMap.isEmpty()) {
            for (School school : schoolList) {
                if (line.contains(school.getName())) {
                    int index = schoolList.indexOf(school);
                    foundMap.put(line.indexOf(school.getName()), schools.get(index).getName());
                    line = line.replace(school.getName(), "");
                }
            }
        }

        // 중복검출(2개 이상) 시 유효 데이터 판단
        if (foundMap.size() > 1) {
            log.info("!!!!!!!!!!!!!중복검출 데이터!!!!!!!!!!!!!\n");

            int i = 0;
            for (Map.Entry<Integer, String> entry : foundMap.entrySet()) {
                log.info("{}. {}\n", ++i, entry.getValue());
            }

            // 판단기준 (댓글에서 처음으로 입력된 학교)
            int firstFound = foundMap.keySet().stream().mapToInt(v -> v).min().orElseThrow();
            log.info("pick: {}", foundMap.get(firstFound));

            return foundMap.get(firstFound);
        }
        return foundMap.values().stream().findFirst().orElse("");
    }

    private boolean isDefaultName(String address, String name) {
        // 지명과 기본학교단위로 이루어진 학교명 검증 (ex: 삼척고등학교, 강릉고등학교 return true)

        name = name.replace(address, "");
        switch (name) {
            case "초":
            case "중":
            case "고":
            case "대":
            case "초등학교":
            case "중학교":
            case "고등학교":
            case "대학교":
            case "여자중":
            case "체육고":
            case "여자고":
            case "예술중":
            case "예술고":
            case "여자대":
            case "여자중학교":
            case "체육고등학교":
            case "여자고등학교":
            case "예술중학교":
            case "예술고등학교":
            case "여자대학교":
                return true;
            default:
                return false;
        }
    }

    private String validSchoolFilter(String schoolName) {
        // 검출된 학교가 현재 이상이 있는 학교인지 검증
        School school = schools.stream().filter(s -> s.getName().equals(schoolName)).findFirst().orElseThrow();

        if (school.getAddress().contains("학교명변경")) {
            return "";
        }

        return schoolName;
    }
}