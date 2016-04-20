package com.slice.textsearch.utils;

public class WordDetails {
	public String word = null;
	public int wordCount = 0;
	public String id = null;

	public WordDetails(String id, int wordCount, String word) {
		this.id = id;
		this.wordCount = wordCount;
		this.word = new String(word);
	}

	public void setWord(String word) {
		this.word = new String(word);
	}

	public void incrementWordCountBy(int count) {
		this.wordCount += count;
	}

	public static String createResponseText(WordDetails wordDetails,
			String fallBackMemo) {
		return ((wordDetails != null ? (String.format(
				"The word `%s` was found %d number of times", wordDetails.word,
				wordDetails.wordCount)) : (String.format(
				"The word '%s' was not found.", wordDetails.word)))
				+ " <=> " + fallBackMemo);
	}
}
