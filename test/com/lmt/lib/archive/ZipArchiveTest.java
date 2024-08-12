package com.lmt.lib.archive;

import java.nio.file.Path;

public class ZipArchiveTest extends ArchiveTest {
	@Override
	protected ArchiveType expectedArchiveType() {
		return ArchiveType.ZIP;
	}

	@Override
	protected Path expectedPath() {
		return TestData.ZIP_ARCHIVE;
	}
}
