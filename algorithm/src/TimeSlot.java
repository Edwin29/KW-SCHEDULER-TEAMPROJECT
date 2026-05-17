import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.annotations.SerializedName;

/**
 * 수업 시간 한 블록을 표현하는 클래스.
 * 예) 수요일 8, 9, 10교시
 */
public class TimeSlot {

    // ──────────────────────────────────────────────
    // 교시별 시간 정보 상수
    // 주간 1~6교시 (75분 단위), 야간 7~11교시 (45분 단위)
    // ──────────────────────────────────────────────
    public static final Map<Integer, String> PERIOD_TIME = new LinkedHashMap<>();

    static {
        PERIOD_TIME.put(1,  "09:00~10:15");
        PERIOD_TIME.put(2,  "10:30~11:45");
        PERIOD_TIME.put(3,  "12:00~13:15");
        PERIOD_TIME.put(4,  "13:30~14:45");
        PERIOD_TIME.put(5,  "15:00~16:15");
        PERIOD_TIME.put(6,  "16:30~17:45");
        PERIOD_TIME.put(7,  "18:00~18:45");
        PERIOD_TIME.put(8,  "18:50~19:35");
        PERIOD_TIME.put(9,  "19:40~20:25");
        PERIOD_TIME.put(10, "20:30~21:15");
        PERIOD_TIME.put(11, "21:20~22:05");
    }

    // ──────────────────────────────────────────────
    // 필드
    // ──────────────────────────────────────────────

    /** 요일. 예) "월", "화", "수", "목", "금", "토" */
    @SerializedName("요일")
    private String day;

    /**
     * 교시 목록.
     * JSON의 "교시" 배열을 그대로 저장.
     * 예) [8, 9, 10]
     */
    @SerializedName("교시")
    private List<Integer> periods;

    // ──────────────────────────────────────────────
    // 생성자
    // ──────────────────────────────────────────────

    /** 기본 생성자. JSON 역직렬화 시 사용. */
    public TimeSlot() {
        this.periods = new ArrayList<>();
    }

    /**
     * 요일과 교시 목록을 받는 생성자.
     *
     * @param day     요일 문자열
     * @param periods 교시 번호 리스트
     */
    public TimeSlot(String day, List<Integer> periods) {
        this.day = day;
        this.periods = new ArrayList<>();
        if (periods != null) {
            this.periods.addAll(periods);
        }
    }

    // ──────────────────────────────────────────────
    // 핵심 메서드
    // ──────────────────────────────────────────────

    /**
     * 다른 TimeSlot과 시간이 겹치는지 판단.
     * 요일이 다르면 무조건 false.
     * 같은 요일이면 교시 목록에 공통 번호가 있으면 true.
     *
     * @param other 비교할 TimeSlot
     * @return 겹치면 true
     */
    public boolean overlaps(TimeSlot other) {
        if (other == null) return false;
        if (this.day == null || other.day == null) return false;
        if (!this.day.equals(other.day)) return false;

        // 교시 목록에 공통 번호가 하나라도 있으면 겹침
        for (int period : this.periods) {
            if (other.periods.contains(period)) return true;
        }
        return false;
    }

    /**
     * 다른 TimeSlot과 연강 관계인지 판단.
     * 같은 요일에서 this의 마지막 교시 + 1 == other의 첫 교시면 연강.
     * 예) this: 수 2~3교시, other: 수 4~5교시 → 연강
     *
     * @param other 다음 수업 TimeSlot
     * @return 연강이면 true
     */
    public boolean isConsecutiveWith(TimeSlot other) {
        if (other == null) return false;
        if (this.day == null || other.day == null) return false;
        if (!this.day.equals(other.day)) return false;
        if (this.periods.isEmpty() || other.periods.isEmpty()) return false;

        return this.getEndPeriod() + 1 == other.getStartPeriod();
    }

    /**
     * 시작 교시 반환. periods 리스트의 첫 번째 값.
     *
     * @return 시작 교시 번호
     */
    public int getStartPeriod() {
        if (periods == null || periods.isEmpty()) return 0;
        return periods.get(0);
    }

    /**
     * 종료 교시 반환. periods 리스트의 마지막 값.
     *
     * @return 종료 교시 번호
     */
    public int getEndPeriod() {
        if (periods == null || periods.isEmpty()) return 0;
        return periods.get(periods.size() - 1);
    }

    // ──────────────────────────────────────────────
    // Getter / Setter
    // ──────────────────────────────────────────────

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public List<Integer> getPeriods() { return periods; }
    public void setPeriods(List<Integer> periods) {
        this.periods = new ArrayList<>();
        if (periods != null) this.periods.addAll(periods);
    }

    @Override
    public String toString() {
        return day + "요일 " + getStartPeriod() + "~" + getEndPeriod() + "교시";
    }
}
