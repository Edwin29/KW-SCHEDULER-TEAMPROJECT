import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 시간표 추천 결과를 담는 클래스.
 * 단순 List<Schedule> 대신 이 클래스를 반환하여
 * 성공/실패 상태와 안내 메시지를 함께 전달.
 */
public class RecommendResult {

    /**
     * 추천 결과 상태.
     * SUCCESS:              정상 결과 있음
     * NO_VALID_SCHEDULE:   하드 제약 만족하는 조합 없음
     * TOO_MANY_CONSTRAINTS: 마스킹 후 후보 과목이 없음
     */
    public enum Status {
        SUCCESS,
        NO_VALID_SCHEDULE,
        TOO_MANY_CONSTRAINTS
    }

    /** 결과 상태. */
    private final Status status;

    /**
     * 만족 개수 기준으로 그룹화된 시간표.
     * key: 만족한 소프트 제약 개수 (예: 2, 1, 0)
     * value: 해당 그룹의 Schedule 목록
     * SUCCESS일 때만 값이 있음.
     */
    private final Map<Integer, List<Schedule>> groupedSchedules;

    /** 실패 시 사용자에게 보여줄 안내 메시지. */
    private final String message;

    // ──────────────────────────────────────────────
    // 정적 팩토리 메서드
    // ──────────────────────────────────────────────

    /**
     * 성공 결과 생성.
     *
     * @param groupedSchedules 그룹화된 시간표
     * @return SUCCESS 상태의 RecommendResult
     */
    public static RecommendResult success(Map<Integer, List<Schedule>> groupedSchedules) {
        return new RecommendResult(Status.SUCCESS, groupedSchedules, null);
    }

    /**
     * 하드 제약 만족 조합 없음 결과 생성.
     *
     * @return NO_VALID_SCHEDULE 상태의 RecommendResult
     */
    public static RecommendResult noValidSchedule() {
        return new RecommendResult(
                Status.NO_VALID_SCHEDULE,
                new LinkedHashMap<>(),
                "조건을 만족하는 시간표가 없습니다. 학점 범위를 조정해보세요."
        );
    }

    /**
     * 마스킹 후 후보 없음 결과 생성.
     *
     * @return TOO_MANY_CONSTRAINTS 상태의 RecommendResult
     */
    public static RecommendResult tooManyConstraints() {
        return new RecommendResult(
                Status.TOO_MANY_CONSTRAINTS,
                new LinkedHashMap<>(),
                "조건이 너무 많아 시간표를 만들 수 없습니다. 공강 또는 비우기 조건을 줄여보세요."
        );
    }

    // ──────────────────────────────────────────────
    // 생성자 (private - 정적 팩토리 메서드로만 생성)
    // ──────────────────────────────────────────────

    private RecommendResult(Status status,
                             Map<Integer, List<Schedule>> groupedSchedules,
                             String message) {
        this.status = status;
        this.groupedSchedules = groupedSchedules;
        this.message = message;
    }

    // ──────────────────────────────────────────────
    // 핵심 메서드
    // ──────────────────────────────────────────────

    /** 성공 여부 확인. */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * 전체 시간표 수 반환.
     * 모든 그룹의 시간표 수 합계.
     *
     * @return 전체 시간표 수
     */
    public int getTotalCount() {
        return groupedSchedules.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    // ──────────────────────────────────────────────
    // Getter
    // ──────────────────────────────────────────────

    public Status getStatus() { return status; }
    public Map<Integer, List<Schedule>> getGroupedSchedules() { return groupedSchedules; }
    public String getMessage() { return message; }
}
