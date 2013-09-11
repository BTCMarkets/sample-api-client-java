package net.btcmarkets.api.sample;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

public class PlaceOrder {
	private static final String API_KEY = "Please sign up on the website to get an api key and replace it here";
	private static final String PRIVATE_KEY = "Replace your private key here";

	public static String BASEURL = "replace the API url here";
	private static String ORDER_CREATE_PATH = "/order/create";  
	private static final String APIKEY_HEADER = "apikey";
	private static final String TIMESTAMP_HEADER = "timestamp";
	private static final String SIGNATURE_HEADER = "signature";
	private static final String ENCODING = "UTF-8";
	private static final String ALGORITHM = "HmacSHA512";

	public static void main(String[] args) throws Exception {
		String response = "";
		try {
			// input parameters for creating a new account. data is posted via https
			String postData = "{\"currency\":\"AUD\",\"instrument\":\"BTC\",\"price\":13000000000,\"volume\":10000000,\"orderSide\":\"Bid\",\"ordertype\":\"Limit\",\"clientRequestId\":\"1\"}";
			
			//get the current timestamp. It's best to use ntp or similar services in order to sync your server time
			String timestamp = Long.toString(System.currentTimeMillis());

			// create the string that needs to be signed   
			String stringToSign = buildStringToSign(ORDER_CREATE_PATH, null, postData, timestamp);

			// build signature to be included in the http header   
			String signature = signRequest(PRIVATE_KEY, stringToSign);

			//full url path
			String url = BASEURL + ORDER_CREATE_PATH;

			response = executeHttpPost(postData, url, API_KEY, PRIVATE_KEY, signature, timestamp);
		} catch (Exception e) {
			System.out.println(e);
		}
		System.out.println(response);
	}

	public static String executeHttpPost(String postData, String url, 
			String apiKey, String privateKey, String signature, String timestamp) throws Exception{
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpResponse httpResponse = null;

		try {
			HttpPost httpPost = new HttpPost(url);

			// post any data that needs to go with http request.  
			if (postData != null) {
				httpPost.setEntity(new StringEntity(postData, ENCODING));
			}
			// Set http headers   
			httpPost.addHeader("Accept", "*/*");
			httpPost.addHeader("Accept-Charset", ENCODING);
			httpPost.addHeader("Content-Type", "application/json");

			// Add signature, timestamp and apiKey to the http header   
			httpPost.addHeader(SIGNATURE_HEADER, signature);
			httpPost.addHeader(APIKEY_HEADER, apiKey);
			httpPost.addHeader(TIMESTAMP_HEADER, timestamp);

			// execute http request     
			httpResponse = httpClient.execute(httpPost);

			if (httpResponse.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException(httpResponse.getStatusLine().getReasonPhrase());
			}
			// return JSON results as String     
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String responseBody = responseHandler.handleResponse(httpResponse);
			return responseBody;

		}catch (Exception e) {
			throw new RuntimeException("unable to execute json call:" + e);
		} finally {
			// close http connection
			if (httpResponse != null) {
				HttpEntity entity = httpResponse.getEntity();
				if (entity != null) {
					entity.consumeContent();
				}
			}
			if (httpClient != null) {
				httpClient.getConnectionManager().shutdown();
			}
		}
	}

	private static String buildStringToSign(String uri, String queryString, 
			String postData, String timestamp) {
		// queryString must be sorted key=value& pairs
		String stringToSign = uri + "\n";
		if (queryString != null) {
			stringToSign += queryString + "\n";
		}
		stringToSign += timestamp + "\n";
		stringToSign += postData;
		return stringToSign;
	}

	private static String signRequest(String secret, String data) {
		String signature = "";
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			SecretKeySpec secret_spec = new SecretKeySpec(Base64.decodeBase64(secret), ALGORITHM);
			mac.init(secret_spec);
			signature = Base64.encodeBase64String(mac.doFinal(data.getBytes()));
		} catch (InvalidKeyException e) {
			System.out.println(e);
		} catch (NoSuchAlgorithmException e) {
			System.out.println(e);
		} catch (Exception e) {
			System.out.println(e);
		}
		return signature;
	}
}
