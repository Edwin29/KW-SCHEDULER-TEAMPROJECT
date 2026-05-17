import java.util.*;
import java.util.stream.Collectors;

/**
 * UI 팀과 연동하기 위한 응답 구조 서비스.
 * 콘솔 출력 대신 식별자 기반 목록을 제공한다.
 */
public class UiCompatibilityService {

    public List<Course> listSelectableCourses(CourseRepository repository, String deptCode) {
        List<Course> all = new ArrayList<>();
        all.addAll(repository.loadMajorCourses(deptCode));
        all.addAll(repository.loadOtherMajorCourses(deptCode));
        all.addAll(repository.loadLiberalArtsCourses());
        return all;
    }

    public List<SkeletonOption> recommendSkeletonOptions(
            List<Course> selectedCourses,
            List<Course> candidatePool,
            UserPreference preference
    ) {
        ScheduleRecommender recommender = new ScheduleRecommender();
        RecommendResult result = recommender.recommendFromSelectedCourseNames(selectedCourses, candidatePool, preference);
        if (!result.isSuccess()) return Collections.emptyList();

        List<Schedule> ranked = new ArrayList<>();
        for (Map.Entry<Integer, List<Schedule>> entry : result.getGroupedSchedules().entrySet()) {
            ranked.addAll(entry.getValue());
        }

        List<SkeletonOption> options = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            options.add(new SkeletonOption("SKEL-" + (i + 1), ranked.get(i)));
        }
        return options;
    }

    public List<FillerOption> recommendFillerOptions(
            Schedule selectedSkeleton,
            List<Course> allCourses,
            HardConstraint hard,
            String deptCode,
            UserPreference preference   // 추가 — 공강 요일 등 소프트 제약 반영
    ) {
        CourseSlotFiller filler = new CourseSlotFiller();
        List<CourseSlotFiller.ScoredCourse> ranked = filler.suggestRankedCourses(
                selectedSkeleton, allCourses, hard, deptCode, preference);

        return ranked.stream()
                .map(sc -> new FillerOption(sc.getCategory(), sc.getCourse()))
                .collect(Collectors.toList());
    }
}
