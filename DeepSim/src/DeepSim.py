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

    def __init__(self, args,simrank, walks, tem_simrank):
        self.args = args
        self.read_graph()
        self.simrank = simrank
        self.walks = walks
        self.tem_simrank = tem_simrank


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

        # Xtrain, ytrain = self.X, self.Y

        # 下面构建神经网络
        middle = self.args.dimensions   # 128维
        learning_rates = [0.001]
        minibatchs = [128]

        # Xtrain = np.array(Xtrain)
        # ytrain = np.array(ytrain)

        # Check the sizes of these numpy arrays
        # print("Xtrain.shape: ", Xtrain.shape)

        embeddings = []
        for learning_rate in learning_rates:
            # initialize paramters
            length = self.args.vertex_num
            w1 = tf.Variable(tf.truncated_normal([length, middle], stddev=0.1))
            b1 = tf.Variable(tf.zeros(middle))

            w2 = tf.Variable(tf.truncated_normal([middle, length], stddev=0.1))
            b2 = tf.Variable(tf.zeros(length))

            x = tf.placeholder(tf.float32, [None, length])

            hidden1 = tf.nn.relu(tf.matmul(x, w1) + b1)

            y = tf.matmul(hidden1, w2) + b2
            embedding = tf.matmul(x, w1) + b1

            # 真实值
            y_ = tf.placeholder(tf.float32, [None, length])
            cross_entropy = tf.reduce_mean(tf.nn.softmax_cross_entropy_with_logits(logits=y, labels=y_))
            reg = tf.nn.l2_loss(w1) + tf.nn.l2_loss(w2)
            loss = cross_entropy + 1e-4*reg

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
                Xtrain, ytrain = get_batch(self.args, self.simrank, self.walks, minibatch * 100, self.tem_simrank)  # 找一个大点的数据集测试效果

                for i in range((int)(20000)):
                    print("iter:", i)
                    batch_xs, batch_ys = get_batch(self.args, self.simrank, self.walks, minibatch, self.tem_simrank)

                    # ss = random.sample([k for k in range(Xtrain.shape[0])], minibatch)
                    # batch_xs = [Xtrain[k] for k in ss]
                    # batch_ys = [ytrain[k] for k in ss]

                    sess.run(train_step, feed_dict={x: batch_xs, y_: batch_ys})

                    if i % 100 == 0:  # 验证集测试
                        # print(sess.run(cross_entropy,feed_dict={x: XCV, y_: yCV}))
                        print("step %d, train cross_entropy: %g, train L2_norm: %g " % (i, sess.run(cross_entropy, feed_dict={x: Xtrain, y_: ytrain}), sess.run(reg, feed_dict={x: Xtrain, y_: ytrain})))
                        print("step %d, train loss: %g" % (i, sess.run(loss, feed_dict={x: Xtrain, y_: ytrain})))
                    if i % 1000==0:
                        # 没到1000轮，保存一次embedding！
                        save_embeddings(self.args.emb_output+str(i), embeddings)

                # 保存权重作为embeddings
                embeddings = sess.run([w1])
                # np.savetxt("W.txt", W_val, delimiter=",", fmt='%f')
                sess.close()

        # 这里保存成文件，不然每次都要重新训练，消耗太大
        # with open("") as f:
        return embeddings




def get_input_output(args, simrank, walks):
    """
    从simrank和walks获取全部的输入、输出样本数据
    :return: 
    """
    # walks = walks[0:10]    # 测试使用小数据

    inputs = []
    outputs = []
    k = args.window_size
    num = 0
    for walk in walks:
        # print(walk)
        num += 1
        if num%1==0:
            print("deal with walk %d" % num)
        for i in range(k, len(walk)-k):
            # x
            x = []
            for j in range(args.vertex_num):
                if j==int(walk[i]):             # 转数字
                    x.append(1.0)
                    # print("i: ",i,"  j:",j)
                else:
                    x.append(0.0)
            inputs.append(x)

            # print("simrank[int(walk[i])]: ", simrank[int(walk[i])])
            # y
            output = walk[i-k:i+k+1]
            # print("output: ",output)
            output_ = []
            # 这里时间复杂度略高，证实想法后再优化
            for j in output:
                flag = 0
                for sim in simrank[int(walk[i])]:
                    # print("sim[0] and sim[1]:  ",sim[0],sim[1])
                    if int(j) == int(sim[0]):
                        flag=1
                        output_.append(float(sim[1]))
                        break
                if flag==0:
                    if (len(simrank[int(walk[i])]) > 0):
                        output_.append(simrank[int(walk[i])][-1][1])  # 不写0，改为sim值中最小的一个
                    else:
                        output_.append(0.0)
                # output_.append(simrank[walk[i]][output[j]])
            # print("output_: ",output_)

            # 使用output & output_ 构造一个|V|维的向量，使得output位置上的值为output_中保存的simrank值
            tot = 0.0
            y = []
            for j in range(args.vertex_num):
                if str(j) in output:
                    t = 0
                    for m in range(len(output)):
                        if int(j)==int(output[m]):
                            t=m
                            break
                    y.append(float(output_[t]))    # 注：simrank值较小
                    tot += float(output_[t])
                else:
                    y.append(0.0)
            # print("y tot :", tot)
            outputs.append(y)

    return inputs,outputs

def get_batch(args, simrank, walks, minibatch, tem_simrank):
    """
    :return: 
    """

    batch_xs = []
    batch_ys = []
    locations = random.sample([i for i in range(len(walks))], minibatch)
    # print("locations", locations)
    k = args.window_size
    num = 0
    for i in locations:
        num +=1
        # if num%1==0:
        #     print("minibatch: ", num)

        walk = walks[i]
        location = random.sample([j for j in range(k, len(walk)-k)], 1) #在当前walk上找一个
        location = location[0]

        x = []
        for j in range(args.vertex_num):
            if j == int(walk[location]):  # 转数字
                x.append(1.0)
                # print("i: ",i,"  j:",j)
            else:
                x.append(0.0)
        batch_xs.append(x)

        # y
        output = walk[location - k:location + k + 1]
        # print("output: ",output)
        output_ = [] * len(output)


        for j in output:
            flag = 0
            sim = simrank[int(walk[location])]
            start, end = 0, len(sim)-1
            while(start<=end):
                mid = int((start+end)/2.0)
                if int(j)==int(sim[mid][0]):
                    flag=1
                    output_.append(float(sim[mid][1]))
                    break
                if int(j) > int(sim[mid][0]):
                    start = mid+1
                else:
                    end = mid-1

            if flag == 0:
                output_.append(tem_simrank[location])  # 不写0，改为sim值中最小的一个
                # if (len(simrank[int(walk[location])]) > 0):
                # else:
                #     output_.append(0.0)
        # print("output_: ",output_)

        # 使用output & output_ 构造一个|V|维的向量，使得output位置上的值为output_中保存的simrank值
        tot = 0.0
        y = []
        for j in range(args.vertex_num):
            if str(j) in output:
                t = 0
                for m in range(len(output)):
                    if int(j) == int(output[m]):
                        t = m
                        break
                y.append(float(output_[t]))  # 注：simrank值较小
                tot += float(output_[t])
            else:
                y.append(0.0)
        # print("y tot :", tot)
        batch_ys.append(y)

    return batch_xs,batch_ys

def save_embeddings(file_path, embeddings):
    """
    在文件file_path中保存embeddings（避免测试中重复训练）
    :param file_path: 
    :param embeddings: 
    :return: 
    """
    with open(file_path, "w") as f:
        f.write(str(len(embeddings)))
        f.write(" ")
        f.write("128\n")
        for i in range(len(embeddings)):
            embedding = embeddings[i]
            f.write(str(i))
            for t in embedding:
                f.write(" ")
                f.write(str(t))
            f.write("\n")
    print("finish saving embeddings.")


def save_data(walks,file_path):
    with open(file_path, "w") as f:
        for walk in walks:
            for t in walk:
                f.write(str(t))
                f.write("\t")
            f.write("\n")

def read_data(file_path):
    walks = []
    with open(file_path, "r") as f:
        lines = f.readlines()
        for line in lines:
            line = line.strip()
            words = line.split("\t")
            line = [w for w in words]
            walks.append(line)
    return walks



def main(args, simrank, walks):
    """
    Design a new neutral network to deal with the classification task on graphs.
    参数包含simrank值以及对应walks，需要按照simrank值将对应walks处理为全部的input和output数据，之后保存为文件，喂给神经网络
    :param args: 
    :param simrank: 
    :param groups: 
    :return: 
    """
    print("DeepSim get inputs and outputs...")
    # X, Y = get_input_output(args, simrank, walks)
    # # save_data(X, "X.txt")
    # # save_data(Y, "Y.txt")
    # # print("save data inputs and outputs.")
    #
    # print("read X and Y...")
    # X = read_data("X.txt")
    # Y = read_data("Y.txt")

    print("DeepSim init.")
    # 获得每组sim值中最小的一个
    tem_simrank = []
    for tem in simrank:
        if len(tem) > 0:
            tem_simrank.append(tem[-1][1])
        else:
            tem_simrank.append(0.0)
    for i in range(len(simrank)):
        simrank[i] = sorted(simrank[i], key=lambda x: (int(x[0]), x[1]) )
    # print("simrank",simrank[1])   #查看其中一个排序的simrnak值
    deepSim = DeepSim(args, simrank, walks, tem_simrank)
    print("DeepSim train begin.")
    embeddings = deepSim.deepSim()   # embedding 需要被保存在args中声明的文件中
    embeddings = embeddings[0]
    print("type(embeddings): ", type(embeddings))
    print("length of embeddings: ", len(embeddings), len(embeddings[0]))
    save_embeddings(args.emb_output, embeddings)







