import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WalmartProductSearch {

    private static final String SENTIMENT_API_KEY = "AIzaSyBqIFEWfWPCZ-17rA4XumrJvoccSzoGbD4";

    public static void main(String[] args) {
        String searchQuery = "Chocolate";
        String serpApiEndpoint = "https://serpapi.com/search";
        String serpApiKey = "14160d0c23e488116db65285e3c14397b55d5759b6443b65e7937540bfb42caa";

        try {
            // Fetch product data from SerpApi
            String serpApiResponse = callSerpApi(serpApiEndpoint, serpApiKey, searchQuery);

            System.out.println("Serp Response" +serpApiResponse);
            // Send the entire SerpApi response to the sentiment API
            String sentimentResponse = callSentimentAPI(serpApiResponse);

            //System.out.println("Sentiment Response" +sentimentResponse);
            // Suggest the best product based on sentiment
            String suggestedResult = parseSentimentResponse(sentimentResponse);

            System.out.println("\nSuggested Product: " + suggestedResult);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String callSerpApi(String endpoint, String apiKey, String query) throws Exception {
        String urlWithParams = endpoint + "?engine=walmart&query=" + query + "&api_key=" + apiKey;

        HttpURLConnection connection = (HttpURLConnection) new URL(urlWithParams).openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } else {
            throw new Exception("Failed to fetch data from SerpApi. HTTP Code: " + responseCode);
        }
    }

    private static String callSentimentAPI(String serpApiResponse) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyBqIFEWfWPCZ-17rA4XumrJvoccSzoGbD4";

        // Add prompt text to the end of the API request
        String promptText = "\nAnalyze the following Walmart product search results and provide only the text output with the best product suggestion and its general review.";

        // Construct the JSON request body according to the correct API format
        String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" +
                serpApiResponse.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") +
                promptText.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") +
                "\"}]}]}";

        // Log the request body for debugging
        System.out.println("Request Body: " + requestBody);

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
            outputStream.writeBytes(requestBody);
            outputStream.flush();
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } else {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorResponse.append(line);
            }
            errorReader.close();
            throw new Exception("Failed to call Sentiment API. HTTP Code: " + responseCode + ". Error Response: " + errorResponse);
        }
    }

    private static String parseSentimentResponse(String response) {
        // Simply return the full response without parsing
        return response;
    }
}
