import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 실행 진입점.
 *
 * 사용법:
 * 1) 기본 데모 실행
 *    java -cp "src:lib/gson-2.10.1.jar" Main
 *
 * 2) JSON 요청 파일 기반 실행 (콘솔 추천)
 *    java -cp "src:lib/gson-2.10.1.jar" Main request.json
 *
 * 3) JSON CLI 모드 (UI 연동용)
 *    java -cp "src:lib/gson-2.10.1.jar" Main --mode json request.json
 */
public class Main {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
        if (args.length >= 2 && "--mode".equals(args[0]) && "json".equalsIgnoreCase(args[1])) {
            String requestPath = args.length >= 3 ? args[2] : null;
            runJsonMode(requestPath);
            return;
        }
        
        RecommendationRequest request = (args.length > 0)
                ? readRequestFromFile(args[0])
                : buildDefaultRequest();

        runConsoleRecommendation(request);
    }

    private static void runJsonMode(String requestPath) {
        UiCliResponse response = new UiCliResponse();
        try {
            if (requestPath == null) {
                throw new IllegalArgumentException("json 모드에서는 요청 파일 경로가 필요합니다.");
            }

            UiCliRequest cliRequest = readUiCliRequestFromFile(requestPath);
            RecommendationRequest request = cliRequest.getRecommendation();
            if (request == null) {
                throw new IllegalArgumentException("recommendation 필드가 비어 있습니다.");
            }
            validateRequest(request);

            CourseRepository repository = new CourseRepository(request.getCatalogPath());
            List<Course> allCourses = repository.loadAll();
            List<Course> selectedCoreCourses = resolveSelectedCoursesByName(
                    allCourses,
                    request.getSelectedCourseNames()
            );
            if (selectedCoreCourses.isEmpty()) {
                throw new IllegalArgumentException("selectedCourseNames가 데이터에서 매칭되지 않았습니다.");
            }

            HardConstraint hard = new HardConstraint(request.getMinCredit(), request.getMaxCredit());
            UserPreference preference = new UserPreference(hard, buildSoftConstraints(request));

            UiCompatibilityService uiService = new UiCompatibilityService();
            List<SkeletonOption> skeletonOptions =
                    uiService.recommendSkeletonOptions(selectedCoreCourses, allCourses, preference);
            response.setSkeletonOptions(skeletonOptions);

            if (skeletonOptions.isEmpty()) {
                response.setStatus("NO_SKELETON");
                response.setMessage("조건을 만족하는 뼈대 시간표가 없습니다.");
                System.out.println(GSON.toJson(response));
                return;
            }

            ScheduleSelectionRequest selection = cliRequest.getSelection();
            if (selection != null && selection.getScheduleId() != null && !selection.getScheduleId().isBlank()) {
                SkeletonOption chosen = skeletonOptions.stream()
                        .filter(s -> s.getScheduleId().equals(selection.getScheduleId()))
                        .findFirst()
                        .orElse(null);

                if (chosen == null) {
                    throw new IllegalArgumentException("유효하지 않은 scheduleId 입니다.");
                }

                Schedule selected = new Schedule(chosen.getCourses());
                List<FillerOption> fillerOptions =
                 uiService.recommendFillerOptions(selected, allCourses, hard, request.getDeptCode(), preference);
                response.setFillerOptions(fillerOptions);

                if (selection.getAddedCourseCodes() != null && !selection.getAddedCourseCodes().isEmpty()) {
                    CourseSlotFiller filler = new CourseSlotFiller();
                    Map<String, Course> byCode = allCourses.stream()
                            .collect(Collectors.toMap(Course::getCourseCode, c -> c, (a, b) -> a));

                    for (String code : selection.getAddedCourseCodes()) {
                        Course c = byCode.get(code);
                        if (c != null && filler.canAddCourse(selected, c, hard)) {
                            selected.addCourse(c);
                        }
                    }
                    response.setFinalSchedule(new SkeletonOption(chosen.getScheduleId(), selected));
                } else {
                    response.setFinalSchedule(chosen);
                }
            }

            response.setStatus("OK");
            response.setMessage("요청 처리 완료");
        } catch (Exception e) {
            response.setStatus("ERROR");
            response.setMessage(e.getMessage());
        }

        System.out.println(GSON.toJson(response));
    }

    private static UiCliRequest readUiCliRequestFromFile(String filePath) throws IOException {
        String json = Files.readString(Path.of(filePath));
        UiCliRequest req = GSON.fromJson(json, UiCliRequest.class);
        return req != null ? req : new UiCliRequest();
    }

    private static RecommendationRequest readRequestFromFile(String filePath) {
        try {
            String json = Files.readString(Path.of(filePath));
            RecommendationRequest request = GSON.fromJson(json, RecommendationRequest.class);
            if (request == null) {
                throw new IllegalArgumentException("요청 JSON이 비어 있습니다.");
            }
            return request;
        } catch (IOException e) {
            throw new RuntimeException("요청 파일을 읽을 수 없습니다: " + filePath, e);
        }
    }

    private static RecommendationRequest buildDefaultRequest() {
        RecommendationRequest request = new RecommendationRequest();
        request.setCatalogPath("../output/courses_2026_1.json");
        request.setDayOff("금");
        request.setEmptySlots(Collections.singletonList(new TimeSlot("월", Arrays.asList(5))));
        request.setSelectedCourseNames(Arrays.asList(
                "객체지향프로그래밍",
                "데이터베이스",
                "컴퓨터그래픽스"
        ));
        request.setMinCredit(9);
        request.setMaxCredit(19);
        return request;
    }

    private static void runConsoleRecommendation(RecommendationRequest request) {
        validateRequest(request);

        CourseRepository repository = new CourseRepository(request.getCatalogPath());
        List<Course> allCourses = repository.loadAll();
        List<Course> majorPool = repository.loadMajorCourses(request.getDeptCode());

        List<Course> selectedCoreCourses = resolveSelectedCoursesByName(
                majorPool,
                request.getSelectedCourseNames()
        );
        if (selectedCoreCourses.isEmpty()) {
            throw new IllegalArgumentException("selectedCourseNames가 학과 풀에서 하나도 매칭되지 않았습니다.");
        }

        HardConstraint hard = new HardConstraint(request.getMinCredit(), request.getMaxCredit());
        UserPreference preference = new UserPreference(hard, buildSoftConstraints(request));

        ScheduleRecommender recommender = new ScheduleRecommender();
        RecommendResult result = recommender.recommendFromSelectedCourseNames(
                selectedCoreCourses, majorPool, preference
        );

        if (!result.isSuccess()) {
            System.out.println(result.getMessage());
            return;
        }

        printResult(result, allCourses, hard, request.getDeptCode());
    }

    private static List<Course> resolveSelectedCoursesByName(List<Course> coursePool, List<String> selectedNames) {
        Map<String, List<Course>> byName = coursePool.stream()
                .filter(c -> c.getName() != null)
                .collect(Collectors.groupingBy(Course::getName));

        List<Course> selected = new ArrayList<>();
        for (String name : selectedNames) {
            List<Course> sameName = byName.get(name);
            if (sameName != null && !sameName.isEmpty()) {
                selected.add(sameName.get(0));
            }
        }
        return selected;
    }

    private static List<SoftConstraint> buildSoftConstraints(RecommendationRequest request) {
        List<SoftConstraint> softs = new ArrayList<>();

        if (request.getDayOff() != null && !request.getDayOff().isBlank()) {
            softs.add(new DayOffConstraint(true, 1, request.getDayOff()));
        }
        if (request.getEmptySlots() != null && !request.getEmptySlots().isEmpty()) {
            softs.add(new EmptySlotConstraint(true, 2, request.getEmptySlots()));
        }
        softs.add(new FirstPeriodConstraint(request.getMaxFirstPeriod() >= 0, 3, request.getMaxFirstPeriod()));

        return softs;
    }

    private static void validateRequest(RecommendationRequest request) {
        if (request.getMinCredit() > request.getMaxCredit()) {
            throw new IllegalArgumentException("minCredit는 maxCredit보다 클 수 없습니다.");
        }
        if (request.getSelectedCourseNames() == null || request.getSelectedCourseNames().isEmpty()) {
            throw new IllegalArgumentException("selectedCourseNames는 최소 1개 이상이어야 합니다.");
        }
    }

    private static void printResult(RecommendResult result,
                                    List<Course> allCourses,
                                    HardConstraint hard,
                                    String deptCode) {
        CourseSlotFiller filler = new CourseSlotFiller();
        int rank = 1;
        System.out.println("\n총 " + result.getTotalCount() + "개 시간표 추천\n");
        for (Map.Entry<Integer, List<Schedule>> entry : result.getGroupedSchedules().entrySet()) {
            int satisfiedCount = entry.getKey();
            List<Schedule> group = entry.getValue();

            System.out.println("══════════════════════════════════════");
            System.out.println("[" + satisfiedCount + "개 조건 만족] " + group.size() + "개 시간표");
            System.out.println("══════════════════════════════════════");

            for (Schedule schedule : group) {
                System.out.println("\n▶ 추천 " + rank++ + "위");
                System.out.println("  총 학점: " + schedule.getTotalCredit()
                        + "학점 / 전공: " + schedule.getMajorCredit() + "학점");

                System.out.println("\n  [수강 과목]");
                for (Course course : schedule.getCourses()) {
                    System.out.println("  - " + course.getName()
                            + " / " + course.getProfessor()
                            + " / " + course.getCredit() + "학점");
                    for (TimeSlot slot : course.getTimeSlots()) {
                        System.out.println("      " + slot);
                    }
                }

                System.out.println("\n  [조건 달성]");
                System.out.print(schedule.getConstraintResult());

                int remaining = hard.getMaxCredit() - schedule.getTotalCredit();
                if (remaining > 0) {
                    System.out.println("\n  [빈 시간 추가 가능 과목] 최대 " + remaining + "학점");
                    Map<String, List<Course>> suggestions =
                            filler.suggestCourses(schedule, allCourses, hard, deptCode);

                    for (Map.Entry<String, List<Course>> s : suggestions.entrySet()) {
                        if (!s.getValue().isEmpty()) {
                            System.out.println("  ─ " + s.getKey());
                            s.getValue().stream().limit(3).forEach(c ->
                                    System.out.println("    · " + c.getName()
                                            + " / " + c.getCredit() + "학점"));
                        }
                    }
                }
                System.out.println();
            }
        }
    }
}