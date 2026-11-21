package team6;

public class TaskNode {
    private String id;
    private int weight;

    // used to track start time and processor number
    private int startTime;
    private int processorNum;

    public TaskNode(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public int getWeight() {
        return this.weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getStartTime() {
        return this.startTime;
    }

    public void setStartTime(int time) {
        this.startTime = time;
    }

    public int getProcessorNum() {
        return this.processorNum;
    }

    public void setProcessorNum(int num) {
        this.processorNum = num;
    }

    @Override
    public String toString() {
        return id;
    }

}
