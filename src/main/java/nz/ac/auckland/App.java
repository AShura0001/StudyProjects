package nz.ac.auckland;

import java.util.ArrayList;
import java.util.List;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.se283.GptClient;
import nz.ac.auckland.se283.prompts.PromptEngineering;


public class App {

    public static void main(String[] args) throws Exception {
        
        GptClient client = new GptClient();

        String input_content = "As a Manny’s food service customer, I want to see different food item types displayed in different colors—RGB = #FF0000 for meats, #A52AFA for grains, and #808000 for vegetables and fruits, so that I can quickly identify my food items by food type.";

        String systemPrompt = PromptEngineering.getPrompt("prompt");

        System.out.println(systemPrompt);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", systemPrompt));
        messages.add(
            new ChatMessage("user", input_content));

        ChatCompletionResult result = client.runOnce(messages, 1, 0.2, 1.0, 2000);
        String response = result.getFirstChoice().getChatMessage().getContent();
        System.out.println(response);
  
    }
}
