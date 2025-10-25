package nz.ac.auckland.se283.tools;

public interface ApiCaller {

  public String call(Object data, String apiKey);

  public void setModel(String model);

  public void setN(int n);

  public void setTemperature(double temperature);

  public void setTopP(double topP);

  public void setMaxTokens(int maxTokens);

  public void setMode(String mode);

  public void setDataFormat(String format);
}
