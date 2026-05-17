import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 사용자 데이터를 CSV 파일로 읽고 쓰는 클래스.
 * 데이터 접근 계층 (Data Access Layer).
 *
 * CSV 형식:
 * 학번|비밀번호|최초로그인|학년|단과대|학과|학과코드|복수전공|부전공
 */
public class UserRepository {

    private static final String FILE_PATH = "users.csv";

    /** 필드 구분자. 학번에 쉼표가 포함될 수 있어 파이프(|) 사용. */
    private static final String DELIMITER = "|";
    private static final String DELIMITER_REGEX = "\\|";

    /** CSV 필드 수. 변경 시 parseLine()도 함께 수정 필요. */
    private static final int FIELD_COUNT = 9;

    // ──────────────────────────────────────────────
    // 핵심 메서드
    // ──────────────────────────────────────────────

    /**
     * CSV 파일에서 전체 사용자 로드.
     * 파일이 없으면 빈 맵 반환 (기본 계정 없음).
     *
     * @return 학번 → User 맵
     */
    public Map<String, User> loadAll() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return new LinkedHashMap<>();

        Map<String, User> userMap = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // 빈 줄, 주석 줄 건너뜀
                if (line.isEmpty() || line.startsWith("#")) continue;

                User user = parseLine(line);
                if (user != null) userMap.put(user.getStudentId(), user);
            }

        } catch (IOException e) {
            System.err.println("[오류] 사용자 파일 읽기 실패: " + e.getMessage());
        }

        return userMap;
    }

    /**
     * 전체 사용자 맵을 CSV 파일에 저장.
     * 기존 파일을 덮어씀.
     *
     * @param userMap 저장할 사용자 맵
     */
    public void saveAll(Map<String, User> userMap) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(FILE_PATH, false), StandardCharsets.UTF_8))) {

            // 파일 헤더 (형식 안내)
            writer.write("# 광운대 스마트 시간표 - 사용자 데이터");
            writer.newLine();
            writer.write("# 형식: 학번|비밀번호|최초로그인|학년|단과대|학과|학과코드|복수전공|부전공");
            writer.newLine();

            for (User user : userMap.values()) {
                writer.write(serialize(user));
                writer.newLine();
            }

        } catch (IOException e) {
            System.err.println("[오류] 사용자 파일 저장 실패: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // 내부 메서드
    // ──────────────────────────────────────────────

    /**
     * CSV 한 줄을 User 객체로 변환.
     * 필드 수가 맞지 않거나 파싱 오류 시 null 반환.
     *
     * @param line CSV 한 줄
     * @return User 객체, 실패 시 null
     */
    private User parseLine(String line) {
        String[] f = line.split(DELIMITER_REGEX, -1);
        if (f.length < FIELD_COUNT) return null;

        try {
            // f[0]: 학번, f[1]: 비밀번호
            User user = new User(f[0].trim(), f[1].trim());

            // f[2]: 최초로그인 여부
            boolean firstLogin = Boolean.parseBoolean(f[2].trim());

            // 학적 정보가 입력된 경우 (최초로그인=false, 학과/학과코드 있음)
            if (!firstLogin
                    && !f[5].trim().isEmpty()   // 학과명
                    && !f[6].trim().isEmpty()) { // 학과코드

                int grade = f[3].trim().isEmpty() ? 0 : Integer.parseInt(f[3].trim());
                user.setupProfile(
                        grade,
                        f[4].trim(),    // 단과대
                        f[5].trim(),    // 학과명
                        f[6].trim(),    // 학과코드
                        f[7].trim(),    // 복수전공
                        f[8].trim()     // 부전공
                );
            }

            return user;

        } catch (Exception e) {
            System.err.println("[경고] 파싱 실패 라인 무시: " + line);
            return null;
        }
    }

    /**
     * User 객체를 CSV 한 줄 문자열로 변환.
     *
     * @param user 변환할 User
     * @return CSV 형식 문자열
     */
    private String serialize(User user) {
        return String.join(DELIMITER,
                user.getStudentId(),
                user.getPasswordForFile(),
                String.valueOf(user.isFirstLogin()),
                user.getGrade() == 0 ? "" : String.valueOf(user.getGrade()),
                nullToEmpty(user.getCollege()),
                nullToEmpty(user.getDepartment()),
                nullToEmpty(user.getDeptCode()),
                nullToEmpty(user.getDoubleMajorDept()),
                nullToEmpty(user.getMinorDept())
        );
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
