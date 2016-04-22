package com.slice.textsearch;

import java.io.File;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class TextSearchCLI {
	private String word;
	private String path;
	private ArrayList<Path> filePaths;
	public static AtomicLong wordCount = new AtomicLong();

	public TextSearchCLI(String path, String word) {
		this.path = path;
		this.word = word;
		this.filePaths = new ArrayList<Path>();
	}

	public TextSearchCLI() {
		this.path = null;
		this.word = null;
		this.filePaths = new ArrayList<Path>();
	}

	public static void main(String[] args) {
		TextSearchCLI cli = new TextSearchCLI();
		try {
			cli.parseArument(args);
			// Breaking up into more atomic functions for ease of unit testing
			cli.createFileList();
			cli.triggerSearch();
		} catch (NullPointerException e) {
			e.printStackTrace();
			System.out.println("Usage: ");
			System.out.println("TextSearchCLI {/path/to/file/or/directory}");
		}
	}

	/***
	 * Could also be done using matcher <code>
	 * Matcher matcher = Pattern.compile(word).matcher(readableChunckString);
	 * int count = 0;
	 * while (matcher.find()) {
	 * count++;
	 * }
	 * </code>
	 */
	public void triggerSearch() {
		long startTimestamp = System.currentTimeMillis();
		for (Path path : filePaths) {
			final byte[] searchByte = word.getBytes(StandardCharsets.UTF_8);
			int lineCount = 0;
			boolean inMiddleOfAWord = false;
			boolean continueToEOL = false;
			try {
				FileChannel channel = FileChannel.open(path,
						StandardOpenOption.READ);
				final long fileSize = channel.size();
				int index = 0;
				while (index < fileSize) {
					int readableChunk = READ_SIZE + searchByte.length;
					int readChunk = (int) Math.min(readableChunk, fileSize
							- index);
					int limit = readableChunk == readChunk ? READ_SIZE
							: (readChunk - searchByte.length);
					MappedByteBuffer buffer = channel.map(MapMode.READ_ONLY,
							index, readChunk);
					index += (readableChunk == readChunk) ? limit : limit
							+ searchByte.length;

					for (int i = 0; i < limit; i++) {
						final byte currentCharacter = buffer.get(i);
						if (continueToEOL) {
							if (currentCharacter == '\n') {
								continueToEOL = false;
								inMiddleOfAWord = false;
								lineCount++;
							}
						} else if (currentCharacter == '\n') {
							lineCount++;
							inMiddleOfAWord = false;
						} else if (currentCharacter == ' ') {
							inMiddleOfAWord = false;
						} else if (!inMiddleOfAWord) {
							if (check(buffer, i, searchByte)) {
								System.out.println("Occurrence #"
										+ wordCount.incrementAndGet()
										+ " at line #" + (lineCount + 1)
										+ " in " + path.toString());
								i += searchByte.length - 1;
								continueToEOL = true;
							} else {
								inMiddleOfAWord = true;
							}
						}
					}
				}
			} catch (Exception e) {
				System.err.println(String.format(
						"Something wrong with path: %s", path));
			}
		}
		System.out.println(String.format("Total time of search: %.2f",
				(System.currentTimeMillis() - startTimestamp) / 1000.0 / 60.0));
		System.out
				.println(String.format("Searched for %s and found it %d times",
						word, wordCount.get()));
	}

	public void createFileList() {
		File file = new File(this.path);
		if (file.isDirectory()) {
			for (File direcoryFiles : file.listFiles()) {
				filePaths.add(Paths.get(direcoryFiles.getAbsolutePath()));
			}
		} else {
			filePaths.add(Paths.get(file.getAbsolutePath()));
		}
	}

	private void parseArument(String[] args) {
		if (args.length == 2) {
			System.out.println("Current args: " + Arrays.toString(args));
			path = new String(args[0]);
			word = new String(args[1]);
		} else {
			System.out.println("Usage: ");
			System.out
					.println("TextSearchCLI {/path/to/file/or/directory} {word}");
		}
	}

	private static final int READ_SIZE = 4096;

	private boolean check(MappedByteBuffer buffer, int index, byte[] searchByte) {
		for (int i = 0; i < searchByte.length; i++) {
			if (Character.toUpperCase((char) searchByte[i]) != buffer.get(index
					+ i)
					&& Character.toLowerCase((char) searchByte[i]) != buffer
							.get(index + i)) {
				return false;
			}
		}
		byte nxt = buffer.get(index + searchByte.length);
		return nxt == '.' || nxt == '?' || nxt == '!' || nxt == ','
				|| nxt == ' ' || nxt == '\n' || nxt == '\r';
	}
}
