import java.util.*;
import java.util.stream.Collectors;

public class CourseSlotFiller {

    public static final String MAJOR = "전공";
    public static final String OTHER_MAJOR = "타전공";
    public static final String LIBERAL_ARTS = "교양";

    public Map<String, List<Course>> suggestCourses(
            Schedule schedule,
            List<Course> allCourses,
            HardConstraint hard,
            String deptCode) {

        int remainingCredit = hard.getMaxCredit() - schedule.getTotalCredit();

        Set<String> selectedNames = schedule.getCourses().stream()
                .map(Course::getName)
                .collect(Collectors.toSet());

        List<Course> fillable = allCourses.stream()
                .filter(c -> !selectedNames.contains(c.getName()))
                .filter(c -> c.getCredit() <= remainingCredit)
                .filter(c -> !hasConflictWith(schedule, c))
                .collect(Collectors.toList());

        Map<String, List<Course>> result = new LinkedHashMap<>();
        result.put(MAJOR, new ArrayList<>());
        result.put(OTHER_MAJOR, new ArrayList<>());
        result.put(LIBERAL_ARTS, new ArrayList<>());

        for (Course course : fillable) {
            if (deptCode.equals(course.getDeptCode()) && course.isMajor()) {
                result.get(MAJOR).add(course);
            } else if (!deptCode.equals(course.getDeptCode()) && course.isMajor()) {
                result.get(OTHER_MAJOR).add(course);
            } else {
                result.get(LIBERAL_ARTS).add(course);
            }
        }

        return result;
    }


    public boolean canAddCourse(Schedule schedule, Course course, HardConstraint hard) {
        if (schedule == null || course == null || hard == null) return false;

        if (schedule.getCourses().stream().anyMatch(c -> Objects.equals(c.getName(), course.getName()))) {
            return false;
        }

        if (schedule.getTotalCredit() + course.getCredit() > hard.getMaxCredit()) {
            return false;
        }

        return !hasConflictWith(schedule, course);
    }

    // ──────────────────────────────────────────────
    // 내부 메서드
    // ──────────────────────────────────────────────

    /**
     * 팀 합의: 모든 기준을 동일 1점으로 평가.
     */
    public List<ScoredCourse> suggestRankedCourses(
            Schedule schedule,
            List<Course> allCourses,
            HardConstraint hard,
            String deptCode,
            UserPreference preference
    ) {
        int remainingCredit = hard.getMaxCredit() - schedule.getTotalCredit();
        Map<String, List<Course>> grouped = suggestCourses(schedule, allCourses, hard, deptCode);
        String preferredDayOff = null;
        DayOffConstraint dayOff = preference == null ? null : preference.getSoftConstraint(DayOffConstraint.class);
        if (dayOff != null && dayOff.isEnabled()) preferredDayOff = dayOff.getDesiredDay();

        List<ScoredCourse> scored = new ArrayList<>();
        for (String category : Arrays.asList(MAJOR, OTHER_MAJOR, LIBERAL_ARTS)) {
            for (Course c : grouped.getOrDefault(category, Collections.emptyList())) {
                List<String> reasons = new ArrayList<>();
                int score = 0;

                if (c.getCredit() == remainingCredit || (remainingCredit >= 3 && c.getCredit() == 3)) {
                    score++;
                    reasons.add("남은 학점 적합");
                }
                if (preferredDayOff != null && !c.hasClassOnDay(preferredDayOff)) {
                    score++;
                    reasons.add("공강 희망 요일 유지");
                }
                if (!createsLongGap(schedule, c, 3)) {
                    score++;
                    reasons.add("3교시 이상 공강 미발생");
                }
                if (isOnlineCourse(c)) {
                    score++;
                    reasons.add("온라인 강의");
                }

                scored.add(new ScoredCourse(category, c, score, reasons));
            }
        }

        scored.sort(Comparator
                .comparingInt(ScoredCourse::getScore).reversed()
                .thenComparingInt(x -> Math.abs(remainingCredit - x.getCourse().getCredit()))
                .thenComparing(x -> x.getCourse().getCourseCode()));

        return scored;
    }

    
    private boolean createsLongGap(Schedule schedule, Course candidate, int thresholdPeriods) {
        Map<String, List<Integer>> byDay = new HashMap<>();
        for (Course c : schedule.getCourses()) {
            for (TimeSlot t : c.getTimeSlots()) {
                byDay.computeIfAbsent(t.getDay(), k -> new ArrayList<>()).addAll(t.getPeriods());
            }
        }
        for (TimeSlot t : candidate.getTimeSlots()) {
            byDay.computeIfAbsent(t.getDay(), k -> new ArrayList<>()).addAll(t.getPeriods());
        }

        for (List<Integer> periods : byDay.values()) {
            if (periods.size() < 2) continue;
            Collections.sort(periods);
            for (int i = 1; i < periods.size(); i++) {
                int gap = periods.get(i) - periods.get(i - 1) - 1;
                if (gap >= thresholdPeriods) return true;
            }
        }
        return false;
    }

    private boolean isOnlineCourse(Course c) {
        String type = c.getLectureType();
        if (type == null) return false;
        return type.contains("원격") || type.toLowerCase().contains("online");
    }

    private boolean hasConflictWith(Schedule schedule, Course course) {
        for (Course existing : schedule.getCourses()) {
            for (TimeSlot a : existing.getTimeSlots()) {
                for (TimeSlot b : course.getTimeSlots()) {
                    if (a.overlaps(b)) return true;
                }
            }
        }
        return false;
    }

    public static class ScoredCourse {
        private final String category;
        private final Course course;
        private final int score;
        private final List<String> reasons;

        public ScoredCourse(String category, Course course, int score, List<String> reasons) {
            this.category = category;
            this.course = course;
            this.score = score;
            this.reasons = reasons;
        }

        public String getCategory() { return category; }
        public Course getCourse() { return course; }
        public int getScore() { return score; }
        public List<String> getReasons() { return reasons; }
    }
}
