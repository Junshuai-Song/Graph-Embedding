"""

Author: Alan

"""

import argparse
import networkx as nx
import node2vec
import DeepSim
from gensim.models import Word2Vec
import numpy as np



def parse_args():
    parser = argparse.ArgumentParser(description="Run node2vec.")

    parser.add_argument('--base_path', nargs='?', default='../../data/BlogCatalog-dataset/data/',
                        help='Input graph path')

    parser.add_argument('--input', nargs='?', default='../../data/BlogCatalog-dataset/data/edges.txt',
                        help='Input graph path')

    parser.add_argument('--simrank_path', nargs='?', default='../../data/BlogCatalog-dataset/data/blog_simrank.txt.sim.txt',
                        help='Input graph path')

    parser.add_argument('--groups', nargs='?', default='../../data/BlogCatalog-dataset/data/group-edges.txt',
                        help='Input graph path')

    parser.add_argument('--output', nargs='?', default='../emb/blog.emb',
                        help='Embeddings path')

    parser.add_argument('--TOPK', default=20, type=int,
                        help='Top K')

    parser.add_argument('--dimensions', type=int, default=128,
                        help='Number of dimensions. Default is 128.')

    parser.add_argument('--walk-length', type=int, default=5,
                        help='Length of walk per source. Default is 80.')

    parser.add_argument('--num-walks', type=int, default=1,
                        help='Number of walks per source. Default is 10.')

    parser.add_argument('--window-size', type=int, default=3,
                        help='Context size for optimization. Default is 10.')

    parser.add_argument('--iter', default=1, type=int,
                        help='Number of epochs in SGD')

    parser.add_argument('--workers', type=int, default=8,
                        help='Number of parallel workers. Default is 8.')

    parser.add_argument('--p', type=float, default=1,
                        help='Return hyperparameter. Default is 1.')

    parser.add_argument('--q', type=float, default=2,   # similar with BFS style strategy.
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
    # parser.set_defaults(directed=False)
    parser.set_defaults(directed=True)

    return parser.parse_args()


def read_simrank(args):
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
                if float(ts[1]) <= 0.000001:
                    continue
                else:
                    sim.append(ts[0])
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


if __name__ == "__main__":
    args = parse_args()
    average_acc, simrank, groups = preprocess_simrank(args)
    # average_acc = 0.8
    average_acc = preprocess_edges(args)
    simrank = 0
    groups = 0
    if(average_acc >= 0.8):
        print("精度达到要求.")
        # DeepSim.main(args, groups)




