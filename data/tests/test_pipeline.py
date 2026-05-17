'''
전체 파이프라인 테스트 (과목 5개만)
'''

from pathlib import Path
from dotenv import load_dotenv
from selenium import webdriver
import os, sys

# src 폴더를 import 경로에 추가
sys.path.append(str(Path(__file__).parent.parent / "src"))

from parser.pdf_parser import parse_pdf
from parser.klas_scraper import login, scrape_all
from merger import merge_data, save

load_dotenv(Path(__file__).parent.parent / ".env")

def main():
    year     = 2026
    semester = 1
    pdf_path = Path(__file__).parent.parent / "input" / f"수강신청자료집_{year}_{semester}.pdf"

    # 1. PDF 파싱
    print("PDF 파싱 중...")
    subjects = parse_pdf(str(pdf_path), year, semester)
    print(f"총 {len(subjects)}개 과목 파싱 완료")

    # 2. 5개만 테스트
    test_subjects = subjects[:5]

    # 3. 로그인 및 강의실 수집
    print("KLAS 로그인 중...")
    driver = webdriver.Chrome()
    login(driver, os.getenv("KLAS_ID"), os.getenv("KLAS_PW"))
    classrooms = scrape_all(driver, test_subjects, year, semester)
    driver.quit()

    # 4. 병합
    merged = merge_data(test_subjects, classrooms)

    # 5. 저장
    save(merged, year, semester)
    print(f"output/subjects_{year}_{semester}.json 저장 완료")

if __name__ == "__main__":
    main()