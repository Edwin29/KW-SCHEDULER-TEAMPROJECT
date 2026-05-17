import com.google.gson.annotations.SerializedName;

/**
 * 강의실 정보를 저장하는 데이터 클래스.
 * Course의 "강의실" 배열 항목 하나에 대응.
 * Python KLAS 스크래핑 결과.
 */
public class ClassRoom {

    /** 요일. 예) "화", "목" */
    @SerializedName("요일")
    private String day;

    /** 건물명. 예) "새빛관", "한울관" */
    @SerializedName("건물")
    private String building;

    /** 강의실 호수. 예) "103", "404" */
    @SerializedName("호수")
    private String roomNumber;

    // ──────────────────────────────────────────────
    // 생성자
    // ──────────────────────────────────────────────

    /** 기본 생성자. Gson 역직렬화 시 사용. */
    public ClassRoom() {}

    public ClassRoom(String day, String building, String roomNumber) {
        this.day = day;
        this.building = building;
        this.roomNumber = roomNumber;
    }

    // ──────────────────────────────────────────────
    // Getter / Setter
    // ──────────────────────────────────────────────

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    @Override
    public String toString() {
        return building + " " + roomNumber + "호 (" + day + "요일)";
    }
}
