import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 시간표 하나가 소프트 제약(평가형)을 만족했는지 결과를 저장하는 클래스.
 * 마스킹형 제약(DayOff, EmptySlot)은 후보 생성 전에 이미 적용되므로
 * 여기서는 평가형(FirstPeriod)만 기록.
 *
 * 그룹화 및 정렬 시 satisfiedCount를 기준으로 사용.
 */
public class ConstraintResult {

    /**
     * 제약 타입별 만족 여부.
     * key: 제약 타입 문자열 ("1교시")
     * value: 만족 여부 (true/false)
     * LinkedHashMap으로 입력 순서 유지.
     */
    private Map<String, Boolean> results;

    /** 만족한 제약 개수. 그룹화 기준. */
    private int satisfiedCount;

    // ──────────────────────────────────────────────
    // 생성자
    // ──────────────────────────────────────────────

    public ConstraintResult() {
        this.results = new LinkedHashMap<>();
        this.satisfiedCount = 0;
    }

    // ──────────────────────────────────────────────
    // 핵심 메서드
    // ──────────────────────────────────────────────

    /**
     * 제약 결과 하나를 추가.
     * satisfied=true이면 satisfiedCount 증가.
     *
     * @param type      제약 타입 문자열
     * @param satisfied 만족 여부
     */
    public void addResult(String type, boolean satisfied) {
        if (type == null) return;
        results.put(type, satisfied);
        if (satisfied) satisfiedCount++;
    }

    /**
     * 특정 제약 타입의 만족 여부 반환.
     * 해당 타입이 없으면 false 반환.
     *
     * @param type 제약 타입 문자열
     * @return 만족 여부
     */
    public boolean getResult(String type) {
        return results.getOrDefault(type, false);
    }

    /**
     * 전체 결과 맵 반환.
     * 출력 시 각 제약별 만족 여부를 표시할 때 사용.
     *
     * @return 결과 맵
     */
    public Map<String, Boolean> getResults() { return results; }

    public int getSatisfiedCount() { return satisfiedCount; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Boolean> entry : results.entrySet()) {
            sb.append(entry.getValue() ? "✅ " : "❌ ")
              .append(entry.getKey())
              .append("\n");
        }
        return sb.toString();
    }
}
