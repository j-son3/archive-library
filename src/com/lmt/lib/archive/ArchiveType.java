package com.lmt.lib.archive;

import static com.lmt.lib.archive.Assertion.*;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import com.lmt.lib.archive.impl.FolderArchive;
import com.lmt.lib.archive.impl.SzjbArchive;

/**
 * アーカイブの種別を表す列挙型です。
 *
 * @author J-SON3
 */
public enum ArchiveType {
	/**
	 * フォルダアーカイブ
	 * <p>フォルダをアーカイブファイルと見なします。指定フォルダ配下の各サブフォルダとファイルがエントリになります。</p>
	 * <p>対応クラス：{@link FolderArchive}</p>
	 */
	FOLDER(p -> new FolderArchive(p), false),
	/**
	 * ZIPファイル
	 * <p>対応クラス：{@link SzjbArchive.Zip}</p>
	 */
	ZIP(p -> new SzjbArchive.Zip(p), true),
	/**
	 * 7-ZIPファイル
	 * <p>対応クラス：{@link SzjbArchive.SevenZip}</p>
	 */
	SEVEN_ZIP(p -> new SzjbArchive.SevenZip(p), true),
	/**
	 * RARファイル
	 * <p>この形式ではRAR4/RAR5の両方に対応し、RAR5形式でのオープンを優先します。</p>
	 * <p>対応クラス：{@link SzjbArchive.Rar}</p>
	 */
	RAR(p -> new SzjbArchive.Rar(p), true),
	/**
	 * アーカイブ種別不明
	 * <p>アーカイブライブラリが対応しない形式の種別であることを表します。</p>
	 */
	UNKNOWN(p -> {throw new UnsupportedOperationException("Can't open archive because unknown archive type.");}, false);

	/** アーカイブのオープン処理インターフェイス */
	@FunctionalInterface
	private interface Creator {
		Archive open(Path path) throws IOException;
	}

	/** アーカイブオープン処理 */
	private Creator mCreator;
	/** アーカイブがファイルであるかどうか */
	private boolean mIsFileArchive;

	/**
	 * コンストラクタ
	 * @param creator アーカイブオープン処理
	 */
	private ArchiveType(Creator creator, boolean isFileArchive) {
		mCreator = creator;
		mIsFileArchive = isFileArchive;
	}

	/**
	 * フォルダアーカイブかどうかを返します。
	 * @return フォルダアーカイブであればtrue
	 * @see #FOLDER
	 */
	public boolean isFolderArchive() {
		return !isUnknown() && !mIsFileArchive;
	}

	/**
	 * ファイルアーカイブかどうかを返します。
	 * @return ファイルアーカイブであればtrue
	 * @see #ZIP
	 * @see #SEVEN_ZIP
	 * @see #RAR
	 */
	public boolean isFileArchive() {
		return !isUnknown() && mIsFileArchive;
	}

	/**
	 * アーカイブ種別が不明かどうかを返します。
	 * @return アーカイブ種別が不明であればtrue
	 * @see #UNKNOWN
	 */
	public boolean isUnknown() {
		return this == UNKNOWN;
	}

	/**
	 * アーカイブの種別に応じたオープン処理を実行します。
	 * <p>引数で指定するパスは、当該アーカイブ種別が対応する形式(ファイルまたはフォルダ)を指定する必要があります。
	 * 非対応の形式を指定すると例外をスローします。</p>
	 * @param path オープンするアーカイブのパス
	 * @return オープンされたアーカイブオブジェクト
	 * @exception NoSuchFileException 指定パスのアーカイブが見つからない
	 * @exception IOException 指定パスのアーカイブの読み取り権限がない
	 * @exception IOException 指定パスがアーカイブとして認識出来ないオブジェクトを示している
	 * @exception IOException アーカイブ形式判定中に任意の例外がスローされた
	 * @exception NullPointerException pathがnull
	 * @exception UnsupportedOperationException {@link #UNKNOWN} に対して当メソッドを呼び出した
	 * @see ArchiveManager#open(Path)
	 */
	public Archive open(Path path) throws IOException {
		assertArgNotNull(path, "path");
		return mCreator.open(path);
	}
}
