package com.slice.textsearch.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class ElasticSearch {
	private String url = null;

	private static final String ELASTIC_SEARCH_BASE_URL = "http://localhost:9200";

	// Make these as enums
	public static final String FILE_INDEX = "file_index";
	public static final String FILE_INDEX_TYPE = "file_index_type";

	public static final String WORD_INDEX = "word_index";
	public static final String WORD_INDEX_TYPE = "word_index_type";

	public static final String WORD_SEARCH_INDEX_TYPE = "word_search_index_type";

	public ElasticSearch() {
		this.url = new String(ELASTIC_SEARCH_BASE_URL);
	}

	public ElasticSearch withIndexType(String indexType) {
		this.url += String.format("/%s", indexType);
		return this;
	}

	public ElasticSearch withIndex(String index) {
		this.url += String.format("/%s", index);
		return this;
	}

	public ElasticSearch withSearch() {
		this.url += "/_search";
		return this;
	}

	public ElasticSearch withId(String id) {
		this.url += String.format("/%s", id);
		return this;
	}

	public ElasticSearch withAll() {
		this.url += "/_all";
		return this;
	}

	public String postIt(String query) {
		URLConnection connection;
		StringBuilder responseBuilder = new StringBuilder();
		try {
			connection = new URL(url).openConnection();
			String charset = "UTF-8";
			connection.setDoOutput(true);
			connection.setRequestProperty("Accept-Charset", charset);
			connection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded;charset=" + charset);

			if (query != null) {
				try (OutputStream output = connection.getOutputStream()) {
					output.write(query.getBytes(charset));
				}
			}
			Thread.sleep(10);
			InputStream response = connection.getInputStream();
			Scanner responseScanner = new Scanner(response);
			while (responseScanner.hasNext()) {
				responseBuilder.append(responseScanner.next());
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return responseBuilder.toString();
	}

	public String putIt(String information) {
		URL url;
		StringBuilder responseBuilder = new StringBuilder();
		try {
			url = new URL(this.url);
			HttpURLConnection httpCon = (HttpURLConnection) url
					.openConnection();
			httpCon.setDoOutput(true);
			httpCon.setRequestMethod("PUT");
			OutputStreamWriter out = new OutputStreamWriter(
					httpCon.getOutputStream());
			out.write(information);
			out.close();
			InputStream response = httpCon.getInputStream();
			Scanner responseScanner = new Scanner(response);
			while (responseScanner.hasNext()) {
				responseBuilder.append(responseScanner.next());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return responseBuilder.toString();
	}

	public String deleteIt() {
		URL url;
		StringBuilder responseBuilder = new StringBuilder();
		try {
			url = new URL(this.url);
			HttpURLConnection httpCon = (HttpURLConnection) url
					.openConnection();
			httpCon.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			httpCon.setRequestMethod("DELETE");
			httpCon.connect();

			InputStream response = httpCon.getInputStream();
			Scanner responseScanner = new Scanner(response);
			while (responseScanner.hasNext()) {
				responseBuilder.append(responseScanner.next());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return responseBuilder.toString();
	}

	public String getUrl() {
		return url;
	}

	public ElasticSearch reset() {
		this.url = new String(ELASTIC_SEARCH_BASE_URL);
		return this;
	}

	public static String createSimpleQuery(String key) {
		String query = String.format(
				"{\"query\": {\"query_string\": {\"query\": \"%s\"}}}", key);
		return query;
	}
}
