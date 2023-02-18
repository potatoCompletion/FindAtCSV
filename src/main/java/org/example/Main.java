package org.example;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Main {
    public static int sum = 0;
    static int[] table;

    public static void main(String[] args) throws IOException, CsvException {
        CSVReader commentsCsvReader = new CSVReader(new FileReader("comments.csv"));
        CSVReader schoolsCsvReader = new CSVReader(new FileReader("fixedOne.csv"));
//        CSVReader csvReader = new CSVReader(new FileReader("전국초중등학교기본정보표준데이터.csv"));
        List<String[]> commentsLines = commentsCsvReader.readAll();
        List<String[]> schoolsLines = schoolsCsvReader.readAll();

//        for (String[] school : schoolsLines) {
//            if (String.join("", school).length() == 2) {
//                System.out.println(String.join("", school));
//            }
//        }

        int count = 0;

        StringBuilder comments = new StringBuilder();
        for (String[] comment : commentsLines) {
//            comments.append(String.join("", comment)
//                    .replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣]", "")
//                    .replace("등학교", "")
//                    .replace("중학교", "중")
//                    .replace("대학교", "대"));
//            comments.append("\n");
            String nowString = String.join("", comment)
                    .replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣]", "") // 한글제외 모든 불필요 문자 제거
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

            for (String[] school : schoolsLines) {
                count++;

                if (nowString.contains(String.join("", school))) {
                    System.out.println(String.join("", school));
                    sum++;
                    break;
                }
//                makeTable(String.join("", school));
//                search(nowString, String.join("", school));
            }

        }
        System.out.println("총 연산 횟수: " + count + "\n찾은 학교 개수: " + sum);

    }

    static int[] makeTable(String pattern) {
        int n = pattern.length();
        int[] table = new int[n];

        int idx=0;
        for(int i=1; i<n; i++) {
            // 일치하는 문자가 발생했을 때(idx>0), 연속적으로 더 일치하지 않으면 idx = table[idx-1]로 돌려준다.
            while(idx>0 && pattern.charAt(i) != pattern.charAt(idx)) {
                idx = table[idx-1];
            }

            if(pattern.charAt(i) == pattern.charAt(idx)) {
                idx += 1;
                table[i] = idx;
            }
        }
        return table;
    }

    public static int search(String str, String pattern) {
        int sLen = str.length();
        int pLen = pattern.length();

        int index = 0;
        for(int i = 0; i < sLen; i++) {
            while(index > 0 && str.charAt(i) != pattern.charAt(index)) {
                index = table[index - 1];
            }

            if(str.charAt(i) == pattern.charAt(index)) {

                if(index == pLen - 1) {
                    index = table[index];
                    sum++;
                    return 1;
                }
                else {
                    index++;
                }
            }
        }
        return 0;
    }
}