"""
pdf_parser.py
수강신청자료집 PDF 읽어서 subject 객체 리스트로 변환.

사용법:
    from parser.pdf_parser import parse_pdf
    subjects = parse_pdf("input/수강신청자료집_2026_1.pdf", 2026, 1)
"""

import re
import sys
from pathlib import Path

try:
    import pdfplumber
except ImportError:
    print("pdfplumber 설치 필요: pip install pdfplumber")
    sys.exit(1)

# model 폴더 경로 추가
sys.path.append(str(Path(__file__).parent.parent))
from model.subjects import Subject, TimeSlot
import json


# ──────────────────────────────────────────────
# 상수
# ──────────────────────────────────────────────

# 강의시간표 시작/끝 페이지 인덱스 

def _load_config(year: int, semester: int) -> dict:
    config_path = Path(__file__).parent.parent.parent / "config" / "parser_config.json"
    with open(config_path, encoding="utf-8") as f:
        config = json.load(f)
        return config[str(year)][str(semester)]
    


# ──────────────────────────────────────────────
# 텍스트 정제
# ──────────────────────────────────────────────

def _clean(text: str | None) -> str:
    """
    PDF 셀 텍스트 정제.
    1) 줄바꿈 제거
    2) 한글 글자 2배 중복 제거 (PDF 폰트 렌더링 버그)
       예) "참참빛빛관관" → "참빛관"
       학정 번호 왜곡 문제땜에 숫자/영문은 건드리지 않음
    """
    if not text:
        return ""

    text = text.replace('\n', '')

    result = []
    i = 0
    while i < len(text):
        ch = text[i]
        is_hangul = '\uAC00' <= ch <= '\uD7A3' ## 유니코드에서 한글 음절 범위(가 ~ 힣)
        if is_hangul and i + 1 < len(text) and text[i + 1] == ch:
            result.append(ch)
            i += 2
        else:
            result.append(ch)
            i += 1

    return ''.join(result).strip()


# ──────────────────────────────────────────────
# 학정번호 파싱
# ──────────────────────────────────────────────

def _parse_subject_code(code: str) -> tuple[str, int] | None:
    """
    학정번호에서 학과코드와 학년 추출.

    학정번호 형식: 학과코드-학년-과목번호-분반
    예) I040-2-7777-01 → 학과코드: I040, 학년: 2
        5080-2-8555-02 → 학과코드: 5080, 학년: 2

    유효하지 않으면 None 반환.
    """
    match = re.match(r'^([A-Z0-9]+)-(\d)-\d{4}-\d{2}$', code)
    if not match:
        return None
    학과코드 = match.group(1)
    학년 = int(match.group(2))
    return 학과코드, 학년


# ──────────────────────────────────────────────
# 강의시간 파싱
# ──────────────────────────────────────────────

def _parse_timeslots(day1: str, time1: str,
                     day2: str, time2: str) -> list[TimeSlot]:
    """
    요일/교시 셀 4개 → TimeSlot 리스트.

    예) day1="화", time1="1", day2="목", time2="2"
        → [TimeSlot(요일="화", 교시=[1]), TimeSlot(요일="목", 교시=[2])]

        day1="수", time1="8,9,10", day2="", time2=""
        → [TimeSlot(요일="수", 교시=[8, 9, 10])]
    """
    slots = []
    for day, time in [(day1, time1), (day2, time2)]:
        day  = _clean(day)
        time = _clean(time)
        if not day or not time:
            continue
        periods = [int(x) for x in re.findall(r'\d+', time)]
        if periods:
            slots.append(TimeSlot(요일=day, 교시=periods))
    return slots


# ──────────────────────────────────────────────
# 행 파싱
# ──────────────────────────────────────────────

def _is_header(row: list) -> bool:
    """헤더 행 여부 판별"""
    if row[0] is None:
        return True
    if '학정번호' in str(row[0]):
        return True
    return False


def _row_to_subject(row: list) -> Subject | None:
    """
    테이블 행 하나 → Subject 객체.
    열 수에 따라 파싱 방식 분기.
    유효하지 않으면 None 반환.
    """
    # 12열: 일반 강의시간표
    # [학정번호, 과목명, 분반제한, 이수구분, 학점, 시수, 교수, 요일1, 시간1, 요일2, 시간2, 강의유형]
    if len(row) == 12:
        code        = _clean(row[0])
        name        = _clean(row[1])
        restriction = _clean(row[2])
        credit_type = _clean(row[3])
        credits     = _clean(row[4])
        professor   = _clean(row[6])
        lec_type    = _clean(row[11])
        timeslots   = _parse_timeslots(row[7], row[8], row[9], row[10])

    # 10열: 특수 교과목 (분반 열 없음)
    # [학정번호, 과목명, 이수구분, 학점, 시수, 교수, 요일1, 시간1, 요일2, 강의유형]
    elif len(row) == 10:
        code        = _clean(row[0])
        name        = _clean(row[1])
        restriction = ""
        credit_type = _clean(row[2])
        credits     = _clean(row[3])
        professor   = _clean(row[5])
        lec_type    = _clean(row[9])
        timeslots   = _parse_timeslots(row[6], row[7], row[7], row[8])

    else:
        return None

    # 학정번호 유효성 검사 + 학과코드/학년 추출
    parsed = _parse_subject_code(code)
    if not parsed:
        return None
    학과코드, 학년 = parsed

    return Subject(
        학정번호  = code,
        과목명    = name,
        학과코드  = 학과코드,
        학년      = 학년,
        이수구분  = credit_type,
        학점      = int(credits) if credits.isdigit() else None,
        담당교수  = professor or None,
        강의시간  = timeslots,
        강의실    = [],            # 스크래핑 후 merger.py에서 채워짐
        강의유형  = lec_type,
        분반제한  = restriction,
    )


# ──────────────────────────────────────────────
# 메인 파싱 함수
# ──────────────────────────────────────────────

def parse_pdf(pdf_path: str, _year: int, _semester: int) -> list[Subject]:
    """
    PDF 파일 → Subject 리스트.

    Args:
        pdf_path: 수강신청자료집 PDF 경로
        year: 연도
        semester: 학기

    Returns:
        파싱된 Subject 객체 리스트
    """
    subjects = []

    config = _load_config(_year, _semester) # 관리자용
    TIMETABLE_START = config["timetable_start"]
    TIMETABLE_END   = config["timetable_end"]

    with pdfplumber.open(pdf_path) as pdf:
        end = min(TIMETABLE_END, len(pdf.pages))

        for page_idx in range(TIMETABLE_START, end):
            page   = pdf.pages[page_idx]
            tables = page.extract_tables()
            
            if not tables:
                continue

            for table in tables:
                for row in table:
                    if not row or _is_header(row):
                        continue
                    subject = _row_to_subject(row)
                    if subject:
                        subjects.append(subject)

    return subjects