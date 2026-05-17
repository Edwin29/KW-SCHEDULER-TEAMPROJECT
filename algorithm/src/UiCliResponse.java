import java.util.ArrayList;
import java.util.List;

/** --mode json 출력 DTO */
public class UiCliResponse {
    private String status;
    private String message;
    private List<SkeletonOption> skeletonOptions = new ArrayList<>();
    private List<FillerOption> fillerOptions = new ArrayList<>();
    private SkeletonOption finalSchedule;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<SkeletonOption> getSkeletonOptions() { return skeletonOptions; }
    public void setSkeletonOptions(List<SkeletonOption> skeletonOptions) { this.skeletonOptions = skeletonOptions; }

    public List<FillerOption> getFillerOptions() { return fillerOptions; }
    public void setFillerOptions(List<FillerOption> fillerOptions) { this.fillerOptions = fillerOptions; }

    public SkeletonOption getFinalSchedule() { return finalSchedule; }
    public void setFinalSchedule(SkeletonOption finalSchedule) { this.finalSchedule = finalSchedule; }
}
