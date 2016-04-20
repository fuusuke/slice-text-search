package com.slice.textsearch.jobs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.slice.textsearch.utils.ElasticSearch;
import com.slice.textsearch.utils.ElasticSearchResposeParser;
import com.slice.textsearch.utils.WordDetails;

public class Indexer implements Job {

	private ElasticSearch elasticSearch = null;
	private static final String WORD_INDEX_MEMO_DELIMITER = " <=> ";

	public Indexer() {
		elasticSearch = new ElasticSearch();
	}

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		/*
		 * Currently it is possible to get duplicate content with different file
		 * names, but better of doing this would be to isolate cases where the
		 * duplication is handled maturely and restrict it from indexing
		 */
		elasticSearch = new ElasticSearch();
		String pathToFileDirectory = getPathToFileDirectory();
		File[] listOfFileObjects = getListOfFileNames(pathToFileDirectory);

		/*
		 * Iterate through all the files For each file, iterate through all the
		 * words and also index the file as "done" For each word, index every
		 * word with count and index every word with position and filename
		 */
		for (File file : listOfFileObjects) {
			try {
				System.out.printf(">>>>>>>>>>> Indexing %s started",
						file.getName());
				String indexMemo = indexFile(file);
				System.out.printf("Indexing %s completed <<<<<<<<<<<<",
						file.getName());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				String indexMemo = String.format(
						"Could not create index for file: %s", file.getName());
			}
		}
	}

	private String indexFile(File file) throws FileNotFoundException {
		// Check if file is already indexed
		String fileName = file.getName();
		String indexMemo = existingIndex(fileName);
		if (indexMemo != null) {
			return indexMemo;
		} else {
			elasticSearch.reset().withIndex(ElasticSearch.FILE_INDEX)
					.withIndexType(ElasticSearch.FILE_INDEX_TYPE);
			String information = createFileNameInformationForPost(fileName);
			String response = elasticSearch.postIt(information);
			if (!ElasticSearchResposeParser
					.isPostItResponseSuccessful(response)) {
				indexMemo = String.format("Could not index file: %s", fileName);
				return indexMemo;
			}
		}
		Scanner fileScanner = new Scanner(file);
		StringBuilder wordIndexMemo = new StringBuilder();
		while (fileScanner.hasNext()) {
			String word = fileScanner.next();
			System.out.printf(">>>>>>>>>>> Indexing %s started", word);
			indexMemo = indexWord(word, ElasticSearch.WORD_INDEX_TYPE);
			System.out.printf("Indexing %s completed <<<<<<<<<<<", word);
			wordIndexMemo.append(indexMemo);
			wordIndexMemo.append(WORD_INDEX_MEMO_DELIMITER);
		}
		return wordIndexMemo.toString();
	}

	/***
	 * Check if the file is already indexed. Do not consider a file to be
	 * re-indexed, if already indexed. This can be improved by saving the date
	 * of last indexed as an additional data with the index. If the file was
	 * modified after the last index, it would reindex the file clearing out its
	 * earlier index.
	 * 
	 * @param fileName
	 * @return
	 */
	public String existingIndex(String fileName) {
		String query = String.format(
				"{\"query\": {\"query_string\": {\"query\": \"%s\"}}}",
				fileName);
		String response = elasticSearch.reset()
				.withIndexType(ElasticSearch.FILE_INDEX)
				.withIndex(ElasticSearch.FILE_INDEX_TYPE).withSearch()
				.postIt(query);
		if (ElasticSearchResposeParser.hasIndex(response)) {
			return String.format("File: %s is already indexed", fileName);
		}
		return null;
	}

	public String createFileNameInformationForPost(String fileName) {
		String information = String.format("{\"file_name\": \"%s\"}", fileName);
		return information;
	}

	public String indexWord(String word, String indexType) {
		/*
		 * Check for existing index for the word. If index is found, get the
		 * count and add 1.
		 */
		String response = existingWordIndex(word, indexType);

		WordDetails wordDetails = ElasticSearchResposeParser
				.getWordDetails(response);
		if (wordDetails != null) {
			wordDetails.incrementWordCountBy(1);
			wordDetails.setWord(word);
			String information = createWordInformationForPost(wordDetails);

			elasticSearch.reset();
			elasticSearch.withIndexType(ElasticSearch.WORD_INDEX)
					.withIndexType(indexType).withId(wordDetails.id);
			response = elasticSearch.putIt(information);

			if (ElasticSearchResposeParser.isWordCountUpdated(response)) {
				return String.format("Word Count updated, word: %s, count: %d",
						wordDetails.word, wordDetails.wordCount);
			} else {
				return String.format(
						"Could not upadate Word Count, word: %s, count: %d",
						wordDetails.word, wordDetails.wordCount);
			}
		} else {
			// New word
			String indexWordMemo = indexNewWord(word, indexType);
			return indexWordMemo;
		}
	}

	public String indexNewWord(String word, String indexType) {
		String response = null;
		WordDetails wordDetails = new WordDetails(null, 1, word);
		String information = createWordInformationForPost(wordDetails);

		elasticSearch.reset();
		elasticSearch.withIndexType(ElasticSearch.WORD_INDEX).withIndexType(
				indexType);
		response = elasticSearch.postIt(information);
		if (ElasticSearchResposeParser.hasIndex(response)) {
			return String.format("Word indexed, word: %s, count: %d",
					wordDetails.word, wordDetails.wordCount);
		} else {
			return String.format("Could not index word, word: %s, count: %d",
					wordDetails.word, wordDetails.wordCount);
		}
	}

	public String existingWordIndex(String word, String indexType) {
		elasticSearch.reset();
		String query = String.format(
				"{\"query\": {\"query_string\": {\"query\": \"%s\"}}}", word);
		elasticSearch.withIndexType(ElasticSearch.WORD_INDEX)
				.withIndexType(indexType).withSearch();

		return elasticSearch.postIt(query);
	}

	public String createWordInformationForPost(WordDetails wordDetails) {
		String information = String.format(
				"{\"word\": \"%s\", \"count\": \"%d\"}", wordDetails.word,
				wordDetails.wordCount);
		return information;
	}

	private File[] getListOfFileNames(String pathToFileDirectory) {
		File directory = new File(pathToFileDirectory);
		if (directory.isDirectory()) {
			return directory.listFiles();
		}
		return null;
	}

	public String getPathToFileDirectory() {
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		InputStream input = classLoader
				.getResourceAsStream("/../app.properties");
		Properties properties = new Properties();
		if (input != null) {
			try {
				properties.load(input);
				return properties.getProperty("textfile.location");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public boolean deleteAll(String response, String index, String indexType) {
		try {
			JSONArray hits = new JSONObject(response).getJSONObject("hits")
					.getJSONArray("hits");
			for (int counter = 0; counter < hits.length(); counter++) {
				String idToDelete = ((JSONObject) (hits.get(counter)))
						.getString("_id");

				elasticSearch.reset();
				elasticSearch.withIndex(index).withIndexType(indexType)
						.withId(idToDelete);
				System.out.println(elasticSearch.deleteIt());
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
