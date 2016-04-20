package com.slice.textsearch;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import com.slice.textsearch.jobs.Indexer;
import com.slice.textsearch.utils.ElasticSearch;
import com.slice.textsearch.utils.ElasticSearchResposeParser;
import com.slice.textsearch.utils.WordDetails;

/***
 * End point to see the usage and to trigger the indexer. TODO: Trigger the
 * indexer with server start
 * 
 * @author fuusuke
 * 
 */
@Path("/search")
public class TextSearch {
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getMessage() {
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		InputStream input = classLoader
				.getResourceAsStream("/../app.properties");
		Properties properties = new Properties();
		if (input != null) {
			try {
				properties.load(input);
				String location = properties.getProperty("textfile.location");
				SchedulerFactory sf = new StdSchedulerFactory();
				Scheduler sche = sf.getScheduler();

				JobDetail job = newJob(Indexer.class).withIdentity("indexer",
						"slice-group").build();
				Trigger trigger = newTrigger()
						.withIdentity("indexer-trigger", "slice-group")
						.withSchedule(
								SimpleScheduleBuilder.simpleSchedule()
										.withIntervalInSeconds(5)).build();
				sche.scheduleJob(job, trigger);
				sche.start();
				return properties.getProperty("textfile.location");
			} catch (IOException | SchedulerException e) {
				e.printStackTrace();
				return e.getMessage();
			}
		}
		return "usage: please use the service as http://localhost/slice-text-search/rest/search/{wordToSearch}";
	}

	/***
	 * "usage: please use the service as http://localhost/slice-text-search/rest/search/{wordToSearch}"
	 * 
	 * @param word
	 * @return
	 */
	@Path("{word}")
	@GET
	public String searchWord(@PathParam("word") String word) {
		ElasticSearch elasticSearch = new ElasticSearch();
		elasticSearch.withIndex(ElasticSearch.WORD_INDEX)
				.withIndexType(ElasticSearch.WORD_INDEX_TYPE).withSearch();
		String query = ElasticSearch.createSimpleQuery(word);
		String response = elasticSearch.postIt(query);
		WordDetails wordDetails = ElasticSearchResposeParser
				.getWordDetails(response);
		String wordSearchCountMemo = indexWordSearchCount(word, elasticSearch);
		return WordDetails.createResponseText(wordDetails, wordSearchCountMemo,
				word);
	}

	private String indexWordSearchCount(String word, ElasticSearch elasticSearch) {
		Indexer indexer = new Indexer();
		return indexer.indexWord(word, ElasticSearch.WORD_SEARCH_INDEX_TYPE);
	}
}