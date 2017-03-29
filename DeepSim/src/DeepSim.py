import gzip
import pickle as cPickle
import numpy as np
import os
import tensorflow as tf
import networkx as nx
import random

# Auther: Alan
"""

"""

class DeepSim:
    topK = 200  # 记录每个顶点topK相似的顶点sim值


    def __init__(self, args, groups):
        self.args = args
        self.read_graph()
        self.groups = groups


    def read_graph(self):
        args = self.args
        if args.weighted:
            G = nx.read_edgelist(args.input, nodetype=int, data=(('weight', float),), create_using=nx.DiGraph(),
                                 delimiter=args.delimiter)
        else:
            G = nx.read_edgelist(args.input, nodetype=int, create_using=nx.DiGraph(), delimiter=args.delimiter)
            for edge in G.edges():
                G[edge[0]][edge[1]]['weight'] = 1
        if not args.directed:
            G = G.to_undirected()

        self.G = G

    def calculate_simrank_all(self):
        """
        计算完整的SimRank值
        :return: 
        """
        simrank = []

        return simrank

    def random_walk(self):

        walks = []

        # 这个跑完也保存到文件中，如果下次文件存在base_path + "walks.txt"，那么就不再重新Walk
        self.walks = walks

    def calculate_simrank_random(self):
        """
        通过游走策略选择的路径计算SimRank值（借助networkx包来简化编码）
        :return: 
        """
        walks = self.random_walk()
        simrank = []    #使用游走出来的路径，再计算random_walk

        return simrank

    def read_groups(self):
        groups = {}
        with open(self.args.groups) as f:
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

        return groups



    def get_batch(label, label_num):
        # the type of label is list.
        ans = []
        #    maxc = -1
        for i in range(len(label)):
            t = []
            #        if label[i] > maxc:
            #            maxc = label[i]
            for j in range(label_num):
                if label[i] == j:
                    t.append(1)
                else:
                    t.append(0)
            ans.append(t)
        # print("maxc: %d" % maxc)
        return ans

    def get_data(self, walks, simrank):
        """
        把原来的Y[0,0,...0,1,0,0] -> [simrank value]
        :param walks: 
        :param simrank: 
        :return: 
        """
        Xtrain = []
        ytrain = []

        return Xtrain, ytrain

    def deepSim(self):
        """
        使用神经网络进行训练，同时使用SimRank进行目标低维表达的指导(放在最后一层，把原来的[0,0,...0,1,0,0] -> [simrank value])
        :return: 
        """
        walks = self.random_walk()
        # simrank = self.calculate_simrank_random();
        simrank = self.calculate_simrank_all()
        # 使用Walks和SimRank获得训练数据，其中将Y
        Xtrain, ytrain = self.get_data(walks, simrank)

        # 下面构建神经网络
        middle = 128
        learning_rates = [0.001]
        minibatchs = [128]

        Xtrain = np.array(Xtrain)
        ytrain = np.array(ytrain)

        # Check the sizes of these numpy arrays
        print("Xtrain.shape : %s" % Xtrain.shape)

        embeddings = []
        for learning_rate in learning_rates:
            # initialize paramters
            length = Xtrain.shape[1]
            w1 = tf.Variable(tf.truncated_normal([length, middle], stddev=0.1))
            b1 = tf.Variable(tf.zeros(middle))

            w2 = tf.Variable(tf.truncated_normal([middle, length], stddev=0.1))
            b2 = tf.Variable(tf.zeros(middle))

            x = tf.placeholder(tf.float32, [None, length])

            hidden1 = tf.nn.relu(tf.matmul(x, w1) + b1)

            y = tf.matmul(hidden1, w2) + b2
            embedding = tf.matmul(x, w1) + b1

            # 真实值
            y_ = tf.placeholder(tf.float32, [None, length])
            cross_entropy = tf.reduce_mean(tf.nn.softmax_cross_entropy_with_logits(logits=y, labels=y_))
            reg = tf.nn.l2_loss(w1)
            loss = cross_entropy + 1e-1*reg

            train_step = tf.train.AdamOptimizer(learning_rate).minimize(loss)  # 学习率
            init_op = tf.global_variables_initializer()
            ##############################
            # LEARNING !

            # minibatchs
            for minibatch in minibatchs:
                print(learning_rate, minibatch)

                sess = tf.Session()
                sess.run(init_op)
                # iterate
                for i in range((int)(200000)):
                    ss = random.sample([k for k in range(Xtrain.shape[0])], minibatch)
                    batch_xs = [Xtrain[k] for k in ss]
                    batch_ys = [ytrain[k] for k in ss]

                    sess.run(train_step, feed_dict={x: batch_xs, y_: batch_ys})

                    if i % 1000 == 0:  # 验证集测试
                        # print(sess.run(cross_entropy,feed_dict={x: XCV, y_: yCV}))
                        print("step %d, cross_entropy: %g, L2_norm: %g " % (i, sess.run(cross_entropy, feed_dict={x: Xtrain, y_: ytrain}), sess.run(reg, feed_dict={x: Xtrain, y_: ytrain})))
                        print("step %d, loss: %g" % (i, sess.run(loss, feed_dict={x: Xtrain, y_: ytrain})))
                embeddings.append(sess.run(embedding, feed_dict={x: Xtrain}))
                sess.close()

        # 这里保存成文件，不然每次都要重新训练，消耗太大
        return embeddings

def test(embedding, labels):
    """
    使用降维后的特征，在不同比例上进行有监督学习，模型使用Logistic Regression
    :param embedding: 各个顶点特征
    :param labels: 各个顶点labels
    :return: 
    """
    num = len(embedding)

    accuracy = 0.8

    return accuracy

def main(args, groups):
    """
    Design a new neutral network to deal with the classification task on graphs.
    :param args: 
    :param simrank: 
    :param groups: 
    :return: 
    """
    print("DeepSim begin.")
    deepSim = DeepSim(args, groups)
    # 获取embedding
    embedding, groups = deepSim.deepSim()
    # 使用降维后的特征，在不同比例上进行有监督学习
    test(embedding, groups)






