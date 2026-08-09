package team6;

public class TaskNode {
    private String id;
    private int weight;

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

    @Override
    public String toString() {
        return id;
    }

}
