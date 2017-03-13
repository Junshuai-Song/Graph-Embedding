### swissRallData.m 
swiss roll data的matlab实现
### LE.py
LE算法的python实现
### IsoMap和LE小结
《IsoMap与LE.pdf》
### 尝试可视化了SimRank的一个Top10结构
初步想通过LE来让Sim值相近的点降维后，在二维空间中也比较相近，之后可视化其相关连边可能更清楚一点；但后来发现，通过networkx包可视化的时候，如果只显示特定部分顶点和边，它会较好地将其它无关边扩展至周围，如simrank.png所示。


### 参考：
1. ISOMAP:《A Global Geometric Framework for Nonlinear Dimensionality Reduction》

2. LE:《Laplacian Eigenmaps for Dimensionality Reduction and Data Representation》

3. Swiss Roll Data数据生成：http://people.cs.uchicago.edu/~dinoj/manifold/swissroll.html

4. LE实现：http://blog.csdn.net/hustlx/article/details/50850342#t1

5. 《Network Representation Learning》-清华崔鹏

6. 《流行学习》-浙大何晓飞




