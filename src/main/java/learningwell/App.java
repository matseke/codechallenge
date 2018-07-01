package learningwell;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Learningwell code challenge
 */
public class App {

    private static final String URL = "http://api.scb.se/OV0104/v1/doris/sv/ssd/START/ME/ME0104/ME0104D/ME0104T4";
    private static final String VALUES = "values";
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {

        JSONObject json = getMetaData();
        if (json == null) return;

        JSONArray variables = (JSONArray) json.get("variables");
        Map<String, String> regionMap = new HashMap<>();
        String query = buildQuery(regionMap, variables);

        String stringResponse = getData(query);
        if (stringResponse.isEmpty()) return;
        JSONObject responseObject = new JSONObject(stringResponse.replace("\uFEFF", "")); // remove BOM
        JSONArray responseData = (JSONArray) responseObject.get("data");
        log.debug("Response values: {}", responseData);

        Map<String, DistrictValue> maxValues = new TreeMap<>();
        responseData.forEach((valueObject) -> collectMaxValues(regionMap, maxValues, (JSONObject) valueObject));
        showResult(maxValues);
    }

    private static void showResult(Map<String, DistrictValue> maxValues) {
        for (Map.Entry<String, DistrictValue> entry : maxValues.entrySet()) {
            System.out.format("%s %s%n", entry.getKey(), entry.getValue());
        }
    }

    private static String getData(String query) {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(URL);
        post.setHeader("Content-type", "application/json");
        StringEntity entity = new StringEntity(query, "UTF-8");
        entity.setContentType("application/json");
        post.setEntity(entity);
        HttpResponse httpResponse;
        HttpEntity responseEntity;
        String stringResponse = "";
        try {
            httpResponse = client.execute(post);
            StatusLine statusLine = httpResponse.getStatusLine();
            int statusCode = statusLine.getStatusCode();

            if (statusCode == 200) {
                responseEntity = httpResponse.getEntity();
                stringResponse = EntityUtils.toString(responseEntity);
                log.debug("The response is: {}", stringResponse);
            } else {
                log.error("Bad response from SCB: {}", httpResponse);
            }
        } catch (IOException e) {
            log.error("Exception when calling the SCB API", e);
        }
        return stringResponse;
    }

    private static String buildQuery(Map<String, String> regionMap, JSONArray variables) {
        log.debug("variables: {}", variables);

        JSONObject region = (JSONObject) variables.get(0);
        log.debug("Region: {}", region);
        JSONArray regionValues = (JSONArray) region.get(VALUES);
        JSONArray regionValueTexts = (JSONArray) region.get("valueTexts");
        log.debug("RegionValues: {}", regionValues);
        log.debug("RegionValueTexts: {}", regionValueTexts);

        Iterator it1 = regionValues.iterator();
        Iterator it2 = regionValueTexts.iterator();
        while (it1.hasNext() && it2.hasNext()) {
            regionMap.put(it1.next().toString(), it2.next().toString());
        }

        JSONObject contentsCode = (JSONObject) variables.get(1);
        log.debug("ContentsCode: {}", contentsCode);

        JSONObject time = (JSONObject) variables.get(2);
        log.debug("Time: {}", time);

        //TODO: use JSONObject, JSONArray etc
        String query = "{\"query\": [";
        query = query + "{\"code\":\"Region\", \"selection\":{\"filter\":\"all\", \"values\":";
        query = query + "[\"*\"]";
        query = query + "}}, ";
        query = query + "{\"code\":\"ContentsCode\", \"selection\":{\"filter\":\"item\", \"values\":";
        query = query + "[\"" + ((JSONArray) contentsCode.get(VALUES)).get(0) + "\"]";
        query = query + "}}, ";
        query = query + "{\"code\":\"Tid\", \"selection\":{\"filter\":\"all\", \"values\":";
        query = query + "[\"*\"]";
        query = query + "}}";
        query = query + "], \"response\": {\"format\":\"json\"}}";

        log.debug("query: {}", query);
        return query;
    }

    private static JSONObject getMetaData() {
        JSONObject json;
        try {
            json = new JSONObject(IOUtils.toString(new URL(URL), Charset.forName("UTF-8")));
            log.debug(json.toString());
        } catch (IOException e) {
            log.error("Failed reading from SCB", e);
            return null;
        }
        return json;
    }

    private static void collectMaxValues(Map<String, String> regionMap,
                                         Map<String, DistrictValue> maxValues,
                                         JSONObject valueObject) {
        String turnoutString = (String) ((JSONArray) valueObject.get(VALUES)).get(0);
        turnoutString = turnoutString.replaceAll("\\.\\.", "0.0"); // replace missing values with 0.0
        double turnout = Double.parseDouble(turnoutString);
        String district = (String) ((JSONArray) valueObject.get("key")).get(0);
        String year = (String) ((JSONArray) valueObject.get("key")).get(1);
        log.debug("year {} district {}, turnout {}", year, regionMap.get(district), turnout);
        double oldMax = -1.0;
        DistrictValue dv = maxValues.get(year);
        if (dv != null) {
            oldMax = dv.getValue();
        }
        if (turnout > oldMax) {
            maxValues.put(year, new DistrictValue(regionMap.get(district), turnout));
        } else if (turnout == oldMax) {
            dv.addDistrict(regionMap.get(district)); // intentionally skipped the null check for dv
        }
    }
}
