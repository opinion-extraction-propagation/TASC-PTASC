package com.ict.sentimentclassify.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.apache.log4j.Logger;

/**
 * append content to file Utility Class
 * 
 * @author lifuxin
 * @email lifuxin1125@gmail.com
 * 
 */
public class AppendContent2File {
	static Logger logger = Logger.getLogger(AppendContent2File.class.getName());

	public String fileName = "";
	public FileOutputStream fileOutputStream;
	public OutputStreamWriter outputStreamWriter;
	public BufferedWriter bufferedWriter;

	public AppendContent2File(String fileName) {
		try {
			this.fileName = fileName;
			this.fileOutputStream = new FileOutputStream(fileName, true);
			this.outputStreamWriter = new OutputStreamWriter(fileOutputStream);
			this.bufferedWriter = new BufferedWriter(outputStreamWriter);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception when AppendContent2File initialize");
		}
	}

	public AppendContent2File(String fileName, boolean append) {
		try {
			this.fileName = fileName;
			this.fileOutputStream = new FileOutputStream(fileName, append);
			this.outputStreamWriter = new OutputStreamWriter(fileOutputStream);
			this.bufferedWriter = new BufferedWriter(outputStreamWriter);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception when outputfileclass initialize");
		}
	}

	public String getFileName() {
		return this.fileName;
	}

	public boolean appendContent2File(String content) {
		try {
			this.bufferedWriter.write(content + "\n");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("writting to " + this.fileName + " error");
			return false;
		}
		return true;
	}

	public boolean appendContent2File(int content) {
		try {
			this.bufferedWriter.write(content + "\n");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("writting to " + this.fileName + " error");
			return false;
		}
		return true;
	}

	public boolean appendContent2File(double content) {
		try {
			this.bufferedWriter.write(content + "\n");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("writting to " + this.fileName + " error");
			return false;
		}
		return true;
	}

	public boolean appendContent2File(float content) {
		try {
			this.bufferedWriter.write(content + "\n");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("writting to " + this.fileName + " error");
			return false;
		}
		return true;
	}

	public boolean closeFileWriter() {
		try {
			this.bufferedWriter.flush();
			this.bufferedWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(this.fileName + " closing error");
			return false;
		}
		return true;
	}
}
