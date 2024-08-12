package com.lmt.lib.archive;

import java.nio.file.Path;

/**
 * アーカイブ内エントリの詳細情報を表すクラスです。
 *
 * @author J-SON3
 */
public abstract class ArchiveEntry {
	/** エントリのオーナー */
	protected Archive owner;
	/** エントリのインデックス値 */
	protected Integer index;
	/** エントリのパス */
	protected Path path;
	/** 場所(フォルダ)かどうか */
	protected boolean isLocation;
	/** コンテンツ(ファイル)かどうか */
	protected boolean isContent;
	/** サイズ(バイト数) */
	protected long size;
	/** 最終更新日時 */
	protected long lastModified;

	/**
	 * このエントリが場所(フォルダ)を表すかどうかを返します。
	 * @return 場所(フォルダ)の場合true
	 */
	public boolean isLocation() {
		return this.isLocation;
	}

	/**
	 * このエントリがコンテンツ(ファイル)を表すかどうかを返します。
	 * @return コンテンツ(ファイル)の場合true
	 */
	public boolean isContent() {
		return this.isContent;
	}

	/**
	 * インデックス値を取得します。
	 * @return インデックス値
	 */
	public int getIndex() {
		return this.index;
	}

	/**
	 * パスを取得します。
	 * @return パス
	 */
	public Path getPath() {
		return this.path;
	}

	/**
	 * サイズ(バイト数)を取得します。
	 * @return サイズ(バイト数)
	 */
	public long getSize() {
		return this.size;
	}

	/**
	 * 最終更新日時を取得します。
	 * @return 最終更新日時
	 */
	public long getLastModified() {
		return this.lastModified;
	}
}
