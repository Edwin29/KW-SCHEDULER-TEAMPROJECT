/**
 * 필수 제약조건을 저장하는 클래스.
 * 이 조건을 하나라도 위반하면 해당 시간표 조합은 무조건 제외.
 *
 * 학점 범위만 필드로 관리하고,
 * 나머지 하드 제약 (시간충돌, 건물이동, 교양중복, 동일과목)은
 * 로직이므로 ScheduleRecommender에서 처리.
 */
public class HardConstraint {

    /**
     * 최소 수강 학점.
     * 2017학번 이후 기본값: 12학점
     * 졸업예정학기: 6학점
     */
    private int minCredit;

    /**
     * 최대 수강 학점.
     * 2017학번 이후 기본값: 19학점
     * 직전학기 평점 3.5 이상: 22학점
     */
    private int maxCredit;

    /** 건물 이동 제약 사용 여부. */
    private boolean enforceBuildingMove = true;

    // ──────────────────────────────────────────────
    // 생성자
    // ──────────────────────────────────────────────

    public HardConstraint() {}

    public HardConstraint(int minCredit, int maxCredit) {
        this.minCredit = minCredit;
        this.maxCredit = maxCredit;
        this.enforceBuildingMove = true;
    }

    // ──────────────────────────────────────────────
    // 핵심 메서드
    // ──────────────────────────────────────────────

    /**
     * 주어진 학점이 범위 안에 있는지 확인.
     *
     * @param credit 검사할 학점
     * @return 범위 내이면 true
     */
    public boolean isInCreditRange(int credit) {
        return credit >= minCredit && credit <= maxCredit;
    }

    // ──────────────────────────────────────────────
    // Getter / Setter
    // ──────────────────────────────────────────────

    public int getMinCredit() { return minCredit; }
    public void setMinCredit(int minCredit) { this.minCredit = minCredit; }

    public int getMaxCredit() { return maxCredit; }
    public void setMaxCredit(int maxCredit) { this.maxCredit = maxCredit; }

    public boolean isEnforceBuildingMove() { return enforceBuildingMove; }
    public void setEnforceBuildingMove(boolean enforceBuildingMove) { this.enforceBuildingMove = enforceBuildingMove; }

    @Override
    public String toString() {
        return "학점 범위: " + minCredit + " ~ " + maxCredit;
    }
}
