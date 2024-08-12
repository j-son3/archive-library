package com.lmt.lib.archive.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.lmt.lib.archive.Archive;
import com.lmt.lib.archive.ArchiveEntry;
import com.lmt.lib.archive.ArchiveType;
import com.lmt.lib.archive.EntryCallback;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

/**
 * 圧縮ファイルをアーカイブと見なすアーカイブクラスです。
 *
 * @author J-SON3
 */
public abstract class SzjbArchive extends Archive {
	/** 対応アーカイブフォーマット一覧 */
	private List<ArchiveFormat> mFormats;
	/** アーカイブI/F */
	private IInArchive mArchive = null;
	/** エントリ総数 */
	private int mNumOfItem = 0;
	/** パスによるエントリマップ */
	private Map<Path, ArchiveEntry> mEntryMap = Collections.emptyMap();
	/** エントリリスト */
	private List<ArchiveEntry> mEntryList = Collections.emptyList();
	/** エントリ情報が全件キャッシュされたかどうか */
	private boolean mCached = false;

	/** ZIPファイルのアーカイブクラス */
	public static class Zip extends SzjbArchive {
		/**
		 * 新しいZIPファイルアーカイブオブジェクトを構築します。
		 * @param path ZIPファイルパス
		 * @exception IOException アーカイブのオープンエラー(例：未知のファイル形式、読み取り権限なし)
		 */
		public Zip(Path path) throws IOException {
			super(ArchiveType.ZIP, List.of(ArchiveFormat.ZIP), path);
		}
	}

	/** RARファイルのアーカイブクラス */
	public static class Rar extends SzjbArchive {
		/**
		 * 新しいRARファイルアーカイブオブジェクトを構築します。
		 * @param path RARファイルパス
		 * @exception IOException アーカイブのオープンエラー(例：未知のファイル形式、読み取り権限なし)
		 */
		public Rar(Path path) throws IOException {
			super(ArchiveType.RAR, List.of(ArchiveFormat.RAR5, ArchiveFormat.RAR), path);
		}
	}

	/** 7-ZIPファイルのアーカイブクラス */
	public static class SevenZip extends SzjbArchive {
		/**
		 * 新しい7-ZIPファイルアーカイブオブジェクトを構築します。
		 * @param path 7-ZIPファイルパス
		 * @exception IOException アーカイブのオープンエラー(例：未知のファイル形式、読み取り権限なし)
		 */
		public SevenZip(Path path) throws IOException {
			super(ArchiveType.SEVEN_ZIP, List.of(ArchiveFormat.SEVEN_ZIP), path);
		}
	}

	/** エントリ情報の実装 */
	private static class EntryImpl extends ArchiveEntry {
		/**
		 * コンストラクタ
		 * @param owner エントリのオーナー
		 * @param archiveFile アーカイブI/F
		 * @param index エントリインデックス
		 * @exception IOException アーカイブI/Fからのプロパティ読み取り失敗
		 */
		EntryImpl(Archive owner, IInArchive archiveFile, int index) throws IOException {
			this.owner = owner;
			this.index = index;
			this.path = Path.of((String)archiveFile.getProperty(index, PropID.PATH));
			this.isLocation = (Boolean)archiveFile.getProperty(index, PropID.IS_FOLDER);
			this.isContent = !this.isLocation;
			this.size = (Long)archiveFile.getProperty(index, PropID.SIZE);
			var date = (Date)archiveFile.getProperty(index, PropID.LAST_MODIFICATION_TIME);
			this.lastModified = (date == null) ? 0L : date.toInstant().toEpochMilli();
		}

		/**
		 * ダミーエントリ用のコンストラクタ
		 * @param owner エントリのオーナー
		 * @param index エントリインデックス
		 */
		EntryImpl(Archive owner, int index) {
			this.owner = owner;
			this.index = index;
			this.path = Path.of("");
			this.isLocation = false;
			this.isContent = false;
			this.size = 0L;
			this.lastModified = 0L;
		}
	}

	/**
	 * 圧縮ファイルアーカイブクラスの共通コンストラクタです。
	 * @param archiveType アーカイブ種別
	 * @param formats 対応アーカイブフォーマット一覧
	 * @param path アーカイブファイルパス
	 * @exception IOException アーカイブのオープンエラー(例：未知のファイル形式、読み取り権限なし)
	 */
	protected SzjbArchive(ArchiveType archiveType, List<ArchiveFormat> formats, Path path) throws IOException {
		super(archiveType, path);
		mFormats = List.copyOf(formats);
		onOpenArchive(path);
	}

	/** {@inheritDoc} */
	@Override
	protected void onOpenArchive(Path path) throws IOException {
		// 指定パスのファイルをランダムアクセスファイルとして開く
		var raFile = (RandomAccessFile)null;
		try {
			raFile = new RandomAccessFile(path.toFile(), "r");
		} catch (FileNotFoundException e) {
			// API仕様はNoSuchFileExceptionを投げることになっているので例外を載せ替える
			throw new NoSuchFileException(path.toString(), null, e.getMessage());
		}

		var formatCount = mFormats.size();
		var raStream = new RandomAccessFileInStream(raFile);
		for (var i = 0; i < formatCount; i++) {
			try {
				// 対応フォーマットの優先順にオープンを試みる
				var format = mFormats.get(i);
				mArchive = net.sf.sevenzipjbinding.SevenZip.openInArchive(format, raStream);
				mNumOfItem = mArchive.getNumberOfItems();
				break;
			} catch (IOException e) {
				// 全ての対応フォーマットを試行した結果オープン失敗した場合はエラーとする
				if ((i + 1) == formatCount) {
					throw e;
				}
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void onCloseArchive() throws IOException {
		mEntryMap = null;
		mEntryList = null;
		mCached = false;
		mArchive.close();
		mArchive = null;
	}

	/** {@inheritDoc} */
	@Override
	protected InputStream onOpenContentByEntry(ArchiveEntry entry) throws IOException {
		return openContentMain(entry.getIndex());
	}

	/** {@inheritDoc} */
	@Override
	protected InputStream onOpenContentByIndex(int index) throws IOException {
		return openContentMain(index);
	}

	/** {@inheritDoc} */
	@Override
	protected InputStream onOpenContentByPath(Path path) throws IOException {
		return openContentMain(getContentWithAssert(path).getIndex());
	}

	/** {@inheritDoc} */
	@Override
	protected byte[] onReadAllBytesByEntry(ArchiveEntry entry) throws IOException {
		return readAllBytesMain(entry.getIndex());
	}

	/** {@inheritDoc} */
	@Override
	protected byte[] onReadAllBytesByIndex(int index) throws IOException {
		return readAllBytesMain(index);
	}

	/** {@inheritDoc} */
	@Override
	protected byte[] onReadAllBytesByPath(Path path) throws IOException {
		return readAllBytesMain(getContentWithAssert(path).getIndex());
	}

	/** {@inheritDoc} */
	@Override
	protected ArchiveEntry onGetEntryByIndex(int index) {
		return mCached ? mEntryList.get(index) : createEntry(index, false);
	}

	/** {@inheritDoc} */
	@Override
	protected ArchiveEntry onGetEntryByPath(Path path) {
		return mEntryMap.get(path);
	}

	/** {@inheritDoc} */
	@Override
	protected void onEnumEntries(EntryCallback callback) throws IOException {
		var isContinue = new AtomicBoolean(true);
		var entryMap = new TreeMap<Path, ArchiveEntry>();
		var entryList = IntStream.range(0, mNumOfItem)
				.takeWhile(i -> isContinue.get())
				.mapToObj(i -> createEntry(i, true))
				.peek(e -> isContinue.set(callback.call(e, e.getIndex() + 1, mNumOfItem)))
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
		return mCached ? mEntryList.size() : mNumOfItem;
	}

	/** {@inheritDoc} */
	@Override
	protected int onGetCapability() {
		return CAPS_INDEX | (mCached ? CAPS_PATH : 0);
	}

	/**
	 * 指定パスのコンテンツエントリ取得(全エントリがキャッシュされていることを前提とする)
	 * @param path コンテンツパス
	 * @return コンテンツエントリ情報
	 * @exception NoSuchFileException 指定パスのコンテンツが存在しない
	 * @exception NoSuchFileException 指定パスのエントリがコンテンツではない
	 */
	private ArchiveEntry getContentWithAssert(Path path) throws IOException {
		var entry = mEntryMap.get(path);
		if (entry == null) {
			// 指定されたパスのコンテンツは存在しない
			throw new NoSuchFileException(path.toString());
		} else if (!entry.isContent()) {
			// 指定されたパスはコンテンツ(ファイル)ではない
			throw new NoSuchFileException(path.toString(), null, "This is not a content");
		} else {
			// 指定コンテンツを返す
			return entry;
		}
	}

	/**
	 * エントリ情報生成
	 * @param index エントリインデックス
	 * @param dummyIfFail 生成失敗時にダミーエントリを生成するかどうか
	 * @return 生成したエントリ情報。入力エラー時、ダミーエントリ生成ONならダミーエントリ、OFFならnull。
	 */
	private ArchiveEntry createEntry(int index, boolean dummyIfFail) {
		try {
			return new EntryImpl(this, mArchive, index);
		} catch (IOException e) {
			// エントリ情報読み取りを試行した結果入力エラーが出る場合はエントリなしと見なす
			return dummyIfFail ? new EntryImpl(this, index) : null;
		}
	}

	/**
	 * コンテンツオープンのメイン処理
	 * @param index エントリインデックス
	 * @return コンテンツの入力ストリーム
	 * @exception IOException {@link #extractFile(int)} に準ずる
	 */
	private InputStream openContentMain(int index) throws IOException {
		return extractFile(index).getInputStream();
	}

	/**
	 * コンテンツのバイトデータ読み込みメイン処理
	 * @param index エントリインデックス
	 * @return コンテンツのバイトデータ
	 * @exception IOException {@link #extractFile(int)} に準ずる
	 */
	private byte[] readAllBytesMain(int index) throws IOException {
		return extractFile(index).getBytes();
	}

	/**
	 * コンテンツ解凍処理
	 * @param index エントリインデックス
	 * @return 解凍後コンテンツアクセス用ストリーム
	 * @exception NoSuchFileException 指定エントリがコンテンツではない
	 * @exception IOException アーカイブI/Fからのプロパティ読み取り失敗
	 * @exception IOException 解凍可能なコンテンツサイズ超過
	 * @exception IOException 入力エラー発生
	 */
	private SzjbMemoryStream extractFile(int index) throws IOException {
		// 指定されたコンテンツの情報を抽出する
		var entry = mCached ? mEntryList.get(index) : new EntryImpl(this, mArchive, index);
		if (!entry.isContent()) {
			// 指定されたエントリがコンテンツではない場合はオープン不可
			throw new NoSuchFileException(entry.getPath().toString(), null, "This is not a content");
		} else if (entry.getSize() > Integer.MAX_VALUE) {
			// オープン可能なサイズの上限を超過した
			var msg = String.format("Too large file size (%dbytes): %s", entry.getSize(), entry.getPath());
			throw new IOException(msg);
		} else {
			// Do nothing
		}

		// コンテンツをメモリ上に解凍する
		var bufferInMem = new SzjbMemoryStream((int)entry.getSize());
		mArchive.extractSlow(index, bufferInMem);
		return bufferInMem;
	}
}
