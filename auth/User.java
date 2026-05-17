/**
 * 사용자 정보를 저장하는 데이터 클래스.
 * 학번, 비밀번호, 학적 정보를 관리.
 */
public class User {

    // ──────────────────────────────────────────────
    // 필드
    // ──────────────────────────────────────────────

    /** 학번. 예) "20210001" */
    private String studentId;

    /** 비밀번호. */
    private String password;

    /**
     * 최초 로그인 여부.
     * true: 아직 학적 정보 미입력 상태
     * false: 학적 정보 입력 완료
     */
    private boolean isFirstLogin = true;

    /** 학년. 1~4 */
    private int grade;

    /** 단과대학명. 예) "전자정보공과대학" */
    private String college;

    /** 학과명. 예) "소프트웨어학부" */
    private String department;

    /**
     * 학과코드.
     * CourseRepository 필터링에 사용.
     * 예) "I020", "I040", "5080"
     */
    private String deptCode;

    /** 복수전공 학과명. 없으면 빈 문자열. */
    private String doubleMajorDept;

    /** 부전공 학과명. 없으면 빈 문자열. */
    private String minorDept;

    // ──────────────────────────────────────────────
    // 생성자
    // ──────────────────────────────────────────────

    /**
     * 최초 가입 시 사용하는 생성자.
     * 학번과 비밀번호만 받고 나머지는 기본값.
     *
     * @param studentId 학번
     * @param password  비밀번호
     */
    public User(String studentId, String password) {
        this.studentId = studentId;
        this.password = password;
    }

    // ──────────────────────────────────────────────
    // 핵심 메서드
    // ──────────────────────────────────────────────

    /**
     * 비밀번호 일치 여부 확인.
     *
     * @param password 입력한 비밀번호
     * @return 일치하면 true
     */
    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    /**
     * 학적 정보 설정.
     * 최초 로그인 후 학적 정보 입력 시 호출.
     * 완료 후 isFirstLogin을 false로 변경.
     *
     * @param grade           학년
     * @param college         단과대학명
     * @param department      학과명
     * @param deptCode        학과코드
     * @param doubleMajorDept 복수전공 학과명
     * @param minorDept       부전공 학과명
     */
    public void setupProfile(int grade, String college, String department,
                             String deptCode, String doubleMajorDept, String minorDept) {
        this.grade = grade;
        this.college = college;
        this.department = department;
        this.deptCode = deptCode;
        this.doubleMajorDept = doubleMajorDept;
        this.minorDept = minorDept;
        this.isFirstLogin = false;
    }

    /**
     * 학적 정보가 완전히 입력됐는지 확인.
     * 알고리즘 실행 전 필수 체크.
     *
     * @return 학과코드까지 입력됐으면 true
     */
    public boolean isProfileComplete() {
        return !isFirstLogin
                && deptCode != null
                && !deptCode.isEmpty();
    }

    // ──────────────────────────────────────────────
    // 파일 저장용 메서드 (UserRepository 전용)
    // ──────────────────────────────────────────────

    /** 비밀번호 반환. UserRepository에서 파일 저장 시에만 사용. */
    String getPasswordForFile() { return password; }

    // ──────────────────────────────────────────────
    // Getter / Setter
    // ──────────────────────────────────────────────

    public String getStudentId() { return studentId; }
    public boolean isFirstLogin() { return isFirstLogin; }
    public int getGrade() { return grade; }
    public String getCollege() { return college; }
    public String getDepartment() { return department; }
    public String getDeptCode() { return deptCode; }
    public String getDoubleMajorDept() { return doubleMajorDept; }
    public String getMinorDept() { return minorDept; }

    @Override
    public String toString() {
        return studentId + " / " + department + " (" + deptCode + ") / " + grade + "학년";
    }
}
