package com.lmt.lib.archive.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.lmt.lib.archive.Archive;
import com.lmt.lib.archive.ArchiveEntry;
import com.lmt.lib.archive.ArchiveType;
import com.lmt.lib.archive.EntryCallback;

/**
 * 1個のフォルダをアーカイブと見なすアーカイブクラスです。
 *
 * @author J-SON3
 */
public class FolderArchive extends Archive {
	/** パスによるエントリマップ */
	private Map<Path, ArchiveEntry> mEntryMap = Collections.emptyMap();
	/** エントリリスト */
	private List<ArchiveEntry> mEntryList = Collections.emptyList();
	/** エントリ情報が全件キャッシュされたかどうか */
	private boolean mCached = false;

	/** エントリ情報の実装 */
	private static class EntryImpl extends ArchiveEntry {
		EntryImpl(Archive owner, Integer index, Path path, BasicFileAttributes attrs) {
			this.owner = owner;
			this.index = index;
			this.path = path;
			this.isLocation = attrs.isDirectory();
			this.isContent = attrs.isRegularFile();
			this.size = attrs.size();
			this.lastModified = attrs.lastModifiedTime().toMillis();
		}
	}

	/**
	 * 新しいフォルダアーカイブオブジェクトを構築します。
	 * @param path アーカイブのパス
	 * @exception NullPointerException pathがnull
	 * @exception IOException {@link Archive#onOpenArchive}参照
	 */
	public FolderArchive(Path path) throws IOException {
		super(ArchiveType.FOLDER, path);
		onOpenArchive(path);
	}

	/** {@inheritDoc} */
	@Override
	protected void onOpenArchive(Path path) throws IOException {
		if (!Files.exists(path)) {
			// 指定されたパスのファイル・ディレクトリは存在しない
			throw new NoSuchFileException(path.toString());
		} else if (!Files.isDirectory(path)) {
			// 指定されたパスはディレクトリではない
			throw new NoSuchFileException(path.toString(), null, "This is not a directory");
		} else {
			// Do nothing
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void onCloseArchive() throws IOException {
		// 全てのリソースを解放する
		mEntryMap = null;
		mEntryList = null;
		mCached = false;
	}

	/** {@inheritDoc} */
	@Override
	protected InputStream onOpenContentByEntry(ArchiveEntry entry) throws IOException {
		return openContentMain(entry.getPath());
	}

	/** {@inheritDoc} */
	@Override
	protected InputStream onOpenContentByIndex(int index) throws IOException {
		return openContentMain(mEntryList.get(index).getPath());
	}

	/** {@inheritDoc} */
	@Override
	protected InputStream onOpenContentByPath(Path path) throws IOException {
		return openContentMain(path);
	}

	/** {@inheritDoc} */
	@Override
	protected byte[] onReadAllBytesByEntry(ArchiveEntry entry) throws IOException {
		return readAllBytesMain(entry.getPath());
	}

	/** {@inheritDoc} */
	@Override
	protected byte[] onReadAllBytesByIndex(int index) throws IOException {
		return readAllBytesMain(mEntryList.get(index).getPath());
	}

	/** {@inheritDoc} */
	@Override
	protected byte[] onReadAllBytesByPath(Path path) throws IOException {
		return readAllBytesMain(path);
	}

	/** {@inheritDoc} */
	@Override
	protected ArchiveEntry onGetEntryByIndex(int index) {
		return mEntryList.get(index);
	}

	/** {@inheritDoc} */
	@Override
	protected ArchiveEntry onGetEntryByPath(Path path) {
		return mCached ? mEntryMap.get(path) : createEntry(getPath().resolve(path), new AtomicInteger(-2));
	}

	/** {@inheritDoc} */
	@Override
	protected void onEnumEntries(EntryCallback callback) throws IOException {
		var rootPath = getPath();
		var isContinue = new AtomicBoolean(true);
		var indexCounter = new AtomicInteger(-1);
		var entryMap = new TreeMap<Path, ArchiveEntry>();
		var entryList = Files.walk(getPath())
				.takeWhile(p -> isContinue.get())
				.filter(p -> !p.equals(rootPath))
				.map(p -> createEntry(p, indexCounter))
				.filter(Objects::nonNull)
				.peek(e -> isContinue.set(callback.call(e, indexCounter.get() + 1, 0)))
				.peek(e -> entryMap.put(e.getPath(), e))
				.collect(Collectors.toList());
		if (isContinue.get()) {
			mCached = true;
			mEntryMap = entryMap;
			mEntryList = entryList;
		}
	}

	/** {@inheritDoc} */
	@Override
	protected int onGetEntryCount() {
		return mEntryList.size();
	}

	/** {@inheritDoc} */
	@Override
	protected int onGetCapability() {
		return CAPS_PATH | (mCached ? CAPS_INDEX : 0);
	}

	/**
	 * コンテンツオープンのメイン処理
	 * @param path コンテンツパス
	 * @return コンテンツの入力ストリーム
	 * @throws IOException 入力エラー発生
	 */
	private InputStream openContentMain(Path path) throws IOException {
		var filePath = getPath().resolve(path);
		if (Files.isDirectory(filePath)) {
			throw new NoSuchFileException(path.toString(), null, "Can not open directory");
		} else {
			return Files.newInputStream(filePath);
		}
	}

	/**
	 * コンテンツのバイトデータ読み込みメイン処理
	 * @param path コンテンツパス
	 * @return コンテンツのバイトデータ
	 * @throws IOException 入力エラー発生
	 */
	private byte[] readAllBytesMain(Path path) throws IOException {
		try (var stream = openContentMain(path)) {
			return stream.readAllBytes();
		}
	}

	/**
	 * エントリ情報生成
	 * @param path エントリパス
	 * @param indexCounter インデックス値のカウンタ(OUT)
	 * @return エントリ情報。入力エラーにより生成失敗時はnull。
	 */
	private ArchiveEntry createEntry(Path path, AtomicInteger indexCounter) {
		try {
			var attrs = Files.readAttributes(path, BasicFileAttributes.class);
			var relPath = getPath().relativize(path);
			return new EntryImpl(this, indexCounter.incrementAndGet(), relPath, attrs);
		} catch (IOException e) {
			return null;  // 例外をスローするような異常ファイル・ディレクトリは無視する
		}
	}
}
