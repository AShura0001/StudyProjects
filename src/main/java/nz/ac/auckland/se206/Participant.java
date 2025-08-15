package nz.ac.auckland.se206;

public class Participant {

  private final String pRole;
  private final String memory;

  public Participant(String pRole, String memory) {
    this.pRole = pRole;
    this.memory = memory;
  }

  public String getRole() {
    return pRole;
  }

  public String getMemory() {
    return memory;
  }
}
