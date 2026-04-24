import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class QuizLeaderboard {

    private static final String BASE_URL = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";
    private static final String REG_NO = "RA2311003010320";
    private static final int TOTAL_POLLS = 10;
    private static final int DELAY = 5000;

    private static final Map<String, Integer> processedEvents = new LinkedHashMap<>();
    private static final Map<String, Integer> results = new HashMap<>();

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        for (int i = 0; i < TOTAL_POLLS; i++) {
            System.out.println("Executing poll " + i + "...");

            String endpoint = BASE_URL + "/quiz/messages?regNo=" + REG_NO + "&poll=" + i;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Response Status: " + response.statusCode());
                handleResponse(response.body());
            } catch (Exception e) {
                System.out.println("Error during poll " + i + ": " + e.getMessage());
            }

            if (i < TOTAL_POLLS - 1) {
                Thread.sleep(DELAY);
            }
        }

        for (Map.Entry<String, Integer> entry : processedEvents.entrySet()) {
            String user = entry.getKey().split("::")[1];
            results.merge(user, entry.getValue(), Integer::sum);
        }

        List<Map.Entry<String, Integer>> leaderboard = new ArrayList<>(results.entrySet());
        leaderboard.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        System.out.println("\n--- Final Leaderboard ---");
        leaderboard.forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));

        submitFinalResults(client, leaderboard);
    }

    private static void handleResponse(String body) {
        int position = 0;
        while (true) {
            int start = body.indexOf("{", position);
            if (start == -1) break;
            int end = body.indexOf("}", start);
            if (end == -1) break;

            String event = body.substring(start, end + 1);
            String round = findValue(event, "roundId");
            String user = findValue(event, "participant");
            String scoreVal = findRawValue(event, "score");

            if (round != null && user != null && scoreVal != null) {
                try {
                    int score = Integer.parseInt(scoreVal.trim());
                    String uniqueKey = round + "::" + user;

                    if (!processedEvents.containsKey(uniqueKey)) {
                        processedEvents.put(uniqueKey, score);
                    }
                } catch (NumberFormatException ignored) {}
            }
            position = end + 1;
        }
    }

    private static void submitFinalResults(HttpClient client, List<Map.Entry<String, Integer>> leaderboard) throws Exception {
        StringBuilder jsonList = new StringBuilder("[");
        for (int i = 0; i < leaderboard.size(); i++) {
            Map.Entry<String, Integer> entry = leaderboard.get(i);
            jsonList.append(String.format("{\"participant\":\"%s\",\"totalScore\":%d}", 
                entry.getKey(), entry.getValue()));
            if (i < leaderboard.size() - 1) jsonList.append(",");
        }
        jsonList.append("]");

        String payload = String.format("{\"regNo\":\"%s\",\"leaderboard\":%s}", REG_NO, jsonList.toString());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/quiz/submit"))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("\nSubmission Status: " + response.statusCode());
        System.out.println("Server Message: " + response.body());
    }

    private static String findValue(String source, String key) {
        String pattern = "\"" + key + "\"";
        int keyIndex = source.indexOf(pattern);
        if (keyIndex == -1) return null;
        int colon = source.indexOf(":", keyIndex);
        int openQuote = source.indexOf("\"", colon);
        int closeQuote = source.indexOf("\"", openQuote + 1);
        return source.substring(openQuote + 1, closeQuote);
    }

    private static String findRawValue(String source, String key) {
        String pattern = "\"" + key + "\"";
        int keyIndex = source.indexOf(pattern);
        if (keyIndex == -1) return null;
        int colon = source.indexOf(":", keyIndex);
        int start = colon + 1;
        while (start < source.length() && (Character.isWhitespace(source.charAt(start)))) start++;
        int end = start;
        while (end < source.length() && (Character.isDigit(source.charAt(end)) || source.charAt(end) == '-')) end++;
        return source.substring(start, end);
    }
}