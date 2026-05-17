import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JSON 파일에서 과목 데이터를 읽어오는 클래스.
 * Python 파싱 결과 JSON 파일을 읽어 Course 객체 리스트로 변환.
 *
 * 한 번 읽은 결과는 캐싱하여 여러 번 호출해도 파일을 한 번만 읽음.
 */
public class CourseRepository {

    /** JSON 파일 경로. 예) "output/courses_2026_1.json" */
    private final String filePath;

    /**
     * 캐시. loadAll() 최초 호출 시 채워지고 이후 재사용.
     * null이면 아직 로드 안 된 상태.
     */
    private List<Course> cache = null;

    // ──────────────────────────────────────────────
    // 생성자
    // ──────────────────────────────────────────────

    /**
     * @param filePath JSON 파일 경로
     */
    public CourseRepository(String filePath) {
        this.filePath = filePath;
    }

    // ──────────────────────────────────────────────
    // 핵심 메서드
    // ──────────────────────────────────────────────

    /**
     * 전체 과목 목록 로드.
     * 최초 호출 시 파일을 읽어 캐시에 저장.
     * 이후 호출은 캐시 반환.
     *
     * @return 전체 Course 리스트
     */
    public List<Course> loadAll() {
        if (cache == null) {
            cache = readFromFile();
        }
        return new ArrayList<>(cache); // 외부에서 캐시 직접 수정 방지
    }

    /**
     * 특정 학과의 전공 과목만 반환.
     * 이수구분이 전필 또는 전선이고 학과코드가 일치하는 과목.
     * UI에서 "전공" 탭 표시 시 사용.
     *
     * @param deptCode 학과코드. 예) "I040"
     * @return 해당 학과 전공 과목 리스트
     */
    public List<Course> loadMajorCourses(String deptCode) {
        return loadAll().stream()
                .filter(c -> deptCode.equals(c.getDeptCode()))
                .filter(c -> "전필".equals(c.getCategory()) || "전선".equals(c.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * 타학과 전공 과목 반환.
     * 이수구분이 전필/전선이지만 학과코드가 다른 과목.
     * UI에서 "타전공" 탭 표시 시 사용.
     *
     * @param deptCode 자기 학과코드 (제외할 코드)
     * @return 타학과 전공 과목 리스트
     */
    public List<Course> loadOtherMajorCourses(String deptCode) {
        return loadAll().stream()
                .filter(c -> !deptCode.equals(c.getDeptCode()))
                .filter(c -> "전필".equals(c.getCategory()) || "전선".equals(c.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * 교양 과목만 반환.
     * 이수구분이 교선 또는 교필인 과목.
     * UI에서 "기타" 탭 표시 시 사용.
     *
     * @return 교양 과목 리스트
     */
    public List<Course> loadLiberalArtsCourses() {
        return loadAll().stream()
                .filter(c -> "교선".equals(c.getCategory()) || "교필".equals(c.getCategory()))
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // 내부 메서드
    // ──────────────────────────────────────────────

    /**
     * JSON 파일을 읽어 Course 리스트로 변환.
     * Gson의 SerializedName 어노테이션으로 한글 키 매핑.
     * 파일 읽기 실패 시 빈 리스트 반환.
     *
     * @return Course 리스트
     */
    private List<Course> readFromFile() {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Course>>() {}.getType();

        try (FileReader reader = new FileReader(filePath)) {
            List<Course> courses = gson.fromJson(reader, listType);
            return courses != null ? courses : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("[오류] JSON 파일 읽기 실패: " + filePath);
            System.err.println("       " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
