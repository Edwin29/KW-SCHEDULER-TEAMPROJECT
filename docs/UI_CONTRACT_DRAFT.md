# UI 연동 계약 초안

## 1) 입력(Request)
`RecommendationRequest`
- `catalogPath`: 과목 JSON 경로
- `deptCode`: 사용자 학과코드
- `selectedCourseNames`: 필수 과목명 목록(2~7개 권장)
- `minCredit`, `maxCredit`
- `avoidFirstPeriod`
- `dayOff`
- `emptySlots`

## 2) 3단계(필수 과목 목록)
- API 성격: `listSelectableCourses(deptCode)`
- 응답: `Course[]` (과목코드, 이름, 학점, 분류)

## 3) 4단계(뼈대 시간표 후보)
- API 성격: `recommendSkeletonOptions(...)`
- 응답: `SkeletonOption[]`
  - `scheduleId` (예: `SKEL-1`)
  - `totalCredit`, `majorCredit`, `satisfiedCount`
  - `courses[]`

## 4) 5단계(빈 시간 추천 과목)
- API 성격: `recommendFillerOptions(selectedSkeleton, ...)`
- 응답: `FillerOption[]`
  - `category` (`전공`/`타전공`/`기타`)
  - `courseCode`, `courseName`, `credit`

## 5) 최종 확정
UI는 사용자가 선택한 `scheduleId`와 추가 `courseCode[]`를 저장/전송하여 최종 시간표를 확정한다.
