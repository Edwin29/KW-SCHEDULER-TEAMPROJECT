'''
PDF 파싱 결과(과목명) + Klas에서 크롤링한 강의실 정보
'''

from parser.pdf_parser import parse_pdf
from parser.klas_scraper import scrape_all
from model.subjects import Subject, ClassRoom
from pathlib import Path
import json
from dataclasses import asdict

def merge_data(subjects: list[Subject], classrooms: dict[str, list[ClassRoom]]) -> list[Subject]:
    # subjects 리스트의 각 Subject 객체에 classrooms 딕셔너리에서 강의실 정보 추가
    for subject in subjects:
        subject.강의실 = classrooms.get(subject.학정번호, [])
    return subjects

def save(subjects: list[Subject], year: int, semester: int):
    output_path = Path(__file__).parent.parent / "output" / f"courses_{year}_{semester}.json"
    data = [asdict(subject) for subject in subjects]
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)