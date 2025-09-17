package nz.ac.auckland;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.se283.GptClient;
import nz.ac.auckland.se283.prompts.PromptEngineering;


public class App {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        
        System.out.println("Starting application...");
        GptClient client = new GptClient();


        JSONParser parser = new JSONParser();
        FileReader input_file = new FileReader("src\\main\\resources\\input\\input.json");
        JSONArray input_json = (JSONArray) parser.parse(input_file);
        
        String systemPrompt = PromptEngineering.getPrompt("prompt");

        System.out.println("Parsed input size: " + input_json.size());
        JSONArray outputArray = new JSONArray();
        for (Object obj: input_json) {
            // parses input json and transfers to the ChatCompletionRequest object to execute.
            JSONObject userStory = (JSONObject) obj;
            String input_content = String.format("%s: %s", userStory.get("US_ID"), userStory.get("US_text"));

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", systemPrompt));
            messages.add(new ChatMessage("user", input_content));

            ChatCompletionResult result = client.runOnce(messages, 1, 0.2, 1.0, 2000);
            String response = result.getFirstChoice().getChatMessage().getContent();
            System.out.println(response);

            // Store the response in a JSON object
            JSONObject outputObj = new JSONObject();
            outputObj.put("US_ID", userStory.get("US_ID"));
            
            JSONParser innerParser = new JSONParser();
            JSONObject responseObj = (JSONObject) innerParser.parse(response);
            outputObj.put("response", responseObj); // <-- nested object, not string

            outputArray.add(outputObj);
        }

        // Write the output array to target/output/output.json, wrapped in a parent object with key 'results'
        JSONObject outputWrapper = new JSONObject();
        outputWrapper.put("results", outputArray);
        File outFile = new File("target/output/output.json");
        outFile.getParentFile().mkdirs(); // Ensure parent directories exist
        System.out.println("Writing to: " + outFile.getAbsolutePath());

        // Create a Gson instance with pretty printing
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Convert json-simple object to JSON string
        String prettyJson = gson.toJson(outputWrapper);

        try (FileWriter file = new FileWriter("target/output/output.json")) {
            file.write(prettyJson);
            file.flush();
            System.out.println("Pretty JSON written to target/output/output.json");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}