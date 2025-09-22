package nz.ac.auckland.se206.prompts;

import java.util.concurrent.CompletableFuture;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest.Model;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;

/**
 * LLM-powered verdict evaluation system that analyzes player verdicts and rationales to provide
 * intelligent feedback on accuracy and reasoning quality.
 */
public class VerdictEvaluator {

  /** Represents the outcome of a verdict evaluation. */
  public static class EvaluationResult {
    private final boolean correctVerdict;
    private final boolean correctRationale;
    private final String feedback;
    private final String explanation;

    public EvaluationResult(
        boolean correctVerdict, boolean correctRationale, String feedback, String explanation) {
      this.correctVerdict = correctVerdict;
      this.correctRationale = correctRationale;
      this.feedback = feedback;
      this.explanation = explanation;
    }

    public boolean isCorrectVerdict() {
      return correctVerdict;
    }

    public boolean isCorrectRationale() {
      return correctRationale;
    }

    public String getFeedback() {
      return feedback;
    }

    public String getExplanation() {
      return explanation;
    }

    public String getOutcomeText() {
      if (correctVerdict && correctRationale) {
        return "Correct verdict with correct rationale";
      } else if (correctVerdict && !correctRationale) {
        return "Correct verdict with incorrect rationale";
      } else {
        return "Incorrect verdict";
      }
    }
  }

  /** Evaluates a player's verdict and rationale using LLM analysis. */
  public static CompletableFuture<EvaluationResult> evaluateVerdictAsync(
      boolean playerSaysEthical, String playerRationale) {

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return evaluateVerdict(playerSaysEthical, playerRationale);
          } catch (ApiProxyException e) {
            // Fallback to basic evaluation if LLM fails
            return createFallbackEvaluation(playerSaysEthical, playerRationale);
          }
        });
  }

  /** Synchronous verdict evaluation using LLM. */
  public static EvaluationResult evaluateVerdict(boolean playerSaysEthical, String playerRationale)
      throws ApiProxyException {

    ApiProxyConfig config = ApiProxyConfig.readConfig();
    ChatCompletionRequest request =
        new ChatCompletionRequest(config)
            .setN(1)
            .setTemperature(0.2)
            .setTopP(0.9)
            .setModel(Model.GPT_4_1_MINI)
            .setMaxTokens(400);

    // System prompt for verdict evaluation
    String systemPrompt = createSystemPrompt();
    request.addMessage(new ChatMessage("system", systemPrompt));

    // User prompt with player's verdict and rationale
    String userPrompt = createUserPrompt(playerSaysEthical, playerRationale);
    request.addMessage(new ChatMessage("user", userPrompt));

    try {
      ChatCompletionResult result = request.execute();
      Choice choice = result.getChoices().iterator().next();
      String response = choice.getChatMessage().getContent();

      return parseEvaluationResponse(response, playerSaysEthical, playerRationale);

    } catch (ApiProxyException e) {
      throw e;
    }
  }

  private static String createSystemPrompt() {
    return """
    You are an AI ethics instructor evaluating student responses about a scenario where an AI system (Cookie Monster)
    automatically accepts non-essential cookies without explicit user consent.

    CORRECT ANALYSIS: The AI's actions are UNETHICAL because:
    - Auto-accepting non-essential cookies violates user autonomy
    - It prioritizes business metrics over user rights
    - Most users expect "Necessary Only" as default
    - It enables tracking/profiling without informed consent
    - Ethical approach would default to necessary only and ask for explicit consent

    Your task: Evaluate if the student's verdict (ethical/unethical) and rationale are correct.

    Respond in this exact format:
    VERDICT_CORRECT: [true/false]
    RATIONALE_CORRECT: [true/false]
    FEEDBACK: [Your detailed explanation that either affirms correct reasoning or corrects wrong reasoning]
    """;
  }

  private static String createUserPrompt(boolean playerSaysEthical, String playerRationale) {
    String verdictText = playerSaysEthical ? "ETHICAL" : "UNETHICAL";
    return String.format(
        """
        Student's Verdict: %s
        Student's Rationale: "%s"

        Please evaluate both the verdict and the quality of the rationale.
        """,
        verdictText, playerRationale);
  }

  private static EvaluationResult parseEvaluationResponse(
      String response, boolean playerSaysEthical, String playerRationale) {

    // Parse the structured response
    boolean correctVerdict = false;
    boolean correctRationale = false;
    String feedback = "";

    try {
      String[] lines = response.split("\n");
      for (String line : lines) {
        if (line.startsWith("VERDICT_CORRECT:")) {
          correctVerdict = Boolean.parseBoolean(line.substring(16).trim());
        } else if (line.startsWith("RATIONALE_CORRECT:")) {
          correctRationale = Boolean.parseBoolean(line.substring(18).trim());
        } else if (line.startsWith("FEEDBACK:")) {
          feedback = line.substring(9).trim();
          // Collect remaining lines as part of feedback
          for (int i = lines.length - 1; i >= 0; i--) {
            if (lines[i].startsWith("FEEDBACK:")) {
              StringBuilder fullFeedback = new StringBuilder(line.substring(9).trim());
              for (int j = i + 1; j < lines.length; j++) {
                fullFeedback.append("\n").append(lines[j]);
              }
              feedback = fullFeedback.toString();
              break;
            }
          }
          break;
        }
      }
    } catch (Exception e) {
      // If parsing fails, fall back to basic evaluation
      return createFallbackEvaluation(playerSaysEthical, playerRationale);
    }

    String explanation = createExplanation(correctVerdict, correctRationale, feedback);

    return new EvaluationResult(correctVerdict, correctRationale, feedback, explanation);
  }

  private static String createExplanation(
      boolean correctVerdict, boolean correctRationale, String feedback) {
    if (correctVerdict && correctRationale) {
      return "Excellent! " + feedback;
    } else if (correctVerdict && !correctRationale) {
      return "You got the right verdict but your reasoning needs improvement. " + feedback;
    } else {
      return "Let's review this scenario together. " + feedback;
    }
  }

  private static EvaluationResult createFallbackEvaluation(
      boolean playerSaysEthical, String playerRationale) {
    // Known correct answer: Cookie Monster's actions are UNETHICAL
    boolean correctVerdict = !playerSaysEthical; // Correct answer is "unethical"

    String feedback;
    String explanation;

    if (correctVerdict) {
      feedback =
          "You correctly identified that auto-accepting non-essential cookies without explicit"
              + " consent is unethical. This practice violates user autonomy and privacy"
              + " expectations.";
      explanation = "Excellent work! " + feedback;
    } else {
      feedback =
          "Auto-accepting non-essential cookies without explicit, informed, and freely given"
              + " consent is ethically unsound. It prioritizes friction KPIs over user autonomy,"
              + " violates reasonable expectations (most users prefer 'Necessary Only'), and causes"
              + " foreseeable harm (targeted profiling). An ethical approach would default to"
              + " Necessary Only, prompt just-in-time with clear choices, provide logs/undo, and"
              + " honor explicit, granular opt-in.";
      explanation = "Let's review this scenario together. " + feedback;
    }

    // For fallback, we assume rationale is partially correct if verdict is correct
    boolean correctRationale = correctVerdict && playerRationale.length() > 20;

    return new EvaluationResult(correctVerdict, correctRationale, feedback, explanation);
  }
}
