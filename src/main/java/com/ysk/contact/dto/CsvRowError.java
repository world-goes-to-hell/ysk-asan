package com.ysk.contact.dto;

/** CSV 가져오기 행 단위 오류. row 는 파일 기준 행 번호(헤더=1행). */
public record CsvRowError(long row, String message) {}
