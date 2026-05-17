import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * 과목 하나의 정보를 저장하는 데이터 클래스.
 * Python 파싱 결과 JSON의 필드 구조와 1:1 대응.
 */
public class Course {

    // ──────────────────────────────────────────────
    // 필드 (JSON 키 → Java 필드명 매핑)
    // ──────────────────────────────────────────────

    /** 학정번호. 예) "I040-2-7777-01" */
    @SerializedName("학정번호")
    private String courseCode;

    /** 과목명. 예) "객체지향프로그래밍" */
    @SerializedName("과목명")
    private String name;

    /** 학과코드. 학정번호 앞 부분. 예) "I040", "0000" */
    @SerializedName("학과코드")
    private String deptCode;

    /** 대상 학년. 예) 2 */
    @SerializedName("학년")
    private int grade;

    /**
     * 이수구분.
     * 전필 / 전선 / 교선 / 교필 / 일선 / 무관
     */
    @SerializedName("이수구분")
    private String category;

    /** 학점 수. 예) 3 */
    @SerializedName("학점")
    private int credit;

    /** 담당교수명. 미기재 시 null. */
    @SerializedName("담당교수")
    private String professor;

    /**
     * 강의 시간 목록.
     * 요일이 다른 수업이 여러 개일 수 있음.
     * 예) 화 1교시, 목 2교시
     */
    @SerializedName("강의시간")
    private List<TimeSlot> timeSlots;

    /**
     * 강의실 목록.
     * Python 스크래핑 결과. 건물명 + 호수.
     */
    @SerializedName("강의실")
    private List<ClassRoom> classRooms;

    /**
     * 강의유형.
     * 예) "소규모강의", "원격수업50%이상", ""
     */
    @SerializedName("강의유형")
    private String lectureType;

    /** 분반 수강 제한. 예) "전정대 2학년", "" */
    @SerializedName("분반제한")
    private String sectionLimit;

    // ──────────────────────────────────────────────
    // 생성자
    // ──────────────────────────────────────────────

    /** 기본 생성자. Gson 역직렬화 시 사용. */
    public Course() {
        this.timeSlots = new ArrayList<>();
        this.classRooms = new ArrayList<>();
    }

    // ──────────────────────────────────────────────
    // 핵심 메서드
    // ──────────────────────────────────────────────

    /**
     * 전공 과목 여부 판단.
     * 이수구분이 전필 또는 전선이면 전공으로 판단.
     *
     * @return 전공이면 true
     */
    public boolean isMajor() {
        return "전필".equals(category) || "전선".equals(category);
    }

    /**
     * 교양 영역 반환.
     * 교선/교필 과목에서 의미 있는 값.
     * 학정번호 앞 4자리가 "0000"이면 교양 과목.
     *
     * @return 이수구분 문자열
     */
    public String getLiberalArtsArea() {
        return category;
    }

    /**
     * 과목 난이도 반환.
     * 학정번호 5번째 자리 숫자로 판단.
     * 교양 중복 신청 검사에 사용.
     * 예) "0000-2-6137-01" → 5번째 자리 = '2'
     *
     * @return 난이도 문자열, 판단 불가 시 null
     */
    public String getDifficulty() {
        if (courseCode == null || courseCode.isBlank()) return null;
        String[] parts = courseCode.split("-");
        return parts.length >= 2 && !parts[1].isBlank() ? parts[1] : null;
    }

    /**
     * 특정 요일에 수업이 있는지 확인.
     * 마스킹 처리 시 사용.
     *
     * @param day 요일 문자열
     * @return 해당 요일 수업이 있으면 true
     */
    public boolean hasClassOnDay(String day) {
        if (day == null || timeSlots == null) return false;
        for (TimeSlot slot : timeSlots) {
            if (day.equals(slot.getDay())) return true;
        }
        return false;
    }

    /**
     * 특정 TimeSlot과 시간이 겹치는지 확인.
     * EmptySlot 마스킹 처리 시 사용.
     *
     * @param target 비교할 TimeSlot
     * @return 겹치면 true
     */
    public boolean overlaps(TimeSlot target) {
        if (target == null || timeSlots == null) return false;
        for (TimeSlot slot : timeSlots) {
            if (slot.overlaps(target)) return true;
        }
        return false;
    }

    // ──────────────────────────────────────────────
    // Getter / Setter
    // ──────────────────────────────────────────────

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDeptCode() { return deptCode; }
    public void setDeptCode(String deptCode) { this.deptCode = deptCode; }

    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getCredit() { return credit; }
    public void setCredit(int credit) { this.credit = credit; }

    public String getProfessor() { return professor; }
    public void setProfessor(String professor) { this.professor = professor; }

    public List<TimeSlot> getTimeSlots() { return timeSlots; }
    public void setTimeSlots(List<TimeSlot> timeSlots) {
        this.timeSlots = new ArrayList<>();
        if (timeSlots != null) this.timeSlots.addAll(timeSlots);
    }

    public List<ClassRoom> getClassRooms() { return classRooms; }
    public void setClassRooms(List<ClassRoom> classRooms) {
        this.classRooms = new ArrayList<>();
        if (classRooms != null) this.classRooms.addAll(classRooms);
    }

    public String getLectureType() { return lectureType; }
    public void setLectureType(String lectureType) { this.lectureType = lectureType; }

    public String getSectionLimit() { return sectionLimit; }
    public void setSectionLimit(String sectionLimit) { this.sectionLimit = sectionLimit; }

    @Override
    public String toString() {
        return name + " (" + courseCode + ") / " + professor + " / " + credit + "학점";
    }
}
