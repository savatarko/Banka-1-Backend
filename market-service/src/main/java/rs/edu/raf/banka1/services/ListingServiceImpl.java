package rs.edu.raf.banka1.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rs.edu.raf.banka1.mapper.ListingMapper;
import rs.edu.raf.banka1.model.ListingHistoryModel;
import rs.edu.raf.banka1.model.ListingModel;
import rs.edu.raf.banka1.repositories.ListingHistoryRepository;
import rs.edu.raf.banka1.repositories.ListingRepository;
import rs.edu.raf.banka1.utils.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ListingServiceImpl implements ListingService{
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ListingMapper listingMapper;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingHistoryRepository listingHistoryRepository;

    @Value("${listingAPItoken}")
    private String listingAPItoken;

    @Value("${alphaVantageAPIToken}")
    private String alphaVantageAPIToken;

    public ListingServiceImpl() {
        // we don't need all fields from response, so we can ignore them
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    //    NOTE: see what to do with this, as API isn't free (almost nothing from this API changes so it is okay to do it once and store it into json file)
//    NOTE: Maybe name/description of the company changes, so we should update it from time to time
    @Override
    public void initializeListings() {
        try {
            String sectorsEncoded = String.join("%20", Constants.sectors);

            String urlStr = "https://api.iex.cloud/v1/data/core/stock_collection/sector?collectionName=" + sectorsEncoded + "&token=" + listingAPItoken;

            URL url = new URL(urlStr);

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set the request method to GET
            connection.setRequestMethod("GET");

            // Set request headers if needed
            connection.setRequestProperty("Content-Type", "application/json");

            // Get the response code
            int responseCode = connection.getResponseCode();

            // Read the response body
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Close the connection
            connection.disconnect();

            // Create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();

            // Parse the JSON array string
            JsonNode jsonArray = objectMapper.readTree(response.toString());

            // Create a new JSON array to store selected fields
            ArrayNode newArray = objectMapper.createArrayNode();

            // Iterate through the JSON array
            for (JsonNode jsonNode : jsonArray) {
                // Extract selected fields
                String symbol = jsonNode.get("symbol").asText();
                String companyName = jsonNode.get("companyName").asText();
                // if primaryExchange is null, we should skip it
                if (jsonNode.get("primaryExchange") == null) {
                    continue;
                }

                String primaryExchange = jsonNode.get("primaryExchange").asText();

                // Create a new JSON object with selected fields
                ObjectNode newObj = objectMapper.createObjectNode();
                newObj.put("symbol", symbol);
                newObj.put("companyName", companyName);
                newObj.put("primaryExchange", primaryExchange);

                // Add the new object to the new JSON array
                newArray.add(newObj);

            }
            // Save the new JSON array to a file
            File file = new File(Constants.listingsFilePath);
            objectMapper.writeValue(file, newArray);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    //    loads listings from json file and updates them with trending data
    @Override
    public List<ListingModel> fetchListings() {
        List<ListingModel> listingModels = fetchListingsName();
        for (ListingModel listingModel : listingModels)
            updateValuesForListing(listingModel);

        return listingModels;
    }

    private List<ListingModel> fetchListingsName(){
        try {
            File file = new File(Constants.listingsFilePath);

            // Read JSON data from the file
            JsonNode rootNode = objectMapper.readTree(file);

            List<ListingModel> listings = new ArrayList<>();

            // Iterate over each element in the JSON array
            for (JsonNode node : rootNode) {
                ListingModel listingModel = new ListingModel();
                listingModel.setTicker(node.path("symbol").asText());
                listingModel.setName(node.path("companyName").asText());
                listingModel.setExchange(node.path("primaryExchange").asText());

                listingModel.setLastRefresh(java.time.LocalDateTime.now());

                // Add the ListingModel object to the list
                listings.add(listingModel);
            }

            return listings;
        }catch (Exception e){
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void updateValuesForListing(ListingModel listingModel){
        try{
            // URL of the alphavantage API endpoint
            String symbol = listingModel.getTicker();
            String apiUrl = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + alphaVantageAPIToken;

            // Fetch JSON data from the API
            JsonNode rootNode = objectMapper.readTree(new URL(apiUrl));

            // Get the "Global Quote" node
            JsonNode globalQuoteNode = rootNode.get("Global Quote");


            // Parse data from the "Global Quote" node
            double high = globalQuoteNode.get("03. high").asDouble();
            double low = globalQuoteNode.get("04. low").asDouble();
            double price = globalQuoteNode.get("05. price").asDouble();
            int volume = globalQuoteNode.get("06. volume").asInt();
            double change = globalQuoteNode.get("09. change").asDouble();

            listingModel.setPrice(price);
            listingModel.setAsk(high);
            listingModel.setBid(low);
            listingModel.setChanged(change);
            listingModel.setVolume(volume);

        }catch (Exception e){
            e.printStackTrace();
            System.out.println(listingModel.getTicker() + " has failed updating");
        }
    }


    @Override
//    updates all listings with new data into database
    public void updateAllListingsDatabase(List<ListingModel> listings) {
        listingRepository.saveAll(listings);
    }

    @Override
    public List<ListingHistoryModel> fetchAllListingsHistory() {
        try{
            List<ListingModel> listingModels = fetchListingsName();
            List<ListingHistoryModel> listingHistoryModels = new ArrayList<>();
            for (ListingModel lmodel : listingModels)
                listingHistoryModels.addAll(fetchSingleListingHistory(lmodel.getTicker()));

            return listingHistoryModels;
        }catch (Exception e) {
            e.printStackTrace();

            return new ArrayList<>();
        }
    }

    @Override
    public List<ListingHistoryModel> fetchSingleListingHistory(String ticker){
        try {

            String apiUrl = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=" + ticker + "&apikey=" + alphaVantageAPIToken;
            JsonNode rootNode = objectMapper.readTree(new URL(apiUrl));

            List<ListingHistoryModel> listingHistoryModels = new ArrayList<>();
            // Get the "Time Series (Daily)" node
            JsonNode timeSeriesNode = rootNode.get("Time Series (Daily)");
            if (timeSeriesNode != null) {
                Iterator<Map.Entry<String, JsonNode>> fields = timeSeriesNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String date = entry.getKey();
                    JsonNode dataNode = entry.getValue();

                    // Get specific fields from each data node
                    double open = dataNode.get("1. open").asDouble();
                    double high = dataNode.get("2. high").asDouble();
                    double low = dataNode.get("3. low").asDouble();
                    double close = dataNode.get("4. close").asDouble();
                    int volume = dataNode.get("5. volume").asInt();

                    // make a new ListingHistoryModel
                    ListingHistoryModel listingHistoryModel = new ListingHistoryModel();
                    listingHistoryModel.setTicker(ticker);
                    listingHistoryModel.setDate(java.time.LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    listingHistoryModel.setPrice(close);
                    listingHistoryModel.setAsk(high);
                    listingHistoryModel.setBid(low);
                    listingHistoryModel.setChanged(close - open);
                    listingHistoryModel.setVolume(volume);


                    listingHistoryModels.add(listingHistoryModel);
                }
            }
            return listingHistoryModels;
        }catch (Exception e){
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    //    if we want to add listing to history for certain day, if we already have saved it, we should just update it
    @Override
    public int addListingToHistory(ListingHistoryModel listingHistoryModel) {
        Optional<ListingHistoryModel> listingHistoryModelOptional = listingHistoryRepository.findByTickerAndDate(listingHistoryModel.getTicker(), listingHistoryModel.getDate());
        if (listingHistoryModelOptional.isPresent()) {
            ListingHistoryModel listingHistoryModel1 = listingHistoryModelOptional.get();
            listingHistoryModel1.setPrice(listingHistoryModel.getPrice());
            listingHistoryModel1.setAsk(listingHistoryModel.getAsk());
            listingHistoryModel1.setBid(listingHistoryModel.getBid());
            listingHistoryModel1.setChanged(listingHistoryModel.getChanged());
            listingHistoryModel1.setVolume(listingHistoryModel.getVolume());

            listingHistoryRepository.save(listingHistoryModel1);
            return 0;
        }else{
            listingHistoryRepository.save(listingHistoryModel);
            return 1;
        }
    }

    //    call it at the end of the day (to save API calls, but other than that, you can call it whenever you want)
    @Override
    public int addAllListingsToHistory(List<ListingHistoryModel> listingHistoryModels) {

        return listingHistoryModels.stream().mapToInt(this::addListingToHistory).sum();
    }


}