/** --mode json 입력 DTO */
public class UiCliRequest {
    private RecommendationRequest recommendation;
    private ScheduleSelectionRequest selection;

    public RecommendationRequest getRecommendation() { return recommendation; }
    public void setRecommendation(RecommendationRequest recommendation) { this.recommendation = recommendation; }

    public ScheduleSelectionRequest getSelection() { return selection; }
    public void setSelection(ScheduleSelectionRequest selection) { this.selection = selection; }
}
