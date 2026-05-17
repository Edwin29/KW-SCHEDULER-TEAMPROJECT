import java.util.ArrayList;
import java.util.List;

/** UI 전송용 뼈대 시간표 옵션 DTO */
public class SkeletonOption {
    private String scheduleId;
    private int totalCredit;
    private int majorCredit;
    private int satisfiedCount;
    private List<Course> courses;

    public SkeletonOption(String scheduleId, Schedule schedule) {
        this.scheduleId = scheduleId;
        this.totalCredit = schedule.getTotalCredit();
        this.majorCredit = schedule.getMajorCredit();
        this.satisfiedCount = schedule.getSatisfiedCount();
        this.courses = new ArrayList<>(schedule.getCourses());
    }

    public String getScheduleId() { return scheduleId; }
    public int getTotalCredit() { return totalCredit; }
    public int getMajorCredit() { return majorCredit; }
    public int getSatisfiedCount() { return satisfiedCount; }
    public List<Course> getCourses() { return courses; }
}
