'''
PDF 파싱       → parse_pdf()
로그인         → login()
강의실 수집    → scrape_all()
병합           → merge_data()
저장           → save()
'''

from parser.pdf_parser import parse_pdf
from parser.klas_scraper import login, scrape_all
from merger import merge_data, save
from selenium import webdriver
from dotenv import load_dotenv
from pathlib import Path
import os

load_dotenv(Path(__file__).parent.parent / ".env")

def main():
    # 1. 연도/학기 입력받기
    year     = int(input("YEAR : "))
    semester = int(input("SEMESTER : "))

    # 2. PDF 경로 자동 설정
    pdf_path = Path(__file__).parent.parent / "input" / f"수강신청자료집_{year}_{semester}.pdf"

    # 3. PDF 파싱
    print("PDF 파싱 중...")
    subjects = parse_pdf(pdf_path, year, semester)
    print(f"총 {len(subjects)}개 과목 파싱 완료")

    # 4. Selenium으로 KLAS 로그인 및 강의실 정보 수집
    print("KLAS 로그인 및 강의실 정보 수집 중...")
    driver = webdriver.Chrome()
    login(driver, os.getenv("KLAS_ID"), os.getenv("KLAS_PW"))
    classrooms = scrape_all(driver, subjects, year, semester)
    driver.quit()
    print("강의실 정보 수집 완료")

    # 5. 데이터 병합
    print("데이터 병합 중...")
    merged_subjects = merge_data(subjects, classrooms)
    print("데이터 병합 완료")

    # 6. 결과 저장
    print("결과 저장 중...")
    save(merged_subjects, year, semester)
    print("모든 작업 완료")

if __name__ == "__main__":
    main()