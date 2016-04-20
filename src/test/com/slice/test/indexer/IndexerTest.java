package com.slice.test.indexer;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.slice.textsearch.jobs.Indexer;
import com.slice.textsearch.utils.ElasticSearch;
import com.slice.textsearch.utils.ElasticSearchResposeParser;
import com.slice.textsearch.utils.WordDetails;

public class IndexerTest {
	private ElasticSearch elasticSearch = null;

	@Before
	public void createData() {
		elasticSearch = new ElasticSearch();
		elasticSearch.withIndex(ElasticSearch.FILE_INDEX).withIndexType(
				ElasticSearch.FILE_INDEX_TYPE);
	}

	@Test
	public void urlMustBeBuild() throws Exception {
		assertEquals(elasticSearch.getUrl(),
				"http://localhost:9200/file_index/file_index_type");
	}

	@Test
	public void searchUrlMustBeBuilt() throws Exception {
		assertEquals(elasticSearch.withSearch().getUrl(),
				"http://localhost:9200/file_index/file_index_type/_search");
	}

	@Test
	public void putAndQueryAndDeleteFileName() throws Exception {
		String fileName = "file_name_1";
		Indexer indexer = new Indexer();
		elasticSearch.reset().withIndex(ElasticSearch.FILE_INDEX)
				.withIndexType(ElasticSearch.FILE_INDEX_TYPE);
		String information = indexer.createFileNameInformationForPost(fileName);
		String response = elasticSearch.postIt(information);
		Thread.sleep(2000);
		assertEquals(
				ElasticSearchResposeParser.isPostItResponseSuccessful(response),
				true);
		assertEquals(indexer.existingIndex(fileName),
				"File: file_name_1 is already indexed");

		elasticSearch = new ElasticSearch();
		elasticSearch.reset().withIndex(ElasticSearch.FILE_INDEX)
				.withIndexType(ElasticSearch.FILE_INDEX_TYPE).withSearch();
		Thread.sleep(2000);
		response = elasticSearch.postIt(null);
		indexer.deleteAll(response, ElasticSearch.FILE_INDEX,
				ElasticSearch.FILE_INDEX_TYPE);

	}

	@Test
	public void putAndQueryAndDeleteWord() throws Exception {
		Indexer indexer = new Indexer();
		WordDetails wordDetails = new WordDetails(null, 1, "word_1");

		elasticSearch.reset().withIndex(ElasticSearch.WORD_INDEX)
				.withIndexType(ElasticSearch.WORD_INDEX_TYPE);

		String information = indexer.createWordInformationForPost(wordDetails);
		String response = elasticSearch.postIt(information);
		assertEquals(
				ElasticSearchResposeParser.isPostItResponseSuccessful(response),
				true);
		response = indexer.existingWordIndex(wordDetails.word,
				ElasticSearch.WORD_INDEX_TYPE);

		elasticSearch = new ElasticSearch();
		elasticSearch.reset().withIndex(ElasticSearch.WORD_INDEX)
				.withIndexType(ElasticSearch.WORD_INDEX_TYPE).withSearch();
		Thread.sleep(2000);
		response = elasticSearch.postIt(null);

		assertEquals(true, response.contains("\"word_1\",\"count\":\"1\""));
		indexer.deleteAll(response, ElasticSearch.WORD_INDEX,
				ElasticSearch.WORD_INDEX_TYPE);
	}

}
