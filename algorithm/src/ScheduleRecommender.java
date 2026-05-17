import java.util.*;
import java.util.stream.Collectors;

/**
 * 시간표 추천 핵심 알고리즘 클래스.
 *
 * 팀 의도:
 * - 사용자가 2~7개 정도 핵심 과목을 먼저 고른다.
 * - 알고리즘은 그 과목들로 "뼈대 시간표"를 안정적으로 만들고,
 *   이후 빈 시간은 별도 추천(CourseSlotFiller)으로 채우도록 돕는다.
 *
 * 내부 동작:
 * 1. 마스킹 적용 (DayOff, EmptySlot)
 * 2. 사용자 선택 과목들(동일 과목의 분반 후보 포함) 조합 생성
 * 3. 하드 제약 검사
 * 4. 소프트 제약 평가 (FirstPeriod)
 * 5. 그룹화 및 정렬
 */
public class ScheduleRecommender {

    private static final List<String> FAR_FROM_NURI = Arrays.asList("새빛관", "비마관", "참빛관");

    /**
     * 레거시 진입점.
     * 과거 방식(후보 전체 조합)과의 호환을 위해 유지.
     */
    public RecommendResult recommend(List<Course> candidates, UserPreference preference) {
        return recommendFromSelectedCourseNames(candidates, candidates, preference);
    }

    /**
     * 팀 의도에 맞춘 진입점.
     *
     * @param selectedCourses 사용자가 고른 핵심 과목 목록(보통 2~7개)
     * @param candidatePool   분반 대체를 찾을 전체 후보 풀
     * @param preference      사용자 제약 조건
     */
    public RecommendResult recommendFromSelectedCourseNames(
            List<Course> selectedCourses,
            List<Course> candidatePool,
            UserPreference preference) {

        if (selectedCourses == null || selectedCourses.isEmpty()) {
            return RecommendResult.noValidSchedule();
        }

        List<Course> maskedPool = applyMasking(
                candidatePool == null ? selectedCourses : candidatePool,
                preference
        );
        if (maskedPool.isEmpty()) {
            return RecommendResult.tooManyConstraints();
        }

        Map<String, List<Course>> sectionCandidatesByName = buildSectionCandidates(selectedCourses, maskedPool);

        List<Schedule> validSchedules = new ArrayList<>();
        List<String> selectedNames = new ArrayList<>(sectionCandidatesByName.keySet());
        generateSchedulesFromSelectedNames(
                selectedNames,
                sectionCandidatesByName,
                preference.getHardConstraint(),
                0,
                new ArrayList<>(),
                validSchedules
        );

        if (validSchedules.isEmpty()) {
            return RecommendResult.noValidSchedule();
        }

        List<SoftConstraint> enabledSofts = preference.getEnabledSoftConstraints();
        for (Schedule schedule : validSchedules) {
            schedule.setConstraintResult(evaluate(schedule, enabledSofts));
        }

        Map<Integer, List<Schedule>> grouped = groupAndSort(validSchedules, enabledSofts);
        return RecommendResult.success(grouped);
    }

    private Map<String, List<Course>> buildSectionCandidates(List<Course> selectedCourses, List<Course> maskedPool) {
        Map<String, List<Course>> byName = new LinkedHashMap<>();

        for (Course selected : selectedCourses) {
            if (selected == null || selected.getName() == null || selected.getName().isBlank()) continue;

            List<Course> sameNameSections = maskedPool.stream()
                    .filter(c -> selected.getName().equals(c.getName()))
                    .collect(Collectors.toList());

            if (sameNameSections.isEmpty()) {
                // 마스킹으로 사라졌거나 풀에 없으면 원본 1개라도 넣어 실패 원인을 명확히 드러내게 함.
                sameNameSections = Collections.singletonList(selected);
            }

            byName.put(selected.getName(), sameNameSections);
        }

        return byName;
    }

    private List<Course> applyMasking(List<Course> candidates, UserPreference preference) {
        List<Course> result = new ArrayList<>(candidates);

        for (SoftConstraint soft : preference.getEnabledSoftConstraints()) {
            if (soft instanceof DayOffConstraint) {
                String day = ((DayOffConstraint) soft).getDesiredDay();
                result = result.stream().filter(c -> !c.hasClassOnDay(day)).collect(Collectors.toList());
            } else if (soft instanceof EmptySlotConstraint) {
                List<TimeSlot> emptySlots = ((EmptySlotConstraint) soft).getDesiredEmptySlots();
                result = result.stream()
                        .filter(c -> emptySlots.stream().noneMatch(c::overlaps))
                        .collect(Collectors.toList());
            }
        }

        return result;
    }

    private void generateSchedulesFromSelectedNames(
            List<String> selectedNames,
            Map<String, List<Course>> sectionCandidatesByName,
            HardConstraint hard,
            int index,
            List<Course> selected,
            List<Schedule> result) {

        int currentCredit = selected.stream().mapToInt(Course::getCredit).sum();
        if (currentCredit > hard.getMaxCredit()) return;

        if (index == selectedNames.size()) {
            Schedule schedule = new Schedule(selected);
            if (isValidSchedule(schedule, hard)) result.add(schedule);
            return;
        }

        String courseName = selectedNames.get(index);
        List<Course> sectionCandidates = sectionCandidatesByName.getOrDefault(courseName, Collections.emptyList());

        for (Course section : sectionCandidates) {
            if (section == null) continue;
            selected.add(section);
            generateSchedulesFromSelectedNames(selectedNames, sectionCandidatesByName, hard, index + 1, selected, result);
            selected.remove(selected.size() - 1);
        }
    }

    private boolean isValidSchedule(Schedule schedule, HardConstraint hard) {
        return !hasTimeConflict(schedule)
                && (!hard.isEnforceBuildingMove() || !hasImpossibleBuildingMove(schedule))
                && !hasDuplicatedSameCourse(schedule);
    }

    private boolean hasTimeConflict(Schedule schedule) { /* unchanged logic */
        List<Course> courses = schedule.getCourses();
        for (int i = 0; i < courses.size(); i++) {
            for (int j = i + 1; j < courses.size(); j++) {
                for (TimeSlot a : courses.get(i).getTimeSlots()) {
                    for (TimeSlot b : courses.get(j).getTimeSlots()) {
                        if (a.overlaps(b)) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasImpossibleBuildingMove(Schedule schedule) {
        List<Course> courses = schedule.getCourses();
        for (Course a : courses) {
            for (Course b : courses) {
                if (a == b) continue;
                if (!isImpossibleBuildingPair(a, b)) continue;
                for (TimeSlot slotA : a.getTimeSlots()) {
                    for (TimeSlot slotB : b.getTimeSlots()) {
                        if (slotA.isConsecutiveWith(slotB)) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isImpossibleBuildingPair(Course a, Course b) {
        String buildingA = getPrimaryBuilding(a);
        String buildingB = getPrimaryBuilding(b);
        if (buildingA == null || buildingB == null) return false;

        return ("누리관".equals(buildingA) && FAR_FROM_NURI.contains(buildingB))
                || ("누리관".equals(buildingB) && FAR_FROM_NURI.contains(buildingA));
    }

    private String getPrimaryBuilding(Course course) {
        if (course.getClassRooms() == null || course.getClassRooms().isEmpty()) return null;
        return course.getClassRooms().get(0).getBuilding();
    }

    /*private boolean hasDuplicatedLiberalArtsDifficulty(Schedule schedule) {
        Set<String> used = new HashSet<>();
        for (Course course : schedule.getCourses()) {
            if (course.isMajor()) continue;

            String area = course.getLiberalArtsArea();
            String difficulty = course.getDifficulty();
            if (area == null || difficulty == null || "수리와자연".equals(area)) continue;

            String key = area + ":" + difficulty;
            if (used.contains(key)) return true;
            used.add(key);
        }
        return false;
    }*/

    private boolean hasDuplicatedSameCourse(Schedule schedule) {
        Set<String> names = new HashSet<>();
        for (Course course : schedule.getCourses()) {
            String name = course.getName();
            if (name == null) continue;
            if (names.contains(name)) return true;
            names.add(name);
        }
        return false;
    }

    private ConstraintResult evaluate(Schedule schedule, List<SoftConstraint> enabledSofts) {
        ConstraintResult result = new ConstraintResult();
        for (SoftConstraint soft : enabledSofts) {
            if (soft instanceof FirstPeriodConstraint) {
                FirstPeriodConstraint constraint = (FirstPeriodConstraint) soft;  // 캐스팅
                long firstPeriodCount = schedule.getCourses().stream()
                        .flatMap(c -> c.getTimeSlots().stream())
                        .filter(slot -> slot.getPeriods().contains(1))
                        .count();
                result.addResult("1교시 " + constraint.getMaxFirstPeriod() + "회 이하",
                        firstPeriodCount <= constraint.getMaxFirstPeriod());
            }
        }
        return result;
    }

    private Map<Integer, List<Schedule>> groupAndSort(List<Schedule> schedules, List<SoftConstraint> enabledSofts) {
        Map<Integer, List<Schedule>> grouped = new TreeMap<>(Comparator.reverseOrder());
        for (Schedule schedule : schedules) {
            grouped.computeIfAbsent(schedule.getSatisfiedCount(), k -> new ArrayList<>()).add(schedule);
        }
        for (List<Schedule> group : grouped.values()) {
            group.sort((a, b) -> compareByPriority(a, b, enabledSofts));
        }
        return grouped;
    }

    private int compareByPriority(Schedule a, Schedule b, List<SoftConstraint> enabledSofts) {
        List<SoftConstraint> sorted = enabledSofts.stream()
                .filter(s -> s instanceof FirstPeriodConstraint)
                .sorted(Comparator.comparingInt(SoftConstraint::getPriority))
                .collect(Collectors.toList());

        for (SoftConstraint ignored : sorted) {
            boolean aResult = a.getConstraintResult().getResult("1교시 없음");
            boolean bResult = b.getConstraintResult().getResult("1교시 없음");
            if (aResult != bResult) return bResult ? 1 : -1;
        }
        return 0;
    }
}
