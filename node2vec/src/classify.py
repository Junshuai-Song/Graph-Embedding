from sklearn import linear_model
import numpy as np

def get_groups(groups_path):
    """
    获得处理好的顶点类别分组
    :param groups_path: 
    :return: 
    """
    groups_labels = []
    with open(groups_path) as f:
        groups = f.readlines();
        class_num = 2
    return groups, groups_labels

def get_data(label, num, model):
    """
    为了保证均匀性，对于每个1/10小组中的数据，我们选择
    :param label: 当前使用的类别label
    :param num: 分成num份
    :param model: word2vec获得的所有顶点的k维特征表示
    :return: 按照类别label划分成num份的list
    """
    X = []
    Y = []

    return X,Y

def get_split(i, X, Y):
    """
    
    :param i: 正反类别各取(i+1)/10 
    :param X: 全部集合X
    :param Y: 全部集合Y
    :return: 
    """

    train_X,train_Y,test_X,test_Y = []

    return train_X,train_Y,test_X,test_Y

def classification(groups_path, model):
    """
    利用输入参数中顶点参数：
    :param groups_path: groups_path 顶点类别文件, model, 生成的每个单词的词向量 
    :param model: 每个顶点的维度向量，由word2vec得来
    :return: None
    """
    groups,groups_labels = get_groups(groups_path);
    num = 10
    for label in groups_labels:
        # 把样本按照类别i分成10分
        X, Y = get_data(label, 10, model)
        for i in range(num):
            train_X,train_Y,test_X,test_Y = get_split(i,X,Y)
            clf = linear_model.LogisticRegression(solver='sag', max_iter=100, random_state=42,multi_class="multinomial").fit(train_X, train_Y)
            Z = clf.predict(np.c_[test_X.ravel(), test_Y.ravel()])
            print(Z)


if __name__ == "__main__":
    print("hello, there is classify.")

