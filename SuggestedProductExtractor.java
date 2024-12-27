import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import org.json.JSONArray;

public class SuggestedProductExtractor {

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

    private static String formatSerpApiResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            StringBuilder formatted = new StringBuilder();
            formatted.append("Top 10 Product Suggestions:\n\n");

            if (jsonObject.has("organic_results")) {
                JSONArray products = jsonObject.getJSONArray("organic_results");
                for (int i = 0; i < Math.min(products.length(), 10); i++) {
                    JSONObject product = products.getJSONObject(i);
                    if (product.has("title")) {
                        formatted.append(i + 1).append(") ").append(product.getString("title")).append("\n");
                    }
                    if (product.has("link")) {
                        formatted.append("   Link: ").append(product.getString("link")).append("\n");
                    }
                    if (product.has("price")) {
                        formatted.append("   Price: ").append(product.getString("price")).append("\n");
                    }
                    if (product.has("rating")) {
                        formatted.append("   Rating: ").append(product.getDouble("rating")).append("\n");
                    }
                    if (product.has("reviews")) {
                        formatted.append("   Reviews: ").append(product.getInt("reviews")).append("\n");
                    }
                    formatted.append("\n");
                }
            }

            return formatted.toString();
        } catch (Exception e) {
            return "Error parsing JSON: " + e.getMessage();
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Walmart Product Search");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 600);

            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(3, 1));

            JLabel queryLabel = new JLabel("Enter Search Query:");
            JTextField queryField = new JTextField();
            JButton submitButton = new JButton("Submit");

            JTextArea outputArea = new JTextArea();
            outputArea.setEditable(false);

            submitButton.addActionListener(e -> {
                String searchQuery = queryField.getText();
                if (searchQuery.isEmpty()) {
                    outputArea.setText("Please enter a search query.");
                    return;
                }

                String formattedQuery = searchQuery.replace(" ", "+");
                String serpApiEndpoint = "https://serpapi.com/search";
                String serpApiKey = "14160d0c23e488116db65285e3c14397b55d5759b6443b65e7937540bfb42caa";

                try {
                    String serpApiResponse = callSerpApi(serpApiEndpoint, serpApiKey, formattedQuery);
                    String formattedSerpResponse = formatSerpApiResponse(serpApiResponse);
                    outputArea.setText(formattedSerpResponse);

                    String sentimentResponse = callSentimentAPI(serpApiResponse);
                    String formattedSentimentResponse = formatJsonResponse(sentimentResponse);
                    outputArea.append("\n" + formattedSentimentResponse);
                } catch (Exception ex) {
                    outputArea.setText("Error: " + ex.getMessage());
                }
            });

            panel.add(queryLabel);
            panel.add(queryField);
            panel.add(submitButton);
            frame.add(panel, BorderLayout.NORTH);
            frame.add(new JScrollPane(outputArea), BorderLayout.CENTER);

            frame.setVisible(true);
        });
    }

    private static String formatJsonResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            StringBuilder formatted = new StringBuilder();
            formatted.append("Best Suggestion:\n\n");

            if (jsonObject.has("candidates")) {
                JSONArray candidates = jsonObject.getJSONArray("candidates");
                for (int i = 0; i < candidates.length(); i++) {
                    JSONObject candidate = candidates.getJSONObject(i);
                    if (candidate.has("content")) {
                        JSONObject content = candidate.getJSONObject("content");
                        JSONArray parts = content.getJSONArray("parts");
                        for (int j = 0; j < parts.length(); j++) {
                            JSONObject part = parts.getJSONObject(j);
                            formatted.append(part.getString("text")).append("\n\n");
                        }
                    }
                }
            }

            return formatted.toString();
        } catch (Exception e) {
            return "Error parsing JSON: " + e.getMessage();
        }
    }
}
