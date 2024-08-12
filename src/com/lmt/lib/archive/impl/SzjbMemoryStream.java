package com.lmt.lib.archive.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZipException;

/**
 * コンテンツをメモリ上へ解凍する用のストリーム
 *
 * <p>7-ZIP J-Bindingライブラリ標準ストリームはメモリ領域が分割されていて、バイトデータを読み取る際にコピーが発生する。
 * それにより当ライブラリ提供の機能のパフォーマンスが低下するためメモリ領域が連続するストリームを用意する。</p>
 *
 * @author J-SON3
 */
class SzjbMemoryStream implements ISequentialOutStream {
	/** 解凍データバッファ */
	private byte[] mBuffer;
	/** バッファへのデータ書き込み位置 */
	private int mPos;

	/**
	 * コンストラクタ
	 * @param size 解凍後データサイズ
	 */
	SzjbMemoryStream(int size) {
		mBuffer = new byte[(int)size];
		mPos = 0;
	}

	/**
	 * 入力ストリーム取得
	 * @return 入力ストリーム
	 */
	InputStream getInputStream() {
		return new ByteArrayInputStream(mBuffer);
	}

	/**
	 * 解凍データバッファ取得
	 * @return 解凍データバッファ
	 */
	byte[] getBytes() {
		return mBuffer;
	}

	/** {@inheritDoc} */
	@Override
	public int write(byte[] data) throws SevenZipException {
		var length = data.length;
		System.arraycopy(data, 0, mBuffer, mPos, length);
		mPos += length;
		return length;
	}
}
