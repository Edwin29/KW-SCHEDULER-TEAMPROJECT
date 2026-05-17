import java.util.ArrayList;
import java.util.List;

/**
 * UI/외부 모듈과 연동하기 위한 입력 DTO.
 * JSON으로 쉽게 직렬화/역직렬화할 수 있도록 단순 구조로 유지.
 */
public class RecommendationRequest {

    private String catalogPath;
    private String deptCode;
    private List<String> selectedCourseNames;
    private int minCredit;
    private int maxCredit;
    private int maxFirstPeriod;
    private String dayOff;
    private List<TimeSlot> emptySlots;

    public RecommendationRequest() {
        this.catalogPath = "output/courses_2026_1.json";
        this.deptCode = "I040";
        this.selectedCourseNames = new ArrayList<>();
        this.minCredit = 12;
        this.maxCredit = 19;
        this.maxFirstPeriod = 0;
        this.emptySlots = new ArrayList<>();
    }

    public String getCatalogPath() { return catalogPath; }
    public void setCatalogPath(String catalogPath) { this.catalogPath = catalogPath; }

    public String getDeptCode() { return deptCode; }
    public void setDeptCode(String deptCode) { this.deptCode = deptCode; }

    public List<String> getSelectedCourseNames() { return selectedCourseNames; }
    public void setSelectedCourseNames(List<String> names) {
        this.selectedCourseNames = names == null ? new ArrayList<>() : new ArrayList<>(names);
    }
    public int getMinCredit() { return minCredit; }
    public void setMinCredit(int minCredit) { this.minCredit = minCredit; }

    public int getMaxCredit() { return maxCredit; }
    public void setMaxCredit(int maxCredit) { this.maxCredit = maxCredit; }

    public int getMaxFirstPeriod() { return maxFirstPeriod; }
    public void setMaxFirstPeriod(int maxFirstPeriod) { this.maxFirstPeriod = maxFirstPeriod; }

    public String getDayOff() { return dayOff; }
    public void setDayOff(String dayOff) { this.dayOff = dayOff; }

    public List<TimeSlot> getEmptySlots() { return emptySlots; }
    public void setEmptySlots(List<TimeSlot> emptySlots) {
        this.emptySlots = emptySlots == null ? new ArrayList<>() : new ArrayList<>(emptySlots);
    }
}
