from dataclasses import dataclass, field
from typing import Optional


@dataclass
class TimeSlot:
    요일: str        # "월" "화" "수" "목" "금" "토"
    교시: list[int]  # [1, 2], [8, 9, 10]


@dataclass
class ClassRoom:
    요일: str        # 강의실이 요일마다 다를 수 있음
    건물: str        # "새빛관"
    호수: str        # "103"


@dataclass
class Subject:
    학정번호: str                        # "I040-2-7777-01"
    과목명: str                          # "객체지향프로그래밍"
    학과코드: str                        # "I040"
    학년: int                            # 2
    이수구분: str                        # "전선" "전필" "교선" "교필"
    학점: Optional[int]                  # 3
    담당교수: Optional[str]              # "김준석"
    강의시간: list[TimeSlot]             # 빈 리스트 = 온라인
    강의실: list[ClassRoom]              # 스크래핑을 통해 채울 예정임
    강의유형: str                        # "소규모강의" "원격수업50%이상" ""
    분반제한: str                        # "인융대 2학년" ""