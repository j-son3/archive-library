package com.lmt.lib.archive;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface TestData {
	// テストデータが格納されたパス
	public static final Path LOCATION = Path.of("test", "com", "lmt", "lib", "archive", "data");
	public static final Path ARCHIVE_LOCATION = LOCATION.resolve("Archive");
	public static final Path STANDARD_TYPE_TESTER_LOCATION = LOCATION.resolve("StandardTypeTester");

	// Archive用(ただし他テストでも使用する)
	public static final Path FOLDER_ARCHIVE = ARCHIVE_LOCATION.resolve("test.dir");
	public static final Path ZIP_ARCHIVE = ARCHIVE_LOCATION.resolve("test.zip");
	public static final Path RAR4_ARCHIVE = ARCHIVE_LOCATION.resolve("test.4.rar");
	public static final Path RAR5_ARCHIVE = ARCHIVE_LOCATION.resolve("test.5.rar");
	public static final Path SEVEN_ZIP_ARCHIVE = ARCHIVE_LOCATION.resolve("test.7z");
	public static final Path BROKEN_ARCHIVE = ARCHIVE_LOCATION.resolve("broken.zip");

	// StandardTypeTester用
	public static final Path ZIP_NAME_FOLDER = STANDARD_TYPE_TESTER_LOCATION.resolve("test.zip");
	public static final Path RAR_NAME_FOLDER = STANDARD_TYPE_TESTER_LOCATION.resolve("test.rar");
	public static final Path SEVEN_ZIP_NAME_FOLDER = STANDARD_TYPE_TESTER_LOCATION.resolve("test.7z");
	public static final Path NON_EXTENSION_FILE = STANDARD_TYPE_TESTER_LOCATION.resolve("non_extension_file");

	// アーカイブ内フォルダ
	public static final Path LOCATION_EN_PATH = Path.of("english");
	public static final Path LOCATION_JP_PATH = Path.of("日本語");
	public static final Path LOCATION_KR_PATH = Path.of("한국어");

	// アーカイブ内コンテンツパス
	public static final Path CONTENT_ASCII_TXT_PATH = LOCATION_EN_PATH.resolve("ascii.txt");
	public static final Path CONTENT_RGB_BMP_PATH = LOCATION_EN_PATH.resolve("rgb.bmp");
	public static final Path CONTENT_DATA_BIN_PATH = LOCATION_JP_PATH.resolve("データ.bin");
	public static final Path CONTENT_GREETING_TXT_PATH = LOCATION_JP_PATH.resolve("挨拶.txt");
	public static final Path CONTENT_HELLO_C_PATH = LOCATION_KR_PATH.resolve("hello.c");
	public static final Path CONTENT_README_TXT_PATH = Path.of("readme.txt");

	// アーカイブ内コンテンツのバイト配列
	public static final byte[] CONTENT_ASCII_TXT_BYTES = bytes(
			"202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f",
			"505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e");
	public static final byte[] CONTENT_RGB_BMP_BYTES = bytes(
			"424d36030000000000003600000028000000100000001000000001001800000000000003000000000000000000000000",
			"000000000000241ced241ced241ced241ced241ced00ff0000ff0000ff0000ff0000ff0000ff00ff0000ff0000ff0000",
			"ff0000ff0000241ced241ced241ced241ced241ced00ff0000ff0000ff0000ff0000ff0000ff00ff0000ff0000ff0000",
			"ff0000ff0000241ced241ced241ced241ced241ced00ff0000ff0000ff0000ff0000ff0000ff00ff0000ff0000ff0000",
			"ff0000ff0000241ced241ced241ced241ced241ced00ff0000ff0000ff0000ff0000ff0000ff00ff0000ff0000ff0000",
			"ff0000ff0000241ced241ced241ced241ced241ced00ff0000ff0000ff0000ff0000ff0000ff00ff0000ff0000ff0000",
			"ff0000ff0000241ced241ced241ced241ced241ced00ff0000ff0000ff0000ff0000ff0000ff00ff0000ff0000ff0000",
			"ff0000ff0000241ced241ced241ced241ced241ced00ff0000ff0000ff0000ff0000ff0000ff00ff0000ff0000ff0000",
			"ff0000ff0000241ced241ced241ced241ced241ced00ff0000ff0000ff0000ff0000ff0000ff00ff0000ff0000ff0000",
			"ff0000ff0000241ced241ced241ced241ced241ced00ff0000ff0000ff0000ff0000ff0000ff00ff0000ff0000ff0000",
			"ff0000ff0000241ced241ced241ced241ced241ced00ff0000ff0000ff0000ff0000ff0000ff00ff0000ff0000ff0000",
			"ff0000ff0000241ced241ced241ced241ced241ced00ff0000ff0000ff0000ff0000ff0000ff00ff0000ff0000ff0000",
			"ff0000ff0000241ced241ced241ced241ced241ced00ff0000ff0000ff0000ff0000ff0000ff00ff0000ff0000ff0000",
			"ff0000ff0000241ced241ced241ced241ced241ced00ff0000ff0000ff0000ff0000ff0000ff00ff0000ff0000ff0000",
			"ff0000ff0000241ced241ced241ced241ced241ced00ff0000ff0000ff0000ff0000ff0000ff00ff0000ff0000ff0000",
			"ff0000ff0000241ced241ced241ced241ced241ced00ff0000ff0000ff0000ff0000ff0000ff00ff0000ff0000ff0000",
			"ff0000ff0000241ced241ced241ced241ced241ced00ff0000ff0000ff0000ff0000ff0000ff00ff0000ff0000ff0000",
			"ff0000ff0000");
	public static final byte[] CONTENT_DATA_BIN_BYTES = bytes(
			"000102030405060708090a0b0c0d0e0f000102030405060708090a0b0c0d0e0f000102030405060708090a0b0c0d0e0f",
			"000102030405060708090a0b0c0d0e0f000102030405060708090a0b0c0d0e0f000102030405060708090a0b0c0d0e0f",
			"000102030405060708090a0b0c0d0e0f000102030405060708090a0b0c0d0e0f000102030405060708090a0b0c0d0e0f",
			"000102030405060708090a0b0c0d0e0f");
	public static final byte[] CONTENT_GREETING_TXT_BYTES = bytes(
			"e3818ae381afe38288e38186e38194e38196e38184e381bee381990d0ae38193e38293e381abe381a1e381af0d0ae381",
			"95e38288e38186e381aae382890d0ae38193e38293e381b0e38293e381af0d0ae3818ae38284e38199e381bfe381aae3",
			"8195e381840d0a");
	public static final byte[] CONTENT_HELLO_C_BYTES = bytes(
			"23696e636c756465203c737464696f2e683e0d0a0d0a696e74206d61696e28696e7420617267632c2063686172202a61",
			"72677629207b0d0a202020207072696e7466282248656c6c6f20576f726c64215c6e22293b0d0a202020207265747572",
			"6e20303b0d0a7d0d0a");
	public static final byte[] CONTENT_README_TXT_BYTES = bytes(
			"54686973206172636869766520697320696E74656E64656420746F207465737420636F6D2E6C6D742E6C69622E617263",
			"686976652E0D0A5468652066696C65732073746F72656420696E2074686520617263686976652068617665206E6F2064",
			"6174612076616C75652E0D0A0D0A4A2D534F4E330D0A");

	// パスによる全コンテンツマップ
	public static final Map<Path, byte[]> ALL_CONTENTS = Map.ofEntries(
			Map.entry(CONTENT_ASCII_TXT_PATH, CONTENT_ASCII_TXT_BYTES),
			Map.entry(CONTENT_RGB_BMP_PATH, CONTENT_RGB_BMP_BYTES),
			Map.entry(CONTENT_DATA_BIN_PATH, CONTENT_DATA_BIN_BYTES),
			Map.entry(CONTENT_GREETING_TXT_PATH, CONTENT_GREETING_TXT_BYTES),
			Map.entry(CONTENT_HELLO_C_PATH, CONTENT_HELLO_C_BYTES),
			Map.entry(CONTENT_README_TXT_PATH, CONTENT_README_TXT_BYTES));

	// 全エントリのパス一覧
	public static final List<Path> ALL_ENTRY_PATHS = List.of(
			LOCATION_EN_PATH, LOCATION_JP_PATH, LOCATION_KR_PATH,
			CONTENT_ASCII_TXT_PATH, CONTENT_RGB_BMP_PATH, CONTENT_DATA_BIN_PATH,
			CONTENT_GREETING_TXT_PATH, CONTENT_HELLO_C_PATH, CONTENT_README_TXT_PATH);

	private static byte[] bytes(String...hexes) {
		var hex = Stream.of(hexes).collect(Collectors.joining());
		var len = hex.length() / 2;
		var ary = new byte[len];
		IntStream.range(0, len).forEach(i -> ary[i] = (byte)Integer.parseInt(hex.substring(i * 2, (i + 1) * 2), 16));
		return ary;
	}
}
