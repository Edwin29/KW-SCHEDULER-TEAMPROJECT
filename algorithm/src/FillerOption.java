import java.util.List;

import com.google.gson.annotations.SerializedName;

/** UI 전송용 추가 과목 옵션 DTO */
public class FillerOption {
    private String category;
    private String courseCode;
    private String courseName;
    private int credit;
    private String professor;
    @SerializedName("강의실")
    private List<ClassRoom> classRooms;
    @SerializedName("강의시간")
    private List<TimeSlot> timeSlots;


    public FillerOption(String category, Course course) {
        this.category = category;
        this.courseCode = course.getCourseCode();
        this.courseName = course.getName();
        this.credit = course.getCredit();
        this.professor = course.getProfessor();
        this.classRooms = course.getClassRooms();
        this.timeSlots = course.getTimeSlots();
    }

    public String getCategory() { return category; }
    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public int getCredit() { return credit; }
    public String getProfessor() { return professor; }
    public List<ClassRoom> getClassRooms() { return classRooms; }
    public List<TimeSlot> getTimeSlots() { return timeSlots; }
}
