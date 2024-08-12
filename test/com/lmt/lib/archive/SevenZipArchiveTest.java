package com.lmt.lib.archive;

import java.nio.file.Path;

public class SevenZipArchiveTest extends ArchiveTest {
	@Override
	protected ArchiveType expectedArchiveType() {
		return ArchiveType.SEVEN_ZIP;
	}

	@Override
	protected Path expectedPath() {
		return TestData.SEVEN_ZIP_ARCHIVE;
	}
}
