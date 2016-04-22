package com.slice.test.cli;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.slice.textsearch.TextSearchCLI;

public class TextSearchCLITest {
	private TextSearchCLI cli;

	@Test
	public void createData() throws IOException {
		File file = new File("test.txt");
		cli = new TextSearchCLI(file.getAbsolutePath(), "26");
		cli.createFileList();
		cli.triggerSearch();
		assertEquals(cli.wordCount.get(), 15L);
	}

}
