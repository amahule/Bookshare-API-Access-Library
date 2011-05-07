package org.bookshare.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

/**
 * This class provides the services needed for getting
 * data from Bookshare's Webservice API.
 *
 */
final public class BookshareWebservice {

	// New endpoint for bookshare API calls. Earlier was service.bookshare.org
	private static final String URL = "api.bookshare.org";
	//private static final String URL = "service.bookshare.org";
	
	/**
	 * Utility method that returns a MD5 encryption of a String.
	 * @param str String to be be encrypted.
	 * @return MD5 encrypted String.
	 */

	public String md5sum(String str){
		byte[] md5sum = null;
		String md5 = "";
		try{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md5sum = md.digest(str.getBytes());	
		}
		catch(NoSuchAlgorithmException e){
			System.out.println(e);
		}

		BigInteger number = new BigInteger(1,md5sum);
		md5 = number.toString(16);
		return md5;
	}

	/**
	 * Converts the InputStream to a String.
	 * @param inputStream InputStream.
	 * @return String representation of the InputStream data.
	 */
	public String convertStreamToString(InputStream inputStream) {
		/*
		 * To convert the InputStream to String we use the BufferedReader.readLine()
		 * method. We iterate until the BufferedReader return null which means
		 * there's no more data to read. Each line will appended to a StringBuilder
		 * and returned as String.
		 */
		// Construct a BufferedReader for the inputStream
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			// Read each line and append a newline character at the end
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				// Close the stream irrespective of whether the read was successful or not
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// Return the trimmed response String
		return sb.toString().trim();
	}
	
	/**
	 * Retrieves the InputStream in response to a requested URI. Method also takes care of 
	 * user authentication.
	 * This InputStream needs to be used in appropriate way to handle the response
	 * depending on the response.
	 * E.g. If the requested URI is for downloading a file, read data
	 * from this InputStream and write to a local file using OutputStream.
	 * E.g. If the requested URI returns a XML or JSON response, make use of
	 * corresponding parsers to handle the data.
	 * 
	 * @param wsPassword password of the Bookshare's web service account.
	 * @param requestUri The request URI.
	 * @return InputStream representing the response.
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public InputStream getResponseStream(String wsPassword, String requestUri) throws
	URISyntaxException, IOException{
		HttpEntity entity = null;
		
		HttpResponse response = getHttpResponse(wsPassword, requestUri);

		// Get hold of the response entity
		entity = response.getEntity();
		return entity.getContent();
	}
	
	/**
	 * Retrieves an HttpResponse for a requested URI.
	 * @param wsPassword password of the Bookshare web service account.
	 * @param requestUri The request URI.
	 * @return HttpResponse A HttpResponse object.
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	// Method for retrieving an HttpResponse
	public HttpResponse getHttpResponse(String wsPassword, String requestUri) throws
	URISyntaxException, IOException{
		DefaultHttpClient httpclient = new DefaultHttpClient();

//		httpclient.getCredentialsProvider().setCredentials(
//				new AuthScope(URL, 443),
//				new UsernamePasswordCredentials(wsUsername, md5sum(wsPassword)));

		HttpHost targetHost = new HttpHost(URL);

//		BasicHttpContext localcontext = new BasicHttpContext();

		/*
		 *  Generate BASIC scheme object and stick it to the local execution context
		 *  Bookshare uses basic authentication scheme.
		 */
//		BasicScheme basicAuth = new BasicScheme();
//		localcontext.setAttribute("preemptive-auth", basicAuth);

		// Add as the first request interceptor
//		httpclient.addRequestInterceptor(new PreemptiveAuth(), 0);

		URI uri = new URI(requestUri);

		// Prepare a HTTP GET Request
		HttpGet httpget = new HttpGet(uri);

		// Execute the request
		HttpResponse response = null;

		// Prepare a HTTP GET Request
		httpget = new HttpGet(uri);
		
		if(wsPassword != null){
			Header header = new BasicHeader("X-password",md5sum(wsPassword));
			httpget.setHeader(header);
		}
		
		// Get the HttpResponse. Earlier the localcontext was used for the basic authentication.
		// Now basic authentication is not needed as the developer key is appended to the request
		//response = httpclient.execute(targetHost, httpget, localcontext);
		response = httpclient.execute(targetHost, httpget);
		
		return response;
	}
	
	/*
	 * Implementation of preemptive basic authentication
	 * Not used now. Was used for earlier API version of
	 * Bookshare webservice, which used HTTP basic authentication
	 */
	private static class PreemptiveAuth implements HttpRequestInterceptor {

		public void process(
				final HttpRequest request, 
				final HttpContext context) throws HttpException, IOException {

			AuthState authState = (AuthState) context.getAttribute(
					ClientContext.TARGET_AUTH_STATE);

			// If no auth scheme available yet, try to initialize it preemptively
			if (authState.getAuthScheme() == null) {
				AuthScheme authScheme = (AuthScheme) context.getAttribute(
						"preemptive-auth");
				CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(
						ClientContext.CREDS_PROVIDER);
				HttpHost targetHost = (HttpHost) context.getAttribute(
						ExecutionContext.HTTP_TARGET_HOST);

				if (authScheme != null) {
					Credentials creds = credsProvider.getCredentials(
							new AuthScope(
									targetHost.getHostName(), 
									targetHost.getPort()));
					if (creds == null) {
						throw new HttpException("No credentials for preemptive authentication");
					}
					authState.setAuthScheme(authScheme);
					authState.setCredentials(creds);
				}
			}

		}
	}

}
