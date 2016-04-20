package com.slice.textsearch.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ElasticSearchResposeParser {
	public static JSONObject parseElasticSearchResponse(String response) {
		try {
			JSONObject obj = new JSONObject(response);
			return obj;
		} catch (JSONException e) {
			return null;
		}
	}

	public static boolean isPostItResponseSuccessful(String response) {
		JSONObject obj = ElasticSearchResposeParser
				.parseElasticSearchResponse(response);
		if (obj != null) {
			int isSucessful = obj.getJSONObject("_shards").getInt("successful");
			return isSucessful > 0 ? true : false;
		}
		return false;
	}

	public static boolean hasIndex(String response) {
		try {
			JSONObject obj = ElasticSearchResposeParser
					.parseElasticSearchResponse(response);
			if (obj != null) {
				int isSucessful = obj.getJSONObject("hits").getInt("total");
				return isSucessful > 0 ? true : false;
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	public static WordDetails getWordDetails(String response) {
		boolean isSucessful = ElasticSearchResposeParser.hasIndex(response);
		System.out.println(isSucessful);
		System.out.println(response);
		if (isSucessful) {
			try {
				int wordCount = 0;
				JSONObject obj = ElasticSearchResposeParser
						.parseElasticSearchResponse(response);
				JSONArray hits = obj.getJSONObject("hits").getJSONArray("hits");
				for (int counter = 0; counter < hits.length(); counter++) {
					wordCount += ((JSONObject) hits.get(0)).getJSONObject(
							"_source").getInt("count");
				}
				String id = ((JSONObject) (obj.getJSONObject("hits")
						.getJSONArray("hits").get(0))).getString("_id");
				String word = ((JSONObject) (obj.getJSONObject("hits")
						.getJSONArray("hits").get(0))).getJSONObject("_source")
						.getString("word");
				return new WordDetails(id, wordCount, word);
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}

	/***
	 * { "_index": "word_index", "_type": "word_index_type",
	 * "_id":"AVQvpiKGlMfsdWLXXyTf", "_version": 3, "_shards": { "total": 2,
	 * "successful": 1, "failed": 0 }, "created": false }
	 * 
	 * @param response
	 * @return
	 */
	public static boolean isWordCountUpdated(String response) {
		System.out.println("isWordCountUpdated");
		System.out.println(response);
		JSONObject obj = ElasticSearchResposeParser
				.parseElasticSearchResponse(response);
		if (obj != null) {
			int isSucessful = obj.getJSONObject("_shards").getInt("successful");
			return isSucessful > 0 ? true : false;
		}
		return false;
	}
}
