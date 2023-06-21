/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.bavitech.zammad;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Http(s) connection manager to Zammad backend<p>
 *
 * Handle the request and response transaction<p>
 *
 * To disable all ssl certificate checks, call disableSslCheck()
 * <p>
 *
 * @author sbodmer
 */
public class ZammadConnectionManager extends Thread {

	String protocol = "http";
	String host = "localhost";
	String token = "";
	String api = "/api/v1";
	int timeout = 15000;    //--- ms
	boolean dump = false;

	File folder = null;
	ArrayList<ZammadRequest> queue = new ArrayList<>();
	HashMap<String, ArrayList<ZammadConnectionListener>> listeners = new HashMap<>();

	public ZammadConnectionManager() {
		this("http", "localhost", "", "/api/v1");
	}

	public ZammadConnectionManager(String protocol, String host, String api) {
		this(protocol, host, "", api);
	}

	public ZammadConnectionManager(String protocol, String host, String token, String api) {
		super("ZammadConnectionManager");
		this.host = host;
		this.token = token;
		this.api = api;
		this.protocol = protocol;

		folder = new File(System.getProperty("user.dir"),"Dumps"+File.separator+"Zammad");
		folder.mkdirs();
	}

	//**************************************************************************
	//*** API
	//**************************************************************************
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getTimeout() {
		return timeout;
	}

	public String getHost() {
		return host;
	}

	public String getApi() {
		return api;
	}

	public void setApi(String api) {
		this.api = api;

	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;

	}

	public String getProtocol() {
		return protocol;
	}

	public void setHost(String host) {
		this.host = host;

	}

	public String getToken() {
		return token;
	}

	public void setToken(String user) {
		this.token = user;

	}

	/**
	 * If the response needs to be dumped in system out<p>
	 *
	 * @param dump
	 */
	public void dumpResponse(boolean dump) {
		this.dump = dump;
	}

	/**
	 * Non blocking sending<p>
	 *
	 * The response is received via async listener mecanism<p>
	 *
	 * @param msg
	 */
	public void send(ZammadRequest req) {
		queue.add(req);

	}

	/**
	 * Send a request in a blocking way (get method)
	 * <p>
	 *
	 * @param url
	 * @return
	 */
	public ZammadResponse sendb(ZammadRequest req) {
		return sendRequest(req);
	}

	public void addConnectionListener(String signature, ZammadConnectionListener listener) {
		ArrayList<ZammadConnectionListener> v = listeners.get(signature);
		if (v == null) {
			//--- No listener added, create vector
			v = new ArrayList<ZammadConnectionListener>();
			v.add(listener);
			listeners.put(signature, v);

		} else //--- Add new
		if (v.contains(listener) == false) v.add(listener);
	}

	public void removeConnectionListener(String signature, ZammadConnectionListener listener) {
		ArrayList<ZammadConnectionListener> v = listeners.get(signature);
		if (v != null) {
			v.remove(listener);
			if (v.isEmpty()) listeners.remove(signature);
		}
	}

	/**
	 * To disable all ssl cert check, call this method
	 */
	public static void disableSslCheck() {
		try {
			TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
					//---
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
					//---
				}
			}};

			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	//**************************************************************************
	//*** RUN
	//**************************************************************************
	@Override
	public void run() {
		try {
			while (isInterrupted() == false) {
				while (!queue.isEmpty()) {
					//--- Check if servers is knop-tech server

					ZammadRequest req = queue.remove(0);
					ZammadResponse response = sendRequest(req);

					ArrayList<ZammadConnectionListener> listener = listeners.get(response.getSignature());
					if (listener != null) {
						for (ZammadConnectionListener li:listener) li.zammadReceivedResponse(response.getSignature(), response);
					}
				}
				sleep(1000);

			}

		} catch (InterruptedException ex) {

		}
	}

	/**
	 * This method will call the Zammad api and return the response<p>
	 *
	 * If no request argument, use GET method, otherwise use POST
	 *
	 * @return
	 */
	private ZammadResponse sendRequest(ZammadRequest req) {
		ZammadResponse response = null;
		try {
			String json = req.toString();
			String u = protocol + "://" + host + api + "/" + req.endPoint;
			if (req.getMethod().equals("")) {
				if (req.getQuery() != null) u +="/search";
				u += "?page=" + req.getOffset() + "&per_page=" + req.getLimit();
				if (!req.getSortBy().equals("")) u +="&sort_by="+req.getSortBy();
				if (!req.getOrderBy().equals("")) u += "&order_by="+req.getOrderBy();
				if (req.getQuery() != null) u +="&query="+req.getQuery();
			}
			URL url = new URL(u);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(timeout);
			
			if (!req.getMethod().equals("")) {
				con.setRequestMethod(req.getMethod());
				
			} else {
				con.setRequestMethod(json.equals("") ? "GET" : "POST");
			}
			con.setUseCaches(false);
			con.setInstanceFollowRedirects(false);
			System.out.println("CALLING["+con.getRequestMethod()+"]:" + url.toString());
			
			
			//--- Send request headers
			con.setRequestProperty("Authorization", "Token token=" + token);
			// con.setRequestProperty("Accept-Language", "" + Locale.getDefault().getLanguage());
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Accept", "application/json");
			Iterator<String> it = req.getHeaders().keySet().iterator();
			//--- Additional headers
			while (it.hasNext()) {
				String h = it.next();
				con.setRequestProperty(h, req.getHeaders().get(h));
			}
			con.setDoOutput(true);
			con.connect();

			//--- Send request body (json)
			if (!json.equals("")) {
				OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
				wr.write(json);
				wr.flush();
				wr.close();
				System.out.println("SENT:" + json);
			}

			//--- Read response header
			int code = con.getResponseCode();
			int length = con.getContentLength();
			String mime = con.getContentType();

			System.out.println("RESPONSE CODE:"+code+" LENGTH:"+length+" MIME:"+mime);
			//--- Dump header headers (like session cookie)
			Map<String, List<String>> headers = con.getHeaderFields();
			it = headers.keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				if (key == null) continue;

				List<String> list = headers.get(key);
				String vals = "";
				for (String s:list) vals += "" + s + " ";

			}
			
			if (code == HttpURLConnection.HTTP_OK) {
				//--- Read response content
				byte buffer[] = new byte[16384];
				con.setReadTimeout(15000);
				InputStream in = con.getInputStream();
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				while (true) {
					int offset = in.read(buffer);
					if (offset == -1) break;
					bout.write(buffer, 0, offset);
				}
				in.close();

				if (dump) {
					System.out.println(">>> ZAMMAD RESPONSE DUMP <<<");
					System.out.println(bout.toString());
					System.out.println("");
				}
				File f = new File(folder,"Response.json");
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(json.getBytes());
				fout.close();
				
				if (mime.startsWith("application/json")) {
					//--- json form response
					json = bout.toString();
					response = new ZammadResponse(req.getSignature(), json);
					bout.reset();
					bout = null;

				}

			} else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
				//--- Ask to authentify again, so clear the session value (stored in cookies)
				response = new ZammadResponse("" + System.currentTimeMillis());
				ZammadSys sys = new ZammadSys(ZammadSys.STATE_ERROR, "Unauthorized Http access " + code);
				response.add(sys);

			} else {
				response = new ZammadResponse("" + System.currentTimeMillis());
				ZammadSys sys = new ZammadSys(ZammadSys.STATE_ERROR, "Http code is " + code);
				response.add(sys);

			}
			con.disconnect();

		} catch (SocketTimeoutException ex) {
			response = new ZammadResponse(req.getSignature());
			ZammadSys sys = new ZammadSys(ZammadSys.STATE_ERROR, "Read timeout");
			response.add(sys);

		} catch (UnknownHostException ex) {
			response = new ZammadResponse(req.getSignature());
			ZammadSys sys = new ZammadSys(ZammadSys.STATE_ERROR, "Unknown host : " + ex.getMessage());
			response.add(sys);

		} catch (Exception ex) {
			response = new ZammadResponse(req.getSignature());
			ZammadSys sys = new ZammadSys(ZammadSys.STATE_ERROR, "Exception : " + ex.getMessage());
			response.add(sys);

		}
		response.setUserData(req.getUserData());

		return response;
	}

	public static void main(String args[]) {
		try {
			String token = "";

			ZammadConnectionManager zammad = new ZammadConnectionManager("https", "tickets.lsi-media.ch", token, "/api/v1");
			zammad.dumpResponse(true);
			zammad.start();

			ZammadRequest req = new ZammadRequest("none", "ticket_articles/by_ticket/16", "");
			ZammadResponse response = zammad.sendb(req);
			// System.out.println("RESPONSE:" + response);
			ArrayList<Object> entries = response.getEntries();
			for (int i = 0;i < entries.size();i++) {
				Object obj = entries.get(i);
				if (obj instanceof ZammadEntry) {
					ZammadEntry entry = (ZammadEntry) obj;
					System.out.println("TICKET:" + entry.getField("id"));
					System.out.println(entry);
					System.out.println("===");
				}
			}

			zammad.interrupt();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
