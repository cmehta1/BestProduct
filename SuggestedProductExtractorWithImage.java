import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.imageio.ImageIO;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * SuggestedProductExtractor class provides a graphical interface to fetch and display Walmart product suggestions.
 * The class communicates with SerpAPI to fetch product data and with the Sentiment API to analyze product reviews.
 */
public class SuggestedProductExtractorWithImage
{
    /**
     * The main entry point for the application. Creates the UI and handles user interactions.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Walmart Product Search");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);

            JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
            JLabel queryLabel = new JLabel("Enter Search Query:");
            JTextField queryField = new JTextField();
            JButton submitButton = new JButton("Search");

            inputPanel.add(queryLabel, BorderLayout.WEST);
            inputPanel.add(queryField, BorderLayout.CENTER);
            inputPanel.add(submitButton, BorderLayout.EAST);

            JPanel outputPanel = new JPanel();
            outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.Y_AXIS));

            JScrollPane scrollPane = new JScrollPane(outputPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

            submitButton.addActionListener(e -> {
                String searchQuery = queryField.getText().trim();
                if (searchQuery.isEmpty()) {
                    outputPanel.removeAll();
                    outputPanel.add(new JLabel("Please enter a search query."));
                    outputPanel.revalidate();
                    outputPanel.repaint();
                    return;
                }

                String formattedQuery = searchQuery.replace(" ", "+");
                String serpApiEndpoint = "https://serpapi.com/search";
                String serpApiKey = "14160d0c23e488116db65285e3c14397b55d5759b6443b65e7937540bfb42caa";

                try {
                    outputPanel.removeAll();

                    // Add a title above the product list
                    JLabel productListTitle = new JLabel("<html><h2>========OUR PRODUCT RECOMMENDATIONS========:</h2></html>");
                    productListTitle.setHorizontalAlignment(SwingConstants.LEFT);
                    outputPanel.add(productListTitle);

                    String serpApiResponse = callSerpApi(serpApiEndpoint, serpApiKey, formattedQuery);
                    JSONObject jsonObject = new JSONObject(serpApiResponse);

                    if (jsonObject.has("organic_results")) {
                        JSONArray products = jsonObject.getJSONArray("organic_results");
                        for (int i = 0; i < Math.min(products.length(), 10); i++) {
                            JSONObject product = products.getJSONObject(i);

                            JPanel productPanel = new JPanel();
                            productPanel.setLayout(new BorderLayout(10, 10));

                            if (product.has("thumbnail")) {
                                productPanel.add(getScaledImageLabel(product.getString("thumbnail"), 100, 100), BorderLayout.WEST);
                            }

                            StringBuilder productDetails = new StringBuilder();
                            if (product.has("title")) {
                                productDetails.append("<html><b>").append(product.getString("title")).append("</b><br>");
                            }
                            if (product.has("primary_offer") && product.getJSONObject("primary_offer").has("offer_price")) {
                                productDetails.append("Price: $")
                                        .append(product.getJSONObject("primary_offer").getDouble("offer_price"))
                                        .append("<br>");
                            }
                            if (product.has("rating")) {
                                productDetails.append("Rating: ").append(product.getDouble("rating")).append(" stars<br>");
                            }
                            if (product.has("reviews")) {
                                productDetails.append("Reviews: ").append(product.getInt("reviews")).append("<br>");
                            }
                            productDetails.append("</html>");

                            JLabel detailsLabel = new JLabel(productDetails.toString());
                            productPanel.add(detailsLabel, BorderLayout.CENTER);

                            outputPanel.add(productPanel);
                        }

                        String sentimentResponse = callSentimentAPI(serpApiResponse);
                        System.out.println("sentiment response: " + sentimentResponse);
                        JPanel recommendationPanel = formatJsonResponse(sentimentResponse);
                        outputPanel.add(recommendationPanel);
                    }

                    outputPanel.revalidate();
                    outputPanel.repaint();
                } catch (Exception ex) {
                    outputPanel.removeAll();
                    outputPanel.add(new JLabel("Error: " + ex.getMessage()));
                    outputPanel.revalidate();
                    outputPanel.repaint();
                }
            });

            frame.setLayout(new BorderLayout(10, 10));
            frame.add(inputPanel, BorderLayout.NORTH);
            frame.add(scrollPane, BorderLayout.CENTER);

            frame.setVisible(true);
        });
    }

    /**
     * Calls the Sentiment API to analyze product reviews.
     *
     * @param serpApiResponse The response from SerpAPI containing product data.
     * @return The formatted response from the Sentiment API.
     * @throws Exception If there is an error during the API call.
     */
    private static String callSentimentAPI(String serpApiResponse) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyBqIFEWfWPCZ-17rA4XumrJvoccSzoGbD4";

        String promptText = "\nAnalyze the following Walmart product search results and provide only the text output with the best product suggestion and its general review. Also create a review analysis and sentiment analysis in 5 lines. Provide thumbnail image link and price for the product as well.";

        String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" +
                serpApiResponse.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") +
                promptText.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") +
                "\"}]}]}";

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

    /**
     * Formats the JSON response from the Sentiment API into a structured panel.
     *
     * @param jsonResponse The JSON response from the Sentiment API.
     * @return A JPanel containing the formatted recommendation.
     */
    private static JPanel formatJsonResponse(String jsonResponse) {
        JPanel recommendationPanel = new JPanel(new BorderLayout(10, 10));
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);

            if (jsonObject.has("candidates")) {
                JSONArray candidates = jsonObject.getJSONArray("candidates");
                for (int i = 0; i < candidates.length(); i++) {
                    JSONObject candidate = candidates.getJSONObject(i);
                    if (candidate.has("content")) {
                        JSONObject content = candidate.getJSONObject("content");
                        JSONArray parts = content.getJSONArray("parts");
                        for (int j = 0; j < parts.length(); j++) {
                            JSONObject part = parts.getJSONObject(j);
                            String recommendationText = part.getString("text");

                            // Extract Thumbnail Image Link
                            String thumbnailUrl = null;
                            String[] lines = recommendationText.split("\n");
                            StringBuilder cleanText = new StringBuilder();
                            for (String line : lines) {
                                if (line.startsWith("Thumbnail Image Link:")) {
                                    thumbnailUrl = line.replace("Thumbnail Image Link:", "").trim();
                                } else {
                                    cleanText.append(line).append("\n");
                                }
                            }

                            // Create a panel to display recommendation text and image
                            JPanel recommendationContent = new JPanel(new BorderLayout(10, 10));

                            if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                                recommendationContent.add(getScaledImageLabel(thumbnailUrl, 150, 150), BorderLayout.WEST);
                            }

                            JLabel recommendationLabel = new JLabel("<html><br><br><b>========OUR PICK========:</b><br>" + cleanText.toString().replace("\n", "<br>") + "</html>");
                            recommendationContent.add(recommendationLabel, BorderLayout.CENTER);

                            recommendationPanel.add(recommendationContent, BorderLayout.CENTER);
                        }
                    }
                }
            }

        } catch (Exception e) {
            recommendationPanel.add(new JLabel("Error parsing JSON: " + e.getMessage()), BorderLayout.CENTER);
        }

        return recommendationPanel;
    }

    /**
     * Calls the SerpAPI to fetch Walmart product suggestions based on a query.
     *
     * @param endpoint The API endpoint URL.
     * @param apiKey The API key for authentication.
     * @param query The search query for products.
     * @return The JSON response from SerpAPI.
     * @throws Exception If there is an error during the API call.
     */
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

    /**
     * Displays a scaled image in a JLabel.
     *
     * @param imageUrl The URL of the image.
     * @param width The desired width of the image.
     * @param height The desired height of the image.
     * @return A JLabel containing the scaled image.
     */
    private static JLabel getScaledImageLabel(String imageUrl, int width, int height) {
        try {
            URL url = new URL(imageUrl);
            BufferedImage img = ImageIO.read(url);
            Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new JLabel(new ImageIcon(scaledImg));
        } catch (Exception e) {
            return new JLabel("Image not available");
        }
    }
}
