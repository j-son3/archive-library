package com.lmt.lib.archive;

import java.nio.file.Path;

public class Rar4ArchiveTest extends ArchiveTest {
	@Override
	protected ArchiveType expectedArchiveType() {
		return ArchiveType.RAR;
	}

	@Override
	protected Path expectedPath() {
		return TestData.RAR4_ARCHIVE;
	}
}
