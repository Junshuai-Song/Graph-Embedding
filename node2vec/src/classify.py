from sklearn import linear_model
import numpy as np

def get_groups(groups_path):
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
    """   
    for group in groups.values():
        s = set()
        for t in group:
            s.add(t)
        if len(s)>1 :
            print(group)
    """
    return groups, labels

def get_data(label, num, model, groups):
    """
    为了保证均匀性，对于每个1/10小组中的数据，我们选择
    :param label: 当前使用的类别label
    :param num: 分成num份
    :param model: word2vec获得的所有顶点的k维特征表示
    :return: 按照类别label划分成num份的list
    """
    X_1 = []
    X_0 = []
    Y_1 = []
    Y_0 = []
    for key in model.wv:
        # print(key)

        if label in groups[key]:    # 01分类
            Y_1.append(1)
            X_1.append(model.wv[key])
        else:
            Y_0.append(0)
            X_0.append(model.wv[key])

    # 之后在两者中将其分为num（10）份
    X = []
    Y = []
    batch_0 = int(1.0 * len(X_0) / num)
    batch_1 = int(1.0 * len(X_1) / num)
    for i in range(num):
        start = i*batch_0
        X.append(X_0[start:start + batch_0])
        Y.append(Y_0[start:start + batch_0])

        start = i*batch_1
        X.append(X_0[start:start + batch_1])
        Y.append(Y_0[start:start + batch_1])

    return X,Y

def get_split(k, num, X, Y):
    """
    
    :param i: 正反类别各取(i+1)/10 
    :param num: 一共分成num份 
    :param X: 全部集合X
    :param Y: 全部集合Y
    :return: 
    """
    train_X, train_Y, test_X, test_Y = []
    batch = len(X)/num
    for i in range(num):
        start = i*batch
        if k==i:
            train_X.append(X[start:start+batch])
            train_Y.append(Y[start:start+batch])
        else:
            test_X.append(X[start:start + batch])
            test_Y.append(Y[start:start + batch])

    return train_X,train_Y,test_X,test_Y

def classification(groups_path, model):
    """
    利用输入参数中顶点参数：
    :param groups_path: groups_path 顶点类别文件, model, 生成的每个单词的词向量 
    :param model: 每个顶点的维度向量，由word2vec得来
    :return: None
    """
    groups,groups_labels = get_groups(groups_path)
    num = 10
    for label in groups_labels:
        # 把样本按照类别i分成10分
        X, Y = get_data(label, 10, model, groups)
        for i in range(num):
            train_X,train_Y,test_X,test_Y = get_split(i,10,X,Y)
            clf = linear_model.LogisticRegression(solver='sag', max_iter=100, random_state=42,multi_class="multinomial").fit(train_X, train_Y)
            Z = clf.predict(np.c_[test_X.ravel(), test_Y.ravel()])
            print(Z)


if __name__ == "__main__":
    print("hello, there is classify.")

