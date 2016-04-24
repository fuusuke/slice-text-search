package com.slice.textsearch.utils;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class WordSearchResponse {
	private static String createWordResponse(Map<String, String> values) {
		JSONObject obj = new JSONObject();
		for (String key : values.keySet()) {
			obj.put(key, values.get(key));
		}
		return obj.toString();
	}

	private static Map<String, String> createWordSearchResponseMap(String word,
			String frequency, String memo) {
		Map<String, String> values = new HashMap<>();
		values.put("word", word);
		values.put("frequency", frequency);
		values.put("memo", memo);
		return values;
	}

	public static String createSuccessWordSearchResponseMap(String word,
			String frequency, String memo) {
		Map<String, String> values = createWordSearchResponseMap(word,
				frequency, memo);
		values.put("result", "SUCCESS");
		return createWordResponse(values);
	}

	public static String createFailureWordSearchResponseMap(String word,
			String frequency, String memo) {
		Map<String, String> values = createWordSearchResponseMap(word,
				frequency, memo);
		values.put("result", "FAILURE");
		return createWordResponse(values);
	}

	public static String createSearchResponse(WordDetails wordDetails,
			String wordSearchCountMemo) {
		Map<String, String> values = new HashMap<>();
		if (wordDetails != null) {
			values.put("word", wordDetails.word);
			values.put("count", String.valueOf(wordDetails.wordCount));
		} else {
			values.put("word", "");
			values.put("count", "");
		}
		values.put("memo", wordSearchCountMemo);
		return createWordResponse(values);
	}
}
