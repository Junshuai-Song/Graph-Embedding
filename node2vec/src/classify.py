from sklearn.multiclass import OneVsRestClassifier
from sklearn import linear_model
import numpy as np
from sklearn import metrics
import random
from sklearn import datasets

def get_groups(args):
    """
    获得处理好的顶点类别分组
    :param groups_path: 
    :return: 
    """
    groups = {}
    labels = set()
    with open(args.groups) as f:
        lines = f.readlines()
        for line in lines:
            words = line.split(",")
            if words[0] in groups:
                t = groups[words[0]]
                del groups[words[0]]
                t.append(words[1][:-1])
                groups[words[0]] = t
            else:
                groups[words[0]] = [words[1][:-1]]
            labels.add(words[1][:-1])

    return groups, labels

def get_data(num, embeddings, groups, class_num):
    """
    为了保证均匀性，对于每个1/10小组中的数据，我们选择
    :param label: 当前使用的类别label
    :param num: 分成num份
    :param model: word2vec获得的所有顶点的k维特征表示
    :return: 按照类别label划分成num份的list
    """
    X = []
    Y = []

    for key in embeddings:
        # print(key)
        y = [0] * class_num
        for label in groups[key]:    # 各个类别
            y[int(label)-1]=1
        Y.append(y)
        X.append(embeddings[key])

    return np.array(X),np.array(Y)

def get_split(k, num, X, Y):
    """
    
    :param i: 正反类别各取(i+1)/10 
    :param num: 一共分成num份 
    :param X: 全部集合X
    :param Y: 全部集合Y
    :return: 
    """

    batch = int(1.0 * len(X) / num)
    train = random.sample(range(0, len(X)), k*batch)
    train_X = X[train]
    train_Y = Y[train]

    tot = [i for i in range(len(X))]
    test = list(set(tot) - set(train))
    test_X = X[test]
    test_Y = Y[test]

    return train_X,train_Y,test_X,test_Y


def read_embedding(args):
    """
    从node2vec中生成的文件中读取embedding
    :param args: 
    :return: 
    """
    with open(args.output) as f :
        lines = f.readlines()
        num = int(lines[0].split(" ")[0])
        dimension = int(lines[0].split(" ")[1])
        lines = lines[1:]
        embeddings = {}
        for line in lines:
            words = line.split(" ")
            embedding = []
            for j in range(1, len(words)):
                embedding.append(float(words[j]))
            embeddings[words[0]] = embedding
    return embeddings

def classification(args):
    """
    利用输入参数中顶点参数：
    :param groups_path: groups_path 顶点类别文件, model, 生成的每个单词的词向量 
    :param model: 每个顶点的维度向量，由word2vec得来
    :return: None
    """

    embeddings = read_embedding(args)
    groups,groups_labels = get_groups(args)
    # print(groups_labels)

    num = 10
    ans = []
    X, Y = get_data(10, embeddings, groups, len(groups_labels))
    for i in range(1, num):
        # 不同比例的训练数据进行测试
        cnt = 0.0
        tot = 0.0
        #for label in groups_labels:
        # 把样本按照类别label分成10分

        train_X,train_Y,test_X,test_Y = get_split(i,10,X,Y)
        print(train_X.shape, train_Y.shape, test_X.shape, test_Y.shape)
        # print(X[0:1])
        # print(Y[0:1])
        """
        if(len(set(train_Y))<=1):
            print("train set length <= 1.")
            continue
        if(len(set(test_Y))<=1):
            print("test set length <= 1.")
            continue
        """

        clf = OneVsRestClassifier(linear_model.LogisticRegression(penalty='l2').fit(train_X, train_Y))
        # Z = clf.predict(np.c_[test_X.ravel(), test_Y.ravel()])
        y_pred = clf.predict(test_X)

        # print("  pre=" + str(np.mean(y_pred == test_Y)) + "  f1-score" + metrics.classification_report(test_Y, y_pred)[145:149])
        print(metrics.classification_report(test_Y, y_pred))
        # cnt += float(metrics.classification_report(test_Y, y_pred)[145:149])
        #tot += 1.0
        #print("i:" + str(i) + "  avg f1-score: " + str(1.0*cnt/tot) + "\n")
        #ans.append(1.0*cnt/tot)
    #print(ans)


import numpy
from sklearn.multiclass import OneVsRestClassifier
from sklearn.linear_model import LogisticRegression
# from itertools import izip
from sklearn.metrics import f1_score
from scipy.io import loadmat
from sklearn.utils import shuffle as skshuffle
import gensim
from collections import defaultdict
from scipy.sparse import lil_matrix
from gensim.models import Word2Vec

class TopKRanker(OneVsRestClassifier):
    def predict(self, X, top_k_list):
        assert X.shape[0] == len(top_k_list)
        probs = numpy.asarray(super(TopKRanker, self).predict_proba(X))
        all_labels = []
        for i, k in enumerate(top_k_list):
            probs_ = probs[i, :]
            labels = self.classes_[probs_.argsort()[-k:]].tolist()
            all_labels.append(labels)
        return all_labels

def sparse2graph(x):
    G = defaultdict(lambda: set())
    cx = x.tocoo()
    for i,j,v in zip(cx.row, cx.col, cx.data):
        G[i].add(j)
    return {str(k): [str(x) for x in v] for k,v in G.items()}

def scoring(args):

    # 0. Files
    embeddings_file = '../../data/BlogCatalog-dataset/data/blog_'+str(0.25)+'_'+str(0.25)+'.emb'
    matfile = "blogcatalog.mat"

    # 1. Load Embeddings
    #model = Word2Vec.load_word2vec_format(embeddings_file)
    model = gensim.models.KeyedVectors.load_word2vec_format(embeddings_file)

    # 2. Load labels
    mat = loadmat(matfile)
    A = mat['network']
    graph = sparse2graph(A)
    labels_matrix = mat['group']
    print("type(labels):", type(labels_matrix))             #type(labels): <class 'scipy.sparse.csc.csc_matrix'>
    print(labels_matrix.shape)
    # Map nodes to their features (note:  assumes nodes are labeled as integers 1:N)
    features_matrix = numpy.asarray([model[str(node)] for node in range(1,len(graph)+1)])

    # 2. Shuffle, to create train/test groups
    shuffles = []
    number_shuffles = 3
    for x in range(number_shuffles):
        shuffles.append(skshuffle(features_matrix, labels_matrix))

    # 3. to score each train/test group
    all_results = defaultdict(list)

    # training_percents = [0.1, 0.5, 0.9]
    # uncomment for all training percents
    training_percents = numpy.asarray(range(1,10))*.1
    for train_percent in training_percents:
        for shuf in shuffles:

            X, y = shuf

            training_size = int(train_percent * X.shape[0])

            X_train = X[:training_size, :]
            y_train_ = y[:training_size]

            y_train = [[] for x in range(y_train_.shape[0])]

            cy = y_train_.tocoo()
            for i, j in zip(cy.row, cy.col):
                y_train[i].append(j)

            assert sum(len(l) for l in y_train) == y_train_.nnz

            X_test = X[training_size:, :]
            y_test_ = y[training_size:]

            y_test = [[] for x in range(y_test_.shape[0])]

            cy = y_test_.tocoo()
            for i, j in zip(cy.row, cy.col):
                y_test[i].append(j)

            # clf = TopKRanker(LogisticRegression())
            clf = TopKRanker(LogisticRegression(penalty='l2'))
            clf.fit(X_train, y_train)

            # find out how many labels should be predicted
            top_k_list = [len(l) for l in y_test]
            preds = clf.predict(X_test, top_k_list)

            results = {}
            #averages = ["samples", "micro", "macro", "weighted"]
            averages = ["micro", "macro"]
            for average in averages:
                results[average] = f1_score(y_test, preds, average=average)
                print(results[average])
            all_results[train_percent].append(results)

    print('Results, using embeddings of dimensionality', X.shape[1])
    print('-------------------')
    for train_percent in sorted(all_results.keys()):
        print('Train percent:', train_percent)
        for x in all_results[train_percent]:
            print(x)
        print('-------------------')


if __name__ == "__main__":
    print("hello, there is classify.")

