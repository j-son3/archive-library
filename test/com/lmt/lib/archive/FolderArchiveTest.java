package com.lmt.lib.archive;

import java.nio.file.Path;

public class FolderArchiveTest extends ArchiveTest {
	@Override
	protected ArchiveType expectedArchiveType() {
		return ArchiveType.FOLDER;
	}

	@Override
	protected Path expectedPath() {
		return TestData.FOLDER_ARCHIVE;
	}
}
