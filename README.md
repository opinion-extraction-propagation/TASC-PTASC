TASC: topic-adaptive sentiment classification
======================================

This is a parallel implementation of multiple-label classification based on Spark MLlib. 

### Overview <br />

    内部循环选择添加非标注数据集
    1.将训练数据split成文本特征和非文本特征训练数据
    2.全局选择非标注数据集，已经被选择过的数据不再被选择
    3.将每轮选择的非标注测试数据集添加到标训练数据集中
     
    外部循环选择添加话题相关情感词
    1.根据当前时刻选择的所有非标注数据集，选择话题相关情感词
    2.然后根据配置文件中的map得到三元组：选择的非标注测试数据集，话题相关情感词 

### Related works <br />
**Please cite the following references in your related work.**

[1] Shenghua Liu, Fuxin Li, Fangtao Li, Xueqi Cheng, and Huawei Shen, “Adaptive co-training svm for sentiment classification on tweets” in Proc. of the 22nd ACM International Conference on Information and Knowledge Management, (CIKM ’13). New York, NY, USA, 2013, pp. 2079–2088. 

[2] Shenghua Liu, Fuxin Li, and Fangtao Li, Xueqi Cheng, “TASC: Topic-Adaptive Sentiment Classification on Dynamic Tweets” IEEE Transactions on Knowledge and Data Engineering (TKDE), preprint 2014.
