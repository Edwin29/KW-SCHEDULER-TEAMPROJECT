import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 조건을 통합 보관하는 클래스.
 * HardConstraint와 SoftConstraint 목록을 함께 관리.
 * ScheduleRecommender에 이 객체 하나만 넘기면 모든 조건 접근 가능.
 */
public class UserPreference {

    /** 필수 제약조건 (학점 범위). */
    private HardConstraint hardConstraint;

    /**
     * 선호 제약조건 목록.
     * DayOffConstraint, EmptySlotConstraint, FirstPeriodConstraint 포함.
     * 활성화된 것만 실제로 적용.
     */
    private List<SoftConstraint> softConstraints;

    // ──────────────────────────────────────────────
    // 생성자
    // ──────────────────────────────────────────────

    public UserPreference() {
        this.softConstraints = new ArrayList<>();
    }

    public UserPreference(HardConstraint hardConstraint, List<SoftConstraint> softConstraints) {
        this.hardConstraint = hardConstraint;
        this.softConstraints = new ArrayList<>();
        if (softConstraints != null) this.softConstraints.addAll(softConstraints);
    }

    // ──────────────────────────────────────────────
    // 핵심 메서드
    // ──────────────────────────────────────────────

    /**
     * 특정 타입의 소프트 제약을 반환.
     * 없으면 null 반환.
     * 예) getSoftConstraint(DayOffConstraint.class)
     *
     * @param type 찾을 소프트 제약 클래스 타입
     * @return 해당 타입의 SoftConstraint, 없으면 null
     */
    public <T extends SoftConstraint> T getSoftConstraint(Class<T> type) {
        for (SoftConstraint soft : softConstraints) {
            if (type.isInstance(soft)) return type.cast(soft);
        }
        return null;
    }

    /**
     * 활성화된 소프트 제약만 필터링하여 반환.
     * ScheduleRecommender에서 마스킹 및 평가 시 사용.
     *
     * @return 활성화된 SoftConstraint 목록
     */
    public List<SoftConstraint> getEnabledSoftConstraints() {
        List<SoftConstraint> enabled = new ArrayList<>();
        for (SoftConstraint soft : softConstraints) {
            if (soft.isEnabled()) enabled.add(soft);
        }
        return enabled;
    }

    /**
     * 소프트 제약 하나를 추가.
     *
     * @param soft 추가할 소프트 제약
     */
    public void addSoftConstraint(SoftConstraint soft) {
        if (soft != null) softConstraints.add(soft);
    }

    // ──────────────────────────────────────────────
    // Getter / Setter
    // ──────────────────────────────────────────────

    public HardConstraint getHardConstraint() { return hardConstraint; }
    public void setHardConstraint(HardConstraint hardConstraint) {
        this.hardConstraint = hardConstraint;
    }

    public List<SoftConstraint> getSoftConstraints() { return softConstraints; }
    public void setSoftConstraints(List<SoftConstraint> softConstraints) {
        this.softConstraints = new ArrayList<>();
        if (softConstraints != null) this.softConstraints.addAll(softConstraints);
    }

    @Override
    public String toString() {
        return "UserPreference{hard=" + hardConstraint +
               ", softConstraints=" + softConstraints + "}";
    }
}
