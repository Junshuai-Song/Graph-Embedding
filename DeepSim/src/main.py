"""

Author: Alan

"""

import argparse
import networkx as nx
import DeepSim
import numpy as np
import classify
import node2vec
from gensim.models import Word2Vec
import gensim
import time


def parse_args():
    parser = argparse.ArgumentParser(description="Run DeepSim.")

    # parser.add_argument('--base_path', nargs='?', default='../../data/BlogCatalog-dataset/data/',
    #                     help='Input graph path')

    parser.add_argument('--input', nargs='?', default='../../data/BlogCatalog-dataset/data/edges.txt',
                        help='Input graph path')

    parser.add_argument('--simrank_path', nargs='?', default='../output_u_u/SimRank/blog_simrank_navie_top20.txt.sim.txt',
                        help='Input graph path')

    # parser.add_argument('--groups', nargs='?', default='../../data/BlogCatalog-dataset/data/group-edges.csv',
    #                     help='Input graph path')

    parser.add_argument('--emb_output', nargs='?', default='../emb/blog.emb',
                        help='Embeddings path')

    parser.add_argument('--TOPK', default=1000, type=int,
                        help='Top K')

    parser.add_argument('--vertex-num', type=int, default=10313,
                        help='Number of vertex.')

    parser.add_argument('--dimensions', type=int, default=128,
                        help='Number of dimensions. Default is 128.')

    parser.add_argument('--walk-length', type=int, default=80,
                        help='Length of walk per source. Default is 80.')

    parser.add_argument('--num-walks', type=int, default=10,
                        help='Number of walks per source. Default is 10.')

    parser.add_argument('--window-size', type=int, default=10,
                        help='Context size for optimization. Default is 10.')

    parser.add_argument('--iter', default=10, type=int,
                        help='Number of epochs in SGD')

    parser.add_argument('--workers', type=int, default=8,
                        help='Number of parallel workers. Default is 8.')

    parser.add_argument('--p', type=float, default=0.25,
                        help='Return hyperparameter. Default is 1.')

    parser.add_argument('--q', type=float, default=0.25,   # similar with BFS style strategy.
                        help='Inout hyperparameter. Default is 1.')

    parser.add_argument('--delimiter', type=str, default=',',
                        help='the delimiter of a graph. Default is ",".')

    parser.add_argument('--weighted', dest='weighted', action='store_true',
                        help='Boolean specifying (un)weighted. Default is unweighted.')
    parser.add_argument('--unweighted', dest='unweighted', action='store_false')
    parser.set_defaults(weighted=False)

    parser.add_argument('--directed', dest='directed', action='store_true',
                        help='Graph is (un)directed. Default is undirected.')
    parser.add_argument('--undirected', dest='undirected', action='store_false')
    parser.set_defaults(directed=False)
    # parser.set_defaults(directed=True)

    return parser.parse_args()


def read_simrank(args):
    """
    输入的simrank值已经是排好序的
    :param args: 
    :return: 返回全部顶点的simrank值，如果当前点没有很相似的顶点，那么当前顶点列表为空
    """
    simrank = []
    with open(args.simrank_path) as f:
        lines = f.readlines()
        # lines = lines[1:]
        for line in lines:
            words = line.split(",")
            # print(words[1:])
            sim = []
            for i in range(1,len(words)):
                if i == len(words)-1:
                    words[i] = words[i][:-1]
                ts = words[i].split(":")
                # print(i,words[i],ts)
                if float(ts[1]) <= 0.00000001:
                    continue
                else:
                    sim.append((ts[0], ts[1]))
            simrank.append(sim)
    return simrank

def read_groups(args):
    groups = {}
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
    """   
    for group in groups.values():
        s = set()
        for t in group:
            s.add(t)
        if len(s)>1 :
            print(group)
    """
    return groups

def preprocess_simrank(args):
    """
    计算simrank值与顶点label共性。（如果最后精度足够高，80%以上，基本就可以做！）
    :param args: 
    :return: 平均精度
    """
    simrank = np.array(read_simrank(args))
    groups = read_groups(args)
    print("simrank.shape: ", len(simrank), len(simrank[1]))
    print("groups.shape: ", len(groups))
    print(groups)
    # print(groups["0"],groups["1"])

    tot = 0.0
    for topk in range(1,args.TOPK+1):
        # 计算前topk的顶点之间的label是否相同
        cnt = 0.0
        cnt_tot = 0.0
        for i in range(len(simrank)):
            for j in range(min(len(simrank[i]), topk)):
                first = i
                second = simrank[i][j]
                first = set(groups[str(first)])
                second = set(groups[str(second)])
                k = 0
                if len(first & second)>0:
                    cnt += 1.0
                    k += 1
                # print(k,first & second)

            cnt_tot += min(len(simrank[i]), topk)
        cnt = cnt/cnt_tot
        print("Top: %d, acc: " % topk, cnt)
        tot += cnt
    print("")
    return tot/args.TOPK, simrank, groups

def preprocess_edges(args):
    """
    看直接相关联的边，两端label相同的概率
    :return: 
    """

    groups = read_groups(args)
    print("groups.shape: ", len(groups))

    cnt = 0.0
    tot = 0.0
    with open(args.input) as f:
        lines = f.readlines()
        for line in lines:
            words = line.split(",")
            from_node = str(words[0])
            to_node = str(words[1][:-1])
            if len(set(groups[from_node]) & set(groups[to_node])) > 0 :
                cnt += 1.0
            tot += 1.0
        print("Edges, acc: ", cnt/tot, "\n")

    return cnt/tot


def read_graph():
    """
	Reads the input network in networkx.
	"""
    if args.weighted:
        G = nx.read_edgelist(args.input, nodetype=int, data=(('weight', float),), create_using=nx.DiGraph(), delimiter=args.delimiter)
    else:
        G = nx.read_edgelist(args.input, nodetype=int, create_using=nx.DiGraph(), delimiter=args.delimiter)
        for edge in G.edges():
            G[edge[0]][edge[1]]['weight'] = 1
    if not args.directed:
        G = G.to_undirected()

    return G


def learn_embeddings(walks):
    """
	Learn embeddings by optimizing the Skip-gram objective using SGD.
	"""
    walks = [list(map(str, walk)) for walk in walks]
    model = Word2Vec(walks, size=args.dimensions, window=args.window_size, min_count=0, sg=1, workers=args.workers, iter=args.iter)
    model.wv.save_word2vec_format(args.output)
    print("Save.")

    return model


def get_walks(args):
    """
	Pipeline for representational learning for all nodes in a graph.
	"""
    print("read graoh...")
    nx_G = read_graph()  # 利用networkx包读取Graph信息
    G = node2vec.Graph(nx_G, args.directed, args.p, args.q)  # 使用node2vec中的公式进行一下处理
    print("preprocess transition probs...")
    G.preprocess_transition_probs()  # 计算新的概率
    # 这里计算完毕，是返回一系列walks的路径，这些路径中允许出现重复点，例如：0->1->5->4->7->1->4 等
    print("begin walks...")
    walks = G.simulate_walks(args.num_walks, args.walk_length)
    # model = learn_embeddings(walks)
    return walks

def save_list(walks,file_path):
    with open(file_path, "w") as f:
        for walk in walks:
            for t in walk:
                f.write(str(t))
                f.write("\t")
            f.write("\n")

def read_list(file_path):
    walks = []
    with open(file_path, "r") as f:
        lines = f.readlines()
        for line in lines:
            line = line.strip()
            words = line.split("\t")
            line = [w for w in words]
            walks.append(line)
    return walks


def print_time(start):
    end = time.clock()
    print('Running time: %s Seconds' % (end - start))


if __name__ == "__main__":
    start = time.clock()
    print_time(start)

    args = parse_args()
    # average_acc, simrank, groups = preprocess_simrank(args)
    # average_acc = preprocess_edges(args)

    print("read simrank...")
    simrank = read_simrank(args)
    # print_time(start)
    # print(simrank[1])
    # walks = get_walks(args)
    # print_time(start)
    # save_list(walks,"./walks.txt")
    print("read walks...")
    walks = read_list("./walks.txt")    # 其中保存的是字符串
    # print(walks)
    print("deal with input/output...")
    print_time(start)
    DeepSim.main(args, simrank, walks)  # 获得通过DeepSim处理后的embedding（传入游走的walks，以及对应simrank值矩阵），作用类似node2vec
    print_time(start)

    # 读取embeddings，
    print_time(start)
    embeddings = gensim.models.KeyedVectors.load_word2vec_format(args.emb_output)
    print_time(start)
    classify.scoring(args, embeddings)      # 按照特定路径读取embedding以及groups分组，进行监督分类，测试embedding在任务上的效果。
    print_time(start)



