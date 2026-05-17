import java.util.ArrayList;
import java.util.List;

/**
 * 소프트 제약조건의 부모 추상 클래스.
 * 사용자가 ON/OFF로 선택하는 선호 조건.
 *
 * 처리 방식에 따라 두 종류로 나뉨:
 *   - 마스킹형: 후보 과목 생성 전에 해당 과목을 아예 제거
 *              (DayOffConstraint, EmptySlotConstraint)
 *   - 평가형:  조합 생성 후 만족 여부를 true/false로 평가
 *              (FirstPeriodConstraint)
 *
 * abstract로 선언하여 직접 생성 불가. 반드시 자식 클래스 사용.
 */
public abstract class SoftConstraint {

    /** 사용자가 이 조건을 활성화했는지 여부. false면 무시. */
    private boolean enabled;

    /**
     * 우선순위. 1~3 사이 값.
     * 평가형 제약에서 그룹 내 정렬 기준으로 사용.
     * 1이 가장 높은 우선순위.
     */
    private int priority;

    protected SoftConstraint(boolean enabled, int priority) {
        this.enabled = enabled;
        this.priority = priority;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
}


// ================================================================

/**
 * 공강 희망 요일 제약조건.
 * 마스킹형: 해당 요일에 수업이 있는 과목을 후보에서 제거.
 * enabled=true이고 desiredDay가 설정된 경우에만 동작.
 */
class DayOffConstraint extends SoftConstraint {

    /** 공강 희망 요일. 예) "금", "수" */
    private String desiredDay;

    public DayOffConstraint(boolean enabled, int priority, String desiredDay) {
        super(enabled, priority);
        this.desiredDay = desiredDay;
    }

    public String getDesiredDay() { return desiredDay; }
    public void setDesiredDay(String desiredDay) { this.desiredDay = desiredDay; }

    @Override
    public String toString() {
        return "공강 희망 요일: " + desiredDay + " (활성화: " + isEnabled() + ")";
    }
}


// ================================================================

/**
 * 비우고 싶은 시간대 제약조건.
 * 마스킹형: 지정한 요일+교시에 수업이 있는 과목을 후보에서 제거.
 * 여러 시간대를 지정할 수 있음.
 * 예) 월요일 5교시, 금요일 5~6교시
 */
class EmptySlotConstraint extends SoftConstraint {

    /** 비우고 싶은 시간대 목록. */
    private List<TimeSlot> desiredEmptySlots;

    public EmptySlotConstraint(boolean enabled, int priority, List<TimeSlot> slots) {
        super(enabled, priority);
        this.desiredEmptySlots = new ArrayList<>();
        if (slots != null) this.desiredEmptySlots.addAll(slots);
    }

    public List<TimeSlot> getDesiredEmptySlots() { return desiredEmptySlots; }
    public void setDesiredEmptySlots(List<TimeSlot> slots) {
        this.desiredEmptySlots = new ArrayList<>();
        if (slots != null) this.desiredEmptySlots.addAll(slots);
    }

    @Override
    public String toString() {
        return "비우고 싶은 시간대: " + desiredEmptySlots.size() + "개 (활성화: " + isEnabled() + ")";
    }
}


// ================================================================

/**
 * 1교시 회피 제약조건.
 * 
 */
class FirstPeriodConstraint extends SoftConstraint {
    private int maxFirstPeriod;
    public FirstPeriodConstraint(boolean enabled, int priority, int maxFirstPeriod) {
        super(enabled, priority);
        this.maxFirstPeriod = maxFirstPeriod;
    }
    public int getMaxFirstPeriod() { return maxFirstPeriod; }
}