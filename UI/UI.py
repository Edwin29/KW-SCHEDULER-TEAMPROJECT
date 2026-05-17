import streamlit as st
import json
import subprocess
import os
import sys

# --- 1. 페이지 기본 설정 ---
st.set_page_config(page_title="KW-SCHEDULER", page_icon="📅", layout="wide")

# --- 2. 상태 관리 (Session State) 초기화 ---
if 'page' not in st.session_state:
    st.session_state.page = 'login'
if 'request_data' not in st.session_state:
    st.session_state.request_data = {}
if 'is_run' not in st.session_state:
    st.session_state.is_run = False
if 'skeleton_options' not in st.session_state:
    st.session_state.skeleton_options = []
if 'filler_options' not in st.session_state:
    st.session_state.filler_options = []
if 'student_id' not in st.session_state:
    st.session_state.student_id = ""
if 'user_db' not in st.session_state:
    st.session_state.user_db = {}
if '_login_key' not in st.session_state:
    st.session_state._login_key = ""
if 'skeleton_filler_map' not in st.session_state:
    st.session_state.skeleton_filler_map = {}
if 'active_skeleton_id' not in st.session_state:
    st.session_state.active_skeleton_id = ""
if 'added_courses' not in st.session_state:
    st.session_state.added_courses = {}

# --- 3. 공통 CSS 테마 적용 ---
def apply_custom_theme():
    st.markdown("""
    <style>
    @import url('https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;700&display=swap');
    .stApp { background-color: #E9E4E0; font-family: 'Noto Sans KR', sans-serif; }
    .main-header {
        background-color: #71554C; height: 65px; display: flex;
        justify-content: space-between; align-items: center;
        padding: 0 40px; color: white;
        border-bottom-left-radius: 15px; border-bottom-right-radius: 15px;
        margin-bottom: 20px; box-shadow: 0 4px 10px rgba(0,0,0,0.1);
    }
    .form-header {
        background-color: #71554c; color: white; text-align: center;
        padding: 15px; border-top-left-radius: 15px; border-top-right-radius: 15px;
        margin-bottom: 20px; box-shadow: 0 4px 10px rgba(0,0,0,0.1); padding-left: 15px;
    }
    .card-header {
        background-color: #71554C; color: white; padding: 12px 20px;
        border-top-left-radius: 15px !important; border-top-right-radius: 15px !important;
        border-bottom-left-radius: 0px !important; border-bottom-right-radius: 0px !important;
        font-size: 18px; font-weight: bold;
    }
    div.stButton > button {
        background-color: #71554C !important; color: white !important;
        border-radius: 10px !important; border: none !important;
        display: inline-block !important; height: auto !important;
        min-height: 45px !important; padding: 10px 20px !important;
        vertical-align: middle; width: auto !important;
        min-width: 120px !important; white-space: nowrap !important;
    }
    div[data-baseweb="select"] { border-radius: 10px !important; }
    button[data-baseweb="tab"] {
        background-color: #D1C4BC !important; border-radius: 20px 20px 0 0 !important;
        margin-right: 10px !important; padding: 10px 30px !important;
        font-weight: bold !important; font-size: 13px;
    }
    div[data-testid="stExpander"] {
        background-color: #D9CFCA !important; border: 1px solid #D3C9C1 !important;
        border-radius: 15px !important;
    }
    div[data-testid="stExpanderDetails"] { background-color: #F4F0ED !important; }
    </style>
    """, unsafe_allow_html=True)

# --- Auth: users.csv 연동 ---
AUTH_CSV_PATH = "auth/users.csv"

def load_users():
    """users.csv에서 전체 사용자 로드. 형식: 학번|비밀번호|최초로그인|학년|단과대|학과|학과코드|복수전공|부전공"""
    users = {}
    try:
        with open(AUTH_CSV_PATH, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith('#'):
                    continue
                fields = line.split('|')
                if len(fields) < 9:
                    continue
                users[fields[0]] = {
                    'password':        fields[1],
                    'isFirstLogin':    fields[2] == 'true',
                    'grade':           fields[3],
                    'college':         fields[4],
                    'department':      fields[5],
                    'deptCode':        fields[6],
                    'doubleMajorDept': fields[7],
                    'minorDept':       fields[8],
                }
    except Exception:
        pass
    return users

def authenticate(student_id, password):
    """로그인 검증. SUCCESS / USER_NOT_FOUND / WRONG_PASSWORD 반환."""
    users = load_users()
    if student_id not in users:
        return 'USER_NOT_FOUND'
    if users[student_id]['password'] != password:
        return 'WRONG_PASSWORD'
    return 'SUCCESS'

def get_user_profile(student_id):
    """사용자 프로필 반환. 없으면 None."""
    users = load_users()
    return users.get(student_id)

@st.cache_data
def load_course_names():
    current_dir = os.getcwd()
    catalog_path = os.path.join(current_dir, "data", "output", "courses_2026_1.json").replace("\\", "/")
    try:
        with open(catalog_path, 'r', encoding='utf-8') as f:
            courses = json.load(f)
            names = list(set([c.get('과목명') for c in courses if '과목명' in c]))
            return sorted(names)
    except Exception:
        return ["객체지향프로그래밍", "데이터베이스", "컴퓨터그래픽스", "이산수학", "운영체제", "컴퓨터네트워크"]

# --- 4-1. 로그인 화면 ---
def show_login_page():
    st.markdown("""
        <header class="main-header">
            <h1 style="font-size: 24px; margin: 0;">KW-SCHEDULER</h1>
        </header>
    """, unsafe_allow_html=True)
    error_msg = None
    _, col, _ = st.columns([1, 1, 1])
    with col:
        st.markdown("<div class='form-header'>Log in</div>", unsafe_allow_html=True)
        st.markdown("<h1 style='text-align:center;color:white;font-size:40px;margin-bottom:20px;padding-left:25px;'>👤</h1>", unsafe_allow_html=True)
        input_id = st.text_input("아이디 (학번)", label_visibility="collapsed", placeholder="아이디(학번)를 입력하세요", key="login_id_input")
        input_pw = st.text_input("비밀번호", type="password", label_visibility="collapsed", placeholder="비밀번호를 입력하세요", key="login_pw_input")
        st.text_input("이름", label_visibility="collapsed", placeholder="이름을 입력하세요 (선택)")
        _, btn_col2, _ = st.columns([1.2, 1, 1])
        with btn_col2:
            if st.button("로그인 진행", use_container_width=False):
                if input_id and input_pw:
                    result = authenticate(input_id, input_pw)
                    if result == 'SUCCESS':
                        profile = get_user_profile(input_id)
                        st.session_state.student_id = input_id
                        st.session_state._login_key = f"{input_id}:{input_pw}"
                        lk = st.session_state._login_key
                        if lk in st.session_state.user_db:
                            saved = st.session_state.user_db[lk]
                            st.session_state.skeleton_options = saved.get('skeleton_options', [])
                            st.session_state.filler_options   = saved.get('filler_options', [])
                            st.session_state.is_run           = saved.get('is_run', False)
                        if profile and profile.get('deptCode'):
                            st.session_state.request_data = {
                                'deptCode':        profile['deptCode'],
                                'doubleMajorCode': profile.get('doubleMajorDept', ''),
                                'minorCode':       profile.get('minorDept', ''),
                                'year':            f"{profile['grade']}학년" if profile['grade'] else '2학년',
                                'college':         profile.get('college', ''),
                                'department':      profile.get('department', ''),
                                'doubleMajor':     profile.get('doubleMajorDept', ''),
                                'minor':           profile.get('minorDept', ''),
                            }
                            st.session_state.page = 'main_schedule'
                        else:
                            st.session_state.request_data = {}
                            st.session_state.page = 'academic_info'
                        st.rerun()
                    elif result == 'USER_NOT_FOUND':
                        error_msg = "등록되지 않은 학번입니다. CLI(auth/Main.java)로 먼저 가입해주세요."
                    elif result == 'WRONG_PASSWORD':
                        error_msg = "비밀번호가 틀렸습니다."
                else:
                    error_msg = "학번과 비밀번호를 모두 입력해주세요."
        if error_msg:
            st.warning(error_msg)

# --- 4-2. 학적 정보 입력 화면 ---
def show_academic_info_page():
    topbar_col, home_col = st.columns([10, 1])
    with topbar_col:
        st.markdown(f"<div class='top-bar'><span style='font-weight:bold;'>KW-SCHEDULER</span><span>👤 {st.session_state.student_id}</span></div>", unsafe_allow_html=True)
    with home_col:
        st.markdown("<div style='height:15px'></div>", unsafe_allow_html=True)
        if st.button("🏠 홈", key="academic_home_btn", use_container_width=True):
            st.session_state.page = 'login'
            st.session_state.student_id = ""
            st.rerun()
    _, col, _ = st.columns([0.5, 1, 0.5])
    with col:
        st.markdown("""
            <style>
            .academic-box {
                background-color: #D3C9C1; border-radius: 10px;
                padding: 20px 35px 1px 35px; box-shadow: 0px 4px 6px rgba(0,0,0,0.1);
                margin-top: 50px; margin-bottom: -470px;
                height: 550px; position: relative; z-index: 0;
            }
            .academic-box .form-header {
                background-color: #71554c; color: white; padding: 15px 15px;
                border-top-left-radius: 15px; border-top-right-radius: 15px;
                margin: -30px -35px 20px -35px; font-weight: bold;
                font-size: 18px; text-align: left;
            }
            div[data-baseweb="select"] > div {
                height: 50px !important; min-height: 45px !important;
                background-color: #EEECE8 !important; border-radius: 15px !important;
                display: flex; align-items: center;
            }
            div[data-testid="stSelectbox"] { width: 100% !important; max-width: 400px; margin-right: auto !important; margin-left: 0 !important; }
            </style>
            <div class='academic-box'><div class='form-header'>학적정보</div>
        """, unsafe_allow_html=True)
        with st.container():
            def make_input_row(label, options, key):
                _,col1, col2,_ = st.columns([0.8,0.8,4,0.5])
                with col1:
                    st.markdown(f"<div style='margin-top:8px;font-weight:bold;color:#333;text-align:left;padding-right:10px;'>{label}</div>", unsafe_allow_html=True)
                with col2:
                    return st.selectbox(label, options, key=key, label_visibility="collapsed")

            COLLEGE_DEPT_MAP = {
                "선택안함": {
                    "선택안함": ""
                },

                "전자정보공과대학": {
                    "전자공학과": "7060",
                    "전자통신공학과": "7070",
                    "전자융합공학과": "7420",
                    "전기공학과": "7320",
                    "전자재료공학과": "7340"
                },

                "인공지능융합대학": {
                    "소프트웨어학부": "I030",
                    "컴퓨터정보공학부": "I020",
                    "정보융합학부": "I040",
                    "인공지능학부": "I043",
                    "로봇학부": "I050"
                },

                "공과대학": {
                    "건축공학과": "1170",
                    "건축학과": "1270",
                    "화학공학과": "1140",
                    "환경공학과": "1160"
                },

                "자연과학대학": {
                    "수학과": "6030",
                    "전자바이오물리학과": "6100",
                    "화학과": "6050",
                    "스포츠융합과학과": "6130",
                    "정보콘텐츠학과": "6120"
                },

                "인문사회과학대학": {
                    "국어국문학과": "3040",
                    "영어산업학과": "3220",
                    "미디어커뮤니케이션학부": "3230",
                    "산업심리학과": "3110",
                    "동북아문화산업학부": "3210"
                },

                "정책법학대학": {
                    "행정학과": "F020",
                    "법학부": "F030",
                    "국제학부": "F040"
                },

                "경영대학": {
                    "경영학부": "5080",
                    "국제통상학부": "5100"
                }
            }
            all_depts = ["해당없음"]
            for depts in COLLEGE_DEPT_MAP.values():
                for d in depts.keys():
                    if d != "선택안함" and d not in all_depts:
                        all_depts.append(d)

            year = make_input_row("학년", ["1학년","2학년","3학년","4학년","초과학기"], "year")
            college = make_input_row("단과대", list(COLLEGE_DEPT_MAP.keys()), "college")
            dept = make_input_row("학과", list(COLLEGE_DEPT_MAP[college].keys()), "dept")
            double_major = make_input_row("복수전공", all_depts, "double_major")
            minor = make_input_row("부전공", all_depts, "minor")

            st.markdown("<br>", unsafe_allow_html=True)
            if st.button("다음 단계로 (시간표 구성)", use_container_width=False):
                dept_code = COLLEGE_DEPT_MAP[college].get(dept, "I040") if dept != "선택안함" else "I040"
                dm_code, mn_code = "", ""
                for depts in COLLEGE_DEPT_MAP.values():
                    if double_major in depts: dm_code = depts[double_major]
                    if minor in depts: mn_code = depts[minor]
                st.session_state.request_data = {
                    "deptCode": dept_code, "doubleMajorCode": dm_code, "minorCode": mn_code,
                    "year": year, "college": college, "department": dept,
                    "doubleMajor": double_major, "minor": minor
                }
                lk = st.session_state._login_key
                if lk:
                    if lk not in st.session_state.user_db:
                        st.session_state.user_db[lk] = {}
                    st.session_state.user_db[lk]['request_data'] = st.session_state.request_data
                st.session_state.page = 'main_schedule'
                st.rerun()

# --- 4-3. 메인 시간표 및 추천 화면 ---
def show_main_schedule_page():
    st.markdown("""
        <style>
            .stApp { background-color: #FFFFFF !important; }
            header[data-testid="stHeader"] { background-color: rgba(0,0,0,0) !important; }
        </style>
    """, unsafe_allow_html=True)

    topbar_col, btn_col = st.columns([8, 2])
    with topbar_col:
        st.markdown(f"""
        <div class='main-header'>
            <span style='font-size:20px;font-weight:bold;'>광운대 시간표 도우미</span>
            <span style='font-size:16px;'>👤 {st.session_state.request_data.get('year','2학년')} {st.session_state.student_id}</span>
        </div>
        """, unsafe_allow_html=True)
    with btn_col:
        st.markdown("<div style='height:15px'></div>", unsafe_allow_html=True)
        bc1, bc2 = st.columns([1,1])
        with bc1:
            if st.button("🏠 홈", key="main_home_btn", use_container_width=True):
                st.session_state.page = 'login'
                st.session_state.student_id = ""
                st.rerun()
        with bc2:
            if st.button("🚪 로그아웃", key="logout_btn", use_container_width=True):
                lk = st.session_state._login_key
                if lk:
                    st.session_state.user_db[lk] = {
                        'request_data': st.session_state.request_data,
                        'skeleton_options': st.session_state.skeleton_options,
                        'filler_options': st.session_state.filler_options,
                        'is_run': st.session_state.is_run,
                    }
                st.session_state.page = 'login'
                st.session_state.student_id = ""
                st.session_state._login_key = ""
                st.session_state.request_data = {}
                st.session_state.skeleton_options = []
                st.session_state.filler_options = []
                st.session_state.is_run = False
                st.session_state.added_courses = {}
                st.rerun()

    # 좌측: 선호조건 / 우측: 시간표 + 추천과목
    left_col, right_col = st.columns([1, 3])

    with left_col:
        st.markdown("<div class='card-header'>선호조건 (User Preferences)</div>", unsafe_allow_html=True)
        with st.expander("조건 설정", expanded=True):
            min_credit = st.slider("최소 학점", 9, 21, 12)
            max_credit = st.slider("최대 학점", 9, 21, 19)
            max_first_period = st.slider("1교시 최대 허용 횟수", 0, 5, 0)
            day_off = st.selectbox("희망 공강 요일", ["없음","월","화","수","목","금"])
            all_courses = load_course_names()
            must_courses = st.multiselect("필수 포함 과목", all_courses,
                default=["객체지향프로그래밍"] if "객체지향프로그래밍" in all_courses else [])
            st.markdown("##### 기피 시간대")
            empty_slot_options = [f"{d}요일 {p}교시" for d in ["월","화","수","목","금","토"] for p in range(1, 10)]
            selected_empty_slots = st.multiselect("비우고 싶은 시간대를 선택하세요", empty_slot_options)

        if st.button("✨ 시간표 추천 생성", use_container_width=True):
            if not must_courses:
                st.warning("⚠️ 최소 1개 이상의 필수 포함 과목을 선택해야 시간표를 생성할 수 있습니다.")
            else:
                empty_slots_dict = {}
                for es in selected_empty_slots:
                    day = es[0]
                    period = int(es.split(" ")[1].replace("교시",""))
                    empty_slots_dict.setdefault(day, []).append(period)
                empty_slots_payload = [{"요일": d, "교시": sorted(ps)} for d, ps in empty_slots_dict.items()]
                run_backend_recommendation(min_credit, max_credit, max_first_period, day_off, must_courses, empty_slots_payload)

    with right_col:
        if st.session_state.is_run:
            if st.session_state.skeleton_options:
                tabs = st.tabs([f"Option {i+1}" for i in range(len(st.session_state.skeleton_options))])
                for i, tab in enumerate(tabs):
                    with tab:
                        skeleton = st.session_state.skeleton_options[i]
                        sid = skeleton.get('scheduleId', f'SKEL-{i+1}')
                        added_courses = st.session_state.added_courses.get(sid, [])
                        tc = skeleton.get('totalCredit', 0) + sum(c.get('credit', 0) for c in added_courses)
                        mc = skeleton.get('majorCredit', 0)
                        sc = skeleton.get('satisfiedCount', 0)

                        st.caption(f"총 {tc}학점 / 목표 {min_credit}~{max_credit}학점 | 전공 {mc}학점 | 만족 제약 {sc}개")

                        # 시간표 그리드
                        render_timetable_grid(skeleton.get('courses', []) + added_courses)

                        # 담은 과목 목록
                        if added_courses:
                            st.markdown("<div style='margin-top:10px;font-weight:bold;color:#6C5449;'>📌 담은 과목</div>", unsafe_allow_html=True)
                            for idx, course in enumerate(added_courses):
                                col_name, col_btn = st.columns([4, 1])
                                with col_name:
                                    st.markdown(
                                        f"<div style='padding:4px 0;font-size:0.9em;'>"
                                        f"{course.get('courseName','')} ({course.get('credit',3)}학점)</div>",
                                        unsafe_allow_html=True
                                    )
                                with col_btn:
                                    if st.button("제거", key=f"remove_{sid}_{idx}"):
                                        st.session_state.added_courses[sid].pop(idx)
                                        st.rerun()

                        # ── 추천과목 섹션 ──
                        st.markdown("<br>", unsafe_allow_html=True)
                        st.markdown("<div class='card-header'>추천과목 (빈 시간 채움)</div>", unsafe_allow_html=True)

                        current_filler = st.session_state.skeleton_filler_map.get(sid, [])

                        # 중복 추천과목 제거: 같은 학정번호(courseCode)가 여러 번 들어오면 첫 번째만 사용
                        seen_codes = set()
                        unique_fillers = []

                        for filler in current_filler:
                            code = filler.get('courseCode') or filler.get('학정번호') or filler.get('courseName') or filler.get('과목명')
                            if code in seen_codes:
                                continue
                            seen_codes.add(code)
                            unique_fillers.append(filler)

                        majors, double_minors, all_liberals = [], [], []

                        for filler in unique_fillers:
                            cat = filler.get('category', '')
                            if cat == '전공':
                                majors.append(filler)
                            elif cat == '타전공':
                                double_minors.append(filler)
                            elif cat == '교양':
                                all_liberals.append(filler)

                        all_fillers = majors + double_minors + all_liberals

                        def render_filler_card(filler, _sid, _idx):
                            cat  = filler.get('category', '과목')
                            name = filler.get('courseName', filler.get('과목명', '과목명'))
                            crd  = filler.get('credit',     filler.get('학점', 3))
                            prof = filler.get('professor',  filler.get('담당교수', ''))
                            time_slots = filler.get('강의시간', filler.get('timeSlots', []))
                            classrooms = filler.get('강의실',   filler.get('classRooms', []))
                            time_lines = []
                            for idx_t, t in enumerate(time_slots):
                                day_t    = t.get('요일', t.get('day', ''))
                                periods  = t.get('교시', t.get('periods', []))
                                period_str = f"{min(periods)}~{max(periods)}교시" if periods else ""
                                room_str = ""
                                if classrooms and idx_t < len(classrooms):
                                    cr = classrooms[idx_t]
                                    bld = cr.get('건물', cr.get('building', ''))
                                    rm  = cr.get('호수', cr.get('roomNumber', ''))
                                    room_str = f" ({bld} {rm})".strip()
                                if day_t and period_str:
                                    time_lines.append(f"{day_t}요일 {period_str}{room_str}")
                            time_html = "<br>".join([f"<span style='font-size:0.78em;color:#555;'>🕐 {tl}</span>" for tl in time_lines]) if time_lines else ""
                            prof_html = f"<span style='font-size:0.8em;color:#666;'>👨‍🏫 {prof}</span><br>" if prof else ""
                            st.markdown(
                                f"<div style='background-color:white;padding:10px;border-radius:5px;"
                                f"margin-top:5px;border-left:5px solid #7895A2;'>"
                                f"<small style='color:#7895A2;font-weight:bold;'>{cat}</small><br>"
                                f"<b style='font-size:0.95em;'>{name}</b> "
                                f"<span style='color:#6C5449;font-weight:bold;'>({crd}학점)</span><br>"
                                f"{prof_html}{time_html}</div>",
                                unsafe_allow_html=True
                            )
                            # 담기 버튼
                            added = st.session_state.added_courses.get(_sid, [])
                            added_names = {c.get('courseName','') for c in added}
                            skeleton_courses = skeleton.get('courses', [])

                            def get_periods_set(course, ts_key='timeSlots', p_key='교시', d_key='요일'):
                                result = set()
                                for t in course.get(ts_key, course.get('강의시간', [])):
                                    d = t.get(d_key, t.get('day', ''))
                                    ps = t.get(p_key, t.get('periods', []))
                                    for p in ps:
                                        result.add((d, p))
                                return result

                            filler_periods = get_periods_set(filler)
                            all_existing = skeleton_courses + added
                            conflict = any(
                                get_periods_set(c, '강의시간', '교시', '요일').intersection(filler_periods)
                                or get_periods_set(c, 'timeSlots', '교시', '요일').intersection(filler_periods)
                                for c in all_existing
                            )
                            already_added = name in added_names
                            course_code = filler.get('courseCode', name)
                            button_key = f"add_{_sid}_{current_page}_{_idx}_{course_code}"
                            if already_added:
                                st.button("✓ 담김", key=button_key, disabled=True, use_container_width=True)
                            elif conflict:
                                st.button("⚠ 시간 충돌", key=button_key, disabled=True, use_container_width=True)
                            else:
                                if st.button("+ 담기", key=button_key, use_container_width=True):
                                    added = st.session_state.added_courses.get(_sid, [])
                                    added_credit = sum(c.get('credit', 0) for c in added)
                                    skeleton_credit = next(
                                        (s.get('totalCredit', 0) for s in st.session_state.skeleton_options
                                        if s.get('scheduleId') == _sid), 0
                                    )
                                    if skeleton_credit + added_credit + crd > max_credit:
                                        st.warning(f"최대 학점({max_credit}학점)을 초과합니다.")
                                    else:
                                        st.session_state.added_courses.setdefault(_sid, []).append(filler)
                                        st.rerun()
                        # 페이지네이션
                        PAGE_SIZE = 5
                        page_key = f"filler_page_{sid}"
                        if page_key not in st.session_state:
                            st.session_state[page_key] = 0
                        current_page = st.session_state[page_key]
                        total_pages = max(1, (len(all_fillers) + PAGE_SIZE - 1) // PAGE_SIZE)
                        page_fillers = all_fillers[current_page * PAGE_SIZE:(current_page + 1) * PAGE_SIZE]

                        if page_fillers:
                            f_cols = st.columns(min(len(page_fillers), 5))
                            for f_idx, f in enumerate(page_fillers):
                                with f_cols[f_idx]:
                                    global_idx = current_page * PAGE_SIZE + f_idx
                                    render_filler_card(f, sid, global_idx)
                        else:
                            st.markdown(
                                "<div style='color:#888;font-size:0.85em;margin-top:5px;padding:5px;'>"
                                "빈 시간에 맞는 추천 과목이 없습니다.</div>",
                                unsafe_allow_html=True
                            )

                        p1, p2, p3 = st.columns([1, 6, 1])
                        with p1:
                            if st.button("◀", key=f"prev_{sid}") and current_page > 0:
                                st.session_state[page_key] -= 1
                                st.rerun()
                        with p2:
                            st.markdown(f"<div style='text-align:center;margin-top:8px;'>{current_page+1} / {total_pages}</div>", unsafe_allow_html=True)
                        with p3:
                            if st.button("▶", key=f"next_{sid}") and current_page < total_pages - 1:
                                st.session_state[page_key] += 1
                                st.rerun()
            else:
                st.warning("⚠️ 설정한 조건에 맞는 시간표 조합을 찾을 수 없습니다. 조건을 완화해보세요.")
        else:
            st.info("👈 좌측의 '시간표 추천 생성' 버튼을 누르면 자바 알고리즘이 실행되어 결과를 계산합니다.")

# --- 5. 백엔드(Java) 연동 ---
def _call_java(payload: dict) -> dict | None:
    """Java 백엔드를 호출하고 리스폰스 dict를 반환. 실패 시 None."""
    try:
        with open('request.json', 'w', encoding='utf-8') as f:
            json.dump(payload, f, ensure_ascii=False, indent=2)
        sep = ";" if sys.platform == "win32" else ":"
        cmd = ["java", "-cp", f"algorithm/out{sep}algorithm/lib/gson-2.10.1.jar", "Main", "--mode", "json", "request.json"]
        result = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', errors='replace')
        if result.returncode == 0 and result.stdout.strip():
            out_str = result.stdout.strip()
            start_idx = out_str.find('{')
            if start_idx != -1:
                out_str = out_str[start_idx:]
            return json.loads(out_str)
        return None
    except Exception as e:
        st.error(f"Java 백엔드 호출 중 오류가 발생했습니다: {e}")
        return None

def run_backend_recommendation(min_c, max_c, max_first_period, day_off, courses, empty_slots_payload):
    st.session_state.added_courses = {}
    st.session_state.is_run = True
    current_dir = os.getcwd()
    catalog_path = os.path.join(current_dir, "data", "output", "courses_2026_1.json").replace("\\", "/")
    base_rec = {
        "catalogPath": catalog_path,
        "deptCode": st.session_state.request_data.get('deptCode', 'I040'),
        "doubleMajorDeptCode": st.session_state.request_data.get('doubleMajorCode', ''),
        "minorDeptCode": st.session_state.request_data.get('minorCode', ''),
        "selectedCourseNames": courses,
        "minCredit": min_c,
        "maxCredit": max_c,
        "maxFirstPeriod": max_first_period,
        "dayOff": day_off if day_off != "없음" else "",
        "emptySlots": empty_slots_payload
    }
    payload_skel = {"recommendation": base_rec}
    resp_skel = _call_java(payload_skel)
    if resp_skel is None:
        st.error("알고리즘 실행 오류가 발생했습니다.")
        st.session_state.skeleton_options = []
        st.session_state.skeleton_filler_map = {}
        return
    status = resp_skel.get("status")
    if status == "ERROR":
        st.error(f"알고리즘 오류: {resp_skel.get('message')}")
        st.session_state.skeleton_options = []
        st.session_state.skeleton_filler_map = {}
        return
    if status == "NO_SKELETON":
        st.warning("조건을 만족하는 뼈대 시간표가 없습니다. 필수 과목을 줄이거나 기피 조건을 완화해보세요.")
        st.session_state.skeleton_options = []
        st.session_state.skeleton_filler_map = {}
        return
    skeletons = resp_skel.get('skeletonOptions', [])[:5]
    st.session_state.skeleton_options = skeletons
    filler_map = {}
    with st.spinner("추천 과목 가져오는 중..."):
        for skel in skeletons:
            sid = skel.get('scheduleId', '')
            if not sid: continue
            payload_filler = {
                "recommendation": base_rec,
                "selection": {"scheduleId": sid, "addedCourseCodes": []}
            }
            resp_f = _call_java(payload_filler)
            filler_map[sid] = resp_f['fillerOptions'] if resp_f and resp_f.get('fillerOptions') else []
    st.session_state.skeleton_filler_map = filler_map
    first_sid = skeletons[0].get('scheduleId', '') if skeletons else ''
    st.session_state.filler_options = filler_map.get(first_sid, [])
    lk = st.session_state._login_key
    if lk:
        st.session_state.user_db.setdefault(lk, {}).update({
            'skeleton_options': skeletons,
            'skeleton_filler_map': filler_map,
            'filler_options': st.session_state.filler_options,
            'is_run': True
        })

# --- 6. 시간표 그리드 렌더링 함수 ---
def render_timetable_grid(courses):
    days = ["Mon","Tue","Wed","Thurs","Fri","Sat"]
    day_map = {"월":"Mon","화":"Tue","수":"Wed","목":"Thurs","금":"Fri","토":"Sat"}
    header_height, row_height, col_width = 45, 65, 50
    html = f"""<table style='width:100%;border-collapse:separate;border-spacing:0;
        background-color:#E6E2DD;text-align:center;table-layout:fixed;
        margin:0;padding:0;border-radius:15px;overflow:hidden;border:1px solid #D3C9C1;'>"""
    html += f"<tr style='background-color:#6C5449;color:white;height:{header_height}px;'>"
    html += f"<th style='width:{col_width}px;border:1px solid #D3C9C1;padding:0;'>교시</th>"
    for d in days:
        html += f"<th style='border:1px solid #D3C9C1;padding:0;'>{d}</th>"
    html += "</tr>"
    for period in range(1, 12):
        html += f"<tr style='height:{row_height}px;'><td style='background-color:#6C5449;color:white;border:1px solid #D3C9C1;padding:0;'>{period}</td>"
        for _ in days: html += "<td style='border:1px solid #D3C9C1;padding:0;'></td>"
        html += "</tr>"
    html += "</table>"

    blocks_html = ""
    ##colors = ["#FBF2DA","#F8FF96","#CFAAEE","#8DB1EF","#85D8C7","#FBFDDD","#A33E43"]
    colors = ["#FBF2DA","#C0E6AD","#9CDFEB","#8DB1EF","#E79999","#FFDCA7","#CEC0FE"]
    for i, course in enumerate(courses):
        c_name = course.get('과목명', course.get('courseName', '알 수 없음'))
        prof   = course.get('담당교수', course.get('professor', ''))
        color  = colors[i % len(colors)]
        times      = course.get('강의시간', course.get('times', []))
        classrooms = course.get('강의실',   course.get('classRooms', []))
        default_room = course.get('classroom', '미정')
        for t_idx, t in enumerate(times):
            day_ko = t.get('요일', t.get('day', ''))
            day_en = day_map.get(day_ko, day_ko)
            if day_en not in days: continue
            classroom_name = default_room
            if classrooms and isinstance(classrooms, list) and t_idx < len(classrooms):
                cr = classrooms[t_idx]
                cr_name = f"{cr.get('건물','')} {cr.get('호수','')}".strip()
                if cr_name: classroom_name = cr_name
            if not classroom_name or classroom_name == ' ':
                classroom_name = '미정'
            day_idx = days.index(day_en)
            periods = t.get('교시', t.get('periods', []))
            if periods and isinstance(periods, list):
                start_p = min(int(p) for p in periods)
                end_p   = max(int(p) for p in periods)
            else:
                start_p = int(t.get('start', 1))
                end_p   = int(t.get('end', start_p))
            top    = header_height + (start_p - 1) * row_height
            height = (end_p - start_p + 1) * row_height
            left   = f"calc({col_width}px + ((100% - {col_width}px) / 6) * {day_idx})"
            width  = f"calc((100% - {col_width}px) / 6)"
            blocks_html += (
                f"<div style='position:absolute;top:{top}px;left:{left};width:{width};height:{height}px;"
                f"padding:0;box-sizing:border-box;z-index:10;'>"
                f"<div style='width:100%;height:100%;background-color:{color};color:black;font-size:0.85em;"
                f"font-weight:bold;box-shadow:inset 0 0 0 1px rgba(0,0,0,0.1);display:flex;flex-direction:column;"
                f"align-items:center;justify-content:center;text-align:center;line-height:1.3;padding:5px;"
                f"overflow:hidden;word-break:keep-all;'>"
                f"<span>{c_name}</span>"
                f"<span style='font-size:0.75em;font-weight:normal;opacity:0.75;'>{prof}</span>"
                f"<span style='font-size:0.8em;font-weight:normal;opacity:0.9;margin-top:4px;'>{classroom_name}</span>"
                f"</div></div>"
            )
    st.markdown(f"<div style='position:relative;margin-top:10px;width:100%;'>{html}{blocks_html}</div>", unsafe_allow_html=True)

# --- 메인 라우터 ---
def main():
    apply_custom_theme()
    if st.session_state.page == 'login':
        show_login_page()
    elif st.session_state.page == 'academic_info':
        show_academic_info_page()
    elif st.session_state.page == 'main_schedule':
        show_main_schedule_page()

if __name__ == "__main__":
    main()
