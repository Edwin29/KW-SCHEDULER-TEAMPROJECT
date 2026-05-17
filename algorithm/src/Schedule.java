import java.util.ArrayList;
import java.util.List;

/**
 * 하나의 완성된 시간표 조합을 저장하는 클래스.
 * 과목 목록, 학점 정보, 소프트 제약 만족 결과를 함께 관리.
 */
public class Schedule {

    /** 선택된 과목 목록. */
    private List<Course> courses;

    /** 총 학점. addCourse() 호출 시 자동 갱신. */
    private int totalCredit;

    /** 전공 학점. addCourse() 호출 시 자동 갱신. */
    private int majorCredit;

    /**
     * 소프트 제약(평가형) 만족 결과.
     * ScheduleRecommender에서 evaluate() 후 설정.
     */
    private ConstraintResult constraintResult;

    // ──────────────────────────────────────────────
    // 생성자
    // ──────────────────────────────────────────────

    public Schedule() {
        this.courses = new ArrayList<>();
        this.constraintResult = new ConstraintResult();
    }

    /**
     * 과목 목록을 받아 시간표를 만드는 생성자.
     * 총 학점과 전공 학점은 자동 계산.
     *
     * @param courses 과목 목록
     */
    public Schedule(List<Course> courses) {
        this();
        if (courses != null) {
            for (Course course : courses) {
                addCourse(course);
            }
        }
    }

    // ──────────────────────────────────────────────
    // 핵심 메서드
    // ──────────────────────────────────────────────

    /**
     * 시간표에 과목 하나를 추가하고 학점 정보를 갱신.
     *
     * @param course 추가할 과목
     */
    public void addCourse(Course course) {
        if (course == null) return;
        courses.add(course);
        totalCredit += course.getCredit();
        if (course.isMajor()) majorCredit += course.getCredit();
    }

    /**
     * 소프트 제약 만족 개수 반환.
     * ConstraintResult에 위임.
     * 그룹화 기준으로 사용.
     *
     * @return 만족한 소프트 제약 개수
     */
    public int getSatisfiedCount() {
        return constraintResult.getSatisfiedCount();
    }

    // ──────────────────────────────────────────────
    // Getter / Setter
    // ──────────────────────────────────────────────

    public List<Course> getCourses() { return courses; }
    public void setCourses(List<Course> courses) {
        this.courses = new ArrayList<>();
        this.totalCredit = 0;
        this.majorCredit = 0;
        if (courses != null) {
            for (Course course : courses) addCourse(course);
        }
    }

    public int getTotalCredit() { return totalCredit; }
    public int getMajorCredit() { return majorCredit; }

    public ConstraintResult getConstraintResult() { return constraintResult; }
    public void setConstraintResult(ConstraintResult constraintResult) {
        this.constraintResult = constraintResult;
    }

    @Override
    public String toString() {
        return "Schedule{totalCredit=" + totalCredit +
               ", majorCredit=" + majorCredit +
               ", satisfied=" + getSatisfiedCount() + "}";
    }
}
