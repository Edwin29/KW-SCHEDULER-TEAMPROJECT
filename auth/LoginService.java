import java.util.Map;

/**
 * 로그인, 회원가입, 학적 정보 관리를 담당하는 서비스 클래스.
 * 비즈니스 로직 계층 (Service Layer).
 *
 * UserRepository를 통해 데이터를 읽고 쓰며,
 * 외부(Main)에서는 이 클래스만 사용하면 됨.
 */
public class LoginService {

    private final Map<String, User> userStore;
    private final UserRepository repository;

    // ──────────────────────────────────────────────
    // 로그인 결과 enum
    // ──────────────────────────────────────────────

    /**
     * 로그인 처리 결과.
     * boolean 대신 enum을 사용하여 실패 이유까지 구분.
     */
    public enum LoginResult {
        SUCCESS,            // 로그인 성공
        USER_NOT_FOUND,     // 등록되지 않은 학번
        WRONG_PASSWORD      // 비밀번호 불일치
    }

    // ──────────────────────────────────────────────
    // 생성자
    // ──────────────────────────────────────────────

    public LoginService() {
        this.repository = new UserRepository();
        this.userStore = repository.loadAll();
    }

    // ──────────────────────────────────────────────
    // 핵심 메서드
    // ──────────────────────────────────────────────

    /**
     * 로그인 인증.
     * 학번 존재 여부 → 비밀번호 일치 여부 순서로 검사.
     *
     * @param studentId 학번
     * @param password  비밀번호
     * @return 로그인 결과
     */
    public LoginResult authenticate(String studentId, String password) {
        if (!userStore.containsKey(studentId)) return LoginResult.USER_NOT_FOUND;
        if (!userStore.get(studentId).checkPassword(password)) return LoginResult.WRONG_PASSWORD;
        return LoginResult.SUCCESS;
    }

    /**
     * 신규 회원가입.
     * 이미 존재하는 학번이면 실패.
     *
     * @param studentId 학번
     * @param password  비밀번호
     * @return 가입 성공이면 true
     */
    public boolean register(String studentId, String password) {
        if (userStore.containsKey(studentId)) return false;

        User newUser = new User(studentId, password);
        userStore.put(studentId, newUser);
        repository.saveAll(userStore);
        return true;
    }

    /**
     * 학적 정보 저장.
     * 로그인 후 최초 프로필 설정 또는 정보 수정 시 사용.
     *
     * @param studentId       학번
     * @param grade           학년
     * @param college         단과대학명
     * @param department      학과명
     * @param deptCode        학과코드 (CourseRepository 필터링용)
     * @param doubleMajorDept 복수전공 학과명
     * @param minorDept       부전공 학과명
     * @return 저장 성공이면 true
     */
    public boolean saveProfile(String studentId, int grade, String college,
                                String department, String deptCode,
                                String doubleMajorDept, String minorDept) {
        User user = userStore.get(studentId);
        if (user == null) return false;

        user.setupProfile(grade, college, department, deptCode, doubleMajorDept, minorDept);
        repository.saveAll(userStore);
        return true;
    }

    /**
     * 사용자 정보 반환.
     * 알고리즘 연동 시 학과코드 등을 가져올 때 사용.
     *
     * @param studentId 학번
     * @return User 객체, 없으면 null
     */
    public User getUser(String studentId) {
        return userStore.get(studentId);
    }

    /**
     * 학적 정보 완성 여부 확인.
     * 알고리즘 실행 전 필수 체크.
     *
     * @param studentId 학번
     * @return 학적 정보 완성이면 true
     */
    public boolean isProfileComplete(String studentId) {
        User user = userStore.get(studentId);
        return user != null && user.isProfileComplete();
    }
}
