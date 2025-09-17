package nz.ac.auckland;


import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;

import netscape.javascript.JSObject;

import java.util.ArrayList;
import java.util.List;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.se283.GptClient;
import nz.ac.auckland.se283.prompts.PromptEngineering;


public class App {

    public static void main(String[] args) throws Exception {
        
        GptClient client = new GptClient();


        JSONParser parser = new JSONParser();
        FileReader input_file = new FileReader("src\\main\\resources\\input\\input.json");
        JSONArray input_json = (JSONArray) parser.parse(input_file);
        for (Object obj: input_json) {
            JSONObject userStory = (JSONObject) obj;
            String input_content = String.format("%s: %s", userStory.get("US_ID"), userStory.get("US_text"));
    
            String systemPrompt = PromptEngineering.getPrompt("prompt");
    
            // System.out.println(systemPrompt);
    
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", systemPrompt));
            messages.add(
                new ChatMessage("user", input_content));
    
            ChatCompletionResult result = client.runOnce(messages, 1, 1, 1.0, 2000);
            String response = result.getFirstChoice().getChatMessage().getContent();
            System.out.println(response);
      
        }
    }
}