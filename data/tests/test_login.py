from pathlib import Path
import os

import pytest
from dotenv import load_dotenv
from selenium import webdriver
from selenium.common.exceptions import WebDriverException
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

load_dotenv(Path(__file__).parent.parent / ".env")

KLAS_LOGIN_URL = "https://klas.kw.ac.kr/usr/cmn/login/LoginForm.do"
KLAS_MAIN_URL = "https://klas.kw.ac.kr/std/cmn/frame/Frame.do"
KLAS_PLAN_URL = "https://klas.kw.ac.kr/std/cps/atnlc/popup/LectrePlanStdView.do"


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


def test_klas_login_and_plan_page_loads(chrome_driver, klas_credentials):
    klas_id, klas_pw = klas_credentials

    chrome_driver.get(KLAS_LOGIN_URL)
    WebDriverWait(chrome_driver, 10).until(
        EC.presence_of_element_located((By.ID, "loginId"))
    )

    chrome_driver.find_element(By.ID, "loginId").send_keys(klas_id)
    chrome_driver.find_element(By.ID, "loginPwd").send_keys(klas_pw)
    chrome_driver.find_element(By.CSS_SELECTOR, "button.btn").click()

    WebDriverWait(chrome_driver, 10).until(EC.url_to_be(KLAS_MAIN_URL))

    chrome_driver.get(KLAS_PLAN_URL + "?selectSubj=U202617777I040012")
    WebDriverWait(chrome_driver, 10).until(
        EC.presence_of_element_located((By.CSS_SELECTOR, "td[colspan='3']"))
    )

    html = chrome_driver.page_source
    assert html
