# KW-SCHEDULER

광운대학교 수강신청 시간표 자동 추천 시스템

---

## 프로젝트 개요

KW-SCHEDULER는 광운대학교 수강신청자료집 PDF와 KLAS 강의 정보를 파싱하여 사용자의 제약 조건에 맞는 최적의 시간표 조합을 자동으로 추천하는 프로그램입니다.

OOP 원칙(캡슐화, 상속, 다형성, 추상화)에 따라 설계되었으며 네 개의 독립 모듈로 구성됩니다.

| 모듈 | 언어 | 역할 |
|------|------|------|
| `data/` | Python | PDF 파싱 + KLAS 스크래핑 → 과목 JSON 생성 |
| `algorithm/` | Java | 시간표 추천 알고리즘 |
| `auth/` | Java | 사용자 인증 및 학적 관리 |
| `UI/` | Python (Streamlit) | 웹 기반 시각화 인터페이스 |

---

## 프로젝트 구조

```
KW-SCHEDULER/
├── data/                          # 데이터 수집 모듈
│   ├── src/
│   │   ├── main.py                # 파이프라인 진입점
│   │   ├── merger.py              # PDF + KLAS 데이터 병합
│   │   ├── parser/
│   │   │   ├── pdf_parser.py      # 수강신청자료집 PDF 파싱
│   │   │   └── klas_scraper.py    # KLAS 웹 스크래핑
│   │   └── model/
│   │       └── subjects.py        # 과목 데이터 모델
│   ├── input/                     # 수강신청자료집 PDF 입력 위치
│   ├── output/                    # courses_2026_1.json 출력 위치
│   ├── config/                    # 파싱 설정 (건물명, 파서 옵션)
│   ├── tests/                     # 단위 테스트
│   └── requirements.txt
│
├── algorithm/                     # 시간표 추천 알고리즘 모듈
│   ├── src/
│   │   ├── Main.java              # 진입점 (콘솔/JSON CLI 모드)
│   │   ├── Course.java            # 과목 데이터 모델
│   │   ├── Schedule.java          # 시간표 조합 모델
│   │   ├── ScheduleRecommender.java    # 뼈대 시간표 생성
│   │   ├── CourseSlotFiller.java       # 빈 시간 과목 추천
│   │   ├── UiCompatibilityService.java # UI 연동 오케스트레이터
│   │   ├── HardConstraint.java         # 필수 제약조건
│   │   ├── SoftConstraint.java         # 선택 제약조건 (추상 클래스)
│   │   ├── UserPreference.java         # 사용자 선호 설정
│   │   ├── CourseRepository.java       # 과목 JSON 로드 및 캐싱
│   │   ├── RecommendationRequest.java  # UI → Java 요청 DTO
│   │   ├── UiCliRequest.java           # CLI 요청 래퍼 DTO
│   │   ├── UiCliResponse.java          # Java → UI 응답 DTO
│   │   ├── SkeletonOption.java         # 뼈대 시간표 응답 DTO
│   │   └── FillerOption.java           # 추천 과목 응답 DTO
│   ├── lib/
│   │   └── gson-2.10.1.jar        # JSON 직렬화 라이브러리
│   └── out/                       # 컴파일된 .class 파일 (빌드 후 생성)
│
├── auth/                          # 사용자 인증 모듈
│   ├── Main.java                  # 인증 CLI 진입점
│   ├── LoginService.java          # 로그인/가입 비즈니스 로직
│   ├── User.java                  # 사용자 도메인 모델
│   ├── UserRepository.java        # users.csv 읽기/쓰기
│   └── users.csv                  # 사용자 데이터 저장소
│
└── UI/                            # 시각화 UI 모듈
    └── UI.py                      # Streamlit 단일 페이지 앱
```

---

## 시스템 요구사항

- **Java** 11 이상
- **Python** 3.10 이상
- **Streamlit** 1.x

---

## 설치 및 실행

### 1단계 — Python 패키지 설치

```bash
pip install streamlit
pip install -r data/requirements.txt
```

### 2단계 — 과목 데이터 생성 (data 모듈)

`data/input/` 폴더에 수강신청자료집 PDF를 넣은 뒤 실행합니다.

```bash
cd KW-SCHEDULER/data
python src/main.py
```

실행 완료 후 `data/output/courses_2026_1.json`이 생성됩니다.

> 이미 `data/output/courses_2026_1.json`이 존재하면 이 단계를 건너뛸 수 있습니다.

### 3단계 — Java 알고리즘 컴파일 (algorithm 모듈)

`KW-SCHEDULER/` 루트에서 실행합니다.

```bash
# Windows
mkdir algorithm\out
del /q algorithm\out\*.class
javac -cp "algorithm/lib/gson-2.10.1.jar" -d algorithm/out -encoding UTF-8 algorithm/src/*.java

# Mac / Linux
mkdir -p algorithm/out
javac -cp "algorithm/lib/gson-2.10.1.jar" -d algorithm/out -encoding UTF-8 algorithm/src/*.java
```

### 4단계 — 회원가입 (auth 모듈)

UI 로그인 전에 auth CLI로 계정을 먼저 등록해야 합니다.

```bash
cd KW-SCHEDULER/auth

# Windows
javac *.java
java -cp "." Main

# Mac / Linux
javac *.java
java -cp . Main
```

대화형 메뉴에서 회원가입 후 학번과 비밀번호를 설정합니다.  
등록된 계정은 `auth/users.csv`에 저장됩니다.

> **직접 편집**: `auth/users.csv`를 텍스트 편집기로 열어 아래 형식으로 직접 추가할 수도 있습니다.
>
> ```
> # 형식: 학번|비밀번호|최초로그인|학년|단과대|학과|학과코드|복수전공|부전공
> 2025000000|1234|false|2|인공지능융합대학|소프트웨어학부|I040||
> ```

### 5단계 — UI 실행

`KW-SCHEDULER/` 루트에서 실행합니다.

```bash
streamlit run UI/UI.py
```

브라우저에서 `http://localhost:8501` 접속 후 로그인합니다.

---

## 사용 방법

### 로그인

`auth/users.csv`에 등록된 학번과 비밀번호로 로그인합니다.  
로그인 성공 시 저장된 학적 정보(학과코드, 학년 등)가 자동으로 복원됩니다.

### 시간표 생성

**1. 선호조건 설정** (좌측 패널)

| 항목 | 설명 |
|------|------|
| 최소/최대 학점 | 목표 학점 범위 (표시용) |
| 1교시 최대 허용 횟수 | 0이면 1교시 수업 없음, N이면 N회 이하 |
| 희망 공강 요일 | 선택한 요일 수업 배제 |
| 필수 포함 과목 | 반드시 넣을 과목 선택 (다중 선택 가능) |
| 기피 시간대 | 비우고 싶은 시간대 선택 |

**2. ✨ 시간표 추천 생성 버튼 클릭**

**3. 결과 확인** (우측 패널)

- **Option 1~5 탭** — 뼈대 시간표 후보 5개 확인
- **시간표 그리드** — 과목명, 교수명, 강의실 표시
- **추천과목 섹션** — 빈 시간에 추가 가능한 과목 목록
  - `+ 담기` 버튼으로 과목 추가
  - 시간 충돌 시 자동으로 담기 비활성화
  - `◀ ▶` 버튼으로 페이지 이동
- **담은 과목** — 추가한 과목 목록 및 제거 가능

---

## 모듈 상세

### algorithm — 2단계 추천 파이프라인

**Phase 1 — 뼈대 시간표 생성**

`ScheduleRecommender`가 선택된 필수 과목들의 모든 분반 조합을 재귀적으로 생성하고, 하드/소프트 제약을 평가하여 만족 개수 기준 상위 5개를 반환합니다.

**Phase 2 — 빈 시간 채우기**

`CourseSlotFiller`가 뼈대의 빈 시간에 추가 가능한 전공/타전공/교양 과목을 점수 기반으로 정렬하여 추천합니다.

**제약조건 클래스 구조**

```
SoftConstraint (추상 클래스)
├── DayOffConstraint       — 희망 공강 요일 마스킹 (마스킹형)
├── EmptySlotConstraint    — 기피 시간대 마스킹 (마스킹형)
└── FirstPeriodConstraint  — 1교시 최대 허용 횟수 평가 (평가형)
```

**UI 연동 통신 방식**

Python UI가 `request.json`을 작성하고 `subprocess`로 Java를 호출합니다.  
Java는 결과를 UTF-8 JSON으로 stdout에 출력하고, Python이 이를 파싱합니다.

```
UI.py  →  request.json 작성  →  java Main --mode json request.json
                                          ↓
UI.py  ←  json.loads()       ←  JSON stdout 출력
```

**요청 JSON 구조**

```json
{
  "recommendation": {
    "catalogPath": "data/output/courses_2026_1.json",
    "deptCode": "I040",
    "selectedCourseNames": ["객체지향프로그래밍"],
    "minCredit": 12,
    "maxCredit": 19,
    "maxFirstPeriod": 0,
    "dayOff": "금",
    "emptySlots": [{"요일": "월", "교시": [1, 2]}]
  },
  "selection": {
    "scheduleId": "SKEL-1",
    "addedCourseCodes": []
  }
}
```

### auth — 사용자 인증

`LoginService`가 `UserRepository`를 통해 `users.csv`를 읽고 씁니다.  
회원가입은 auth CLI에서, 로그인 검증은 UI에서 동일한 CSV를 직접 읽어 처리합니다.

```
auth/Main.java (CLI)  →  회원가입 → users.csv 저장
UI/UI.py              →  users.csv 읽어 로그인 검증
```

### data — 과목 데이터 파이프라인

```
수강신청자료집.pdf
    ↓ pdf_parser.py
PDF 과목 데이터
    +
KLAS 웹 스크래핑 (klas_scraper.py)
    ↓ merger.py
data/output/courses_2026_1.json   ←  algorithm & UI 에서 사용
```

---

## 학과코드 참고

| 단과대 | 학과 | 코드 |
|--------|------|------|
| 전자정보공과대학 | 전자공학과 | 7060 |
| 전자정보공과대학 | 전자통신공학과 | 7070 |
| 전자정보공과대학 | 전자융합공학과 | 7420 |
| 전자정보공과대학 | 전기공학과 | 7320 |
| 전자정보공과대학 | 전자재료공학과 | 7340 |
| 인공지능융합대학 | 컴퓨터정보공학부 | I020 |
| 인공지능융합대학 | 소프트웨어학부 | I030 |
| 인공지능융합대학 | 정보융합학부 | I040 |
| 인공지능융합대학 | 로봇학부 | I050 |
| 인공지능융합대학 | 지능형로봇학과 | I060 |
| 공과대학 | 건축공학과 | 1170 |
| 공과대학 | 건축학과 | 1270 |
| 공과대학 | 화학공학과 | 1140 |
| 공과대학 | 환경공학과 | 1160 |
| 자연과학대학 | 수학과 | 6030 |
| 자연과학대학 | 전자바이오물리학과 | 6100 |
| 자연과학대학 | 화학과 | 6050 |
| 자연과학대학 | 스포츠융합과학과 | 6130 |
| 자연과학대학 | 정보콘텐츠학과 | 6120 |
| 인문사회과학대학 | 국어국문학과 | 3040 |
| 인문사회과학대학 | 영어산업학과 | 3220 |
| 인문사회과학대학 | 미디어커뮤니케이션학부 | 3230 |
| 인문사회과학대학 | 산업심리학과 | 3110 |
| 인문사회과학대학 | 동북아문화산업학부 | 3210 |
| 정책법학대학 | 행정학과 | F020 |
| 정책법학대학 | 법학부 | F030 |
| 정책법학대학 | 국제학부 | F040 |
| 경영대학 | 경영학부 | 5080 |
| 경영대학 | 국제통상학부 | 5100 |

전체 학과코드는 `UI/UI.py`의 `COLLEGE_DEPT_MAP`에서 확인할 수 있습니다.

---

## 알려진 제한사항

- 필수 과목 학점 합이 최소 학점 이상이면 추천과목이 표시되지 않는 버그 (수정 예정)
- 회원가입 UI 미구현 — `auth/Main.java` CLI로만 가입 가능
- 과목 선택 시 분반 구분 미지원 — 동명 과목의 최적 분반은 알고리즘이 자동 선택
