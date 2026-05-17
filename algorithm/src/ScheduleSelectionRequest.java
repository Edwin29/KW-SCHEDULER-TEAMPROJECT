import java.util.ArrayList;
import java.util.List;

/**
 * UI 4/5단계 선택 결과를 전달하는 DTO.
 */
public class ScheduleSelectionRequest {
    private String scheduleId;
    private List<String> addedCourseCodes;

    public ScheduleSelectionRequest() {
        this.addedCourseCodes = new ArrayList<>();
    }

    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }

    public List<String> getAddedCourseCodes() { return addedCourseCodes; }
    public void setAddedCourseCodes(List<String> addedCourseCodes) {
        this.addedCourseCodes = addedCourseCodes == null ? new ArrayList<>() : new ArrayList<>(addedCourseCodes);
    }
}
