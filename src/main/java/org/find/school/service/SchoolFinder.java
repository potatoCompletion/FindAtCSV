package org.find.school.service;

import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvException;
import org.find.school.dto.School;
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
    private List<Map<Integer, String>> holdSchoolResults = new ArrayList<>();
    private int foundIndexSum = 0;
    private static final String DUPLICATED = "중복검출";

    public SchoolFinder(String sourceFilePath, String schoolDataFilePath, String locationDataFilePath) throws IOException, CsvException {
        // 소스파일 세팅 (comments.csv)
        CSVReader sourceFileCsvReader = new CSVReader(new FileReader(sourceFilePath));
        sourceFileCsvReader.skip(1);
        sourceFileLines = sourceFileCsvReader.readAll();

        // 학교데이터 세팅 (전국초중고대데이터.csv)
        schools = new CsvToBeanBuilder<School>(new FileReader(schoolDataFilePath))
                .withType(School.class)
                .build()
                .parse();
        Comparator<School> desc = (s1, s2) -> Integer.compare(s2.getName().length(), s1.getName().length());
        schools.sort(desc);
        filteredSchools = getFilteredSchools(schools);  // 약어를 사용한 댓글들을 검출하기 위한 학교데이터 약어버전

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
        int autoPickCount = 0;
        Map<String, Integer> schoolCountMap = new HashMap<>();

        for (String[] line : sourceFileLines) {
            String filteredLine;
            String foundSchool;

            // 1차 필터링 (한글 제외한 모든 문자제거)
            filteredLine = String.join("", line)
                    .replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣]", "");

            if (!filteredLine.contains("대학교")) {    // 사대부고, 사대부중 필터링을 위한 검증
                foundSchool = filteringSchool(filteredLine, schools);
                if (foundSchool.equals(DUPLICATED)) {
                    log.info("중복검출 라인:\n\n{}\n", String.join("", line));
                    continue;
                }
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
            if (foundSchool.equals(DUPLICATED)) {
                log.info("중복검출 라인:\n\n{}\n", String.join("", line));
                continue;
            }
            if (!foundSchool.isBlank()) {
                foundSchool = validSchoolFilter(foundSchool); // 현재 이상이 있는 학교인지 검증 (학교명변경 등)
            }
            if (!foundSchool.isBlank()) {
                schoolCountMap.merge(foundSchool, 1, Integer::sum);

                log.info("약어에서 발견: {}\n\n{}\n", foundSchool, line);
                secondFilteredCount++;
                continue;
            }

            log.warn("(WARNING)알수없는 라인\n\n{}\n", String.join("", line));
        }

        int averageFoundIndex = getAverageFoundIndex(firstFilteredCount + secondFilteredCount);
        log.info("중복 검출 댓글 수: {}개", holdSchoolResults.size());
        log.info("사용자들의 학교명 평균 위치 인덱스: {}", averageFoundIndex);
        for (Map<Integer, String> holdResults : holdSchoolResults) {
            int nearIndex = getNearIndex(averageFoundIndex, holdResults.keySet());
            schoolCountMap.merge(holdResults.get(nearIndex), 1, Integer::sum);
            log.info("사용자 평균 데이터에 의거 선택 값: {}", holdResults.get(nearIndex));
            autoPickCount++;
        }

        log.info("==========모든 필터링 종료==========");
        log.info("1차 필터(한글제외 모든 문자제거) 발견 학교 수: {}개", firstFilteredCount);
        log.info("2차 필터(약어) 발견 학교 수: {}개", secondFilteredCount);
        log.info("사용자 평균 데이터에 의거해 결정된 학교 수: {}개", autoPickCount);
        log.info("발견된 학교 수: {}개",
                firstFilteredCount + secondFilteredCount + autoPickCount);
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
        Map<Integer, String> foundSchoolMap = new HashMap<>();   // 검출 학교 저장 리스트
        List<String> foundAddressList = new ArrayList<>(); // 댓글에 언급된 지역명 저장할 리스트
        List<School> schoolsInAddress;  // 특정 지역에 소재하고 있는 학교 리스트
        Comparator<School> desc = (s1, s2) -> Integer.compare(s2.getName().length(), s1.getName().length());

        // 댓글에 소재지에 대한 정보 있는지 검출
        for (String address : addressList) {
            if (line.contains(address)) {
                foundAddressList.add(address);
            }
        }

        // 소재지가 파악되었다면 해당 소재지에 존재하는 학교로 우선 탐색
        for (String address : foundAddressList) {
            schoolsInAddress = schoolList.stream().filter(s -> s.getAddress().contains(address)).sorted(desc).toList();
            for (School school : schoolsInAddress) {
                if (isDefaultName(address, school.getName())) { // 지명 + 기본이름으로 쓰는 학교 건너뛰기 (ex: 삼척중학교, 강릉고등학교)
                    continue;
                }
                if (line.contains(school.getName().replace(address, ""))) { // 지명을 쓰지 않은 학교명까지 커버하기 위해 지명 삭제 후 검출
                    int index = schoolList.indexOf(school);
                    foundSchoolMap.put(line.indexOf(school.getName().replace(address, "")),
                            schools.get(index).getName());
                    foundIndexSum += line.indexOf(school.getName());
                    line = line.replace(school.getName().replace(address, ""), "");
                }
            }
            if (!foundSchoolMap.isEmpty()) {  // 검출 되었다면 더이상 진행하지 않고 중단
                break;
            }
        }

        // 검출 안되었을 경우 전체 탐색
        if (foundSchoolMap.isEmpty()) {
            for (School school : schoolList) {
                if (line.contains(school.getName())) {
                    int index = schoolList.indexOf(school);
                    foundSchoolMap.put(line.indexOf(school.getName()), schools.get(index).getName());
                    foundIndexSum += line.indexOf(school.getName());
                    line = line.replace(school.getName(), "");
                }
            }
        }

        // 중복검출(2개 이상) 시 저장 후 판단 보류
        int normalFound = 1;
        if (foundSchoolMap.size() > normalFound) {
            log.info("!!!!!!!!!!!!!중복검출 데이터 감지 판단 보류!!!!!!!!!!!!!\n");
            holdSchoolResults.add(foundSchoolMap);  // 보류된 판단은 모든 검출이 끝나고 평균값에 의거해 처리한다

            return DUPLICATED;
        }

        return foundSchoolMap.values().stream().findFirst().orElse("");
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

    private int getAverageFoundIndex(int totalFoundCount) {
        return foundIndexSum / totalFoundCount;
    }

    private int getNearIndex(int averageIndex, Set<Integer> indexSet) {
        int nearIndex = indexSet.stream().findFirst().orElseThrow();
        for (int i : indexSet) {
            int abs = Math.abs(averageIndex - i);
            if (abs < nearIndex) {
                nearIndex = i;
            }
        }

        return nearIndex;
    }
}