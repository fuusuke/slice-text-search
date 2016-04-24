package com.slice.textsearch.jobs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.slice.textsearch.utils.ElasticSearch;
import com.slice.textsearch.utils.ElasticSearchResposeParser;
import com.slice.textsearch.utils.WordDetails;
import com.slice.textsearch.utils.WordSearchResponse;

/***
 * Job to index every word in a file. This index is to provide ready to serve
 * value: number of times the work occurs. It is a batch process, that goes
 * through all the files, the first run would be long, the following ones can be
 * optimized as follows:
 * <ul>
 * <li>Check for files added after the last index run</li>
 * <li>Check for files modified after the last index run</li>
 * <li>Ignore stop-words for indexing</li>
 * </ul>
 * 
 * Currently it is possible to get duplicate content with different file names,
 * but better of doing this would be to isolate cases where the duplication is
 * handled maturely and restrict it from indexing.
 * 
 * <ul>
 * <li>Iterate through all the files For each file</li>
 * <li>iterate through all the words and also index the file as "done" for each
 * word</li>
 * <li>Index every word with count and index every word with position and
 * filename</li>
 * </ul>
 * 
 * @author fuusuke
 * 
 */
public class Indexer implements Job {

	private ElasticSearch elasticSearch = null;
	private static final String WORD_INDEX_MEMO_DELIMITER = " <=> ";

	public Indexer() {
		elasticSearch = new ElasticSearch();
	}

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		elasticSearch = new ElasticSearch();
		String pathToFileDirectory = getPathToFileDirectory();
		File[] listOfFileObjects = getListOfFile(pathToFileDirectory);

		for (File file : listOfFileObjects) {
			try {
				// TODO: Memo should be class for storing all actionable
				// attributes.
				System.out.println(new Date() + " - Started indexing "
						+ file.getName());
				System.out.println();
				// TODO: Improve this by creating each file index as a separate
				// task.
				String indexMemo = indexFile(file);
				// System.out.println(indexMemo);
				System.out.println(new Date() + " - Completed indexing "
						+ file.getName());
			} catch (FileNotFoundException | InterruptedException e) {
				String indexMemo = String.format(
						"Could not create index for file: %s", file.getName());
				System.out.println(indexMemo);
			}
		}
	}

	private String indexFile(File file) throws FileNotFoundException,
			InterruptedException {
		// Check if file is already indexed
		String fileName = FilenameUtils.removeExtension(file.getName());
		String indexMemo = existingIndex(fileName);
		if (indexMemo != null) {
			return indexMemo;
		} else {
			elasticSearch.reset().withIndex(ElasticSearch.FILE_INDEX)
					.withIndexType(ElasticSearch.FILE_INDEX_TYPE);
			String information = createFileNameInformationForPost(fileName);
			String response = new String(elasticSearch.postIt(information));
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
			word = word.replaceAll("[^a-zA-Z0-9]", "");
			Thread.sleep(100);
			if (Arrays.asList(WordDetails.stopwords).contains(
					word.toLowerCase())) {
				continue;
			}
			indexMemo = indexWord(word.toLowerCase(),
					ElasticSearch.WORD_INDEX_TYPE);
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
		String query = new String(String.format(
				"{\"query\": {\"query_string\": {\"query\": \"%s\"}}}",
				fileName));
		String response = new String(elasticSearch.reset()
				.withIndexType(ElasticSearch.FILE_INDEX)
				.withIndex(ElasticSearch.FILE_INDEX_TYPE).withSearch()
				.postIt(query));
		if (ElasticSearchResposeParser.hasIndex(response)) {
			return String.format("File: %s is already indexed", fileName);
		}
		return null;
	}

	public String createFileNameInformationForPost(String fileName) {
		String information = new String(String.format(
				"{\"file_name\": \"%s\"}", fileName));
		return information;
	}

	public String indexWord(String word, String indexType) {
		/*
		 * Check for existing index for the word. If index is found, get the
		 * count and add 1.
		 */
		String response = new String(existingWordIndex(word, indexType));

		WordDetails wordDetails = ElasticSearchResposeParser
				.getWordDetails(response);
		if (wordDetails != null) {
			wordDetails.incrementWordCountBy(1);
			wordDetails.setWord(word);
			String information = createWordInformationForPost(wordDetails);

			elasticSearch.reset();
			elasticSearch.withIndexType(ElasticSearch.WORD_INDEX)
					.withIndexType(indexType).withId(wordDetails.id);
			response = new String(elasticSearch.putIt(information));

			if (ElasticSearchResposeParser.isWordCountUpdated(response)) {
				return String.format(
						"Word Count updated, word: \'%s\', frequency: %d",
						wordDetails.word, wordDetails.wordCount);
			} else {
				return String
						.format("Could not upadate Word Count, word: \'%s\', frequency: %d",
								wordDetails.word, wordDetails.wordCount);
			}
		} else {
			// New word
			String indexWordMemo = indexNewWord(word, indexType);
			return indexWordMemo;
		}
	}

	public String indexNewWord(String word, String indexType) {
		WordDetails wordDetails = new WordDetails(null, 1, word);
		String information = createWordInformationForPost(wordDetails);

		elasticSearch.reset();
		elasticSearch.withIndexType(ElasticSearch.WORD_INDEX).withIndexType(
				indexType);
		String response = new String(elasticSearch.postIt(information));
		if (ElasticSearchResposeParser.hasIndex(response)) {
			return WordSearchResponse
					.createSuccessWordSearchResponseMap(wordDetails.word,
							String.valueOf(wordDetails.wordCount), "");
		} else {
			return WordSearchResponse
					.createFailureWordSearchResponseMap(wordDetails.word,
							String.valueOf(wordDetails.wordCount), "");
		}
	}

	public String existingWordIndex(String word, String indexType) {
		elasticSearch.reset();
		String query = new String(String.format(
				"{\"query\": {\"query_string\": {\"query\": \"%s\"}}}", word));
		elasticSearch.withIndexType(ElasticSearch.WORD_INDEX)
				.withIndexType(indexType).withSearch();

		return elasticSearch.postIt(query);
	}

	public String createWordInformationForPost(WordDetails wordDetails) {
		String information = new String(String.format(
				"{\"word\": \"%s\", \"count\": \"%d\"}", wordDetails.word,
				wordDetails.wordCount));
		return information;
	}

	private File[] getListOfFile(String pathToFileDirectory) {
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
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
