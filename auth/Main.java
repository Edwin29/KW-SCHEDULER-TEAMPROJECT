import java.util.*;

/**
 * 전체 프로그램 실행 진입점.
 * 회원기능(auth)과 알고리즘(algorithm)을 연동.
 *
 * 흐름:
 * 1. 로그인 / 회원가입
 * 2. 학적 정보 설정 (최초 로그인 시)
 * 3. 시간표 생성 조건 입력
 * 4. 후보 과목 선택
 * 5. 시간표 추천 결과 출력
 */
public class Main {

    // JSON 파일 경로 (Python 파트 output 폴더)
    private static final String COURSES_JSON_PATH = "../data/output/courses_2026_1.json";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        LoginService loginService = new LoginService();

        System.out.println("\n========================================");
        System.out.println("  광운대 스마트 시간표 시스템");
        System.out.println("========================================");

        // ── 1단계: 로그인 ─────────────────────────────
        String studentId = login(scanner, loginService);
        if (studentId == null) {
            scanner.close();
            return;
        }

        // ── 2단계: 학적 정보 설정 (최초 로그인 시) ────
        if (!loginService.isProfileComplete(studentId)) {
            System.out.println("\n⚠️  학적 정보가 없습니다. 먼저 학적 정보를 입력해주세요.");
            setupProfile(scanner, loginService, studentId);
        }

        // ── 3단계: 메인 메뉴 ──────────────────────────
        runMainMenu(scanner, loginService, studentId);
        scanner.close();
    }

    // ──────────────────────────────────────────────
    // 1단계: 로그인
    // ──────────────────────────────────────────────

    /**
     * 로그인 처리.
     * 미등록 학번이면 회원가입 유도.
     *
     * @return 로그인 성공한 학번, 종료 시 null
     */
    private static String login(Scanner scanner, LoginService loginService) {
        while (true) {
            System.out.print("\n▶ 학번 입력 ('exit' 입력 시 종료): ");
            String studentId = scanner.nextLine().trim();

            if (studentId.equalsIgnoreCase("exit")) {
                System.out.println("프로그램을 종료합니다.");
                return null;
            }

            System.out.print("▶ 비밀번호 입력: ");
            String password = scanner.nextLine().trim();

            LoginService.LoginResult result = loginService.authenticate(studentId, password);

            switch (result) {
                case SUCCESS:
                    System.out.println("\n✅ 로그인 성공! 환영합니다.");
                    return studentId;

                case USER_NOT_FOUND:
                    // 미등록 학번 → 회원가입 유도
                    System.out.println("\n💡 등록되지 않은 학번입니다.");
                    System.out.print("   입력하신 정보로 가입하시겠습니까? (Y/N): ");
                    String answer = scanner.nextLine().trim();

                    if (answer.equalsIgnoreCase("Y")) {
                        if (loginService.register(studentId, password)) {
                            System.out.println("🎉 가입 완료! 자동으로 로그인됩니다.");
                            return studentId;
                        } else {
                            System.out.println("❌ 가입 실패. 다시 시도해주세요.");
                        }
                    }
                    break;

                case WRONG_PASSWORD:
                    System.out.println("❌ 비밀번호가 틀렸습니다. 다시 시도해주세요.");
                    break;
            }
        }
    }

    // ──────────────────────────────────────────────
    // 2단계: 학적 정보 설정
    // ──────────────────────────────────────────────

    /**
     * 학적 정보 입력 받아 저장.
     * 최초 로그인 시 또는 정보 수정 시 호출.
     */
    private static void setupProfile(Scanner scanner, LoginService loginService, String studentId) {
        System.out.println("\n--- 학적 정보 입력 ---");

        // 학년
        int grade = 0;
        while (true) {
            System.out.print("학년 (1~4): ");
            try {
                grade = Integer.parseInt(scanner.nextLine().trim());
                if (grade >= 1 && grade <= 4) break;
                System.out.println("  ⚠️ 1~4 사이 숫자를 입력하세요.");
            } catch (NumberFormatException e) {
                System.out.println("  ⚠️ 숫자로 입력해주세요.");
            }
        }

        System.out.print("단과대학명: ");
        String college = scanner.nextLine().trim();

        System.out.print("학과명: ");
        String department = scanner.nextLine().trim();

        // 학과코드 - CourseRepository 필터링에 필수
        System.out.print("학과코드 (예: I040, I020, 5080): ");
        String deptCode = scanner.nextLine().trim();

        System.out.print("복수전공 학과명 (없으면 엔터): ");
        String doubleMajor = scanner.nextLine().trim();

        System.out.print("부전공 학과명 (없으면 엔터): ");
        String minor = scanner.nextLine().trim();

        if (loginService.saveProfile(studentId, grade, college, department,
                deptCode, doubleMajor, minor)) {
            System.out.println("✅ 학적 정보 저장 완료!");
        } else {
            System.out.println("❌ 저장 실패.");
        }
    }

    // ──────────────────────────────────────────────
    // 3단계: 메인 메뉴
    // ──────────────────────────────────────────────

    private static void runMainMenu(Scanner scanner, LoginService loginService, String studentId) {
        while (true) {
            System.out.println("\n----------------------------------------");
            System.out.println("  [ 메인 메뉴 ]");
            System.out.println("  1. 시간표 추천받기");
            System.out.println("  2. 내 프로필 확인");
            System.out.println("  3. 학적 정보 수정");
            System.out.println("  4. 로그아웃");
            System.out.println("----------------------------------------");
            System.out.print("▶ 번호 선택: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    // 학적 정보 완성 여부 재확인
                    if (!loginService.isProfileComplete(studentId)) {
                        System.out.println("⚠️  시간표 추천을 위해 먼저 학적 정보를 입력해주세요.");
                        setupProfile(scanner, loginService, studentId);
                    } else {
                        runScheduleRecommendation(scanner, loginService, studentId);
                    }
                    break;
                case "2":
                    showProfile(loginService, studentId);
                    break;
                case "3":
                    setupProfile(scanner, loginService, studentId);
                    break;
                case "4":
                    System.out.println("\n👋 로그아웃 되었습니다.");
                    return;
                default:
                    System.out.println("⚠️  1~4 중에서 선택해주세요.");
            }
        }
    }

    // ──────────────────────────────────────────────
    // 시간표 추천 플로우 (auth → algorithm 연동)
    // ──────────────────────────────────────────────

    /**
     * 시간표 추천 전체 플로우.
     * 사용자 정보를 auth에서 가져와 algorithm에 전달.
     *
     * @param studentId 로그인된 학번
     */
    private static void runScheduleRecommendation(Scanner scanner,
                                                   LoginService loginService,
                                                   String studentId) {
        User user = loginService.getUser(studentId);

        // ── 과목 데이터 로드 ──────────────────────────
        CourseRepository repository = new CourseRepository(COURSES_JSON_PATH);
        List<Course> allCourses = repository.loadAll();

        if (allCourses.isEmpty()) {
            System.out.println("❌ 과목 데이터를 불러올 수 없습니다. JSON 파일 경로를 확인해주세요.");
            return;
        }

        // ── 조건 입력 ─────────────────────────────────
        System.out.println("\n--- 수강 조건 입력 ---");
        HardConstraint hard = inputHardConstraint(scanner);
        List<SoftConstraint> softs = inputSoftConstraints(scanner);
        UserPreference preference = new UserPreference(hard, softs);

        // ── 후보 과목 선택 ────────────────────────────
        // 사용자 학과코드로 전공/타전공/교양 탭 분류
        String deptCode = user.getDeptCode();
        List<Course> candidates = selectCandidates(scanner, repository, deptCode);

        if (candidates.isEmpty()) {
            System.out.println("⚠️  과목을 하나 이상 선택해야 시간표를 생성할 수 있습니다.");
            return;
        }

        // ── 시간표 추천 실행 ──────────────────────────
        System.out.println("\n시간표를 생성하는 중...");
        ScheduleRecommender recommender = new ScheduleRecommender();

        RecommendationRequest request = new RecommendationRequest();
        request.setCatalogPath(COURSES_JSON_PATH);
        request.setDeptCode(deptCode);
        request.setMinCredit(hard.getMinCredit());
        request.setMaxCredit(hard.getMaxCredit());
        request.setAvoidFirstPeriod(preference.getSoftConstraint(FirstPeriodConstraint.class) != null);

        DayOffConstraint dayOff = preference.getSoftConstraint(DayOffConstraint.class);
        if (dayOff != null) request.setDayOff(dayOff.getDesiredDay());

        EmptySlotConstraint emptySlot = preference.getSoftConstraint(EmptySlotConstraint.class);
        if (emptySlot != null) request.setEmptySlots(emptySlot.getDesiredEmptySlots());

        request.setSelectedCourseNames(candidates.stream().map(Course::getName).distinct().collect(java.util.stream.Collectors.toList()));

        RecommendResult result = recommender.recommendFromSelectedCourseNames(
                candidates,
                repository.loadAll(),
                preference
        );

        // ── 결과 출력 + 사용자 선택 ─────────────────────
        if (!result.isSuccess()) {
            System.out.println("\n" + result.getMessage());
            return;
        }

        Schedule selectedSkeleton = chooseSkeletonSchedule(scanner, result);
        if (selectedSkeleton == null) return;

        runFillerSelection(scanner, selectedSkeleton, allCourses, hard, deptCode);

        System.out.println("\n[최종 시간표]");
        printSingleSchedule(selectedSkeleton, hard);
    }

    /**
     * 하드 제약 입력 받기.
     * 최소/최대 학점 범위.
     */
    private static HardConstraint inputHardConstraint(Scanner scanner) {
        System.out.println("\n[필수 조건]");

        int minCredit = 12, maxCredit = 19;

        System.out.print("최소 학점 (기본 12): ");
        String minInput = scanner.nextLine().trim();
        if (!minInput.isEmpty()) {
            try { minCredit = Integer.parseInt(minInput); }
            catch (NumberFormatException e) { System.out.println("  입력 오류. 기본값(12) 사용."); }
        }

        System.out.print("최대 학점 (기본 19, 평점 3.5 이상이면 22): ");
        String maxInput = scanner.nextLine().trim();
        if (!maxInput.isEmpty()) {
            try { maxCredit = Integer.parseInt(maxInput); }
            catch (NumberFormatException e) { System.out.println("  입력 오류. 기본값(19) 사용."); }
        }

        HardConstraint hard = new HardConstraint(minCredit, maxCredit);
        System.out.print("건물 이동 제약(누리관-원거리 연강 금지)을 적용할까요? (기본 Y, Y/N): ");
        String move = scanner.nextLine().trim();
        if (!move.isEmpty()) hard.setEnforceBuildingMove(move.equalsIgnoreCase("Y"));
        return hard;
    }

    /**
     * 소프트 제약 입력 받기.
     * 공강 요일, 비우기 시간대, 1교시 회피.
     */
    private static List<SoftConstraint> inputSoftConstraints(Scanner scanner) {
        List<SoftConstraint> softs = new ArrayList<>();
        System.out.println("\n[선호 조건] (Y/N으로 선택)");

        // 공강 요일 (우선순위 1)
        System.out.print("공강 희망 요일이 있나요? (Y/N): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.print("  공강 희망 요일 입력 (예: 금): ");
            String day = scanner.nextLine().trim();
            if (!day.isEmpty()) {
                softs.add(new DayOffConstraint(true, 1, day));
                System.out.println("  ✅ " + day + "요일 공강 조건 추가");
            }
        }

        // 비우고 싶은 시간대 (우선순위 2)
        System.out.print("비우고 싶은 시간대가 있나요? (Y/N): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("Y")) {
            List<TimeSlot> emptySlots = inputEmptySlots(scanner);
            if (!emptySlots.isEmpty()) {
                softs.add(new EmptySlotConstraint(true, 2, emptySlots));
                System.out.println("  ✅ 비우기 시간대 " + emptySlots.size() + "개 추가");
            }
        }

        // 1교시 회피 (우선순위 3)
        System.out.print("1교시 수업을 피하고 싶나요? (Y/N): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("Y")) {
            softs.add(new FirstPeriodConstraint(true, 3));
            System.out.println("  ✅ 1교시 회피 조건 추가");
        }

        return softs;
    }

    /**
     * 비우고 싶은 시간대 입력 받기.
     * 여러 개 입력 가능.
     */
    private static List<TimeSlot> inputEmptySlots(Scanner scanner) {
        List<TimeSlot> slots = new ArrayList<>();

        while (true) {
            System.out.print("  요일 입력 (예: 월 / 완료하면 엔터): ");
            String day = scanner.nextLine().trim();
            if (day.isEmpty()) break;

            System.out.print("  교시 입력 (예: 5 또는 5,6): ");
            String periodsInput = scanner.nextLine().trim();

            List<Integer> periods = new ArrayList<>();
            try {
                for (String p : periodsInput.split(",")) {
                    periods.add(Integer.parseInt(p.trim()));
                }
                slots.add(new TimeSlot(day, periods));
                System.out.println("  → " + day + "요일 " + periodsInput + "교시 추가됨");
            } catch (NumberFormatException e) {
                System.out.println("  ⚠️ 교시는 숫자로 입력해주세요.");
            }
        }

        return slots;
    }

    /**
     * 후보 과목 선택.
     * 전공/타전공/교양 탭별로 보여주고 사용자가 학정번호로 선택.
     */
    private static List<Course> selectCandidates(Scanner scanner,
                                                   CourseRepository repository,
                                                   String deptCode) {
        List<Course> selected = new ArrayList<>();

        while (true) {
            System.out.println("\n--- 과목 선택 ---");
            System.out.println("  1. 전공 과목 보기");
            System.out.println("  2. 타전공 과목 보기");
            System.out.println("  3. 교양 과목 보기");
            System.out.println("  4. 선택 완료 (" + selected.size() + "개 선택됨)");
            System.out.print("▶ 번호 선택: ");

            String choice = scanner.nextLine().trim();
            List<Course> pool = new ArrayList<>();

            switch (choice) {
                case "1": pool = repository.loadMajorCourses(deptCode); break;
                case "2": pool = repository.loadOtherMajorCourses(deptCode); break;
                case "3": pool = repository.loadLiberalArtsCourses(); break;
                case "4":
                    return selected;
                default:
                    System.out.println("⚠️ 1~4 중에서 선택해주세요.");
                    continue;
            }

            // 과목 목록 출력
            printCourseList(pool);

            // 학정번호로 선택
            System.out.print("\n학정번호 입력 (여러 개면 쉼표 구분, 예: I040-2-7777-01,I040-2-8481-02): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            // 선택된 과목 추가
            Set<String> selectedCodes = new HashSet<>();
            selected.forEach(c -> selectedCodes.add(c.getCourseCode()));

            for (String code : input.split(",")) {
                code = code.trim();
                final String finalCode = code;
                Optional<Course> found = pool.stream()
                        .filter(c -> c.getCourseCode().equals(finalCode))
                        .findFirst();

                if (found.isPresent()) {
                    if (!selectedCodes.contains(finalCode)) {
                        selected.add(found.get());
                        selectedCodes.add(finalCode);
                        System.out.println("  ✅ " + found.get().getName() + " 추가됨");
                    } else {
                        System.out.println("  ⚠️ " + finalCode + " 이미 선택된 과목입니다.");
                    }
                } else {
                    System.out.println("  ⚠️ " + finalCode + " 목록에 없는 학정번호입니다.");
                }
            }
        }
    }

    // ──────────────────────────────────────────────
    // 출력 메서드
    // ──────────────────────────────────────────────

    /** 과목 목록 출력. */
    private static void printCourseList(List<Course> courses) {
        if (courses.isEmpty()) {
            System.out.println("  조회된 과목이 없습니다.");
            return;
        }
        System.out.println("\n  " + courses.size() + "개 과목");
        for (Course c : courses) {
            System.out.printf("  %-25s %-15s %s%n",
                    c.getCourseCode(), c.getName(), c.getCredit() + "학점");
        }
    }

    /** 추천 결과 전체 출력. */
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

                // 빈 시간 추천 과목
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


    private static Schedule chooseSkeletonSchedule(Scanner scanner, RecommendResult result) {
        List<Schedule> ranked = flattenRanked(result);
        if (ranked.isEmpty()) return null;

        System.out.println("\n[4단계] 뼈대 시간표 선택");
        for (int i = 0; i < ranked.size(); i++) {
            Schedule s = ranked.get(i);
            System.out.println("  " + (i + 1) + ") " + s.getTotalCredit() + "학점 / 조건만족 " + s.getSatisfiedCount() + "개");
        }

        while (true) {
            System.out.print("선택할 번호 입력 (취소: 0): ");
            String input = scanner.nextLine().trim();
            try {
                int idx = Integer.parseInt(input);
                if (idx == 0) return null;
                if (idx >= 1 && idx <= ranked.size()) return ranked.get(idx - 1);
            } catch (NumberFormatException ignored) {}
            System.out.println("⚠️ 올바른 번호를 입력해주세요.");
        }
    }

    private static List<Schedule> flattenRanked(RecommendResult result) {
        List<Schedule> ranked = new ArrayList<>();
        for (Map.Entry<Integer, List<Schedule>> entry : result.getGroupedSchedules().entrySet()) {
            ranked.addAll(entry.getValue());
        }
        return ranked;
    }

    private static void runFillerSelection(Scanner scanner, Schedule schedule, List<Course> allCourses, HardConstraint hard, String deptCode) {
        CourseSlotFiller filler = new CourseSlotFiller();

        while (true) {
            int remaining = hard.getMaxCredit() - schedule.getTotalCredit();
            if (remaining <= 0) break;

            Map<String, List<Course>> suggestions = filler.suggestCourses(schedule, allCourses, hard, deptCode);
            List<Course> flat = new ArrayList<>();

            System.out.println("\n[5단계] 추가 과목 선택 (남은 " + remaining + "학점)");
            for (String key : Arrays.asList(CourseSlotFiller.MAJOR, CourseSlotFiller.OTHER_MAJOR, CourseSlotFiller.LIBERAL_ARTS)) {
                List<Course> list = suggestions.getOrDefault(key, Collections.emptyList());
                if (list.isEmpty()) continue;
                System.out.println("  - " + key);
                for (Course c : list.stream().limit(5).collect(java.util.stream.Collectors.toList())) {
                    flat.add(c);
                    System.out.println("    " + flat.size() + ") " + c.getCourseCode() + " / " + c.getName() + " / " + c.getCredit() + "학점");
                }
            }

            if (flat.isEmpty()) {
                System.out.println("추가 가능한 과목이 없습니다.");
                break;
            }

            System.out.print("추가할 번호 입력 (완료: 0): ");
            String input = scanner.nextLine().trim();
            int idx;
            try { idx = Integer.parseInt(input); }
            catch (NumberFormatException e) { System.out.println("⚠️ 숫자를 입력해주세요."); continue; }
            if (idx == 0) break;
            if (idx < 1 || idx > flat.size()) {
                System.out.println("⚠️ 범위를 벗어난 번호입니다.");
                continue;
            }

            Course chosen = flat.get(idx - 1);
            if (filler.canAddCourse(schedule, chosen, hard)) {
                schedule.addCourse(chosen);
                System.out.println("✅ 추가됨: " + chosen.getName());
            } else {
                System.out.println("⚠️ 현재 시간표에 추가할 수 없는 과목입니다.");
            }
        }
    }

    private static void printSingleSchedule(Schedule schedule, HardConstraint hard) {
        System.out.println("총 학점: " + schedule.getTotalCredit() + " / 전공 " + schedule.getMajorCredit());
        for (Course course : schedule.getCourses()) {
            System.out.println("- " + course.getCourseCode() + " " + course.getName() + " (" + course.getCredit() + "학점)");
        }
        System.out.println("남은 학점: " + (hard.getMaxCredit() - schedule.getTotalCredit()));
    }

    /** 프로필 출력. */
    private static void showProfile(LoginService loginService, String studentId) {
        User user = loginService.getUser(studentId);
        System.out.println("\n--- 내 프로필 ---");
        System.out.println("학번:     " + user.getStudentId());
        System.out.println("학년:     " + (user.getGrade() == 0 ? "미설정" : user.getGrade() + "학년"));
        System.out.println("단과대학: " + (user.getCollege() != null ? user.getCollege() : "미설정"));
        System.out.println("학과:     " + (user.getDepartment() != null ? user.getDepartment() : "미설정"));
        System.out.println("학과코드: " + (user.getDeptCode() != null ? user.getDeptCode() : "미설정"));
        System.out.println("복수전공: " + (isNotEmpty(user.getDoubleMajorDept()) ? user.getDoubleMajorDept() : "없음"));
        System.out.println("부전공:   " + (isNotEmpty(user.getMinorDept()) ? user.getMinorDept() : "없음"));
    }

    private static boolean isNotEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}
