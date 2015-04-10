package com.ict.sentimentclassify.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;

import org.apache.log4j.Logger;

/**
 * 
 * @author Felix
 * @email: lifuxin1125@gmail.com
 */
public class FileUtil {
	static Logger logger = Logger.getLogger(FileUtil.class);

	/**
	 * test whether path is exist or not
	 * 
	 * @param path
	 * @return if exist return true, else return false
	 */
	public static boolean pathIsExist(String path) {
		File file = new File(path);
		return file.exists();
	}

	/**
	 * fisrt test whether directory is exist or not, if not exist, then create
	 * path else return true
	 * 
	 * @param dirPath
	 * @return true exist or create directory success, else false
	 */
	public static boolean createDir(String dirPath) {
		File file = new File(dirPath);
		if (false == file.exists()) {
			return file.mkdirs();
		}
		return true;
	}

	/**
	 * create path and its father directory
	 * 
	 * @param path
	 * @return
	 */
	public static boolean createFile(String path) {
		File file = new File(path);
		if (file.exists() == false) {
			if (file.getParentFile().exists() == false) {
				if (file.getParentFile().mkdirs() == false) {
					return false;
				}
			}
		}

		try {
			return file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("IOException:", e);
			return false;
		}

	}

	/**
	 * save content into file
	 * 
	 * @param content
	 * @param charset
	 * @param path
	 * @return
	 */
	public static String saveFile(String content, String charset,
			String rootPath, String path) {

		long timeNow = Calendar.getInstance().getTimeInMillis();
		String filePath = path + "/" + timeNow + ".html";
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(new File(
					rootPath + filePath));
			// OutputStreamWriter outputStreamWriter = new
			// OutputStreamWriter(fileOutputStream,charset);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
					fileOutputStream);
			// outputStreamWriter.write(new String(content.getBytes(charset)));
			outputStreamWriter.write(content);
			outputStreamWriter.flush();
			outputStreamWriter.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			logger.error("FileNotFoundException: ", e);
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("IOException: ", e);
			return null;
		}
		return filePath;
	}

	/**
	 * merge file directory and file name with '/'
	 * 
	 * @return merged path
	 */
	public static String mergeFileDirAndName(String fileDir, String fileName) {
		if (fileName == null || fileName.length() == 0)
			logger.error("fileName is empty");
		if (fileDir.endsWith("/")) {
			return fileDir + fileName;
		} else {
			return fileDir + "/" + fileName;
		}
	}
}
