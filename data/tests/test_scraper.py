from pathlib import Path
import json
import os
import re

import pytest
from bs4 import BeautifulSoup
from dotenv import load_dotenv
from selenium import webdriver
from selenium.common.exceptions import WebDriverException
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

load_dotenv(Path(__file__).parent.parent / ".env")

KLAS_LOGIN_URL = "https://klas.kw.ac.kr/usr/cmn/login/LoginForm.do"
KLAS_MAIN_URL = "https://klas.kw.ac.kr/std/cmn/frame/Frame.do"
TEST_URL = "https://klas.kw.ac.kr/std/cps/atnlc/popup/LectrePlanStdView.do?selectSubj=U202617777I040012"


def _load_buildings() -> dict:
    path = Path(__file__).parent.parent / "config" / "buildings.json"
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def _parse_classroom(html: str) -> list:
    buildings = _load_buildings()
    soup = BeautifulSoup(html, "html.parser")
    classrooms = []

    for tr in soup.find_all("tr"):
        ths = tr.find_all("th")
        has_sugang = any("수강인원" in th.text for th in ths)
        if not has_sugang:
            continue

        td = tr.find("td", {"colspan": "3"})
        if not td or not td.text.strip():
            continue
        text = td.text.strip()

        for item in text.split(","):
            item = item.strip()
            match_room = re.search(r"\((.+?)\)", item)
            if not match_room:
                continue
            room_text = match_room.group(1)
            요일 = item[0]
            match_bld = re.match(r"([^\d]+)(\d+)", room_text)
            if not match_bld:
                continue
            건물약칭 = match_bld.group(1)
            호수 = match_bld.group(2)
            건물 = buildings.get(건물약칭, 건물약칭)
            classrooms.append({"요일": 요일, "건물": 건물, "호수": 호수})

    return classrooms


@pytest.fixture
def klas_credentials():
    klas_id = os.getenv("KLAS_ID")
    klas_pw = os.getenv("KLAS_PW")
    if not klas_id or not klas_pw:
        pytest.skip("KLAS credentials are not configured")
    return klas_id, klas_pw


@pytest.fixture
def chrome_driver():
    try:
        driver = webdriver.Chrome()
    except WebDriverException as exc:
        pytest.skip(f"Chrome WebDriver unavailable: {exc}")

    yield driver
    driver.quit()


def test_parse_classroom_from_live_page(chrome_driver, klas_credentials):
    klas_id, klas_pw = klas_credentials

    chrome_driver.get(KLAS_LOGIN_URL)
    WebDriverWait(chrome_driver, 10).until(
        EC.presence_of_element_located((By.ID, "loginId"))
    )

    chrome_driver.find_element(By.ID, "loginId").send_keys(klas_id)
    chrome_driver.find_element(By.ID, "loginPwd").send_keys(klas_pw)
    chrome_driver.find_element(By.CSS_SELECTOR, "button.btn").click()

    WebDriverWait(chrome_driver, 10).until(EC.url_to_be(KLAS_MAIN_URL))

    chrome_driver.get(TEST_URL)
    WebDriverWait(chrome_driver, 10).until(
        EC.presence_of_element_located((By.CSS_SELECTOR, "td[colspan='3']"))
    )

    result = _parse_classroom(chrome_driver.page_source)
    assert isinstance(result, list)
