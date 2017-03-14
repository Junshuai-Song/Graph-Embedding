## 代码来自：http://snap.stanford.edu/node2vec/#code

原文代码较为完善，这里主要对代码增加一些注释及补充说明，

参见：https://github.com/songjs1993/Graph-Embedding/blob/master/node2vec/node2vec.pdf

### 注
原文中个人觉得公示表达有点小错误，整体思路是对的，对于文中公式（2）中：f(u)表示顶点u的词向量；但是f(ni)表示的是顶点ni的一个辅助变量theta，是待训练参数（词向量也是待训练的）！并不是顶点ni的词向量。所以最好不要用同一个符号。可以写成g(ui)*f(u)等。

解释可以参见word2vec的基本推导，此处引：http://blog.csdn.net/itplus/article/details/37998797
