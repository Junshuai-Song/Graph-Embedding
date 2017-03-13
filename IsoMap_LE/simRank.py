'''
对SimRank值进行可视化
效果很不理想，基本什么都看不出来。几百个点就一团乱了。还是依赖统计信息，直方图什么的比较好。

Auther：Alan
'''

import argparse
import numpy as np
import networkx as nx
import matplotlib.pyplot as plt
import math


def parse_args():
    '''
    Parses the node2vec arguments.
    '''
    parser = argparse.ArgumentParser(description="Run node2vec.")

    parser.add_argument('--input', nargs='?', default='./data/0_333_5038.txt',
	                    help='Input graph path')
    parser.add_argument('--input_W', nargs='?', default='./data/0_333_5038_simrank_navie_top10.txt.sim.txt',
	                    help='Input graph path')

    parser.add_argument('--output', nargs='?', default='./data/output.txt',
	                    help='Embeddings path')

    parser.add_argument('--dimensions', type=int, default=128,
	                    help='Number of dimensions. Default is 128.')

    parser.add_argument('--walk-length', type=int, default=80,
	                    help='Length of walk per source. Default is 80.')

    parser.add_argument('--num-walks', type=int, default=10,
	                    help='Number of walks per source. Default is 10.')

    parser.add_argument('--window-size', type=int, default=10,
                    	help='Context size for optimization. Default is 10.')

    parser.add_argument('--iter', default=1, type=int,
                      help='Number of epochs in SGD')

    parser.add_argument('--workers', type=int, default=8,
	                    help='Number of parallel workers. Default is 8.')

    parser.add_argument('--p', type=float, default=1,
	                    help='Return hyperparameter. Default is 1.')

    parser.add_argument('--q', type=float, default=1,
	                    help='Inout hyperparameter. Default is 1.')

    parser.add_argument('--weighted', dest='weighted', action='store_true',
	                    help='Boolean specifying (un)weighted. Default is unweighted.')
    parser.add_argument('--unweighted', dest='unweighted', action='store_false')
    parser.set_defaults(weighted=False)

    parser.add_argument('--directed', dest='directed', action='store_true',
	                    help='Graph is (un)directed. Default is undirected.')
    parser.add_argument('--undirected', dest='undirected', action='store_false')
    parser.set_defaults(directed=False)

    return parser.parse_args()

def read_graph():
    '''
    Reads the input Graph in networkx.
    '''
    G = nx.read_edgelist(args.input, nodetype=int, data=(('weight',float),), create_using=nx.DiGraph())

    if not args.directed:
        G = G.to_undirected()

    return G

def read_simRank(input_W):
    # 从input_w路径中读取SimRank值，构造权重矩阵。
    sim = []
    with open(input_W, "r" ) as f:
        lines = f.readlines()
        for line in lines:
            w = []
            words = line.split(' ')
            for k in range(len(words)):
                if k==0:
                    continue
                elif k==len(words)-1:
                    w.append(words[k][:-1])    
                else:
                    w.append(words[k])
                
            sim.append(w)
    return sim
    
def learn_embeddings(input_W, k):
    '''
    使用LE进行Graph上顶点特征提取（权重基于计算的SimRank值）
    '''
    sim = read_simRank(input_W)   
    m = len(sim)
    
    W=np.mat(np.zeros([m,m]))   # 333*333
    D=np.mat(np.zeros([m,m]))  
    print("m = %d " % m)
    
    for i in range(m):  
#        k_index= knn(dataMat[i,:],dataMat,k)  
        k_index= sim[i] # 第i个顶点
        for j in range(min(k,len(k_index))):  
            t = k_index[j].split(':')
            local = int(t[0])
            value = float(t[1])
            W[i,local] = value
            D[i,i]+=W[i,local]  
        if D[i,i]==0:
            D[i,i] += 1e-6  # 不能为奇异矩阵
    L=D-W  
#    Dinv=np.linalg.inv(D)  
    
    X=np.dot(D.I,L)  
    lamda,f=np.linalg.eig(X)  

    return W,lamda,f,m
    
    

def show(W,lamda,f,m):
    fm,fn = np.shape(f)
    print('fm,fn:',fm,fn  )
    
    
    lamdaIndicies = np.argsort(lamda)  
    first=0  
    second=0  
    print(lamdaIndicies[0], lamdaIndicies[1])
    for i in range(fm):  
        if lamda[lamdaIndicies[i]].real > 1e-2 :    # 找到特征值比较小的2个，但是不为0的！
            print(lamda[lamdaIndicies[i]])
            first=lamdaIndicies[i]  
            second=lamdaIndicies[i+1]  
            break
    print(first, second)
    
    
    
    for i in range(m):
        nx_G = read_graph() # 利用networkx包读取Graph信息
        
        color = []
        vertex = []
        vertex.append(i)
        for j in range(m):
            if j==i:
                color.append(0.7)
            elif W[i,j]==0:
                color.append(0.1)    # 为0或1
            else:
                color.append(0.9)
                vertex.append(j)    #顶点j有关的所有边
#        print(vertex)
        # 通过相关联的点进行加边
        for ii in range(m):
            for jj in range(m):
                if nx_G.has_edge(ii,jj):
                    if (ii in vertex or jj in vertex):
                        continue
                    else:
                        nx_G.remove_edge(ii,jj)

        """
        fig=plt.figure('lowdata')  
        ax2 = fig.add_subplot(111)
        ax2.scatter(f[:,first], f[:,second],  c=color, cmap=plt.cm.Spectral)
        
        plt.show()
        """

        nx.draw_spring(nx_G,  with_labels=True, node_color=color)
        return
        
    
    return

    

if __name__ == "__main__":
    args = parse_args()
    W,lamda,f,m = learn_embeddings(args.input_W, 10)   # k = topK = 10
    show(W,lamda,f,m)