'''
학정번호에서 강의계획서 url 추출하는 방법

I040  -  2  -  7777  -  01
학과코드  학년  과목번호  분반

→ U + 2026 + 1 + 7777 + I040 + 01 + 2
     연도   학기  과목번호 학과코드 분반 학년
'''
import json
import re
import time
from pathlib import Path
from model.subjects import Subject, TimeSlot, ClassRoom
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from bs4 import BeautifulSoup

KLAS_LOGIN_URL = "https://klas.kw.ac.kr/usr/cmn/login/LoginForm.do"

def login(driver, klas_id: str, klas_pw: str) -> None:
    driver.get(KLAS_LOGIN_URL)                           # 1. 로그인 페이지 이동
    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.ID, "loginId")) # 2. 아이디 입력
    )
    driver.find_element(By.ID, "loginId").send_keys(klas_id) # 3. 비밀번호 입력
    driver.find_element(By.ID, "loginPwd").send_keys(klas_pw) # 4. 로그인 버튼 클릭
    driver.find_element(By.CSS_SELECTOR, "button.btn").click() # 5. 로그인 완료 대기
    KLAS_MAIN_URL = "https://klas.kw.ac.kr/std/cmn/frame/Frame.do"
    WebDriverWait(driver, 10).until(
        EC.url_to_be(KLAS_MAIN_URL)
    )

def _load_buildings() -> dict:
    path = Path(__file__).parent.parent.parent / "config" / "buildings.json"
    with open(path, encoding="utf-8") as f:
        return json.load(f)
    
def _build_url(학정번호: str, year: int, semester: int) -> str:
    # 학정번호 형식: 학과코드-학년-과목번호-분반
    # 예) I040-2-7777-01 → U202617777I040012
    code = 학정번호.split('-')
    return f"https://klas.kw.ac.kr/std/cps/atnlc/popup/LectrePlanStdView.do?selectSubj=U{year}{semester}{code[2]}{code[0]}{code[3]}{code[1]}"

def _parse_classroom(html: str) -> list[ClassRoom]:
    buildings = _load_buildings()
    soup = BeautifulSoup(html, "html.parser")
    classrooms = []
    DAYS = {"월", "화", "수", "목", "금", "토"}

    for tr in soup.find_all("tr"):
        ths = tr.find_all("th")
        has_sugang = any("수강인원" in th.text for th in ths)
        if not has_sugang:
            continue

        td = tr.find("td", {"colspan": "3"})
        if not td or not td.text.strip():
            continue
        text = td.text.strip()

        # 요일+교시+강의실 패턴 직접 추출
        matches = re.findall(r'([월화수목금토])?\s*[\d,\s]+교시\s*\(([^)]+)\)', text)
        
        for day, room_text in matches:
            match_bld = re.match(r'([^\d]+)(\d+)', room_text)
            if not match_bld:
                continue
            building_abbr = match_bld.group(1)
            room_number   = match_bld.group(2)
            building_name = buildings.get(building_abbr, building_abbr)
            classrooms.append(ClassRoom(요일=day, 건물=building_name, 호수=room_number))

    return classrooms

def scrape_classroom(driver, 학정번호: str, year: int, semester: int) -> list[ClassRoom]:
    url = _build_url(학정번호, year, semester)
    driver.get(url)
    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.CSS_SELECTOR, "td[colspan='3']"))
    )
    html = driver.page_source
    return _parse_classroom(html)

def scrape_all(driver, subjects: list[Subject], year: int, semester: int, delay: float = 0.5) -> dict[str, list[ClassRoom]]:
    # 학정번호를 키로 하는 딕셔너리 반환
    # 예) {"I040-2-7777-01": [ClassRoom(...), ...], ...}
    result = {}
    total = len(subjects)
    
    for idx, subject in enumerate(subjects, start=1):
        classrooms = scrape_classroom(driver, subject.학정번호, year, semester)
        result[subject.학정번호] = classrooms
        print(f"[{idx}/{total}] {subject.과목명} → 강의실 {len(classrooms)}개")
        time.sleep(delay)
    
    return result
