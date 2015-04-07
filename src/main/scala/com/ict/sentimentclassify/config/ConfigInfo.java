package com.ict.sentimentclassify.config;

/**
 * configuration information some parameters are from config.php which is used
 * for preprocessing
 * 
 * @author lifuxin
 * @Email: lifuxin1125@gmail.com
 * 
 */
public class ConfigInfo {

	/**
	 * data set
	 */
	private String trainFile;
	private String testFile;
	private String classifyStatResultDir;

	private String topicwordFile;

	private String positiveTopicWordWeightFile;
	private String neutraltopicwordweightfile;
	private String negativetopicwordweightfile;

	private String tweetWordStatFile;
	private String trainTweetWordStatFile;
	private String topicWordFreqStatFile;
	private String trainTopicWordFreqStatFile;

	public String getTweetWordStatFile() {
		return tweetWordStatFile;
	}

	public void setTweetWordStatFile(String tweetWordStatFile) {
		this.tweetWordStatFile = tweetWordStatFile;
	}

	public String getTrainTweetWordStatFile() {
		return trainTweetWordStatFile;
	}

	public void setTrainTweetWordStatFile(String trainTweetWordStatFile) {
		this.trainTweetWordStatFile = trainTweetWordStatFile;
	}

	public String getTopicWordFreqStatFile() {
		return topicWordFreqStatFile;
	}

	public void setTopicWordFreqStatFile(String topicWordFreqStatFile) {
		this.topicWordFreqStatFile = topicWordFreqStatFile;
	}

	public String getTrainTopicWordFreqStatFile() {
		return trainTopicWordFreqStatFile;
	}

	public void setTrainTopicWordFreqStatFile(String trainTopicWordFreqStatFile) {
		this.trainTopicWordFreqStatFile = trainTopicWordFreqStatFile;
	}

	public String getPositiveTopicWordWeightFile() {
		return positiveTopicWordWeightFile;
	}

	public void setPositiveTopicWordWeightFile(
			String positiveTopicWordWeightFile) {
		this.positiveTopicWordWeightFile = positiveTopicWordWeightFile;
	}

	public String getNeutraltopicwordweightfile() {
		return neutraltopicwordweightfile;
	}

	public void setNeutraltopicwordweightfile(String neutraltopicwordweightfile) {
		this.neutraltopicwordweightfile = neutraltopicwordweightfile;
	}

	public String getNegativetopicwordweightfile() {
		return negativetopicwordweightfile;
	}

	public void setNegativetopicwordweightfile(
			String negativetopicwordweightfile) {
		this.negativetopicwordweightfile = negativetopicwordweightfile;
	}

	public String getTopicwordFile() {
		return topicwordFile;
	}

	public void setTopicwordFile(String topicwordFile) {
		this.topicwordFile = topicwordFile;
	}

	/**
	 * feature and feature position
	 */
	private int emocValuePosition;
	private int numNegativeWordsPosition;
	private int labelPosition;
	private int numPublicSentimentWord;
	private int numPrivateSentimentWord;
	private int numNontextFeature;

	/**
	 * model parameters
	 */
	private int numPeriod;
	private double classifyResultThreshold;
	private int numTopKTestData2TrainSet;
	private int maxIteration;
	private int numTopKPrivateWord;

	/**
	 * classify result visualization parameter
	 */
	private int numLayer;

	public String getTrainFileName() {
		return trainFile;
	}

	public void setTrainFileName(String trainFileName) {
		this.trainFile = trainFileName;
	}

	public String getTestFileName() {
		return testFile;
	}

	public void setTestFileName(String testFileName) {
		this.testFile = testFileName;
	}

	public String getClassifyStatResultDir() {
		return classifyStatResultDir;
	}

	public void setClassifyStatResultDir(String classifyStatResultDir) {
		this.classifyStatResultDir = classifyStatResultDir;
	}

	public int getEmocValuePosition() {
		return emocValuePosition;
	}

	public void setEmocValuePosition(int emocValuePosition) {
		this.emocValuePosition = emocValuePosition;
	}

	public int getNumNegativeWordsPosition() {
		return numNegativeWordsPosition;
	}

	public void setNumNegativeWordsPosition(int numNegativeWordsPosition) {
		this.numNegativeWordsPosition = numNegativeWordsPosition;
	}

	public int getLabelPosition() {
		return labelPosition;
	}

	public void setLabelPosition(int labelPosition) {
		this.labelPosition = labelPosition;
	}

	public int getNumPublicSentimentWord() {
		return numPublicSentimentWord;
	}

	public void setNumPublicSentimentWord(int numPublicSentimentWord) {
		this.numPublicSentimentWord = numPublicSentimentWord;
	}

	public int getNumPrivateSentimentWord() {
		return numPrivateSentimentWord;
	}

	public void setNumPrivateSentimentWord(int numPrivateSentimentWord) {
		this.numPrivateSentimentWord = numPrivateSentimentWord;
	}

	public int getNumNontextFeature() {
		return numNontextFeature;
	}

	public void setNumNontextFeature(int numNontextFeature) {
		this.numNontextFeature = numNontextFeature;
	}

	public int getNumPeriod() {
		return numPeriod;
	}

	public void setNumPeriod(int numPeriod) {
		this.numPeriod = numPeriod;
	}

	public double getClassifyResultThreshold() {
		return classifyResultThreshold;
	}

	public void setClassifyResultThreshold(double classifyResultThreshold) {
		this.classifyResultThreshold = classifyResultThreshold;
	}

	public int getNumTopKTestData2TrainSet() {
		return numTopKTestData2TrainSet;
	}

	public void setNumTopKTestData2TrainSet(int numTopKTestData2TrainSet) {
		this.numTopKTestData2TrainSet = numTopKTestData2TrainSet;
	}

	public int getMaxIteration() {
		return maxIteration;
	}

	public void setMaxIteration(int maxIteration) {
		this.maxIteration = maxIteration;
	}

	public int getNumTopKPrivateWord() {
		return numTopKPrivateWord;
	}

	public void setNumTopKPrivateWord(int numTopKPrivateWord) {
		this.numTopKPrivateWord = numTopKPrivateWord;
	}

	public int getNumLayer() {
		return numLayer;
	}

	public void setNumLayer(int numLayer) {
		this.numLayer = numLayer;
	}

}
